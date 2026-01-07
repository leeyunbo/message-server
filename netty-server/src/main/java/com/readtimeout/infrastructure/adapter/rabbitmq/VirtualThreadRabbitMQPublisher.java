package com.readtimeout.infrastructure.adapter.rabbitmq;

import com.readtimeout.core.domain.exception.MessagePublishException;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.outbound.AsyncMessagePublisher;
import com.readtimeout.infrastructure.config.RabbitMQProperties;
import com.readtimeout.infrastructure.support.MessagePublisherMetrics;
import com.readtimeout.infrastructure.support.MessageSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.mode", havingValue = "virtual")
public class VirtualThreadRabbitMQPublisher implements AsyncMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;
    private final MessageSerializer serializer;
    private final MessagePublisherMetrics metrics;

    private final ExecutorService virtualExecutor;
    private final Semaphore concurrencyLimiter;
    private final AtomicInteger pendingTasks;
    private final int maxConcurrency;

    public VirtualThreadRabbitMQPublisher(
            RabbitTemplate rabbitTemplate,
            RabbitMQProperties properties,
            MessageSerializer serializer,
            MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.serializer = serializer;
        this.metrics = MessagePublisherMetrics.forVirtualThread(meterRegistry);
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.maxConcurrency = properties.threadPool().queueCapacity();
        this.concurrencyLimiter = new Semaphore(maxConcurrency);
        this.pendingTasks = new AtomicInteger(0);

        meterRegistry.gauge("virtual_thread_pending_tasks", pendingTasks);
        meterRegistry.gauge("virtual_thread_available_permits", concurrencyLimiter, Semaphore::availablePermits);

        log.info("VirtualThreadRabbitMQPublisher initialized: maxConcurrency={}", maxConcurrency);
    }

    @Override
    public CompletableFuture<Void> publish(SendMessage sendMessage) {
        if (!concurrencyLimiter.tryAcquire()) {
            metrics.getPublishFailureCounter().increment();
            return CompletableFuture.failedFuture(
                    new MessagePublishException(
                            String.format("Concurrency limit reached (%d). Service unavailable.",
                                    maxConcurrency)));
        }

        pendingTasks.incrementAndGet();

        return CompletableFuture.runAsync(() -> {
            try {
                publishToRabbitMQ(sendMessage);
            } finally {
                pendingTasks.decrementAndGet();
                concurrencyLimiter.release();
            }
        }, virtualExecutor);
    }

    private static final long CONFIRM_TIMEOUT_MS = 5000;

    private void publishToRabbitMQ(SendMessage sendMessage) {
        long startTime = System.nanoTime();

        try {
            org.springframework.amqp.core.Message amqpMessage = serializer.serialize(sendMessage);

            Boolean confirmed = rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(
                        properties.exchange(),
                        properties.routingKey(),
                        amqpMessage
                );
                return operations.waitForConfirms(CONFIRM_TIMEOUT_MS);
            });

            if (confirmed == null || !confirmed) {
                throw new MessagePublishException("Message NACK'd or confirm timeout [id=" + sendMessage.getId() + "]");
            }

            metrics.recordPublishSuccess(sendMessage.getId(), startTime);
            log.debug("Published message [id={}] confirmed by broker (virtual thread)", sendMessage.getId());
        } catch (MessagePublishException e) {
            metrics.recordPublishFailure(sendMessage.getId(), startTime, e);
            throw e;
        } catch (Exception e) {
            metrics.recordPublishFailure(sendMessage.getId(), startTime, e);
            throw new MessagePublishException("Failed to publish message to RabbitMQ", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down VirtualThreadRabbitMQPublisher...");
        virtualExecutor.shutdown();
        try {
            if (!virtualExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                virtualExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

package com.readtimeout.infrastructure.adapter.rabbitmq;

import com.readtimeout.core.domain.exception.MessagePublishException;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.outbound.AsyncMessagePublisher;
import com.readtimeout.infrastructure.config.RabbitMQProperties;
import com.readtimeout.infrastructure.support.MessagePublisherMetrics;
import com.readtimeout.infrastructure.support.MessageSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.mode", havingValue = "non-blocking", matchIfMissing = true)
public class NonBlockingRabbitMQPublisher implements AsyncMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;
    private final MessageSerializer serializer;
    private final MessagePublisherMetrics metrics;
    private final long confirmTimeout;

    public NonBlockingRabbitMQPublisher(
            RabbitTemplate rabbitTemplate,
            RabbitMQProperties properties,
            MessageSerializer serializer,
            MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.serializer = serializer;
        this.metrics = MessagePublisherMetrics.forNonBlocking(meterRegistry, null);
        this.confirmTimeout = properties.confirmTimeoutMs() > 0 ? properties.confirmTimeoutMs() : 5000L;

        log.info("NonBlockingRabbitMQPublisher initialized (confirmTimeout={}ms)", confirmTimeout);
    }

    @Override
    public CompletableFuture<Void> publish(SendMessage sendMessage) {
        long startTime = System.nanoTime();
        String messageId = sendMessage.getId();

        try {
            Message amqpMessage = serializer.serialize(sendMessage);

            Boolean confirmed = rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(properties.exchange(), properties.routingKey(), amqpMessage);
                return operations.waitForConfirms(confirmTimeout);
            });

            if (confirmed == null || !confirmed) {
                throw new MessagePublishException("Message NACK'd or confirm timeout [id=" + messageId + "]");
            }

            metrics.recordPublishSuccess(messageId, startTime);
            log.debug("Message [id={}] published to RabbitMQ", messageId);
            return CompletableFuture.completedFuture(null);

        } catch (MessagePublishException e) {
            metrics.recordPublishFailure(messageId, startTime, e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            MessagePublishException ex = new MessagePublishException("Failed to publish: " + e.getMessage(), e);
            metrics.recordPublishFailure(messageId, startTime, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }
}

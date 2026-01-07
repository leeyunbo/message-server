package com.readtimeout.infrastructure.adapter.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.readtimeout.core.domain.exception.BackpressureRejectedException;
import com.readtimeout.core.domain.exception.MessagePublishException;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.outbound.ReactiveMessagePublisher;
import com.readtimeout.infrastructure.config.RabbitMQProperties;
import com.readtimeout.infrastructure.support.MessagePublisherMetrics;
import com.readtimeout.infrastructure.support.MessageSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.OutboundMessageResult;
import reactor.rabbitmq.Sender;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * V4: Reactive Sink 기반 Publisher (Publisher Confirms 적용)
 *
 * 특징:
 * - per-EventLoop Sink: 스레드별 전용 무한 스트림
 * - Channel 재활용: 스트림 기반으로 채널 효율적 사용
 * - Publisher Confirms: correlationId로 결과 매핑
 * - 200 = RabbitMQ ACK 확인됨
 * - 503 = Backpressure 또는 NACK/timeout
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "server.mode", havingValue = "reactive")
public class ReactiveRabbitMQPublisher implements ReactiveMessagePublisher {

    private static final int MAX_PENDING_REQUESTS = 12000;

    private final Sender sender;
    private final RabbitMQProperties properties;
    private final MessageSerializer serializer;
    private final MessagePublisherMetrics metrics;
    private final Duration confirmTimeout;

    private final ConcurrentHashMap<Thread, SinkContext> sinkContexts;
    private final ConcurrentHashMap<String, PendingRequest> pendingRequests;

    public ReactiveRabbitMQPublisher(
            Sender sender,
            RabbitMQProperties properties,
            MessageSerializer serializer,
            MeterRegistry meterRegistry) {
        this.sender = sender;
        this.properties = properties;
        this.serializer = serializer;
        this.metrics = MessagePublisherMetrics.forReactive(meterRegistry);
        this.confirmTimeout = Duration.ofMillis(
                properties.confirmTimeoutMs() > 0 ? properties.confirmTimeoutMs() : 5000L);
        this.sinkContexts = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();

        meterRegistry.gauge("reactive_sink_contexts", sinkContexts, ConcurrentHashMap::size);
        meterRegistry.gauge("reactive_pending_requests", pendingRequests, ConcurrentHashMap::size);

        log.info("ReactiveRabbitMQPublisher created with Sink + Publisher Confirms (timeout={}ms)",
                confirmTimeout.toMillis());
    }

    @Override
    public Mono<Void> publish(SendMessage sendMessage) {
        if (pendingRequests.size() >= MAX_PENDING_REQUESTS) {
            return Mono.error(new BackpressureRejectedException(
                    String.format("Too many pending requests (%d)", MAX_PENDING_REQUESTS)));
        }

        String messageId = sendMessage.getId();
        long startTime = System.nanoTime();

        return Mono.<Void>create(monoSink -> {
                    PendingRequest pending = new PendingRequest(monoSink, startTime);
                    pendingRequests.put(messageId, pending);

                    SinkContext ctx = getOrCreateSinkContext();
                    OutboundMessage outboundMessage = createOutboundMessage(sendMessage);

                    Sinks.EmitResult result = ctx.sink().tryEmitNext(outboundMessage);

                    if (result.isFailure()) {
                        pendingRequests.remove(messageId);
                        metrics.recordPublishFailure(messageId, startTime,
                                new RuntimeException("Failed to emit: " + result));
                        monoSink.error(new BackpressureRejectedException("Sink buffer full: " + result));
                    }
                })
                .timeout(confirmTimeout)
                .doOnError(TimeoutException.class, e -> {
                    PendingRequest pending = pendingRequests.remove(messageId);
                    if (pending != null) {
                        metrics.recordPublishFailure(messageId, startTime,
                                new MessagePublishException("Confirm timeout"));
                    }
                });
    }

    private SinkContext getOrCreateSinkContext() {
        Thread currentThread = Thread.currentThread();
        return sinkContexts.computeIfAbsent(currentThread, this::createSinkContext);
    }

    private SinkContext createSinkContext(Thread thread) {
        Sinks.Many<OutboundMessage> sink = Sinks.many().unicast().onBackpressureBuffer();

        Disposable subscription = sender.sendWithPublishConfirms(
                        sink.asFlux()
                                .doOnNext(msg -> log.info("Sending to RabbitMQ: {}", msg.getProperties().getCorrelationId()))
                                .publishOn(Schedulers.boundedElastic())
                )
                .doOnNext(result -> log.info("Received confirm: {} ack={}",
                        result.getOutboundMessage().getProperties().getCorrelationId(), result.isAck()))
                .subscribe(
                        this::handleConfirmResult,
                        error -> handleStreamError(thread, error),
                        () -> log.warn("Sink flux completed unexpectedly for thread: {}", thread.getName())
                );

        log.info("Created SinkContext for thread: {}", thread.getName());
        return new SinkContext(sink, subscription);
    }

    private void handleConfirmResult(OutboundMessageResult result) {
        String correlationId = result.getOutboundMessage().getProperties().getCorrelationId();
        PendingRequest pending = pendingRequests.remove(correlationId);
        if (pending == null) {
            log.debug("No pending request for correlationId: {}", correlationId);
            return;
        }

        if (result.isAck()) {
            metrics.recordPublishSuccess(correlationId, pending.startTime());
            log.debug("Message [id={}] confirmed by broker (ACK)", correlationId);
            pending.sink().success();
        } else {
            MessagePublishException ex = new MessagePublishException(
                    "Message NACK'd by broker [id=" + correlationId + "]");
            metrics.recordPublishFailure(correlationId, pending.startTime(), ex);
            log.warn("Message [id={}] rejected by broker (NACK)", correlationId);
            pending.sink().error(ex);
        }
    }

    private void handleStreamError(Thread thread, Throwable error) {
        log.error("Sink flux error for thread {} - removing context", thread.getName(), error);
        sinkContexts.remove(thread);
    }

    private OutboundMessage createOutboundMessage(SendMessage sendMessage) {
        Message amqpMessage = serializer.serialize(sendMessage);

        return new OutboundMessage(
                properties.exchange(),
                properties.routingKey(),
                new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .deliveryMode(2)
                        .correlationId(sendMessage.getId())
                        .build(),
                amqpMessage.getBody()
        );
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ReactiveRabbitMQPublisher ({} sink contexts, {} pending)...",
                sinkContexts.size(), pendingRequests.size());

        // 1. Sink complete → 새 메시지 수락 중단
        sinkContexts.forEach((thread, ctx) -> {
            ctx.sink().tryEmitComplete();
        });

        // 2. pending 요청 완료 대기 (최대 confirmTimeout)
        long waitStart = System.currentTimeMillis();
        long maxWaitMs = confirmTimeout.toMillis();
        while (!pendingRequests.isEmpty() && (System.currentTimeMillis() - waitStart) < maxWaitMs) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 3. timeout 후 남은 요청 실패 처리
        if (!pendingRequests.isEmpty()) {
            log.warn("Forcing shutdown with {} pending requests", pendingRequests.size());
            pendingRequests.keySet().forEach(id -> {
                PendingRequest pending = pendingRequests.remove(id);
                if (pending != null) {
                    pending.sink().error(new MessagePublishException("Shutdown timeout"));
                }
            });
        }

        // 4. Subscription 정리
        sinkContexts.forEach((thread, ctx) -> {
            if (!ctx.subscription().isDisposed()) {
                ctx.subscription().dispose();
            }
        });
        sinkContexts.clear();

        log.info("ReactiveRabbitMQPublisher shutdown complete");
    }

    private record SinkContext(
            Sinks.Many<OutboundMessage> sink,
            Disposable subscription
    ) {}

    private record PendingRequest(
            MonoSink<Void> sink,
            long startTime
    ) {}
}

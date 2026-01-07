package com.readtimeout.infrastructure.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class MessagePublisherMetrics {

    private static final long DEFAULT_HIGH_LATENCY_THRESHOLD_MS = 5L;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final Timer publishTimer;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final long highLatencyThresholdMs;

    private MessagePublisherMetrics(Builder builder) {
        this.publishTimer = builder.publishTimer;
        this.publishSuccessCounter = builder.publishSuccessCounter;
        this.publishFailureCounter = builder.publishFailureCounter;
        this.highLatencyThresholdMs = builder.highLatencyThresholdMs;
    }

    public static Builder builder(MeterRegistry meterRegistry) {
        return new Builder(meterRegistry);
    }

    public static MessagePublisherMetrics forNonBlocking(
            MeterRegistry meterRegistry,
            ExecutorBackpressureManager backpressureManager) {
        return forNonBlocking(meterRegistry, backpressureManager, DEFAULT_HIGH_LATENCY_THRESHOLD_MS);
    }

    public static MessagePublisherMetrics forNonBlocking(
            MeterRegistry meterRegistry,
            ExecutorBackpressureManager backpressureManager,
            long highLatencyThresholdMs) {
        return builder(meterRegistry)
                .mode("non-blocking")
                .version("v2")
                .highLatencyThresholdMs(highLatencyThresholdMs)
                .withBackpressure(backpressureManager)
                .build();
    }

    public static MessagePublisherMetrics forReactive(MeterRegistry meterRegistry) {
        return forReactive(meterRegistry, DEFAULT_HIGH_LATENCY_THRESHOLD_MS);
    }

    public static MessagePublisherMetrics forReactive(MeterRegistry meterRegistry, long highLatencyThresholdMs) {
        return builder(meterRegistry)
                .mode("reactive")
                .version("v3")
                .highLatencyThresholdMs(highLatencyThresholdMs)
                .build();
    }

    public static MessagePublisherMetrics forVirtualThread(MeterRegistry meterRegistry) {
        return forVirtualThread(meterRegistry, DEFAULT_HIGH_LATENCY_THRESHOLD_MS);
    }

    public static MessagePublisherMetrics forVirtualThread(MeterRegistry meterRegistry, long highLatencyThresholdMs) {
        return builder(meterRegistry)
                .mode("virtual")
                .version("v4")
                .highLatencyThresholdMs(highLatencyThresholdMs)
                .build();
    }

    public void recordPublishSuccess(String messageId, long startTime) {
        long latencyNanos = System.nanoTime() - startTime;
        publishTimer.record(latencyNanos, TimeUnit.NANOSECONDS);
        publishSuccessCounter.increment();

        long latencyMs = latencyNanos / NANOS_PER_MILLI;
        if (latencyMs > highLatencyThresholdMs) {
            log.warn("High publish latency [id={}]: {}ms (threshold: {}ms)", messageId, latencyMs, highLatencyThresholdMs);
        }
    }

    public void recordPublishFailure(String messageId, long startTime, Exception e) {
        long latencyNanos = System.nanoTime() - startTime;
        publishTimer.record(latencyNanos, TimeUnit.NANOSECONDS);
        publishFailureCounter.increment();

        log.error("Background publish failed [id={}]: {}", messageId, e.getMessage(), e);
    }

    public static class Builder {
        private final MeterRegistry meterRegistry;
        private String mode = "default";
        private String version = "v1";
        private long highLatencyThresholdMs = DEFAULT_HIGH_LATENCY_THRESHOLD_MS;
        private ExecutorBackpressureManager backpressureManager;

        private Timer publishTimer;
        private Counter publishSuccessCounter;
        private Counter publishFailureCounter;

        private Builder(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder highLatencyThresholdMs(long thresholdMs) {
            this.highLatencyThresholdMs = thresholdMs;
            return this;
        }

        public Builder withBackpressure(ExecutorBackpressureManager manager) {
            this.backpressureManager = manager;
            return this;
        }

        public MessagePublisherMetrics build() {
            String timerName = "non-blocking".equals(mode)
                    ? "rabbitmq.publish.latency"
                    : "rabbitmq.publish.duration";
            String tagKey = "non-blocking".equals(mode) ? "type" : "mode";

            this.publishTimer = Timer.builder(timerName)
                    .description("RabbitMQ publish latency (" + mode + ")")
                    .tag(tagKey, mode)
                    .tag("version", version)
                    .publishPercentileHistogram()
                    .register(meterRegistry);

            this.publishSuccessCounter = Counter.builder("rabbitmq.publish.success")
                    .description("Number of successful publishes to RabbitMQ (" + mode + ")")
                    .tag(tagKey, mode)
                    .register(meterRegistry);

            this.publishFailureCounter = Counter.builder("rabbitmq.publish.failure")
                    .description("Number of failed publishes to RabbitMQ (" + mode + ")")
                    .tag(tagKey, mode)
                    .register(meterRegistry);

            if (backpressureManager != null) {
                registerQueueGauges();
            }

            return new MessagePublisherMetrics(this);
        }

        private void registerQueueGauges() {
            Gauge.builder("rabbitmq.executor.queue.size", backpressureManager, ExecutorBackpressureManager::getCurrentQueueSize)
                    .description("Current executor queue depth")
                    .tag("type", mode)
                    .register(meterRegistry);

            Gauge.builder("rabbitmq.executor.queue.utilization", backpressureManager, ExecutorBackpressureManager::getUtilizationPercentageDouble)
                    .description("Executor queue utilization percentage (0-100)")
                    .tag("type", mode)
                    .register(meterRegistry);
        }
    }
}

package com.readtimeout.infrastructure.support;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class BlockingMetrics {

    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final long HIGH_LATENCY_THRESHOLD_MS = 5L;

    private final Timer publishTimer;

    private BlockingMetrics(Timer publishTimer) {
        this.publishTimer = publishTimer;
    }

    public static BlockingMetrics create(MeterRegistry meterRegistry) {
        Timer publishTimer = Timer.builder("rabbitmq.publish.latency")
                .description("RabbitMQ message publish latency (including Publisher Confirms)")
                .tag("version", "v1")
                .tag("type", "blocking")
                .publishPercentileHistogram()
                .minimumExpectedValue(java.time.Duration.ofMillis(1))
                .maximumExpectedValue(java.time.Duration.ofSeconds(10))
                .register(meterRegistry);

        return new BlockingMetrics(publishTimer);
    }

    public void recordPublishLatency(String messageId, long startTime) {
        long latencyNanos = System.nanoTime() - startTime;
        publishTimer.record(latencyNanos, TimeUnit.NANOSECONDS);

        long latencyMs = latencyNanos / NANOS_PER_MILLI;
        if (latencyMs > HIGH_LATENCY_THRESHOLD_MS) {
            log.warn("High publish latency detected [id={}]: {}ms", messageId, latencyMs);
        }
    }
}

package com.readtimeout.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq")
public record RabbitMQProperties(
        String host,
        int port,
        String username,
        String password,
        String virtualHost,
        String exchange,
        String routingKey,
        String queueName,
        boolean publisherConfirms,
        boolean publisherReturns,
        long confirmTimeoutMs,
        ThreadPoolConfig threadPool,
        BackpressureConfig backpressure,
        MetricsConfig metrics
) {
    public record ThreadPoolConfig(
            int coreSize,
            int maxSize,
            int queueCapacity,
            int keepAliveSeconds
    ) {
        public ThreadPoolConfig {
            if (keepAliveSeconds <= 0) {
                keepAliveSeconds = 60;
            }
        }
    }

    public record BackpressureConfig(
            boolean enabled,
            double thresholdPercentage
    ) {
        public BackpressureConfig {
            if (thresholdPercentage <= 0 || thresholdPercentage > 1.0) {
                thresholdPercentage = 0.8;
            }
        }
    }

    public record MetricsConfig(
            long highLatencyThresholdMs
    ) {
        public MetricsConfig {
            if (highLatencyThresholdMs <= 0) {
                highLatencyThresholdMs = 5L;
            }
        }
    }
}

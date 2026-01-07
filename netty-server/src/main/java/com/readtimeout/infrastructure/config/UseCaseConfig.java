package com.readtimeout.infrastructure.config;

import com.readtimeout.core.application.service.AsyncMessageSendService;
import com.readtimeout.core.application.service.BlockingMessageSendService;
import com.readtimeout.core.application.service.ReactiveMessageSendService;
import com.readtimeout.core.domain.port.inbound.AsyncMessageSendUseCase;
import com.readtimeout.core.domain.port.inbound.MessageSendUseCase;
import com.readtimeout.core.domain.port.inbound.ReactiveMessageSendUseCase;
import com.readtimeout.core.domain.port.outbound.*;
import com.readtimeout.infrastructure.support.ConcurrencyLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class UseCaseConfig {

    private static final int MAX_CONCURRENT_REQUESTS = 15000;

    // ============ ConcurrencyLimiter Beans ============

    @Bean
    @ConditionalOnProperty(name = "server.mode", havingValue = "non-blocking", matchIfMissing = true)
    public ConcurrencyLimiter asyncConcurrencyLimiter(MeterRegistry meterRegistry) {
        return createLimiter("async_available_permits", meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "server.mode", havingValue = "virtual")
    public ConcurrencyLimiter virtualConcurrencyLimiter(MeterRegistry meterRegistry) {
        return createLimiter("virtual_available_permits", meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "server.mode", havingValue = "reactive")
    public ConcurrencyLimiter reactiveConcurrencyLimiter(MeterRegistry meterRegistry) {
        return createLimiter("reactive_available_permits", meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "server.mode", havingValue = "reactive-pool")
    public ConcurrencyLimiter reactivePoolConcurrencyLimiter(MeterRegistry meterRegistry) {
        return createLimiter("reactive_pool_available_permits", meterRegistry);
    }

    private ConcurrencyLimiter createLimiter(String metricName, MeterRegistry meterRegistry) {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(MAX_CONCURRENT_REQUESTS);
        meterRegistry.gauge(metricName, limiter, ConcurrencyLimiter::availablePermits);
        return limiter;
    }

    // ============ UseCase Beans ============

    @Bean
    @ConditionalOnProperty(name = "server.mode", havingValue = "blocking")
    public MessageSendUseCase blockingMessageSendUseCase(
            ExternalApiPort externalApiPort,
            MessageLogPort messageLogPort,
            MessagePublisher messagePublisher) {
        return new BlockingMessageSendService(externalApiPort, messageLogPort, messagePublisher);
    }

    @Bean
    @ConditionalOnProperty(name = "server.mode", havingValue = "non-blocking", matchIfMissing = true)
    public AsyncMessageSendUseCase asyncMessageSendUseCase(
            ExternalApiPort externalApiPort,
            MessageLogPort messageLogPort,
            AsyncMessagePublisher messagePublisher,
            ThreadPoolExecutor rabbitExecutor) {
        return new AsyncMessageSendService(externalApiPort, messageLogPort, messagePublisher, rabbitExecutor);
    }

    @Bean
    @ConditionalOnProperty(name = "server.mode", havingValue = "virtual")
    public AsyncMessageSendUseCase virtualMessageSendUseCase(
            ExternalApiPort externalApiPort,
            MessageLogPort messageLogPort,
            AsyncMessagePublisher messagePublisher,
            Executor virtualThreadExecutor) {
        return new AsyncMessageSendService(externalApiPort, messageLogPort, messagePublisher, virtualThreadExecutor);
    }

    @Bean
    @ConditionalOnProperty(name = "server.mode", havingValue = "reactive")
    public ReactiveMessageSendUseCase reactiveMessageSendUseCase(
            ReactiveExternalApiPort externalApiPort,
            ReactiveMessageLogPort messageLogPort,
            ReactiveMessagePublisher messagePublisher) {
        return new ReactiveMessageSendService(externalApiPort, messageLogPort, messagePublisher);
    }

    @Bean
    @ConditionalOnProperty(name = "server.mode", havingValue = "reactive-pool")
    public ReactiveMessageSendUseCase reactivePoolMessageSendUseCase(
            ReactiveExternalApiPort externalApiPort,
            ReactiveMessageLogPort messageLogPort,
            ReactiveMessagePublisher messagePublisher) {
        return new ReactiveMessageSendService(externalApiPort, messageLogPort, messagePublisher);
    }
}

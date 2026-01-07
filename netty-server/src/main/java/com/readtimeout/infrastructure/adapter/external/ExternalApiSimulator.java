package com.readtimeout.infrastructure.adapter.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * 외부 API 호출 시뮬레이션
 * - Blocking: Thread.sleep
 * - Reactive: Mono.delay
 */
@Slf4j
@Component
public class ExternalApiSimulator {

    private final long delayMs;

    public ExternalApiSimulator(@Value("${external-api.delay-ms:50}") long delayMs) {
        this.delayMs = delayMs;
        log.info("ExternalApiSimulator initialized with delay={}ms", delayMs);
    }

    public Map<String, Object> callExternalApiBlocking(String messageId) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("External API call interrupted", e);
        }

        log.debug("External API called (blocking) for messageId={}", messageId);
        return Map.of(
                "messageId", messageId,
                "validated", true,
                "timestamp", System.currentTimeMillis()
        );
    }

    public Mono<Map<String, Object>> callExternalApiReactive(String messageId) {
        return Mono.delay(Duration.ofMillis(delayMs))
                .map(tick -> {
                    log.debug("External API called (reactive) for messageId={}", messageId);
                    return Map.<String, Object>of(
                            "messageId", messageId,
                            "validated", true,
                            "timestamp", System.currentTimeMillis()
                    );
                });
    }
}

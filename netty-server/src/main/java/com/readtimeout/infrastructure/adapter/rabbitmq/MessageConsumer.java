package com.readtimeout.infrastructure.adapter.rabbitmq;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 공통 메시지 컨슈머 (V1-V5 모두 사용)
 *
 * 역할:
 * - 큐에서 메시지 소비
 * - 로그 기록 후 즉시 ACK
 * - 메시지 쌓임 방지
 *
 * 설정:
 * - consumer.enabled=true (기본값)로 활성화
 * - prefetch-count로 한 번에 가져올 메시지 수 조절
 */
@Component
@ConditionalOnProperty(name = "consumer.enabled", havingValue = "true", matchIfMissing = true)
public class MessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);

    private final Counter consumedCounter;
    private final Counter errorCounter;

    public MessageConsumer(MeterRegistry meterRegistry) {
        this.consumedCounter = Counter.builder("rabbitmq_messages_consumed_total")
                .description("Total messages consumed")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("rabbitmq_consumer_errors_total")
                .description("Total consumer errors")
                .register(meterRegistry);

        log.info("MessageConsumer initialized - consuming from queue");
    }

    @RabbitListener(queues = "${rabbitmq.queue-name}", concurrency = "${consumer.concurrency:10}")
    public void consume(byte[] message) {
        try {
            consumedCounter.increment();

            if (log.isDebugEnabled()) {
                log.debug("Consumed message: {} bytes", message.length);
            }
            // ACK는 자동으로 처리됨 (acknowledge-mode: auto가 기본값)
        } catch (Exception e) {
            errorCounter.increment();
            log.error("Failed to consume message", e);
            // 예외를 던지지 않으면 메시지는 ACK 처리됨
        }
    }
}

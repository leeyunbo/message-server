package com.readtimeout.infrastructure.adapter.persistence;

import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.outbound.MessageLogPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("'${server.mode:non-blocking}'.equals('blocking') or '${server.mode:non-blocking}'.equals('non-blocking') or '${server.mode:non-blocking}'.equals('virtual')")
public class MessageLogAdapter implements MessageLogPort {

    private final MessageLogRepository repository;
    private final Timer dbTimer;

    public MessageLogAdapter(MessageLogRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.dbTimer = Timer.builder("db.save.duration")
                .description("Database save duration")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    @Override
    public void save(SendMessage message) {
        dbTimer.record(() -> {
            MessageLog log = MessageLog.create(message.getId(), message.getContent());
            repository.save(log);
        });
    }
}

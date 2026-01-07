package com.readtimeout.infrastructure.adapter.persistence;

import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.outbound.ReactiveMessageLogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${server.mode}'.equals('reactive') or '${server.mode}'.equals('reactive-pool')")
public class ReactiveMessageLogAdapter implements ReactiveMessageLogPort {

    private final ReactiveMessageLogRepository repository;

    @Override
    public Mono<Void> save(SendMessage message) {
        ReactiveMessageLog log = ReactiveMessageLog.create(message.getId(), message.getContent());
        return repository.save(log).then();
    }
}

package com.readtimeout.infrastructure.adapter.external;

import com.readtimeout.core.domain.port.outbound.ReactiveExternalApiPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${server.mode}'.equals('reactive') or '${server.mode}'.equals('reactive-pool')")
public class ReactiveExternalApiAdapter implements ReactiveExternalApiPort {

    private final ExternalApiSimulator simulator;

    @Override
    public Mono<Void> validate(String messageId) {
        return simulator.callExternalApiReactive(messageId).then();
    }
}

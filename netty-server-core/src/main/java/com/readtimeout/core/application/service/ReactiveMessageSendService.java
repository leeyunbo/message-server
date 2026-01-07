package com.readtimeout.core.application.service;

import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.inbound.ReactiveMessageSendUseCase;
import com.readtimeout.core.domain.port.outbound.ReactiveExternalApiPort;
import com.readtimeout.core.domain.port.outbound.ReactiveMessageLogPort;
import com.readtimeout.core.domain.port.outbound.ReactiveMessagePublisher;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ReactiveMessageSendService implements ReactiveMessageSendUseCase {

    private final ReactiveExternalApiPort externalApiPort;
    private final ReactiveMessageLogPort messageLogPort;
    private final ReactiveMessagePublisher messagePublisher;

    @Override
    public Mono<Void> send(SendMessage message) {
        return externalApiPort.validate(message.getId())
                .then(messageLogPort.save(message))
                .then(messagePublisher.publish(message));
    }
}

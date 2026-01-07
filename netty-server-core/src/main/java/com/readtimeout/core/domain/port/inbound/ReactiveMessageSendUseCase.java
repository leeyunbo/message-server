package com.readtimeout.core.domain.port.inbound;

import com.readtimeout.core.domain.model.SendMessage;
import reactor.core.publisher.Mono;

public interface ReactiveMessageSendUseCase {
    Mono<Void> send(SendMessage message);
}

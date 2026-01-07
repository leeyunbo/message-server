package com.readtimeout.core.domain.port.outbound;

import com.readtimeout.core.domain.model.SendMessage;
import reactor.core.publisher.Mono;

public interface ReactiveMessageLogPort {
    Mono<Void> save(SendMessage message);
}

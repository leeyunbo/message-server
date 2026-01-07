package com.readtimeout.core.domain.port.outbound;

import reactor.core.publisher.Mono;

public interface ReactiveExternalApiPort {
    Mono<Void> validate(String messageId);
}

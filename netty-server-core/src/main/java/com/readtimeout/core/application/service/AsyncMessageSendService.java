package com.readtimeout.core.application.service;

import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.inbound.AsyncMessageSendUseCase;
import com.readtimeout.core.domain.port.outbound.ExternalApiPort;
import com.readtimeout.core.domain.port.outbound.MessageLogPort;
import com.readtimeout.core.domain.port.outbound.AsyncMessagePublisher;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RequiredArgsConstructor
public class AsyncMessageSendService implements AsyncMessageSendUseCase {

    private final ExternalApiPort externalApiPort;
    private final MessageLogPort messageLogPort;
    private final AsyncMessagePublisher messagePublisher;
    private final Executor executor;

    @Override
    public CompletableFuture<Void> send(SendMessage message) {
        return CompletableFuture.runAsync(() -> {
            externalApiPort.validate(message.getId());
            messageLogPort.save(message);
            messagePublisher.publish(message).join();
        }, executor);
    }
}

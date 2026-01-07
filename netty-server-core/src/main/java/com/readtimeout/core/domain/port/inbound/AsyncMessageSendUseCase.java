package com.readtimeout.core.domain.port.inbound;

import com.readtimeout.core.domain.model.SendMessage;

import java.util.concurrent.CompletableFuture;

public interface AsyncMessageSendUseCase {
    CompletableFuture<Void> send(SendMessage message);
}

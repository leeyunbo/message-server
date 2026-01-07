package com.readtimeout.core.domain.port.inbound;

import com.readtimeout.core.domain.model.SendMessage;

public interface MessageSendUseCase {
    void send(SendMessage message);
}

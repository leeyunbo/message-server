package com.readtimeout.core.application.service;

import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.inbound.MessageSendUseCase;
import com.readtimeout.core.domain.port.outbound.ExternalApiPort;
import com.readtimeout.core.domain.port.outbound.MessageLogPort;
import com.readtimeout.core.domain.port.outbound.MessagePublisher;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlockingMessageSendService implements MessageSendUseCase {

    private final ExternalApiPort externalApiPort;
    private final MessageLogPort messageLogPort;
    private final MessagePublisher messagePublisher;

    @Override
    public void send(SendMessage message) {
        externalApiPort.validate(message.getId());
        messageLogPort.save(message);
        messagePublisher.publish(message);
    }
}

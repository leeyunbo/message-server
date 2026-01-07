package com.readtimeout.core.domain.port.outbound;

import com.readtimeout.core.domain.exception.MessagePublishException;
import com.readtimeout.core.domain.model.SendMessage;

public interface MessagePublisher {

    void publish(SendMessage sendMessage) throws MessagePublishException;
}

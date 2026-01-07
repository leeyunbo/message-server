package com.readtimeout.core.domain.port.outbound;

import com.readtimeout.core.domain.model.SendMessage;

public interface MessageLogPort {
    void save(SendMessage message);
}

package com.readtimeout.core.domain.port.outbound;

public interface ExternalApiPort {
    void validate(String messageId);
}

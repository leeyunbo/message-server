package com.readtimeout.core.domain.model;

import com.readtimeout.core.domain.exception.ValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * RequestMetadata Value Object
 * <p>
 * HTTP 요청에 대한 메타데이터를 표현하는 불변 값 객체.
 */
public record RequestMetadata(String requestId, String clientIp, Instant timestamp) {
    public RequestMetadata {
        if (requestId == null || requestId.isBlank()) {
            throw new ValidationException("Request ID cannot be null or blank");
        }
        if (clientIp == null || clientIp.isBlank()) {
            throw new ValidationException("Client IP cannot be null or blank");
        }
        if (timestamp == null) {
            throw new ValidationException("Timestamp cannot be null");
        }

    }
}

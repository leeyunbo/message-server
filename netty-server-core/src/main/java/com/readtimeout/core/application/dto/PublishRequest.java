package com.readtimeout.core.application.dto;

import com.readtimeout.core.domain.exception.ValidationException;
import com.readtimeout.core.domain.model.RequestMetadata;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * PublishRequest DTO
 *
 * 메시지 발행 요청을 표현하는 Data Transfer Object.
 * Presentation Layer에서 Application Layer로 데이터 전달 시 사용.
 */
@Getter
@EqualsAndHashCode
public class PublishRequest {
    private final String requestId;
    private final String content;
    private final RequestMetadata metadata;

    public PublishRequest(String requestId, String content, RequestMetadata metadata) {
        if (requestId == null || requestId.isBlank()) {
            throw new ValidationException("Request ID cannot be null or blank");
        }
        if (content == null || content.isEmpty()) {
            throw new ValidationException("Content cannot be null or empty");
        }
        if (metadata == null) {
            throw new ValidationException("Metadata cannot be null");
        }
        this.requestId = requestId;
        this.content = content;
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "PublishRequest{" +
                "requestId='" + requestId + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                ", metadata=" + metadata +
                '}';
    }
}

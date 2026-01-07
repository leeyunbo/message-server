package com.readtimeout.core.application.dto;

import com.readtimeout.core.domain.model.PublishStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * PublishResponse DTO
 *
 * 메시지 발행 응답을 표현하는 Data Transfer Object.
 * Application Layer에서 Presentation Layer로 데이터 전달 시 사용.
 */
@Getter
@EqualsAndHashCode
public class PublishResponse {
    private final PublishStatus status;
    private final String requestId;
    private final Long mqLatencyMs;
    private final String errorMessage;

    private PublishResponse(PublishStatus status, String requestId, Long mqLatencyMs, String errorMessage) {
        this.status = status;
        this.requestId = requestId;
        this.mqLatencyMs = mqLatencyMs;
        this.errorMessage = errorMessage;
    }

    /**
     * 성공 응답 생성
     */
    public static PublishResponse success(String requestId, long latencyNanos) {
        return new PublishResponse(PublishStatus.OK, requestId, latencyNanos / 1_000_000, null);
    }

    /**
     * 실패 응답 생성
     */
    public static PublishResponse failure(String requestId, String errorMessage) {
        return new PublishResponse(PublishStatus.ERROR, requestId, null, errorMessage);
    }

    /**
     * Queue 과부하 응답 생성
     */
    public static PublishResponse queueOverloaded() {
        return new PublishResponse(PublishStatus.ERROR, null, null, "Queue is overloaded");
    }

    public String getStatusValue() {
        return status.getValue();
    }

    public boolean isSuccess() {
        return PublishStatus.OK.equals(status);
    }

    @Override
    public String toString() {
        return "PublishResponse{" +
                "status='" + status.getValue() + '\'' +
                ", requestId='" + requestId + '\'' +
                ", mqLatencyMs=" + mqLatencyMs +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

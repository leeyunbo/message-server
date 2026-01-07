package com.readtimeout.core.domain.port.outbound;

/**
 * MetricsCollector Port (Outbound)
 *
 * 메트릭을 수집하는 Port Interface.
 * Infrastructure Layer가 이 인터페이스를 구현 (Prometheus, CloudWatch 등).
 *
 * Hexagonal Architecture의 Driven Port (출력 포트).
 */
public interface MetricsCollector {
    /**
     * MQ 발행 지연 시간 기록
     *
     * @param nanos 지연 시간 (나노초)
     */
    void recordPublishLatency(long nanos);

    /**
     * MQ 발행 성공 기록
     */
    void recordPublishSuccess();

    /**
     * MQ 발행 실패 기록
     */
    void recordPublishFailure();

    /**
     * HTTP 요청 카운트 증가
     */
    void incrementRequestCount();

    /**
     * HTTP 요청 처리 시간 기록
     *
     * @param nanos 처리 시간 (나노초)
     */
    void recordRequestProcessingTime(long nanos);
}

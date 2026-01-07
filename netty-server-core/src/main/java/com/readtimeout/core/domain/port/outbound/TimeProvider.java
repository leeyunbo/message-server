package com.readtimeout.core.domain.port.outbound;

import java.time.Instant;

/**
 * TimeProvider Port
 *
 * 시간 관련 기능을 추상화하는 출력 포트.
 * 테스트 시 시간 모킹을 가능하게 함.
 */
public interface TimeProvider {

    /**
     * 현재 시간을 나노초 단위로 반환.
     * 성능 측정에 사용.
     */
    long getCurrentTimeNanos();

    /**
     * 현재 시간을 Instant로 반환.
     */
    Instant now();
}

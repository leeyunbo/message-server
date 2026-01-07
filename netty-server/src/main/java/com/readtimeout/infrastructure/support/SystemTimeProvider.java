package com.readtimeout.infrastructure.support;

import com.readtimeout.core.domain.port.outbound.TimeProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * SystemTimeProvider
 *
 * TimeProvider의 기본 구현체.
 * 시스템 시간을 사용하여 시간 정보 제공.
 */
@Component
public class SystemTimeProvider implements TimeProvider {

    @Override
    public long getCurrentTimeNanos() {
        return System.nanoTime();
    }

    @Override
    public Instant now() {
        return Instant.now();
    }
}

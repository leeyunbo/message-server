package com.readtimeout.infrastructure.support;

import com.readtimeout.core.domain.exception.BackpressureRejectedException;
import lombok.Getter;

import java.util.concurrent.Semaphore;

public class ConcurrencyLimiter {

    private final Semaphore semaphore;
    @Getter
    private final int maxPermits;

    public ConcurrencyLimiter(int maxPermits) {
        this.maxPermits = maxPermits;
        this.semaphore = new Semaphore(maxPermits);
    }

    public void acquire() {
        if (!semaphore.tryAcquire()) {
            throw new BackpressureRejectedException("Concurrency limit reached: " + maxPermits);
        }
    }

    public void release() {
        semaphore.release();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}

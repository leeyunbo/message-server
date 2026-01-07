package com.readtimeout.infrastructure.support;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class ExecutorBackpressureManager {

    private final ThreadPoolExecutor executor;
    @Getter
    private final int queueCapacity;
    @Getter
    private final int overloadThreshold;
    @Getter
    private final double thresholdPercentage;

    public ExecutorBackpressureManager(ThreadPoolExecutor executor, int queueCapacity, double thresholdPercentage) {
        this.executor = executor;
        this.queueCapacity = queueCapacity;
        this.thresholdPercentage = thresholdPercentage;
        this.overloadThreshold = (int) (queueCapacity * thresholdPercentage);
    }

    public boolean isOverloaded() {
        int totalPending = getTotalPendingCount();
        boolean overloaded = totalPending >= overloadThreshold;

        if (overloaded) {
            log.warn("Executor overloaded: active={}, queued={}, total={}/{} ({}%)",
                    getActiveCount(), getCurrentQueueSize(), totalPending,
                    queueCapacity, getUtilizationPercentage());
        }

        return overloaded;
    }

    /**
     * 처리 중(active) + 대기 중(queued) = 전체 pending
     */
    public int getTotalPendingCount() {
        return getActiveCount() + getCurrentQueueSize();
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public int getCurrentQueueSize() {
        return executor.getQueue().size();
    }

    public int getUtilizationPercentage() {
        return (int) ((double) getCurrentQueueSize() / queueCapacity * 100);
    }

    public double getUtilizationPercentageDouble() {
        return (double) getCurrentQueueSize() / queueCapacity * 100.0;
    }
}

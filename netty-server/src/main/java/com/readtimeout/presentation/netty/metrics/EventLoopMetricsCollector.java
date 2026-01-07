package com.readtimeout.presentation.netty.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * EventLoopMetricsCollector - 모든 버전에서 사용
 */
@Component
public class EventLoopMetricsCollector {
    private static final Logger log = LoggerFactory.getLogger(EventLoopMetricsCollector.class);

    private final MeterRegistry registry;
    private final String version;
    private EventLoopGroup workerGroup;

    private final AtomicInteger totalThreads = new AtomicInteger(0);
    private final AtomicInteger pendingTasks = new AtomicInteger(0);

    // EventLoop Lag 측정 (핵심 메트릭!)
    private DistributionSummary eventLoopLagSummary;


    public EventLoopMetricsCollector(MeterRegistry registry,
                                     @Value("${server.mode:blocking}") String serverMode) {
        this.registry = registry;
        this.version = modeToVersion(serverMode);
    }

    private String modeToVersion(String mode) {
        return switch (mode) {
            case "blocking" -> "v1";
            case "non-blocking" -> "v2";
            case "reactive" -> "v3";
            case "virtual" -> "v4";
            default -> "v1";
        };
    }

    public void registerEventLoopGroup(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
        this.totalThreads.set(getEventLoopCount());

        Gauge.builder("netty.eventloop.threads", totalThreads, AtomicInteger::get)
                .description("Total number of EventLoop threads")
                .tag("version", version)
                .register(registry);

        Gauge.builder("netty.eventloop.pending.tasks", this, EventLoopMetricsCollector::getPendingTasksCount)
                .description("Number of pending tasks in EventLoop queue")
                .tag("version", version)
                .register(registry);

        // EventLoop Lag Distribution (스케줄링 지연 측정)
        this.eventLoopLagSummary = DistributionSummary.builder("netty.eventloop.lag.ms")
                .description("EventLoop scheduling lag in milliseconds")
                .tag("version", version)
                .publishPercentileHistogram()
                .minimumExpectedValue(0.1)
                .maximumExpectedValue(1000.0)
                .register(registry);

        log.info("EventLoop metrics registered: {} threads", totalThreads.get());

        startPeriodicUpdate();
        startEventLoopLagMeasurement();
    }

    private int getEventLoopCount() {
        if (workerGroup == null) {
            return 0;
        }

        int count = 0;
        for (EventExecutor executor : workerGroup) {
            count++;
        }
        return count;
    }

    private int getPendingTasksCount() {
        if (workerGroup == null) {
            return 0;
        }

        int total = 0;
        for (EventExecutor executor : workerGroup) {
            if (executor instanceof SingleThreadEventLoop eventLoop) {
                total += eventLoop.pendingTasks();
            }
        }
        return total;
    }

    private void startPeriodicUpdate() {
        if (workerGroup == null) {
            return;
        }

        EventExecutor executor = workerGroup.iterator().next();
        executor.scheduleAtFixedRate(() -> {
            try {
                int currentPending = getPendingTasksCount();
                pendingTasks.set(currentPending);

                if (currentPending > 100) {
                    log.warn("High EventLoop pending tasks detected: {}", currentPending);
                }
            } catch (Exception e) {
                log.error("Error updating EventLoop metrics", e);
            }
        }, 5, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * EventLoop Lag 측정 시작
     *
     * 작업을 스케줄한 시간과 실제 실행 시간의 차이를 측정
     * → EventLoop가 얼마나 밀리고 있는지 확인
     */
    private void startEventLoopLagMeasurement() {
        if (workerGroup == null) {
            return;
        }

        // 각 EventLoop마다 주기적으로 lag 측정
        for (EventExecutor executor : workerGroup) {
            executor.scheduleAtFixedRate(() -> {
                try {
                    measureEventLoopLag(executor);
                } catch (Exception e) {
                    log.error("Error measuring EventLoop lag", e);
                }
            }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
        }

        log.info("EventLoop lag measurement started");
    }

    /**
     * 실제 EventLoop Lag 측정
     *
     * @param executor EventLoop executor
     */
    private void measureEventLoopLag(EventExecutor executor) {
        long scheduleTime = System.nanoTime();

        executor.execute(() -> {
            long executeTime = System.nanoTime();
            long lagNanos = executeTime - scheduleTime;
            double lagMs = lagNanos / 1_000_000.0;

            // 메트릭 기록
            if (eventLoopLagSummary != null) {
                eventLoopLagSummary.record(lagMs);
            }

            // 높은 lag 경고 (50ms 이상)
            if (lagMs > 50.0) {
                log.warn("⚠️ High EventLoop lag detected: {:.2f}ms", lagMs);
            }
        });
    }

}

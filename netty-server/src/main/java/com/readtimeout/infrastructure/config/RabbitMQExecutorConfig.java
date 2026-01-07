package com.readtimeout.infrastructure.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ Executor 관련 설정
 *
 * 책임:
 * - Non-blocking 모드용 ThreadPoolExecutor 생성
 * - Graceful shutdown 처리
 */
@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQExecutorConfig.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30L;
    private static final long FORCE_SHUTDOWN_TIMEOUT_SECONDS = 5L;

    private final RabbitMQProperties properties;
    private ThreadPoolExecutor executor;

    public RabbitMQExecutorConfig(RabbitMQProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "rabbitExecutor")
    @ConditionalOnProperty(name = "server.mode", havingValue = "non-blocking", matchIfMissing = true)
    public ThreadPoolExecutor rabbitExecutor() {
        int corePoolSize = properties.threadPool().coreSize();
        int maxPoolSize = properties.threadPool().maxSize();
        int queueCapacity = properties.threadPool().queueCapacity();
        int keepAliveSeconds = properties.threadPool().keepAliveSeconds();

        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("rabbitmq-publisher-" + thread.threadId());
                    thread.setDaemon(false);
                    return thread;
                }
        );

        this.executor.allowCoreThreadTimeOut(true);

        log.info("RabbitMQ Thread Pool created: core={}, max={}, queue={}, keepAlive={}s",
                corePoolSize, maxPoolSize, queueCapacity, keepAliveSeconds);

        return this.executor;
    }

    @PreDestroy
    public void shutdownRabbitExecutor() {
        if (executor == null) {
            return;
        }

        log.info("Shutting down RabbitMQ executor (queueSize={})...",
                executor.getQueue().size());

        executor.shutdown();

        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in {}s, forcing shutdown (remaining tasks: {})",
                        SHUTDOWN_TIMEOUT_SECONDS, executor.getQueue().size());
                executor.shutdownNow();

                if (!executor.awaitTermination(FORCE_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    log.error("Executor did not terminate after force shutdown");
                }
            }

            log.info("RabbitMQ executor shutdown complete");

        } catch (InterruptedException e) {
            log.error("Shutdown interrupted, forcing immediate shutdown");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

package com.readtimeout.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "netty")
public record NettyProperties(
        int httpPort,
        int bossThreads,
        int workerThreads,
        int soBacklog,
        boolean keepAlive,
        int maxContentLength
) {}

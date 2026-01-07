package com.readtimeout.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 설정 (Blocking/Non-blocking 모드용)
 */
@Slf4j
@Configuration
@ConditionalOnExpression("'${server.mode:non-blocking}'.equals('blocking') or '${server.mode:non-blocking}'.equals('non-blocking') or '${server.mode:non-blocking}'.equals('virtual')")
@EnableJpaRepositories(basePackages = "com.readtimeout.infrastructure.adapter.persistence")
public class JpaConfig {

    public JpaConfig() {
        log.info("JPA configuration enabled for blocking/non-blocking mode");
    }
}

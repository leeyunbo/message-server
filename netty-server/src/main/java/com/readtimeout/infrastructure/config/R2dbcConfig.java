package com.readtimeout.infrastructure.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * R2DBC 설정 (Reactive 모드용)
 */
@Slf4j
@Configuration
@ConditionalOnExpression("'${server.mode}'.equals('reactive') or '${server.mode}'.equals('reactive-pool')")
@EnableR2dbcRepositories(basePackages = "com.readtimeout.infrastructure.adapter.persistence")
public class R2dbcConfig {

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));
        log.info("R2DBC ConnectionFactoryInitializer configured with schema.sql");
        return initializer;
    }
}

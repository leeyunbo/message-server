package com.readtimeout.infrastructure.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Listener Container 설정
 *
 * 책임:
 * - Consumer를 위한 ListenerContainerFactory 생성
 * - Prefetch, Concurrency 설정
 */
@Configuration
@ConditionalOnProperty(name = "consumer.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQListenerConfig {

    @Value("${consumer.concurrency:10}")
    private int concurrency;

    @Value("${consumer.prefetch-count:250}")
    private int prefetchCount;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            CachingConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(concurrency * 2);
        factory.setPrefetchCount(prefetchCount);

        return factory;
    }
}

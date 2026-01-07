package com.readtimeout.infrastructure.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.ChannelPool;
import reactor.rabbitmq.ChannelPoolFactory;
import reactor.rabbitmq.ChannelPoolOptions;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

/**
 * Reactive RabbitMQ 설정
 *
 * 책임:
 * - Reactive 모드용 Sender 생성
 * - NIO 기반 비동기 연결 설정
 * - Channel Pool 설정 (reactive-pool 모드)
 *
 * 적용 모드: reactive, reactive-pool
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
@ConditionalOnExpression("'${server.mode}'.equals('reactive') or '${server.mode}'.equals('reactive-pool')")
public class ReactiveRabbitMQConfig {

    private static final int CHANNEL_POOL_MAX_SIZE = 50;

    private final RabbitMQProperties properties;

    public ReactiveRabbitMQConfig(RabbitMQProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Sender reactiveSender() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(properties.host());
        factory.setPort(properties.port());
        factory.setUsername(properties.username());
        factory.setPassword(properties.password());
        factory.setVirtualHost(properties.virtualHost());
        factory.useNio();

        Mono<Connection> connectionMono = Mono.fromCallable(factory::newConnection)
                .cache();

        ChannelPool channelPool = ChannelPoolFactory.createChannelPool(
                connectionMono,
                new ChannelPoolOptions().maxCacheSize(CHANNEL_POOL_MAX_SIZE)
        );

        SenderOptions senderOptions = new SenderOptions()
                .connectionFactory(factory)
                .channelPool(channelPool);

        log.info("ReactiveRabbitMQConfig initialized with ChannelPool (maxSize={})", CHANNEL_POOL_MAX_SIZE);

        return RabbitFlux.createSender(senderOptions);
    }
}

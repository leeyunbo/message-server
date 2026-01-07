package com.readtimeout.infrastructure.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Connection 관련 설정
 *
 * 책임:
 * - ConnectionFactory 생성 및 구성
 * - RabbitTemplate 생성
 */
@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQConnectionConfig {

    private final RabbitMQProperties properties;

    public RabbitMQConnectionConfig(RabbitMQProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CachingConnectionFactory rabbitConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(properties.host());
        factory.setPort(properties.port());
        factory.setUsername(properties.username());
        factory.setPassword(properties.password());
        factory.setVirtualHost(properties.virtualHost());

        CachingConnectionFactory cachingFactory = new CachingConnectionFactory(factory);

        if (properties.publisherConfirms()) {
            cachingFactory.setPublisherConfirmType(
                    CachingConnectionFactory.ConfirmType.CORRELATED
            );
        }

        if (properties.publisherReturns()) {
            cachingFactory.setPublisherReturns(true);
        }

        return cachingFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory rabbitConnectionFactory) {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory);

        if (properties.publisherConfirms()) {
            template.setMandatory(true);
        }

        return template;
    }
}

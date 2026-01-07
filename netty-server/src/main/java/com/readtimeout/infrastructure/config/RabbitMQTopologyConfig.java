package com.readtimeout.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Topology 관련 설정
 *
 * 책임:
 * - Exchange 생성
 * - Queue 생성
 * - Binding 설정
 */
@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQTopologyConfig {

    private final RabbitMQProperties properties;

    public RabbitMQTopologyConfig(RabbitMQProperties properties) {
        this.properties = properties;
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(
                properties.exchange(),
                true,  // durable
                false  // autoDelete
        );
    }

    @Bean
    public Queue queue() {
        return new Queue(
                properties.queueName(),
                true,  // durable
                false, // exclusive
                false  // autoDelete
        );
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(properties.routingKey());
    }
}

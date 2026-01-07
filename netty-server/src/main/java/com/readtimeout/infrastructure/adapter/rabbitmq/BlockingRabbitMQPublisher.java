package com.readtimeout.infrastructure.adapter.rabbitmq;

import com.readtimeout.core.domain.exception.MessagePublishException;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.outbound.MessagePublisher;
import com.readtimeout.infrastructure.config.RabbitMQProperties;
import com.readtimeout.infrastructure.support.BlockingMetrics;
import com.readtimeout.infrastructure.support.MessageSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.mode", havingValue = "blocking")
public class BlockingRabbitMQPublisher implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;
    private final MessageSerializer serializer;
    private final BlockingMetrics metrics;
    private final long confirmTimeout;

    public BlockingRabbitMQPublisher(
            RabbitTemplate rabbitTemplate,
            RabbitMQProperties properties,
            MessageSerializer serializer,
            MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.serializer = serializer;
        this.metrics = BlockingMetrics.create(meterRegistry);
        this.confirmTimeout = properties.confirmTimeoutMs() > 0 ? properties.confirmTimeoutMs() : 5000L;

        log.info("BlockingRabbitMQPublisher initialized (confirmTimeout={}ms)", confirmTimeout);
    }

    @Override
    public void publish(SendMessage sendMessage) throws MessagePublishException {
        long startTime = System.nanoTime();
        String messageId = sendMessage.getId();

        try {
            Message amqpMessage = serializer.serialize(sendMessage);

            Boolean confirmed = rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(properties.exchange(), properties.routingKey(), amqpMessage);
                return operations.waitForConfirms(confirmTimeout);
            });

            if (confirmed == null || !confirmed) {
                throw new MessagePublishException("Message NACK'd or confirm timeout [id=" + messageId + "]");
            }

            metrics.recordPublishLatency(messageId, startTime);
            log.debug("Message [id={}] published to RabbitMQ", messageId);
        } catch (MessagePublishException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to publish message [id={}]: {}", messageId, e.getMessage(), e);
            throw new MessagePublishException("Failed to publish message: " + e.getMessage(), e);
        }
    }
}

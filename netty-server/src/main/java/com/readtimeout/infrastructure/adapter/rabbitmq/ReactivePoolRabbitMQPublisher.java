package com.readtimeout.infrastructure.adapter.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.readtimeout.core.domain.exception.MessagePublishException;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.outbound.ReactiveMessagePublisher;
import com.readtimeout.infrastructure.config.RabbitMQProperties;
import com.readtimeout.infrastructure.support.MessagePublisherMetrics;
import com.readtimeout.infrastructure.support.MessageSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.OutboundMessageResult;
import reactor.rabbitmq.Sender;

import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.mode", havingValue = "reactive-pool")
public class ReactivePoolRabbitMQPublisher implements ReactiveMessagePublisher {

    private final Sender sender;
    private final RabbitMQProperties properties;
    private final MessageSerializer serializer;
    private final MessagePublisherMetrics metrics;
    private final Duration confirmTimeout;

    public ReactivePoolRabbitMQPublisher(
            Sender sender,
            RabbitMQProperties properties,
            MessageSerializer serializer,
            MeterRegistry meterRegistry) {
        this.sender = sender;
        this.properties = properties;
        this.serializer = serializer;
        this.metrics = MessagePublisherMetrics.forReactive(meterRegistry);
        this.confirmTimeout = Duration.ofMillis(
                properties.confirmTimeoutMs() > 0 ? properties.confirmTimeoutMs() : 5000L);

        log.info("ReactivePoolRabbitMQPublisher initialized (confirmTimeout={}ms)", confirmTimeout.toMillis());
    }

    @Override
    public Mono<Void> publish(SendMessage sendMessage) {
        long startTime = System.nanoTime();
        String messageId = sendMessage.getId();

        OutboundMessage outboundMessage = createOutboundMessage(sendMessage);

        return sender.sendWithPublishConfirms(Mono.just(outboundMessage))
                .single()
                .timeout(confirmTimeout)
                .flatMap(result -> handleConfirmResult(result, messageId, startTime));
    }

    private Mono<Void> handleConfirmResult(OutboundMessageResult result, String messageId, long startTime) {
        if (result.isAck()) {
            metrics.recordPublishSuccess(messageId, startTime);
            log.debug("Message [id={}] confirmed by broker", messageId);
            return Mono.empty();
        }

        MessagePublishException ex = new MessagePublishException("Message NACK'd by broker [id=" + messageId + "]");
        metrics.recordPublishFailure(messageId, startTime, ex);
        log.warn("Message [id={}] rejected by broker", messageId);
        return Mono.error(ex);
    }

    private OutboundMessage createOutboundMessage(SendMessage sendMessage) {
        Message amqpMessage = serializer.serialize(sendMessage);

        return new OutboundMessage(
                properties.exchange(),
                properties.routingKey(),
                new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .deliveryMode(2)
                        .correlationId(sendMessage.getId())
                        .build(),
                amqpMessage.getBody()
        );
    }
}

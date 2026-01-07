package com.readtimeout.infrastructure.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readtimeout.core.domain.model.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Message 직렬화 담당
 *
 * Domain Message를 AMQP Message로 변환.
 * AmqpMessagePayload를 통해 JSON 구조를 명시적으로 관리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSerializer {

    private final ObjectMapper objectMapper;

    public Message serialize(SendMessage sendMessage) {
        byte[] messageBytes = serializeToJson(sendMessage);
        MessageProperties properties = createMessageProperties(sendMessage);

        return MessageBuilder
                .withBody(messageBytes)
                .andProperties(properties)
                .build();
    }

    private byte[] serializeToJson(SendMessage sendMessage) {
        AmqpMessagePayload payload = AmqpMessagePayload.from(sendMessage);
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", sendMessage.getId(), e);
            throw new IllegalStateException("Message serialization failed", e);
        }
    }

    private MessageProperties createMessageProperties(SendMessage sendMessage) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(sendMessage.getId());
        properties.setTimestamp(Date.from(sendMessage.getCreatedAt()));
        return properties;
    }
}

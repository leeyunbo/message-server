package com.readtimeout.infrastructure.support;

import com.readtimeout.core.domain.model.SendMessage;

import java.time.Instant;

/**
 * AMQP 메시지 페이로드 DTO
 *
 * Domain Entity(Message)를 AMQP JSON 형식으로 변환하기 위한 전용 DTO.
 * JSON 구조를 명시적으로 정의하여 직렬화 안정성을 보장.
 *
 * @param id 메시지 식별자
 * @param content 메시지 내용
 * @param createdAt 생성 시간 (ISO-8601 형식)
 */
public record AmqpMessagePayload(
        String id,
        String content,
        String createdAt
) {
    /**
     * Domain Message로부터 Payload 생성
     */
    public static AmqpMessagePayload from(SendMessage sendMessage) {
        return new AmqpMessagePayload(
                sendMessage.getId(),
                sendMessage.getContent(),
                sendMessage.getCreatedAt().toString()
        );
    }

    /**
     * Payload를 Domain Message로 변환 (역직렬화 시 사용)
     */
    public SendMessage toDomain() {
        return new SendMessage(id, content, Instant.parse(createdAt));
    }
}

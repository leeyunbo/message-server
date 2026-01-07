package com.readtimeout.core.domain.model;

import com.readtimeout.core.domain.exception.ValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

/**
 * Message Entity
 *
 * 메시지를 표현하는 Domain Entity.
 * 식별자(id)를 가지며 불변 객체로 설계됨.
 * Entity의 동등성은 ID 기반.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SendMessage {
    @EqualsAndHashCode.Include
    private final String id;
    private final String content;
    private final Instant createdAt;

    /**
     * Message 생성자 (현재 시간 자동 설정)
     *
     * @param id 메시지 식별자 (필수, null 불가)
     * @param content 메시지 내용 (필수, null 불가)
     * @throws ValidationException id 또는 content가 null이거나 빈 값인 경우
     */
    public SendMessage(String id, String content) {
        this(id, content, Instant.now());
    }

    /**
     * Message 생성자 (시간 직접 지정, 테스트 용이성)
     *
     * @param id 메시지 식별자 (필수, null 불가)
     * @param content 메시지 내용 (필수, null 불가)
     * @param createdAt 생성 시간 (필수, null 불가)
     * @throws ValidationException 필수 값이 null이거나 빈 값인 경우
     */
    public SendMessage(String id, String content, Instant createdAt) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Message ID cannot be null or blank");
        }
        if (content == null) {
            throw new ValidationException("Message content cannot be null");
        }
        if (createdAt == null) {
            throw new ValidationException("CreatedAt cannot be null");
        }

        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", contentLength=" + content.length() +
                ", createdAt=" + createdAt +
                '}';
    }
}

package com.readtimeout.core.domain.exception;

/**
 * MessagePublishException
 *
 * 메시지 발행 중 발생하는 예외를 표현하는 Domain Exception.
 * 예: RabbitMQ 연결 실패, 큐 오버플로우 등
 */
public class MessagePublishException extends DomainException {
    public MessagePublishException(String message) {
        super(message);
    }

    public MessagePublishException(String message, Throwable cause) {
        super(message, cause);
    }
}

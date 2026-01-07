package com.readtimeout.core.domain.exception;

/**
 * ValidationException
 *
 * 도메인 객체 생성 시 유효성 검증 실패를 표현하는 예외.
 * 예: 필수 필드 누락, 잘못된 형식 등
 */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

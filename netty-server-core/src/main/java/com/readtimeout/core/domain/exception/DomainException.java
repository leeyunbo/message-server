package com.readtimeout.core.domain.exception;

/**
 * DomainException
 *
 * 모든 도메인 예외의 기본 클래스.
 * 도메인 레이어에서 발생하는 모든 예외는 이 클래스를 상속해야 함.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

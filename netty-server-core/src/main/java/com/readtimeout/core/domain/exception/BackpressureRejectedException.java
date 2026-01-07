package com.readtimeout.core.domain.exception;

/**
 * BackpressureRejectedException
 *
 * Backpressure로 인해 요청이 거부되었을 때 발생하는 예외.
 * 예: Semaphore 제한 초과, 큐 용량 초과 등
 */
public class BackpressureRejectedException extends MessagePublishException {
    public BackpressureRejectedException(String message) {
        super(message);
    }

    public BackpressureRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.readtimeout.core.domain.port.outbound;

import com.readtimeout.core.domain.model.SendMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 비동기 메시지 발행을 위한 Outbound Port
 *
 * CompletableFuture 기반의 비동기 발행을 지원하며,
 * Non-blocking 모드와 Virtual Thread 모드에서 사용됨.
 *
 * MessagePublisher(동기)와 독립적인 인터페이스로,
 * 비동기 방식만 제공하여 블로킹 호출을 방지함.
 */
public interface AsyncMessagePublisher {

    /**
     * 메시지를 비동기로 발행
     *
     * @param sendMessage 발행할 메시지
     * @return 발행 완료를 나타내는 CompletableFuture
     *         - 성공 시: Void로 완료
     *         - 실패 시: MessagePublishException으로 완료
     */
    CompletableFuture<Void> publish(SendMessage sendMessage);
}

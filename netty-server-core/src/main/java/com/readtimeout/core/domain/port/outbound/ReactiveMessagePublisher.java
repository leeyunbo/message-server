package com.readtimeout.core.domain.port.outbound;

import com.readtimeout.core.domain.model.SendMessage;
import reactor.core.publisher.Mono;

/**
 * 리액티브 메시지 발행을 위한 Outbound Port
 *
 * Reactor Mono 기반의 비동기 발행을 지원하며,
 * Reactive 모드에서 사용됨.
 *
 * MessagePublisher(동기)와 독립적인 인터페이스로,
 * 리액티브 방식만 제공하여 블로킹 호출을 방지함.
 */
public interface ReactiveMessagePublisher {

    /**
     * 메시지를 리액티브하게 발행
     *
     * @param sendMessage 발행할 메시지
     * @return 발행 완료를 나타내는 Mono
     *         - 성공 시: empty Mono로 완료
     *         - 실패 시: MessagePublishException으로 에러 시그널
     */
    Mono<Void> publish(SendMessage sendMessage);
}

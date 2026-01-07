package com.readtimeout.core.domain.port.inbound;

import com.readtimeout.core.application.dto.PublishRequest;
import com.readtimeout.core.application.dto.PublishResponse;

/**
 * MessagePublishUseCase Port (Inbound)
 *
 * 메시지 발행 Use Case Interface.
 * Presentation Layer가 이 인터페이스를 호출함.
 *
 * Hexagonal Architecture의 Driving Port (입력 포트).
 */
public interface MessagePublishUseCase {
    /**
     * 메시지 발행
     *
     * @param request 발행 요청
     * @return 발행 응답
     */
    PublishResponse publishMessage(PublishRequest request);
}

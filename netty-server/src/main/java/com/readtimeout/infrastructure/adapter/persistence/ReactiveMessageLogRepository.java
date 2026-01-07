package com.readtimeout.infrastructure.adapter.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * R2DBC Repository (Reactive) - V3용
 */
@Repository
public interface ReactiveMessageLogRepository extends ReactiveCrudRepository<ReactiveMessageLog, Long> {
    Mono<ReactiveMessageLog> findByMessageId(String messageId);
}

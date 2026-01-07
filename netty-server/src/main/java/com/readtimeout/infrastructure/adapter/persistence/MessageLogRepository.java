package com.readtimeout.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository (Blocking) - V1, V2용
 */
@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {
    Optional<MessageLog> findByMessageId(String messageId);
}

package com.readtimeout.infrastructure.adapter.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * R2DBC Entity (Reactive) - V3용
 */
@Table("message_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactiveMessageLog {

    @Id
    private Long id;

    @Column("message_id")
    private String messageId;

    @Column("content")
    private String content;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("status")
    private String status;

    public static ReactiveMessageLog create(String messageId, String content) {
        return ReactiveMessageLog.builder()
                .messageId(messageId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .status("PENDING")
                .build();
    }
}

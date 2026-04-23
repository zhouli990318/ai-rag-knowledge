package com.silver.ai.infrastructure.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("chat_trace")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTraceEntity {

    @Id
    private Long id;

    @Column("trace_id")
    private String traceId;

    @Column("conversation_id")
    private Long conversationId;

    @Column("message_id")
    private Long messageId;

    @Column("total_duration_ms")
    @Builder.Default
    private long totalDurationMs = 0;

    @Column("created_at")
    private LocalDateTime createdAt;
}

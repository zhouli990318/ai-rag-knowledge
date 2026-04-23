package com.silver.ai.infrastructure.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("chat_trace_span")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTraceSpanEntity {

    @Id
    private Long id;

    @Column("trace_id")
    private String traceId;

    @Column("span_id")
    private String spanId;

    private String stage;

    @Column("start_time")
    private LocalDateTime startTime;

    @Column("end_time")
    private LocalDateTime endTime;

    @Column("duration_ms")
    @Builder.Default
    private long durationMs = 0;

    @Builder.Default
    private boolean success = true;

    @Column("error_message")
    private String errorMessage;

    @Column("attributes_json")
    private String attributesJson;
}

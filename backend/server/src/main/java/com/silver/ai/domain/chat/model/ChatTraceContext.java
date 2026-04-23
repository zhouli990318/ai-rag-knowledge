package com.silver.ai.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话链路追踪上下文 — 聚合一次对话请求的所有 Span。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTraceContext {

    private String traceId;
    private Long conversationId;
    private Long messageId;
    private LocalDateTime startTime;
    @Builder.Default
    private List<TraceSpan> spans = new ArrayList<>();

    /** 创建新的追踪上下文 */
    public static ChatTraceContext create(Long conversationId) {
        return ChatTraceContext.builder()
                .traceId(java.util.UUID.randomUUID().toString().replace("-", ""))
                .conversationId(conversationId)
                .startTime(LocalDateTime.now())
                .build();
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    /** 开始一个阶段 Span */
    public TraceSpan startSpan(OrchestrationStage stage) {
        TraceSpan span = TraceSpan.start(this.traceId, stage);
        this.spans.add(span);
        return span;
    }

    /** 获取总耗时 */
    public long totalDurationMs() {
        return spans.stream().mapToLong(TraceSpan::getDurationMs).sum();
    }
}

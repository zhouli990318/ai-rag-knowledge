package com.silver.ai.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 链路追踪 Span — 值对象。
 * 每个编排阶段生成一条 Span 记录。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceSpan {

    private String traceId;
    private String spanId;
    private OrchestrationStage stage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 耗时毫秒 */
    private long durationMs;
    private boolean success;
    private String errorMessage;
    /** 额外可观测属性（如: rewrittenQuery, intentDomain, retrievedCount） */
    private Map<String, Object> attributes;

    /** 创建一个已启动的 Span */
    public static TraceSpan start(String traceId, OrchestrationStage stage) {
        String spanId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return TraceSpan.builder()
                .traceId(traceId)
                .spanId(spanId)
                .stage(stage)
                .startTime(LocalDateTime.now())
                .success(true)
                .attributes(new java.util.LinkedHashMap<>())
                .build();
    }

    /** 完成 Span 并记录耗时 */
    public TraceSpan finish() {
        this.endTime = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        return this;
    }

    /** 标记失败 */
    public TraceSpan fail(String error) {
        this.success = false;
        this.errorMessage = error;
        return finish();
    }

    /** 添加属性 */
    public TraceSpan attr(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new java.util.LinkedHashMap<>();
        }
        this.attributes.put(key, value);
        return this;
    }
}

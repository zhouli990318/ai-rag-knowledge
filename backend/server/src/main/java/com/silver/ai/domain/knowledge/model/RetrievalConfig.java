package com.silver.ai.domain.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 检索配置 — 值对象（不可变）
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalConfig {

    @Builder.Default
    private int topK = 5;
    @Builder.Default
    private double similarityThreshold = 0.7;
    private String filterExpression;

    public static RetrievalConfig defaultConfig() {
        return RetrievalConfig.builder().build();
    }
}

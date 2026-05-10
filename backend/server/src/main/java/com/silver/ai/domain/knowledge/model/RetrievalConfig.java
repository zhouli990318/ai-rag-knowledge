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
    private double similarityThreshold = 0.6;
    private String filterExpression;
    @Builder.Default
    private RetrievalMode retrievalMode = RetrievalMode.HYBRID;
    @Builder.Default
    private double keywordWeight = 0.3;
    @Builder.Default
    private double vectorWeight = 0.7;

    public enum RetrievalMode {
        /** 纯向量语义检索 */
        VECTOR,
        /** 纯关键词 BM25 检索 */
        KEYWORD,
        /** 混合检索：向量 + 关键词 RRF 融合 */
        HYBRID
    }

    public boolean isHybrid() {
        return retrievalMode == RetrievalMode.HYBRID;
    }

    public boolean isKeywordOnly() {
        return retrievalMode == RetrievalMode.KEYWORD;
    }

    public static RetrievalConfig defaultConfig() {
        return RetrievalConfig.builder().build();
    }
}

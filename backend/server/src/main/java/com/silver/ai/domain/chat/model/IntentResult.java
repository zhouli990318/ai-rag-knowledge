package com.silver.ai.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 意图识别结果 — 值对象
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {

    /** 一级分类：领域 */
    private String domain;
    /** 二级分类：类目 */
    private String category;
    /** 三级分类：话题 */
    private String topic;
    /** 置信度 0.0 ~ 1.0 */
    private double confidence;
    /** 是否需要澄清 */
    private boolean needsClarification;
    /** 澄清提示语（当 needsClarification=true 时有值） */
    private String clarificationPrompt;
    /** 路由建议：RETRIEVAL / TOOL / DIRECT / HYBRID */
    @Builder.Default
    private RoutingAdvice routingAdvice = RoutingAdvice.RETRIEVAL;

    /** 是否为高置信结果 */
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }

    /** 路由建议枚举 */
    public enum RoutingAdvice {
        /** 走知识检索 */
        RETRIEVAL,
        /** 走 MCP 工具 */
        TOOL,
        /** 模型直答 */
        DIRECT,
        /** 检索 + 工具混合 */
        HYBRID
    }

    /** 快速构建一个默认的检索意图 */
    public static IntentResult defaultRetrieval() {
        return IntentResult.builder()
                .domain("general")
                .category("qa")
                .topic("unknown")
                .confidence(0.5)
                .needsClarification(false)
                .routingAdvice(RoutingAdvice.RETRIEVAL)
                .build();
    }
}

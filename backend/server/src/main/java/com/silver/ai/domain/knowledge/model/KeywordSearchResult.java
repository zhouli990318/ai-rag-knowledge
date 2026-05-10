package com.silver.ai.domain.knowledge.model;

import java.util.Map;

/**
 * 关键词搜索结果 — 用于 BM25 全文检索返回的领域值对象。
 */
public record KeywordSearchResult(
        String content,
        Map<String, Object> metadata,
        double score
) {
}

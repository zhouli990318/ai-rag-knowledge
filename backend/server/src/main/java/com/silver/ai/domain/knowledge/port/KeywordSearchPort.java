package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.KeywordSearchResult;

import java.util.List;
import java.util.Map;

/**
 * 关键词搜索端口 — BM25 全文检索抽象。
 */
public interface KeywordSearchPort {

    /**
     * 基于关键词的全文搜索（BM25 / ts_rank）
     *
     * @param query          查询文本
     * @param topK           返回结果数量上限
     * @param filterMetadata 过滤条件（如 knowledge_base_id）
     * @return 按相关性降序排列的搜索结果
     */
    List<KeywordSearchResult> keywordSearch(String query, int topK, Map<String, Object> filterMetadata);
}

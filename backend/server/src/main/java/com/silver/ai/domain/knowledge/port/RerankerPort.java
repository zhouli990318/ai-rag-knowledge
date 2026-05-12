package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.RankedResult;

import java.util.List;

/**
 * 文档重排序端口。
 */
public interface RerankerPort {

    List<RankedResult> rerank(String query, List<String> documents, int topN);
}
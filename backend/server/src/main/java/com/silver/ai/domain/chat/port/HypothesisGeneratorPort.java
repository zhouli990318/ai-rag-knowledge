package com.silver.ai.domain.chat.port;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * HyDE 假设文档生成端口。
 */
public interface HypothesisGeneratorPort {

    /**
     * 基于改写后的查询与上下文生成一段可用于向量召回的假设文档。
     */
    Mono<String> generate(String rewrittenQuery, List<String> conversationContext);
}
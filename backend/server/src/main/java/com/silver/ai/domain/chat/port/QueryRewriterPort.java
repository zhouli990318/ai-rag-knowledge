package com.silver.ai.domain.chat.port;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 问题重写端口 — 领域层纯接口，由 infrastructure 提供实现。
 */
public interface QueryRewriterPort {

    /**
     * 基于上下文重写用户查询（指代消解、上下文补全）。
     */
    Mono<String> rewrite(String originalQuery, List<String> conversationContext);

    /**
     * 将复杂问题拆分为可独立检索的子问题。
     */
    Mono<List<String>> decompose(String query);
}

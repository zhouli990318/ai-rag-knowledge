package com.silver.ai.domain.chat.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.port.QueryRewriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 查询规划领域服务 — 编排查询重写和子问题拆分。
 */
@Slf4j
@RequiredArgsConstructor
public class QueryPlanningDomainService {

    private final QueryRewriterPort queryRewriter;
    private final ChatOrchestratorConfig config;

    /**
     * 对用户查询进行重写和拆分。
     */
    public Mono<QueryPlan> plan(String originalQuery, List<String> conversationContext) {
        return queryRewriter.rewrite(originalQuery,
                conversationContext.stream()
                        .limit(config.getRewriteContextRounds() * 2L)
                        .toList())
                .flatMap(rewritten -> queryRewriter.decompose(rewritten)
                        .map(subQueries -> new QueryPlan(originalQuery, rewritten, subQueries)));
    }

    /**
     * 查询规划结果。
     */
    public record QueryPlan(
            String originalQuery,
            String rewrittenQuery,
            List<String> subQueries
    ) {
        /** 获取最终用于检索的查询列表 */
        public List<String> retrievalQueries() {
            return subQueries != null && !subQueries.isEmpty() ? subQueries : List.of(rewrittenQuery);
        }
    }
}

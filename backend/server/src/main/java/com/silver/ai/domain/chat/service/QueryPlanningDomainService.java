package com.silver.ai.domain.chat.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.port.HypothesisGeneratorPort;
import com.silver.ai.domain.chat.port.QueryRewriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 查询规划领域服务 — 编排查询重写和子问题拆分。
 */
@Slf4j
@RequiredArgsConstructor
public class QueryPlanningDomainService {

    private final QueryRewriterPort queryRewriter;
    private final HypothesisGeneratorPort hypothesisGenerator;
    private final ChatOrchestratorConfig config;

    /**
     * 对用户查询进行重写和拆分。
     */
    public Mono<QueryPlan> plan(String originalQuery, List<String> conversationContext) {
        List<String> limitedContext = limitContext(conversationContext);
        if (!config.isRewriteEnabled()) {
            return Mono.just(QueryPlan.originalOnly(originalQuery));
        }

        return queryRewriter.rewrite(originalQuery, limitedContext)
                .flatMap(rewrittenQuery -> {
                    String rewritten = normalize(rewrittenQuery, originalQuery);
                    if (!config.isMultiPathRetrievalEnabled()) {
                        return Mono.just(QueryPlan.singleRewritten(originalQuery, rewritten));
                    }

                    Mono<List<String>> subQueriesMono = queryRewriter.decompose(rewritten)
                            .map(this::sanitizeSubQueries)
                            .defaultIfEmpty(List.of(rewritten));
                    Mono<String> hypothesisMono = config.isHydeEnabled()
                            ? hypothesisGenerator.generate(rewritten, limitedContext)
                                    .map(this::normalizeOptional)
                                    .defaultIfEmpty("")
                            : Mono.just("");

                    return Mono.zip(subQueriesMono, hypothesisMono)
                            .map(tuple -> buildPlan(originalQuery, rewritten, tuple.getT1(), tuple.getT2()));
                });
    }

    private List<String> limitContext(List<String> conversationContext) {
        if (conversationContext == null || conversationContext.isEmpty()) {
            return List.of();
        }
        return conversationContext.stream()
                .filter(Objects::nonNull)
                .limit(config.getRewriteContextRounds() * 2L)
                .toList();
    }

    private QueryPlan buildPlan(String originalQuery, String rewrittenQuery, List<String> subQueries, String hypothesis) {
        List<String> normalizedQueries = sanitizeSubQueries(subQueries);
        if (normalizedQueries.isEmpty()) {
            normalizedQueries = List.of(rewrittenQuery);
        }

        List<RetrievalQueryVariant> variants = new ArrayList<>();
        for (String query : normalizedQueries) {
            RetrievalQuerySource source = query.equals(rewrittenQuery)
                    ? RetrievalQuerySource.REWRITTEN
                    : RetrievalQuerySource.DECOMPOSED;
            variants.add(new RetrievalQueryVariant(query, source));
        }

        String normalizedHypothesis = normalizeOptional(hypothesis);
        if (!normalizedHypothesis.isBlank() && normalizedQueries.stream().noneMatch(normalizedHypothesis::equals)) {
            variants.add(new RetrievalQueryVariant(normalizedHypothesis, RetrievalQuerySource.HYDE));
        }

        return new QueryPlan(originalQuery, rewrittenQuery, normalizedQueries, variants);
    }

    private List<String> sanitizeSubQueries(List<String> subQueries) {
        if (subQueries == null || subQueries.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(subQueries.stream()
                .map(this::normalizeOptional)
                .filter(query -> !query.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    private String normalize(String candidate, String fallback) {
        String normalized = normalizeOptional(candidate);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String normalizeOptional(String candidate) {
        return candidate == null ? "" : candidate.trim();
    }

    /**
     * 查询规划结果。
     */
    public record QueryPlan(
            String originalQuery,
            String rewrittenQuery,
            List<String> subQueries,
            List<RetrievalQueryVariant> retrievalVariants
    ) {
        public QueryPlan(String originalQuery, String rewrittenQuery, List<String> subQueries) {
            this(originalQuery, rewrittenQuery, subQueries, null);
        }

        public QueryPlan {
            subQueries = subQueries == null ? List.of() : List.copyOf(subQueries);
            retrievalVariants = retrievalVariants == null
                    ? deriveVariants(rewrittenQuery, subQueries)
                    : List.copyOf(retrievalVariants);
        }

        public static QueryPlan originalOnly(String originalQuery) {
            return new QueryPlan(
                    originalQuery,
                    originalQuery,
                    List.of(originalQuery),
                    List.of(new RetrievalQueryVariant(originalQuery, RetrievalQuerySource.ORIGINAL))
            );
        }

        public static QueryPlan singleRewritten(String originalQuery, String rewrittenQuery) {
            return new QueryPlan(
                    originalQuery,
                    rewrittenQuery,
                    List.of(rewrittenQuery),
                    List.of(new RetrievalQueryVariant(rewrittenQuery, RetrievalQuerySource.REWRITTEN))
            );
        }

        /** 获取最终用于检索的查询列表 */
        public List<String> retrievalQueries() {
            if (retrievalVariants != null && !retrievalVariants.isEmpty()) {
                return retrievalVariants.stream().map(RetrievalQueryVariant::query).toList();
            }
            return !subQueries.isEmpty() ? subQueries : List.of(rewrittenQuery);
        }

        private static List<RetrievalQueryVariant> deriveVariants(String rewrittenQuery, List<String> subQueries) {
            if (subQueries == null || subQueries.isEmpty()) {
                return List.of(new RetrievalQueryVariant(rewrittenQuery, RetrievalQuerySource.REWRITTEN));
            }
            return subQueries.stream()
                    .map(query -> new RetrievalQueryVariant(query, RetrievalQuerySource.REWRITTEN))
                    .toList();
        }
    }

    public record RetrievalQueryVariant(String query, RetrievalQuerySource source) {

        public boolean supportsKeywordSearch() {
            return source != RetrievalQuerySource.HYDE;
        }
    }

    public enum RetrievalQuerySource {
        ORIGINAL,
        REWRITTEN,
        DECOMPOSED,
        HYDE
    }
}

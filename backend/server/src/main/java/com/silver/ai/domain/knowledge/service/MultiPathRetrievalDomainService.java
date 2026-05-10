package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.knowledge.model.*;
import com.silver.ai.domain.knowledge.port.KeywordSearchPort;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 多路检索领域服务 — 支持多子问题并行检索、去重、重排序融合。
 * 支持三种检索模式：VECTOR（纯向量）、KEYWORD（纯关键词 BM25）、HYBRID（混合 RRF 融合）。
 */
@Slf4j
@RequiredArgsConstructor
public class MultiPathRetrievalDomainService {

    private final VectorStorePort vectorStore;
    private final KeywordSearchPort keywordSearch;
    private final PromptRendererPort promptRenderer;
    private final ChatOrchestratorConfig config;

    /** RRF 融合常数 k，业界标准值 */
    private static final int RRF_K = 60;

    /**
     * 多路检索入口：根据检索模式分发到不同检索路径。
     *
     * @param kb           知识库
     * @param subQueries   子查询列表（可能是原始查询或拆分后的子问题）
     * @param intentResult 意图结果（用于路由决策和检索增强）
     * @return 融合后的 RAG 系统提示
     */
    public String retrieveAndFuse(KnowledgeBase kb, List<String> subQueries, IntentResult intentResult) {
        if (subQueries == null || subQueries.isEmpty()) {
            return "";
        }

        var retrievalConfig = kb.getRetrievalConfig();
        Map<String, Object> filter = Map.of(VectorMetadataKeys.KNOWLEDGE_BASE_ID, String.valueOf(kb.getId()));

        log.debug("Multi-path retrieval start: kbId={}, mode={}, topK={}, threshold={}, subQueries={}",
                kb.getId(), retrievalConfig.getRetrievalMode(), retrievalConfig.getTopK(),
                retrievalConfig.getSimilarityThreshold(), subQueries);

        List<VectorDocument> rankedDocs;

        switch (retrievalConfig.getRetrievalMode()) {
            case KEYWORD -> rankedDocs = keywordOnlySearch(subQueries, retrievalConfig, filter);
            case HYBRID -> rankedDocs = hybridSearch(subQueries, retrievalConfig, filter);
            default -> rankedDocs = vectorOnlySearch(subQueries, retrievalConfig, filter);
        }

        if (rankedDocs.isEmpty()) {
            log.debug("No relevant documents found for queries in knowledge base: {}", kb.getName());
            return "";
        }

        log.debug("Multi-path retrieval resolved {} documents for kbId={} using mode={}",
            rankedDocs.size(), kb.getId(), retrievalConfig.getRetrievalMode());

        String context = rankedDocs.stream()
                .map(VectorDocument::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        return buildRagSystemPrompt(context);
    }

    /**
     * 纯向量语义检索（原有逻辑）。
     */
    private List<VectorDocument> vectorOnlySearch(List<String> subQueries, RetrievalConfig retrievalConfig,
                                                   Map<String, Object> filter) {
        List<VectorDocument> allDocs;
        if (subQueries.size() == 1) {
            allDocs = vectorStore.similaritySearch(
                    subQueries.getFirst(), retrievalConfig.getTopK(),
                    retrievalConfig.getSimilarityThreshold(), filter);
        } else {
            allDocs = parallelSearch(subQueries, retrievalConfig.getTopK(),
                    retrievalConfig.getSimilarityThreshold(), filter);
        }

        List<VectorDocument> uniqueDocs = deduplicateByContent(allDocs);
        return rerankAndTruncate(uniqueDocs, subQueries, retrievalConfig.getTopK());
    }

    /**
     * 纯关键词 BM25 检索。
     */
    private List<VectorDocument> keywordOnlySearch(List<String> subQueries, RetrievalConfig retrievalConfig,
                                                    Map<String, Object> filter) {
        List<VectorDocument> allDocs = new ArrayList<>();
        for (String query : subQueries) {
            List<KeywordSearchResult> results = keywordSearch.keywordSearch(query, retrievalConfig.getTopK(), filter);
            results.forEach(r -> allDocs.add(new VectorDocument(r.content(), r.metadata())));
        }

        List<VectorDocument> uniqueDocs = deduplicateByContent(allDocs);
        log.debug("Keyword-only multi-path search resolved {} raw documents, {} after deduplication",
            allDocs.size(), uniqueDocs.size());
        return uniqueDocs.size() > retrievalConfig.getTopK()
                ? uniqueDocs.subList(0, retrievalConfig.getTopK())
                : uniqueDocs;
    }

    /**
     * 混合检索：并行执行向量搜索和关键词搜索，使用 RRF（Reciprocal Rank Fusion）融合结果。
     * RRF 公式：score(d) = Σ 1/(k + rank_i(d))，k=60
     */
    private List<VectorDocument> hybridSearch(List<String> subQueries, RetrievalConfig retrievalConfig,
                                              Map<String, Object> filter) {
        int topK = retrievalConfig.getTopK();
        // 每路多取一些以提高融合质量
        int fetchSize = topK * 2;

        // 并行执行两路检索
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Future<List<VectorDocument>> vectorFuture = executor.submit(() -> {
                List<VectorDocument> docs;
                if (subQueries.size() == 1) {
                    docs = vectorStore.similaritySearch(
                            subQueries.getFirst(), fetchSize,
                            retrievalConfig.getSimilarityThreshold(), filter);
                } else {
                    docs = parallelSearch(subQueries, fetchSize,
                            retrievalConfig.getSimilarityThreshold(), filter);
                }
                return deduplicateByContent(docs);
            });

            Future<List<KeywordSearchResult>> keywordFuture = executor.submit(() -> {
                List<KeywordSearchResult> allKw = new ArrayList<>();
                for (String query : subQueries) {
                    allKw.addAll(keywordSearch.keywordSearch(query, fetchSize, filter));
                }
                return allKw;
            });

            List<VectorDocument> vectorResults = vectorFuture.get(
                    config.getRetrievalTimeoutSeconds(), TimeUnit.SECONDS);
            List<KeywordSearchResult> keywordResults = keywordFuture.get(
                    config.getRetrievalTimeoutSeconds(), TimeUnit.SECONDS);

            log.debug("Hybrid retrieval raw results: vector={}, keyword={}",
                    vectorResults.size(), keywordResults.size());

            return rrfFuse(vectorResults, keywordResults, topK);
        } catch (TimeoutException e) {
            log.warn("Hybrid search timed out after {}s, falling back to vector only",
                    config.getRetrievalTimeoutSeconds());
            return vectorOnlySearch(subQueries, retrievalConfig, filter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Hybrid search interrupted");
            return List.of();
        } catch (ExecutionException e) {
            log.warn("Hybrid search failed: {}, falling back to vector only", e.getCause().getMessage());
            return vectorOnlySearch(subQueries, retrievalConfig, filter);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * RRF（Reciprocal Rank Fusion）分数融合。
     * 将向量检索和关键词检索的排名结果按 RRF 公式融合后排序。
     */
    private List<VectorDocument> rrfFuse(List<VectorDocument> vectorResults,
                                          List<KeywordSearchResult> keywordResults,
                                          int topK) {
        // contentKey -> RRF score
        Map<String, Double> rrfScores = new LinkedHashMap<>();
        // contentKey -> VectorDocument (保留原始内容)
        Map<String, VectorDocument> docMap = new LinkedHashMap<>();

        // 向量检索结果按排名计算 RRF 分数
        for (int rank = 0; rank < vectorResults.size(); rank++) {
            VectorDocument doc = vectorResults.get(rank);
            String key = contentKey(doc.content());
            if (key == null) continue;
            docMap.putIfAbsent(key, doc);
            rrfScores.merge(key, 1.0 / (RRF_K + rank + 1), Double::sum);
        }

        // 关键词检索结果按排名计算 RRF 分数
        for (int rank = 0; rank < keywordResults.size(); rank++) {
            KeywordSearchResult kw = keywordResults.get(rank);
            String key = contentKey(kw.content());
            if (key == null) continue;
            docMap.putIfAbsent(key, new VectorDocument(kw.content(), kw.metadata()));
            rrfScores.merge(key, 1.0 / (RRF_K + rank + 1), Double::sum);
        }

        // 按 RRF 分数降序排列，截断到 topK
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> docMap.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 生成用于去重和融合的内容键。
     */
    private String contentKey(String content) {
        if (content == null) return null;
        String trimmed = content.trim();
        if (trimmed.length() > config.getDeduplicatePrefixLength()) {
            return trimmed.substring(0, config.getDeduplicatePrefixLength());
        }
        return trimmed;
    }

    /**
     * 并行执行多个子查询的向量搜索。
     */
    private List<VectorDocument> parallelSearch(List<String> queries, int topK, double threshold,
                                          Map<String, Object> filter) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            List<Callable<List<VectorDocument>>> tasks = queries.stream()
                    .<Callable<List<VectorDocument>>>map(query -> () ->
                            vectorStore.similaritySearch(query, topK, threshold, filter))
                    .toList();

            List<Future<List<VectorDocument>>> futures = executor.invokeAll(
                    tasks, config.getRetrievalTimeoutSeconds(), TimeUnit.SECONDS);

            List<VectorDocument> allResults = new ArrayList<>();
            for (Future<List<VectorDocument>> future : futures) {
                try {
                    if (!future.isCancelled()) {
                        allResults.addAll(future.get());
                    } else {
                        log.warn("Sub-query retrieval timed out after {}s", config.getRetrievalTimeoutSeconds());
                    }
                } catch (CancellationException e) {
                    log.warn("Sub-query retrieval was cancelled due to timeout");
                } catch (ExecutionException e) {
                    log.warn("Sub-query retrieval failed: {}", e.getCause().getMessage());
                }
            }
            return allResults;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Parallel search interrupted");
            return List.of();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 基于文本内容去重（相同文本片段只保留一个，保留得分最高的）。
     */
    private List<VectorDocument> deduplicateByContent(List<VectorDocument> docs) {
        Map<String, VectorDocument> seen = new LinkedHashMap<>();
        for (VectorDocument doc : docs) {
            String text = doc.content();
            if (text == null) continue;
            String contentKey = text.trim();
            if (contentKey.length() > config.getDeduplicatePrefixLength()) {
                contentKey = contentKey.substring(0, config.getDeduplicatePrefixLength());
            }
            seen.putIfAbsent(contentKey, doc);
        }
        return new ArrayList<>(seen.values());
    }

    /**
     * 简易重排序：按与查询的关键词重叠度打分，截断到 topK。
     * 后续可替换为 Cross-Encoder 模型。
     */
    private List<VectorDocument> rerankAndTruncate(List<VectorDocument> docs, List<String> queries, int topK) {
        if (docs.size() <= topK) {
            return docs;
        }

        Set<String> queryTerms = queries.stream()
                .flatMap(q -> Arrays.stream(q.split("[\\s，。、？！,.?!]+")))
                .filter(s -> s.length() > 1)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return docs.stream()
                .sorted((a, b) -> {
                    double scoreA = computeOverlapScore(a.content(), queryTerms);
                    double scoreB = computeOverlapScore(b.content(), queryTerms);
                    return Double.compare(scoreB, scoreA); // 降序
                })
                .limit(topK)
                .toList();
    }

    private double computeOverlapScore(String text, Set<String> queryTerms) {
        if (text == null || queryTerms.isEmpty()) return 0;
        String lowerText = text.toLowerCase();
        long matchCount = queryTerms.stream().filter(lowerText::contains).count();
        return (double) matchCount / queryTerms.size();
    }

    private String buildRagSystemPrompt(String context) {
        return promptRenderer.render(PromptTemplates.RAG_SYSTEM, Map.of("context", context));
    }
}

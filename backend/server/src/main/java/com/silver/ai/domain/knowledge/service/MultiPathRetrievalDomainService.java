package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.VectorDocument;
import com.silver.ai.domain.knowledge.model.VectorMetadataKeys;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 多路检索领域服务 — 支持多子问题并行检索、去重、重排序融合。
 */
@Slf4j
@RequiredArgsConstructor
public class MultiPathRetrievalDomainService {

    private final VectorStorePort vectorStore;
    private final PromptRendererPort promptRenderer;
    private final ChatOrchestratorConfig config;

    /**
     * 多路检索入口：对每个子查询并行执行向量搜索，然后去重排序融合。
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

        // 多路并行检索
        List<VectorDocument> allDocs;
        if (subQueries.size() == 1) {
            allDocs = vectorStore.similaritySearch(
                    subQueries.getFirst(), retrievalConfig.getTopK(),
                    retrievalConfig.getSimilarityThreshold(), filter);
        } else {
            allDocs = parallelSearch(subQueries, retrievalConfig.getTopK(),
                    retrievalConfig.getSimilarityThreshold(), filter);
        }

        if (allDocs.isEmpty()) {
            log.debug("No relevant documents found for queries in knowledge base: {}", kb.getName());
            return "";
        }

        // 去重
        List<VectorDocument> uniqueDocs = deduplicateByContent(allDocs);

        // 重排序 + 截断到 topK
        List<VectorDocument> rankedDocs = rerankAndTruncate(uniqueDocs, subQueries, retrievalConfig.getTopK());

        String context = rankedDocs.stream()
                .map(VectorDocument::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        return buildRagSystemPrompt(context);
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

package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.infrastructure.ai.PromptTemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 多路检索领域服务 — 支持多子问题并行检索、去重、重排序融合。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiPathRetrievalDomainService {

    private final VectorStorePort vectorStore;
    private final PromptTemplateEngine promptTemplateEngine;
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
        Map<String, Object> filter = Map.of("knowledge_base_id", String.valueOf(kb.getId()));

        // 多路并行检索
        List<Document> allDocs;
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
        List<Document> uniqueDocs = deduplicateByContent(allDocs);

        // 重排序 + 截断到 topK
        List<Document> rankedDocs = rerankAndTruncate(uniqueDocs, subQueries, retrievalConfig.getTopK());

        String context = rankedDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        return buildRagSystemPrompt(context);
    }

    /**
     * 并行执行多个子查询的向量搜索。
     */
    private List<Document> parallelSearch(List<String> queries, int topK, double threshold,
                                          Map<String, Object> filter) {
        // 使用虚拟线程并行执行
        List<Future<List<Document>>> futures;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            futures = queries.stream()
                    .map(query -> executor.submit(() ->
                            vectorStore.similaritySearch(query, topK, threshold, filter)))
                    .toList();

            List<Document> allResults = new ArrayList<>();
            for (Future<List<Document>> future : futures) {
                try {
                    allResults.addAll(future.get(10, TimeUnit.SECONDS));
                } catch (Exception e) {
                    log.warn("Sub-query retrieval failed: {}", e.getMessage());
                }
            }
            return allResults;
        }
    }

    /**
     * 基于文本内容去重（相同文本片段只保留一个，保留得分最高的）。
     */
    private List<Document> deduplicateByContent(List<Document> docs) {
        Map<String, Document> seen = new LinkedHashMap<>();
        for (Document doc : docs) {
            String contentKey = doc.getText().trim();
            if (contentKey.length() > 200) {
                contentKey = contentKey.substring(0, 200);
            }
            seen.putIfAbsent(contentKey, doc);
        }
        return new ArrayList<>(seen.values());
    }

    /**
     * 简易重排序：按与查询的关键词重叠度打分，截断到 topK。
     * 后续可替换为 Cross-Encoder 模型。
     */
    private List<Document> rerankAndTruncate(List<Document> docs, List<String> queries, int topK) {
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
                    double scoreA = computeOverlapScore(a.getText(), queryTerms);
                    double scoreB = computeOverlapScore(b.getText(), queryTerms);
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
        return promptTemplateEngine.render(PromptTemplates.RAG_SYSTEM, Map.of("context", context));
    }
}

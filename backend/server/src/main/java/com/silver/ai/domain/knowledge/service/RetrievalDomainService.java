package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.knowledge.model.*;
import com.silver.ai.domain.knowledge.port.KeywordSearchPort;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 检索增强领域服务 - VectorStorePort is JDBC-based, stays blocking.
 * Called from boundedElastic threads in the service layer.
 * 支持 VECTOR / KEYWORD / HYBRID 三种检索模式。
 */
@Slf4j
@RequiredArgsConstructor
public class RetrievalDomainService {

    private final VectorStorePort vectorStore;
    private final KeywordSearchPort keywordSearch;
    private final PromptRendererPort promptRenderer;

    private static final int RRF_K = 60;

    public String retrieveContext(KnowledgeBase kb, String query) {
        var config = kb.getRetrievalConfig();
        Map<String, Object> filter = Map.of(VectorMetadataKeys.KNOWLEDGE_BASE_ID, String.valueOf(kb.getId()));

        log.debug("Retrieval start: kbId={}, mode={}, topK={}, threshold={}, query={}",
            kb.getId(), config.getRetrievalMode(), config.getTopK(),
            config.getSimilarityThreshold(), query);

        List<VectorDocument> results;

        switch (config.getRetrievalMode()) {
            case KEYWORD -> {
                List<KeywordSearchResult> kwResults = keywordSearch.keywordSearch(
                        query, config.getTopK(), filter);
                results = kwResults.stream()
                        .map(r -> new VectorDocument(r.content(), r.metadata()))
                        .toList();
            }
            case HYBRID -> results = hybridSearch(query, config, filter);
            default -> results = vectorStore.similaritySearch(
                query, config.getTopK(), config.getSimilarityThreshold(), filter);
        }

        if (results.isEmpty()) {
            log.debug("No relevant documents found for query in knowledge base: {}", kb.getName());
            return "";
        }

        log.debug("Retrieval resolved {} documents for kbId={} using mode={}",
            results.size(), kb.getId(), config.getRetrievalMode());

        String context = results.stream()
                .map(VectorDocument::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        return buildRagSystemPrompt(context);
    }

    public List<VectorDocument> search(KnowledgeBase kb, String query, int topK) {
        Map<String, Object> filter = Map.of(VectorMetadataKeys.KNOWLEDGE_BASE_ID, String.valueOf(kb.getId()));
        RetrievalConfig config = kb.getRetrievalConfig();
        return switch (config.getRetrievalMode()) {
            case KEYWORD -> keywordSearch.keywordSearch(query, topK, filter).stream()
                    .map(result -> new VectorDocument(result.content(), result.metadata()))
                    .toList();
            case HYBRID -> hybridSearch(query, config, filter);
            default -> vectorStore.similaritySearch(query, topK, config.getSimilarityThreshold(), filter);
        };
    }

    /**
     * 单查询混合检索：向量 + 关键词，RRF 融合。
     */
    private List<VectorDocument> hybridSearch(String query, RetrievalConfig config,
                                               Map<String, Object> filter) {
        int fetchSize = config.getTopK() * 2;

        List<VectorDocument> vectorResults = vectorStore.similaritySearch(
            query, fetchSize, config.getSimilarityThreshold(), filter);
        List<KeywordSearchResult> keywordResults = keywordSearch.keywordSearch(
                query, fetchSize, filter);

        if (vectorResults.isEmpty() && keywordResults.isEmpty()) {
            log.debug("Hybrid retrieval returned no vector or keyword results");
        }

        return rrfFuse(vectorResults, keywordResults, config.getTopK());
    }

    private List<VectorDocument> rrfFuse(List<VectorDocument> vectorResults,
                                          List<KeywordSearchResult> keywordResults,
                                          int topK) {
        Map<String, Double> rrfScores = new LinkedHashMap<>();
        Map<String, VectorDocument> docMap = new LinkedHashMap<>();

        for (int rank = 0; rank < vectorResults.size(); rank++) {
            VectorDocument doc = vectorResults.get(rank);
            String key = contentKey(doc.content());
            if (key == null) continue;
            docMap.putIfAbsent(key, doc);
            rrfScores.merge(key, 1.0 / (RRF_K + rank + 1), Double::sum);
        }

        for (int rank = 0; rank < keywordResults.size(); rank++) {
            KeywordSearchResult kw = keywordResults.get(rank);
            String key = contentKey(kw.content());
            if (key == null) continue;
            docMap.putIfAbsent(key, new VectorDocument(kw.content(), kw.metadata()));
            rrfScores.merge(key, 1.0 / (RRF_K + rank + 1), Double::sum);
        }

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> docMap.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    private String contentKey(String content) {
        if (content == null) return null;
        String trimmed = content.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private String buildRagSystemPrompt(String context) {
        return promptRenderer.render(PromptTemplates.RAG_SYSTEM, Map.of("context", context));
    }
}

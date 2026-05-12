package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.knowledge.model.*;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.domain.knowledge.port.KeywordSearchPort;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.port.RerankerPort;
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
    private final DocumentChunkRepository documentChunkRepository;
    private final RerankerPort rerankerPort;
    private final MetadataFilterParser metadataFilterParser = new MetadataFilterParser();

    private static final int RRF_K = 60;

    public String retrieveContext(KnowledgeBase kb, String query) {
        return retrieveContext(kb, query, null);
    }

    public String retrieveContext(KnowledgeBase kb, String query, String dynamicFilterExpression) {
        var config = kb.getRetrievalConfig();
        Map<String, Object> filter = buildFilter(kb, dynamicFilterExpression);
        if (metadataFilterParser.isContradictory(filter)) {
            return "";
        }

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

        int finalTopK = config.isRerankerEnabled() ? config.getRerankerTopK() : config.getTopK();
        List<VectorDocument> finalDocs = postProcess(results, query, config, finalTopK);

        String context = finalDocs.stream()
                .map(VectorDocument::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        return buildRagSystemPrompt(context);
    }

    public List<VectorDocument> search(KnowledgeBase kb, String query, int topK) {
        return search(kb, query, topK, null);
    }

    public List<VectorDocument> search(KnowledgeBase kb, String query, int topK, String dynamicFilterExpression) {
        Map<String, Object> filter = buildFilter(kb, dynamicFilterExpression);
        if (metadataFilterParser.isContradictory(filter)) {
            return List.of();
        }
        RetrievalConfig config = kb.getRetrievalConfig();
        List<VectorDocument> results = switch (config.getRetrievalMode()) {
            case KEYWORD -> keywordSearch.keywordSearch(query, topK, filter).stream()
                    .map(result -> new VectorDocument(result.content(), result.metadata()))
                    .toList();
            case HYBRID -> hybridSearch(query, config, filter);
            default -> vectorStore.similaritySearch(query, topK, config.getSimilarityThreshold(), filter);
        };
        return postProcess(results, query, config, topK);
    }

    private Map<String, Object> buildFilter(KnowledgeBase kb, String dynamicFilterExpression) {
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put(VectorMetadataKeys.KNOWLEDGE_BASE_ID, String.valueOf(kb.getId()));
        filter = metadataFilterParser.merge(filter, kb.getRetrievalConfig().getFilterExpression());
        return metadataFilterParser.merge(filter, dynamicFilterExpression);
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

    private List<VectorDocument> postProcess(List<VectorDocument> docs, String query,
                                             RetrievalConfig config, int topK) {
        List<VectorDocument> expandedDocs = expandWithWindow(docs, config.getWindowSize());
        return rerankAndTruncate(expandedDocs, query, topK, config);
    }

    private List<VectorDocument> expandWithWindow(List<VectorDocument> docs, int windowSize) {
        if (docs.isEmpty() || windowSize <= 0) {
            return docs;
        }

        List<VectorDocument> expanded = new ArrayList<>();
        Map<Long, ParentWindow> parentWindows = new LinkedHashMap<>();
        for (VectorDocument doc : docs) {
            Long parentId = readLong(doc.metadata(), VectorMetadataKeys.PARENT_CHUNK_ID);
            Integer chunkIndex = readInteger(doc.metadata(), VectorMetadataKeys.CHUNK_INDEX);
            if (parentId == null || chunkIndex == null) {
                expanded.add(doc);
                continue;
            }
            parentWindows.compute(parentId, (key, existing) -> mergeWindow(existing, doc, chunkIndex, windowSize));
        }

        for (ParentWindow window : parentWindows.values()) {
            List<DocumentChunk> chunks = documentChunkRepository
                    .findChildrenWindow(window.parentId(), window.startChunkIndex(), window.endChunkIndex())
                    .collectList()
                    .block();
            if (chunks == null || chunks.isEmpty()) {
                expanded.add(window.anchor());
                continue;
            }
            String content = chunks.stream().map(DocumentChunk::getContent).collect(Collectors.joining("\n"));
            expanded.add(new VectorDocument(content, window.anchor().metadata()));
        }
        return expanded;
    }

    private ParentWindow mergeWindow(ParentWindow existing, VectorDocument anchor, int chunkIndex, int windowSize) {
        int start = Math.max(0, chunkIndex - windowSize);
        int end = chunkIndex + windowSize;
        if (existing == null) {
            return new ParentWindow(readLong(anchor.metadata(), VectorMetadataKeys.PARENT_CHUNK_ID), start, end, anchor);
        }
        return new ParentWindow(existing.parentId(), Math.min(existing.startChunkIndex(), start),
                Math.max(existing.endChunkIndex(), end), existing.anchor());
    }

    private List<VectorDocument> rerankAndTruncate(List<VectorDocument> docs, String query, int topK,
                                                   RetrievalConfig config) {
        if (docs.size() <= topK) {
            return docs;
        }

        if (config.isRerankerEnabled() && rerankerPort != null) {
            try {
                List<String> documents = docs.stream().map(VectorDocument::content).toList();
                List<RankedResult> ranked = rerankerPort.rerank(query, documents, Math.min(topK, docs.size()));
                if (!ranked.isEmpty()) {
                    return ranked.stream()
                            .sorted(Comparator.comparingDouble(RankedResult::relevanceScore).reversed())
                            .map(RankedResult::originalIndex)
                            .filter(index -> index >= 0 && index < docs.size())
                            .map(docs::get)
                            .distinct()
                            .limit(topK)
                            .toList();
                }
            } catch (Exception ex) {
                log.warn("Reranker failed, falling back to overlap scoring: {}", ex.getMessage());
            }
        }

        Set<String> queryTerms = Arrays.stream(query.split("[\\s，。、？！,.?!]+"))
                .filter(s -> s.length() > 1)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return docs.stream()
                .sorted((a, b) -> Double.compare(computeOverlapScore(b.content(), queryTerms),
                        computeOverlapScore(a.content(), queryTerms)))
                .limit(topK)
                .toList();
    }

    private double computeOverlapScore(String text, Set<String> queryTerms) {
        if (text == null || queryTerms.isEmpty()) return 0;
        String lowerText = text.toLowerCase();
        long matchCount = queryTerms.stream().filter(lowerText::contains).count();
        return (double) matchCount / queryTerms.size();
    }

    private Long readLong(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer readInteger(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private record ParentWindow(Long parentId, int startChunkIndex, int endChunkIndex, VectorDocument anchor) {
    }

    private String buildRagSystemPrompt(String context) {
        return promptRenderer.render(PromptTemplates.RAG_SYSTEM, Map.of("context", context));
    }
}

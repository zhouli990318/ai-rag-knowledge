package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.VectorDocument;
import com.silver.ai.domain.knowledge.model.VectorMetadataKeys;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检索增强领域服务 - VectorStorePort is JDBC-based, stays blocking.
 * Called from boundedElastic threads in the service layer.
 */
@Slf4j
@RequiredArgsConstructor
public class RetrievalDomainService {

    private final VectorStorePort vectorStore;
    private final PromptRendererPort promptRenderer;

    public String retrieveContext(KnowledgeBase kb, String query) {
        var config = kb.getRetrievalConfig();
        Map<String, Object> filter = Map.of(VectorMetadataKeys.KNOWLEDGE_BASE_ID, String.valueOf(kb.getId()));

        List<VectorDocument> relevantDocs = vectorStore.similaritySearch(
                query, config.getTopK(), config.getSimilarityThreshold(), filter);

        if (relevantDocs.isEmpty()) {
            log.debug("No relevant documents found for query in knowledge base: {}", kb.getName());
            return "";
        }

        String context = relevantDocs.stream()
                .map(VectorDocument::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        return buildRagSystemPrompt(context);
    }

    public List<VectorDocument> search(KnowledgeBase kb, String query, int topK) {
        Map<String, Object> filter = Map.of(VectorMetadataKeys.KNOWLEDGE_BASE_ID, String.valueOf(kb.getId()));
        return vectorStore.similaritySearch(query, topK, kb.getRetrievalConfig().getSimilarityThreshold(), filter);
    }

    private String buildRagSystemPrompt(String context) {
        return promptRenderer.render(PromptTemplates.RAG_SYSTEM, Map.of("context", context));
    }
}

package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.infrastructure.ai.PromptTemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检索增强领域服务 - VectorStorePort is JDBC-based, stays blocking.
 * Called from boundedElastic threads in the service layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalDomainService {

    private final VectorStorePort vectorStore;
    private final PromptTemplateEngine promptTemplateEngine;

    public String retrieveContext(KnowledgeBase kb, String query) {
        var config = kb.getRetrievalConfig();
        Map<String, Object> filter = Map.of("knowledge_base_id", String.valueOf(kb.getId()));

        List<Document> relevantDocs = vectorStore.similaritySearch(
                query, config.getTopK(), config.getSimilarityThreshold(), filter);

        if (relevantDocs.isEmpty()) {
            log.debug("No relevant documents found for query in knowledge base: {}", kb.getName());
            return "";
        }

        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        return buildRagSystemPrompt(context);
    }

    public List<Document> search(KnowledgeBase kb, String query, int topK) {
        Map<String, Object> filter = Map.of("knowledge_base_id", String.valueOf(kb.getId()));
        return vectorStore.similaritySearch(query, topK, kb.getRetrievalConfig().getSimilarityThreshold(), filter);
    }

    private String buildRagSystemPrompt(String context) {
        return promptTemplateEngine.render(PromptTemplates.RAG_SYSTEM, Map.of("context", context));
    }
}

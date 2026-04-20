package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.infrastructure.ai.PromptTemplateEngine;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetrievalDomainServiceTest {

    @Test
    void retrieveContextShouldReturnRenderedPromptWhenResultsExist() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, new PromptTemplateEngine());
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(7L)
                .name("kb")
                .retrievalConfig(RetrievalConfig.builder().topK(3).similarityThreshold(0.6).build())
                .build();
        when(vectorStore.similaritySearch("query", 3, 0.6, java.util.Map.of("knowledge_base_id", "7")))
                .thenReturn(List.of(new Document("ctx1"), new Document("ctx2")));

        String result = service.retrieveContext(knowledgeBase, "query");

        assertTrue(result.contains("ctx1"));
        assertTrue(result.contains("ctx2"));
        assertTrue(result.contains("参考资料"));
    }

    @Test
    void searchShouldDelegateToVectorStore() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, new PromptTemplateEngine());
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().id(9L).build();
        List<Document> documents = List.of(new Document("result"));
        when(vectorStore.similaritySearch("q", 5, 0.7, java.util.Map.of("knowledge_base_id", "9"))).thenReturn(documents);

        List<Document> result = service.search(knowledgeBase, "q", 5);

        assertEquals(documents, result);
        verify(vectorStore).similaritySearch("q", 5, 0.7, java.util.Map.of("knowledge_base_id", "9"));
    }
}
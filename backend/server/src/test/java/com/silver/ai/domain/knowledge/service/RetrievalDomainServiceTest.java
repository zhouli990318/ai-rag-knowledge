package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.domain.knowledge.model.VectorDocument;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetrievalDomainServiceTest {

    @Test
    void retrieveContextShouldReturnRenderedPromptWhenResultsExist() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, promptRenderer);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(7L)
                .name("kb")
                .retrievalConfig(RetrievalConfig.builder().topK(3).similarityThreshold(0.6).build())
                .build();
        when(vectorStore.similaritySearch("query", 3, 0.6, Map.of("knowledge_base_id", "7")))
                .thenReturn(List.of(new VectorDocument("ctx1", Map.of()), new VectorDocument("ctx2", Map.of())));
        when(promptRenderer.render(any(), anyMap())).thenReturn("参考资料:\nctx1\nctx2");

        String result = service.retrieveContext(knowledgeBase, "query");

        assertTrue(result.contains("ctx1"));
        assertTrue(result.contains("ctx2"));
        assertTrue(result.contains("参考资料"));
    }

    @Test
    void searchShouldDelegateToVectorStore() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, promptRenderer);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().id(9L).build();
        List<VectorDocument> documents = List.of(new VectorDocument("result", Map.of()));
        when(vectorStore.similaritySearch("q", 5, 0.7, Map.of("knowledge_base_id", "9"))).thenReturn(documents);

        List<VectorDocument> result = service.search(knowledgeBase, "q", 5);

        assertEquals(documents, result);
        verify(vectorStore).similaritySearch("q", 5, 0.7, Map.of("knowledge_base_id", "9"));
    }
}
package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.knowledge.model.DocumentChunk;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.domain.knowledge.model.VectorDocument;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.domain.knowledge.port.KeywordSearchPort;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.port.RerankerPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetrievalDomainServiceTest {

    @Test
    void retrieveContextShouldReturnRenderedPromptWhenResultsExist() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        KeywordSearchPort keywordSearch = mock(KeywordSearchPort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, keywordSearch, promptRenderer,
                documentChunkRepository, null);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(7L)
                .name("kb")
                .retrievalConfig(RetrievalConfig.builder()
                        .topK(3)
                        .similarityThreshold(0.6)
                        .retrievalMode(RetrievalConfig.RetrievalMode.VECTOR)
                        .build())
                .build();
        when(vectorStore.similaritySearch("query", 3, 0.6, Map.of("knowledge_base_id", "7")))
                .thenReturn(List.of(new VectorDocument("ctx1", Map.of()), new VectorDocument("ctx2", Map.of())));
        when(promptRenderer.render(PromptTemplates.RAG_SYSTEM, Map.of("context", "ctx1\n\n---\n\nctx2")))
            .thenReturn("参考资料:\nctx1\nctx2");

        String result = service.retrieveContext(knowledgeBase, "query");

        assertTrue(result.contains("ctx1"));
        assertTrue(result.contains("ctx2"));
        assertTrue(result.contains("参考资料"));
    }

    @Test
    void searchShouldDelegateToVectorStore() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        KeywordSearchPort keywordSearch = mock(KeywordSearchPort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, keywordSearch, promptRenderer,
                documentChunkRepository, null);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(9L)
                .retrievalConfig(RetrievalConfig.builder()
                        .similarityThreshold(0.6)
                        .retrievalMode(RetrievalConfig.RetrievalMode.VECTOR)
                        .build())
                .build();
        List<VectorDocument> documents = List.of(new VectorDocument("result", Map.of()));
        when(vectorStore.similaritySearch("q", 5, 0.6, Map.of("knowledge_base_id", "9"))).thenReturn(documents);

        List<VectorDocument> result = service.search(knowledgeBase, "q", 5);

        assertEquals(documents, result);
                verify(vectorStore).similaritySearch("q", 5, 0.6, Map.of("knowledge_base_id", "9"));
    }

    @Test
    void retrieveContextShouldReturnEmptyWhenVectorModeHasNoHits() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        KeywordSearchPort keywordSearch = mock(KeywordSearchPort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, keywordSearch, promptRenderer,
                documentChunkRepository, null);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(7L)
                .name("kb")
                .retrievalConfig(RetrievalConfig.builder()
                        .topK(3)
                        .similarityThreshold(0.3)
                        .retrievalMode(RetrievalConfig.RetrievalMode.VECTOR)
                        .build())
                .build();
        when(vectorStore.similaritySearch("query", 3, 0.3, Map.of("knowledge_base_id", "7")))
                .thenReturn(List.of());

        String result = service.retrieveContext(knowledgeBase, "query");

        assertTrue(result.isEmpty());
        verify(vectorStore).similaritySearch("query", 3, 0.3, Map.of("knowledge_base_id", "7"));
        verify(keywordSearch, never()).keywordSearch("query", 3, Map.of("knowledge_base_id", "7"));
    }

    @Test
    void searchShouldUseKeywordModeWhenConfigured() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        KeywordSearchPort keywordSearch = mock(KeywordSearchPort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, keywordSearch, promptRenderer,
                documentChunkRepository, null);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(7L)
                .retrievalConfig(RetrievalConfig.builder()
                        .topK(3)
                        .similarityThreshold(0.3)
                        .retrievalMode(RetrievalConfig.RetrievalMode.KEYWORD)
                        .build())
                .build();
        when(keywordSearch.keywordSearch("query", 3, Map.of("knowledge_base_id", "7")))
                .thenReturn(List.of(new com.silver.ai.domain.knowledge.model.KeywordSearchResult("ctx-keyword", Map.of(), 0.9)));

        List<VectorDocument> result = service.search(knowledgeBase, "query", 3);

        assertEquals(List.of(new VectorDocument("ctx-keyword", Map.of())), result);
        verify(keywordSearch).keywordSearch("query", 3, Map.of("knowledge_base_id", "7"));
    }

    @Test
    void retrieveContextShouldExpandChildWindowWhenParentMetadataPresent() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        KeywordSearchPort keywordSearch = mock(KeywordSearchPort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, keywordSearch, promptRenderer,
                documentChunkRepository, null);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(7L)
                .name("kb")
                .retrievalConfig(RetrievalConfig.builder()
                        .topK(1)
                        .windowSize(1)
                        .retrievalMode(RetrievalConfig.RetrievalMode.VECTOR)
                        .build())
                .build();
        when(vectorStore.similaritySearch("query", 1, 0.6, Map.of("knowledge_base_id", "7")))
                .thenReturn(List.of(new VectorDocument("child-1", Map.of(
                        "parent_chunk_id", "99",
                        "chunk_index", 1
                ))));
        when(documentChunkRepository.findChildrenWindow(99L, 0, 2)).thenReturn(Flux.just(
                DocumentChunk.builder().content("child-0").build(),
                DocumentChunk.builder().content("child-1").build(),
                DocumentChunk.builder().content("child-2").build()
        ));
        when(promptRenderer.render(PromptTemplates.RAG_SYSTEM, Map.of("context", "child-0\nchild-1\nchild-2")))
                .thenReturn("expanded");

        String result = service.retrieveContext(knowledgeBase, "query");

        assertEquals("expanded", result);
        verify(documentChunkRepository).findChildrenWindow(99L, 0, 2);
    }

    @Test
    void searchShouldMergeKnowledgeBaseAndDynamicMetadataFilters() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        KeywordSearchPort keywordSearch = mock(KeywordSearchPort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        RetrievalDomainService service = new RetrievalDomainService(vectorStore, keywordSearch, promptRenderer,
                documentChunkRepository, null);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(9L)
                .retrievalConfig(RetrievalConfig.builder()
                        .similarityThreshold(0.6)
                        .retrievalMode(RetrievalConfig.RetrievalMode.VECTOR)
                        .filterExpression("file_type IN (md,pdf)")
                        .build())
                .build();
        List<VectorDocument> documents = List.of(new VectorDocument("result", Map.of()));
        Map<String, Object> mergedFilter = Map.of(
                "knowledge_base_id", "9",
                "file_type", List.of("md", "pdf"),
                "file_name", "README.md"
        );
        when(vectorStore.similaritySearch("q", 5, 0.6, mergedFilter)).thenReturn(documents);

        List<VectorDocument> result = service.search(knowledgeBase, "q", 5, "file_name = README.md");

        assertEquals(documents, result);
        verify(vectorStore).similaritySearch("q", 5, 0.6, mergedFilter);
    }
}
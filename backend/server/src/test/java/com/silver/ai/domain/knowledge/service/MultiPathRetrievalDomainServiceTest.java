package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.KeywordSearchResult;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.domain.knowledge.model.VectorDocument;
import com.silver.ai.domain.knowledge.port.KeywordSearchPort;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiPathRetrievalDomainServiceTest {

    @Test
        void retrieveAndFuseShouldReturnEmptyWhenVectorModeHasNoHits() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        KeywordSearchPort keywordSearch = mock(KeywordSearchPort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .deduplicatePrefixLength(200)
                .retrievalTimeoutSeconds(5)
                .build();
        MultiPathRetrievalDomainService service = new MultiPathRetrievalDomainService(
                vectorStore, keywordSearch, promptRenderer, config);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(2L)
                .name("kb")
                .retrievalConfig(RetrievalConfig.builder()
                        .topK(3)
                        .similarityThreshold(0.3)
                        .retrievalMode(RetrievalConfig.RetrievalMode.VECTOR)
                        .build())
                .build();
        when(vectorStore.similaritySearch("小明是谁", 3, 0.3, Map.of("knowledge_base_id", "2")))
                .thenReturn(List.of());

        String result = service.retrieveAndFuse(
                knowledgeBase, List.of("小明是谁"), IntentResult.defaultRetrieval());

        assertTrue(result.isEmpty());
        verify(vectorStore).similaritySearch("小明是谁", 3, 0.3, Map.of("knowledge_base_id", "2"));
        verify(keywordSearch, never()).keywordSearch("小明是谁", 3, Map.of("knowledge_base_id", "2"));
    }

    @Test
    void retrieveAndFuseShouldUseKeywordModeWhenConfigured() {
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        KeywordSearchPort keywordSearch = mock(KeywordSearchPort.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .deduplicatePrefixLength(200)
                .retrievalTimeoutSeconds(5)
                .build();
        MultiPathRetrievalDomainService service = new MultiPathRetrievalDomainService(
                vectorStore, keywordSearch, promptRenderer, config);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(2L)
                .name("kb")
                .retrievalConfig(RetrievalConfig.builder()
                        .topK(3)
                        .similarityThreshold(0.3)
                        .retrievalMode(RetrievalConfig.RetrievalMode.KEYWORD)
                        .build())
                .build();
        when(keywordSearch.keywordSearch("小明是谁", 3, Map.of("knowledge_base_id", "2")))
                .thenReturn(List.of(new KeywordSearchResult("小明是研发工程师", Map.of(), 0.8)));
        when(promptRenderer.render(any(), anyMap())).thenReturn("参考资料:\n小明是研发工程师");

        String result = service.retrieveAndFuse(
                knowledgeBase, List.of("小明是谁"), IntentResult.defaultRetrieval());

        assertTrue(result.contains("小明是研发工程师"));
        verify(keywordSearch).keywordSearch("小明是谁", 3, Map.of("knowledge_base_id", "2"));
                verify(vectorStore, never()).similaritySearch("小明是谁", 3, 0.3, Map.of("knowledge_base_id", "2"));
    }
}
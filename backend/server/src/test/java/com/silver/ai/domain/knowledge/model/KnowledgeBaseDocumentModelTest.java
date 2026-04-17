package com.silver.ai.domain.knowledge.model;

import com.silver.ai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeBaseDocumentModelTest {

    @Test
    void knowledgeBaseShouldUpdateStateAndGuardInactiveAccess() {
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().name("kb").documentCount(1).build();
        ChunkStrategy strategy = ChunkStrategy.builder().chunkSize(256).chunkOverlap(32).build();
        RetrievalConfig config = RetrievalConfig.builder().topK(8).similarityThreshold(0.55).build();

        knowledgeBase.incrementDocumentCount();
        knowledgeBase.decrementDocumentCount();
        knowledgeBase.updateChunkStrategy(strategy);
        knowledgeBase.updateRetrievalConfig(config);
        knowledgeBase.updateInfo("kb-2", "desc");
        knowledgeBase.updateEmbeddingConfig(2L, "embed-model", 768);

        assertEquals(1, knowledgeBase.getDocumentCount());
        assertEquals(strategy, knowledgeBase.getChunkStrategy());
        assertEquals(config, knowledgeBase.getRetrievalConfig());
        assertEquals("kb-2", knowledgeBase.getName());
        assertEquals("desc", knowledgeBase.getDescription());
        assertEquals(2L, knowledgeBase.getEmbeddingProviderId());
        assertEquals("embed-model", knowledgeBase.getEmbeddingModel());
        assertEquals(768, knowledgeBase.getEmbeddingDimensions());

        KnowledgeBase inactive = KnowledgeBase.builder().name("disabled").active(false).build();
        assertThrows(BusinessException.class, inactive::ensureActive);
    }

    @Test
    void documentShouldTransitionAcrossStatuses() {
        Document document = Document.builder().build();

        document.markProcessing();
        assertEquals(DocumentStatus.PROCESSING, document.getStatus());

        document.markIndexed(4);
        assertEquals(DocumentStatus.INDEXED, document.getStatus());
        assertEquals(4, document.getChunkCount());
        assertNull(document.getErrorMessage());

        document.markFailed("parse error");
        assertEquals(DocumentStatus.FAILED, document.getStatus());
        assertEquals("parse error", document.getErrorMessage());
    }
}
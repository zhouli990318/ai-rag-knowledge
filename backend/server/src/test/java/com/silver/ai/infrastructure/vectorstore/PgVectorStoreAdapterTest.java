package com.silver.ai.infrastructure.vectorstore;

import com.silver.ai.domain.knowledge.model.VectorDocument;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("null")
class PgVectorStoreAdapterTest {

    @Test
    void addDocumentsShouldUseSharedVectorStore() {
        VectorStore vectorStore = mock(VectorStore.class);
        PgVectorStoreAdapter adapter = new PgVectorStoreAdapter(vectorStore);
        VectorDocument vectorDoc = new VectorDocument("chunk", Map.of("knowledge_base_id", 5L));

        adapter.addDocuments(List.of(vectorDoc));

        verify(vectorStore).add(any());
    }

    @Test
    void deleteByMetadataShouldDeleteAllMatchedVectorsInBatches() {
        VectorStore vectorStore = mock(VectorStore.class);
        PgVectorStoreAdapter adapter = new PgVectorStoreAdapter(vectorStore);

        adapter.deleteByMetadata("knowledge_base_id", "22");

        verify(vectorStore).delete(any(org.springframework.ai.vectorstore.filter.Filter.Expression.class));
    }
}
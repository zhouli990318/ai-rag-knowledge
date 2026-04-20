package com.silver.ai.infrastructure.vectorstore;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("null")
class PgVectorStoreAdapterTest {

    @Test
    void addDocumentsShouldUseSharedVectorStore() {
        VectorStore vectorStore = mock(VectorStore.class);
        PgVectorStoreAdapter adapter = new PgVectorStoreAdapter(vectorStore);
        Document document = new Document("chunk", Map.of("knowledge_base_id", 5L));

        adapter.addDocuments(List.of(document));

        verify(vectorStore).add(List.of(document));
    }
}
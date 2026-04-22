package com.silver.ai.infrastructure.vectorstore;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void deleteByMetadataShouldDeleteAllMatchedVectorsInBatches() {
        VectorStore vectorStore = mock(VectorStore.class);
        PgVectorStoreAdapter adapter = new PgVectorStoreAdapter(vectorStore);
        Document batchOneDoc = mock(Document.class);
        Document batchTwoDoc = mock(Document.class);
        when(batchOneDoc.getId()).thenReturn("doc-1");
        when(batchTwoDoc.getId()).thenReturn("doc-2");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(batchOneDoc))
                .thenReturn(List.of(batchTwoDoc))
                .thenReturn(List.of());

        adapter.deleteByMetadata("knowledge_base_id", "22");

        verify(vectorStore, times(3)).similaritySearch(any(SearchRequest.class));
        verify(vectorStore).delete(org.mockito.ArgumentMatchers.<List<String>>argThat(ids -> ids.size() == 1 && ids.contains("doc-1")));
        verify(vectorStore).delete(org.mockito.ArgumentMatchers.<List<String>>argThat(ids -> ids.size() == 1 && ids.contains("doc-2")));
    }
}
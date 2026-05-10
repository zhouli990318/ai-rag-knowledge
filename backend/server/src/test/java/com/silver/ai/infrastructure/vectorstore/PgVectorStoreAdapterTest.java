package com.silver.ai.infrastructure.vectorstore;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.silver.ai.domain.knowledge.model.VectorDocument;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void similaritySearchShouldLogRawSimilarityScoresAtDebugLevel() {
        VectorStore vectorStore = mock(VectorStore.class);
        PgVectorStoreAdapter adapter = new PgVectorStoreAdapter(vectorStore);
        Document hit = Document.builder()
                .text("小明是小红的朋友")
                .metadata(Map.of("knowledge_base_id", "2", "document_id", "16", "chunk_index", 0))
                .score(0.628205207677949)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit));

        Logger logger = (Logger) LoggerFactory.getLogger(PgVectorStoreAdapter.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);

        try {
            List<VectorDocument> results = adapter.similaritySearch(
                    "小明", 5, 0.7, Map.of("knowledge_base_id", "2"));

            assertTrue(results.size() == 1);
            assertTrue(appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("rawSimilarity=0.628205")));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
        }
    }
}
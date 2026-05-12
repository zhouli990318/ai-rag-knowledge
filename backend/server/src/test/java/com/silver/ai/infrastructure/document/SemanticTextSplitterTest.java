package com.silver.ai.infrastructure.document;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.provider.port.EmbeddingPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticTextSplitterTest {

    @Test
    void splitShouldBreakWhenAdjacentSentenceSimilarityDropsBelowThreshold() {
        EmbeddingPort embeddingPort = mock(EmbeddingPort.class);
        SemanticTextSplitter splitter = new SemanticTextSplitter(embeddingPort);
        ChunkStrategy strategy = ChunkStrategy.builder()
                .type(ChunkStrategy.ChunkType.SEMANTIC)
                .chunkSize(220)
                .chunkOverlap(20)
                .semanticThreshold(0.5)
                .build();
        String text = "Alpha topic sentence one with enough padding to exceed the minimum semantic chunk length. "
                + "Alpha follow up sentence keeps the same topic and adds more detail for the first chunk. "
                + "Beta topic starts here with very different meaning and enough extra words to stand alone.";

        when(embeddingPort.embedBatch(eq(7L), any())).thenReturn(List.of(
                new float[]{1F, 0F},
                new float[]{0.9F, 0.1F},
                new float[]{0F, 1F}
        ));

        List<String> chunks = splitter.split(text, strategy, 7L);

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).contains("Alpha topic sentence one"));
        assertTrue(chunks.get(0).contains("Alpha follow up sentence"));
        assertTrue(chunks.get(1).contains("Beta topic starts here"));
    }

    @Test
    void splitShouldFallbackToFixedSizeWhenEmbeddingCountMismatches() {
        EmbeddingPort embeddingPort = mock(EmbeddingPort.class);
        SemanticTextSplitter splitter = new SemanticTextSplitter(embeddingPort);
        ChunkStrategy strategy = ChunkStrategy.builder()
                .type(ChunkStrategy.ChunkType.SEMANTIC)
                .chunkSize(10)
                .chunkOverlap(0)
                .semanticThreshold(0.5)
                .build();
        String text = "Sentence one. Sentence two.";

        when(embeddingPort.embedBatch(eq(1L), any())).thenReturn(List.of(new float[]{1F, 0F}));

        List<String> chunks = splitter.split(text, strategy, 1L);

        assertEquals(List.of("Sentence o", "ne. Senten", "ce two."), chunks);
    }
}
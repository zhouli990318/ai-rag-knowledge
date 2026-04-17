package com.silver.ai.infrastructure.document;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurableTextSplitterTest {

    private final ConfigurableTextSplitter splitter = new ConfigurableTextSplitter();

    @Test
    void splitShouldReturnEmptyForBlankInput() {
        assertTrue(splitter.split("   ", ChunkStrategy.defaultStrategy()).isEmpty());
    }

    @Test
    void splitShouldSupportFixedSizeStrategy() {
        List<String> chunks = splitter.split("abcdefgh", ChunkStrategy.builder()
                .type(ChunkStrategy.ChunkType.FIXED_SIZE)
                .chunkSize(4)
                .chunkOverlap(0)
                .build());

        assertEquals(List.of("abcd", "efgh"), chunks);
    }

    @Test
    void splitShouldSupportParagraphStrategy() {
        List<String> chunks = splitter.split("p1\n\n p2\n\n p3", ChunkStrategy.builder()
                .type(ChunkStrategy.ChunkType.PARAGRAPH)
                .chunkSize(10)
                .chunkOverlap(0)
                .build());

        assertEquals(List.of("p1 p2 p3"), chunks);
    }

    @Test
    void splitShouldSupportRecursiveStrategy() {
        String text = "This is a very long paragraph that should be split recursively because it exceeds the limit.";

        List<String> chunks = splitter.split(text, ChunkStrategy.builder()
                .type(ChunkStrategy.ChunkType.RECURSIVE)
                .chunkSize(20)
                .chunkOverlap(5)
                .build());

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
    }

    @Test
    void splitAllShouldFlattenAndFilterBlankChunks() {
        List<String> chunks = splitter.splitAll(List.of("abcd", "", "efgh"), ChunkStrategy.builder()
                .type(ChunkStrategy.ChunkType.FIXED_SIZE)
                .chunkSize(2)
                .chunkOverlap(0)
                .build());

        assertEquals(List.of("ab", "cd", "ef", "gh"), chunks);
    }
}
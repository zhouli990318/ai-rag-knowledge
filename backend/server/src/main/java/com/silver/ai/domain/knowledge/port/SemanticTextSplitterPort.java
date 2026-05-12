package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;

import java.util.List;

/**
 * 语义分块端口。
 */
public interface SemanticTextSplitterPort {

    List<String> split(String text, ChunkStrategy strategy, Long embeddingProviderId);

    List<String> splitAll(List<String> texts, ChunkStrategy strategy, Long embeddingProviderId);
}
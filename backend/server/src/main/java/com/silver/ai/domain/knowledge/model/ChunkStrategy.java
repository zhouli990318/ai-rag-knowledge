package com.silver.ai.domain.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 分片策略 — 值对象（不可变）
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkStrategy {

    @Builder.Default
    private ChunkType type = ChunkType.FIXED_SIZE;
    @Builder.Default
    private int chunkSize = 800;
    @Builder.Default
    private int chunkOverlap = 200;
    @Builder.Default
    private double semanticThreshold = 0.5;
    @Builder.Default
    private int childChunkSize = 200;
    @Builder.Default
    private int windowSize = 2;
    @Builder.Default
    private boolean enableParentChild = false;

    public enum ChunkType {
        FIXED_SIZE,
        SENTENCE,
        PARAGRAPH,
        RECURSIVE,
        SEMANTIC
    }

    public static ChunkStrategy defaultStrategy() {
        return ChunkStrategy.builder().build();
    }
}

package com.silver.ai.domain.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    public enum ChunkLevel {
        PARENT,
        CHILD
    }

    private Long id;
    private Long documentId;
    private Long parentId;
    private int chunkIndex;
    private String content;
    @Builder.Default
    private ChunkLevel chunkLevel = ChunkLevel.CHILD;
    @Builder.Default
    private Map<String, Object> metadata = Map.of();
    private LocalDateTime createdAt;
}

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

    private Long id;
    private Long documentId;
    private int chunkIndex;
    private String content;
    @Builder.Default
    private Map<String, Object> metadata = Map.of();
    private LocalDateTime createdAt;
}

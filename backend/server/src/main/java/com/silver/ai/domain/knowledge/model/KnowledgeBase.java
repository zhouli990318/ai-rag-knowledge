package com.silver.ai.domain.knowledge.model;

import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库 — 聚合根
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase {

    private Long id;
    private String name;
    private String description;
    /** 使用的嵌入模型提供商 ID */
    private Long embeddingProviderId;
    private String embeddingModel;
    @Builder.Default
    private Integer embeddingDimensions = 1536;
    @Builder.Default
    private ChunkStrategy chunkStrategy = ChunkStrategy.defaultStrategy();
    @Builder.Default
    private RetrievalConfig retrievalConfig = RetrievalConfig.defaultConfig();
    @Builder.Default
    private int documentCount = 0;
    @Builder.Default
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void incrementDocumentCount() {
        this.documentCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void decrementDocumentCount() {
        if (this.documentCount > 0) {
            this.documentCount--;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void updateChunkStrategy(ChunkStrategy strategy) {
        this.chunkStrategy = strategy;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRetrievalConfig(RetrievalConfig config) {
        this.retrievalConfig = config;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateEmbeddingConfig(Long providerId, String model, Integer dimensions) {
        this.embeddingProviderId = providerId;
        this.embeddingModel = model;
        this.embeddingDimensions = dimensions;
        this.updatedAt = LocalDateTime.now();
    }

    public void ensureActive() {
        if (!active) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "知识库已停用: " + name);
        }
    }
}

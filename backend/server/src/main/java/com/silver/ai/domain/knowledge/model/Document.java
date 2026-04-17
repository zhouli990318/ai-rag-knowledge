package com.silver.ai.domain.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    private Long id;
    private Long knowledgeBaseId;
    private String fileName;
    private String fileType;
    private long fileSize;
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;
    @Builder.Default
    private int chunkCount = 0;
    private String errorMessage;
    private LocalDateTime createdAt;

    public void markProcessing() {
        this.status = DocumentStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void markIndexed(int chunkCount) {
        this.status = DocumentStatus.INDEXED;
        this.chunkCount = chunkCount;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}

package com.silver.ai.domain.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ETL 任务 — 实体，跟踪文档入库的分阶段状态。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlTask {

    private Long id;
    private Long knowledgeBaseId;
    private Long documentId;
    @Builder.Default
    private EtlTaskType taskType = EtlTaskType.DOCUMENT;
    @Builder.Default
    private EtlStage currentStage = EtlStage.PENDING;
    @Builder.Default
    private int progress = 0;
    private String errorMessage;
    private String metadataJson;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    public enum EtlTaskType {
        DOCUMENT, GIT_IMPORT, URL_CRAWL
    }

    public enum EtlStage {
        PENDING, FETCH, PARSE, ENHANCE, CHUNK, VECTORIZE, WRITE, COMPLETED, FAILED
    }

    public void advanceTo(EtlStage stage, int progress) {
        this.currentStage = stage;
        this.progress = progress;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.currentStage = EtlStage.COMPLETED;
        this.progress = 100;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.currentStage = EtlStage.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}

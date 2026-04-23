package com.silver.ai.infrastructure.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("etl_task")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlTaskEntity {

    @Id
    private Long id;

    @Column("knowledge_base_id")
    private Long knowledgeBaseId;

    @Column("document_id")
    private Long documentId;

    @Column("task_type")
    @Builder.Default
    private String taskType = "DOCUMENT";

    @Column("current_stage")
    @Builder.Default
    private String currentStage = "PENDING";

    @Builder.Default
    private int progress = 0;

    @Column("error_message")
    private String errorMessage;

    @Column("metadata_json")
    private String metadataJson;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}

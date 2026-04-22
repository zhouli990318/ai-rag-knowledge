package com.silver.ai.infrastructure.persistence.entity;

import com.silver.ai.domain.knowledge.model.DocumentStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("document")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {

    @Id
    private Long id;

    @Column("knowledge_base_id")
    private Long knowledgeBaseId;

    @Column("file_name")
    private String fileName;

    @Column("file_type")
    private String fileType;

    @Column("file_size")
    private long fileSize;

    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column("chunk_count")
    @Builder.Default
    private int chunkCount = 0;

    @Column("error_message")
    private String errorMessage;

    @Column("created_at")
    private LocalDateTime createdAt;
}

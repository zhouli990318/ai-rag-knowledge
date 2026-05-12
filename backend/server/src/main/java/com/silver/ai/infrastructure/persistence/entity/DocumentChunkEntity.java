package com.silver.ai.infrastructure.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("document_chunk")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunkEntity {

    @Id
    private Long id;

    @Column("document_id")
    private Long documentId;

    @Column("parent_id")
    private Long parentId;

    @Column("chunk_index")
    private int chunkIndex;

    @Column("chunk_level")
    private String chunkLevel;

    private String content;

    @Column("metadata_json")
    private String metadataJson;

    @Column("created_at")
    private LocalDateTime createdAt;
}

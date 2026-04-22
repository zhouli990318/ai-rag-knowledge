package com.silver.ai.infrastructure.persistence.entity;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("knowledge_base")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseEntity {

    @Id
    private Long id;

    private String name;

    private String description;

    @Column("embedding_provider_id")
    private Long embeddingProviderId;

    @Column("embedding_model")
    private String embeddingModel;

    @Column("embedding_dimensions")
    @Builder.Default
    private Integer embeddingDimensions = 1536;

    @Column("chunk_type")
    @Builder.Default
    private ChunkStrategy.ChunkType chunkType = ChunkStrategy.ChunkType.FIXED_SIZE;

    @Column("chunk_size")
    @Builder.Default
    private int chunkSize = 800;

    @Column("chunk_overlap")
    @Builder.Default
    private int chunkOverlap = 200;

    @Column("retrieval_top_k")
    @Builder.Default
    private int retrievalTopK = 5;

    @Column("retrieval_threshold")
    @Builder.Default
    private double retrievalThreshold = 0.7;

    @Column("retrieval_filter")
    private String retrievalFilter;

    @Column("document_count")
    @Builder.Default
    private int documentCount = 0;

    @Builder.Default
    private boolean active = true;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}

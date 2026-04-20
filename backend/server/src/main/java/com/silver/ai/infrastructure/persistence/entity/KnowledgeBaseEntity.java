package com.silver.ai.infrastructure.persistence.entity;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_base")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "embedding_provider_id")
    private Long embeddingProviderId;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "embedding_dimensions")
    @Builder.Default
    private Integer embeddingDimensions = 1536;

    // Chunk strategy as flat columns
    @Column(name = "chunk_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChunkStrategy.ChunkType chunkType = ChunkStrategy.ChunkType.FIXED_SIZE;

    @Column(name = "chunk_size")
    @Builder.Default
    private int chunkSize = 800;

    @Column(name = "chunk_overlap")
    @Builder.Default
    private int chunkOverlap = 200;

    // Retrieval config as flat columns
    @Column(name = "retrieval_top_k")
    @Builder.Default
    private int retrievalTopK = 5;

    @Column(name = "retrieval_threshold")
    @Builder.Default
    private double retrievalThreshold = 0.7;

    @Column(name = "retrieval_filter")
    private String retrievalFilter;

    @Column(name = "document_count")
    @Builder.Default
    private int documentCount = 0;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

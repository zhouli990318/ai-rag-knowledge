package com.silver.ai.infrastructure.persistence.entity;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
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

    @Column("semantic_threshold")
    @Builder.Default
    private double semanticThreshold = 0.5;

    @Column("child_chunk_size")
    @Builder.Default
    private int childChunkSize = 200;

    @Column("window_size")
    @Builder.Default
    private int windowSize = 2;

    @Column("enable_parent_child")
    @Builder.Default
    private boolean enableParentChild = false;

    @Column("retrieval_top_k")
    @Builder.Default
    private int retrievalTopK = 5;

    @Column("retrieval_threshold")
    @Builder.Default
    private double retrievalThreshold = 0.6;

    @Column("retrieval_filter")
    private String retrievalFilter;

    @Column("retrieval_mode")
    @Builder.Default
    private RetrievalConfig.RetrievalMode retrievalMode = RetrievalConfig.RetrievalMode.HYBRID;

    @Column("keyword_weight")
    @Builder.Default
    private double keywordWeight = 0.3;

    @Column("vector_weight")
    @Builder.Default
    private double vectorWeight = 0.7;

    @Column("reranker_enabled")
    @Builder.Default
    private boolean rerankerEnabled = false;

    @Column("reranker_top_k")
    @Builder.Default
    private int rerankerTopK = 5;

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

package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.silver.ai.infrastructure.persistence.jpa.JpaKnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class KnowledgeBaseRepositoryAdapter implements KnowledgeBaseRepository {

    private final JpaKnowledgeBaseRepository jpa;

    @Override
    public KnowledgeBase save(KnowledgeBase kb) {
        KnowledgeBaseEntity entity = toEntity(kb);
        entity = jpa.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<KnowledgeBase> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<KnowledgeBase> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    private KnowledgeBaseEntity toEntity(KnowledgeBase d) {
        return KnowledgeBaseEntity.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .embeddingProviderId(d.getEmbeddingProviderId())
                .embeddingModel(d.getEmbeddingModel())
                .embeddingDimensions(d.getEmbeddingDimensions())
                .chunkType(d.getChunkStrategy().getType())
                .chunkSize(d.getChunkStrategy().getChunkSize())
                .chunkOverlap(d.getChunkStrategy().getChunkOverlap())
                .retrievalTopK(d.getRetrievalConfig().getTopK())
                .retrievalThreshold(d.getRetrievalConfig().getSimilarityThreshold())
                .retrievalFilter(d.getRetrievalConfig().getFilterExpression())
                .documentCount(d.getDocumentCount())
                .active(d.isActive())
                .build();
    }

    private KnowledgeBase toDomain(KnowledgeBaseEntity e) {
        return KnowledgeBase.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .embeddingProviderId(e.getEmbeddingProviderId())
                .embeddingModel(e.getEmbeddingModel())
                .embeddingDimensions(e.getEmbeddingDimensions())
                .chunkStrategy(ChunkStrategy.builder()
                        .type(e.getChunkType())
                        .chunkSize(e.getChunkSize())
                        .chunkOverlap(e.getChunkOverlap())
                        .build())
                .retrievalConfig(RetrievalConfig.builder()
                        .topK(e.getRetrievalTopK())
                        .similarityThreshold(e.getRetrievalThreshold())
                        .filterExpression(e.getRetrievalFilter())
                        .build())
                .documentCount(e.getDocumentCount())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

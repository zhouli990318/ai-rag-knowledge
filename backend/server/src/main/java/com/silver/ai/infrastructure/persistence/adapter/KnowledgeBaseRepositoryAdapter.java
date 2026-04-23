package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcKnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class KnowledgeBaseRepositoryAdapter implements KnowledgeBaseRepository {

    private final R2dbcKnowledgeBaseRepository r2dbc;

    @Override
    public Mono<KnowledgeBase> save(KnowledgeBase kb) {
        KnowledgeBaseEntity entity = toEntity(kb);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            return r2dbc.save(entity).map(this::toDomain);
        }
        entity.setUpdatedAt(now);
        return r2dbc.findById(entity.getId())
                .flatMap(existing -> {
                    entity.setCreatedAt(existing.getCreatedAt());
                    return r2dbc.save(entity);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    entity.setCreatedAt(now);
                    return r2dbc.save(entity);
                }))
                .map(this::toDomain);
    }

    @Override
    public Mono<KnowledgeBase> findById(Long id) {
        return r2dbc.findById(id).map(this::toDomain);
    }

    @Override
    public Flux<KnowledgeBase> findAll() {
        return r2dbc.findAll().map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return r2dbc.deleteById(id);
    }

    @Override
    public Mono<Boolean> existsByName(String name) {
        return r2dbc.existsByName(name);
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

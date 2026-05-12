package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.DocumentChunkEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface R2dbcDocumentChunkRepository extends ReactiveCrudRepository<DocumentChunkEntity, Long> {
    Flux<DocumentChunkEntity> findByDocumentIdOrderByChunkIndexAsc(Long documentId);
    Flux<DocumentChunkEntity> findByParentIdOrderByChunkIndexAsc(Long parentId);
    Flux<DocumentChunkEntity> findByParentIdAndChunkIndexBetweenOrderByChunkIndexAsc(Long parentId, int startChunkIndex, int endChunkIndex);
    Mono<Void> deleteByDocumentId(Long documentId);
}

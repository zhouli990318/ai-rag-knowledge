package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.DocumentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface R2dbcDocumentRepository extends ReactiveCrudRepository<DocumentEntity, Long> {
    Flux<DocumentEntity> findByKnowledgeBaseId(Long knowledgeBaseId);
    Mono<Void> deleteByKnowledgeBaseId(Long knowledgeBaseId);
}

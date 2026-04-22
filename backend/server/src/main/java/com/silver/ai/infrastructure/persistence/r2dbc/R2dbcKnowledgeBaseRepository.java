package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.KnowledgeBaseEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface R2dbcKnowledgeBaseRepository extends ReactiveCrudRepository<KnowledgeBaseEntity, Long> {
    Mono<Boolean> existsByName(String name);
}

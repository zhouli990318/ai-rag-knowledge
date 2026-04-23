package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.EtlTaskEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface R2dbcEtlTaskRepository extends ReactiveCrudRepository<EtlTaskEntity, Long> {

    Flux<EtlTaskEntity> findByKnowledgeBaseId(Long knowledgeBaseId);

    Flux<EtlTaskEntity> findByCurrentStage(String currentStage);
}

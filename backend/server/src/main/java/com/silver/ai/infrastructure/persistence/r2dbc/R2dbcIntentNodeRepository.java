package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.IntentNodeEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface R2dbcIntentNodeRepository extends ReactiveCrudRepository<IntentNodeEntity, Long> {

    Flux<IntentNodeEntity> findByParentId(Long parentId);

    Flux<IntentNodeEntity> findByStatus(String status);

    Flux<IntentNodeEntity> findByStatusAndLevel(String status, int level);
}

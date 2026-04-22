package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.ConversationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface R2dbcConversationRepository extends ReactiveCrudRepository<ConversationEntity, Long> {
    Flux<ConversationEntity> findAllByOrderByUpdatedAtDesc();
}

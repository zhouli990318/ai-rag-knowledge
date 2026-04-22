package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.ChatMessageEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface R2dbcChatMessageRepository extends ReactiveCrudRepository<ChatMessageEntity, Long> {
    Flux<ChatMessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    Mono<Void> deleteByConversationId(Long conversationId);
}

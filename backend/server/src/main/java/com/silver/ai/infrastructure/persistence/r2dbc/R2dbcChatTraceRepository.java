package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.ChatTraceEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface R2dbcChatTraceRepository extends ReactiveCrudRepository<ChatTraceEntity, Long> {

    Mono<ChatTraceEntity> findByTraceId(String traceId);

    Flux<ChatTraceEntity> findByConversationId(Long conversationId);
}

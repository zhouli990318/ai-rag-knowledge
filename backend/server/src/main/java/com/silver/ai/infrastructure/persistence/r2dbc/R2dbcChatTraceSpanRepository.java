package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.infrastructure.persistence.entity.ChatTraceSpanEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface R2dbcChatTraceSpanRepository extends ReactiveCrudRepository<ChatTraceSpanEntity, Long> {

    Flux<ChatTraceSpanEntity> findByTraceId(String traceId);
}

package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.ChatTraceContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 链路追踪仓储端口
 */
public interface ChatTraceRepository {

    Mono<ChatTraceContext> save(ChatTraceContext trace);

    Mono<ChatTraceContext> findByTraceId(String traceId);

    Flux<ChatTraceContext> findByConversationId(Long conversationId);
}

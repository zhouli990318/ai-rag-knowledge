package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.ChatTraceContext;
import com.silver.ai.domain.chat.port.ChatTraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 链路追踪应用服务 — 为 TraceController 提供应用层编排。
 */
@Service
@RequiredArgsConstructor
public class TraceAppService {

    private final ChatTraceRepository chatTraceRepository;

    public Mono<ChatTraceContext> getTrace(String traceId) {
        return chatTraceRepository.findByTraceId(traceId);
    }

    public Flux<ChatTraceContext> getTracesByConversation(Long conversationId) {
        return chatTraceRepository.findByConversationId(conversationId);
    }
}

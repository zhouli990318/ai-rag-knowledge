package com.silver.ai.domain.provider.port;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AI 对话模型端口 — 领域层定义，基础设施层实现
 */
public interface ChatModelPort {

    /**
     * 流式对话
     */
    Flux<String> streamChat(Long providerId, String model, List<Message> messages, List<ToolCallback> toolCallbacks);

    /**
     * 非流式对话（响应式）
     */
    Mono<String> chat(Long providerId, String model, List<Message> messages, List<ToolCallback> toolCallbacks);
}

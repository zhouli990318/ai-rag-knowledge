package com.silver.ai.domain.chat.model;

/**
 * 领域消息 — 用于 AI 对话端口的轻量值对象，替代 Spring AI Message。
 * 与 ChatMessage（持久化实体）不同，DomainMessage 仅承载发送给 LLM 的消息内容。
 */
public record DomainMessage(
        MessageRole role,
        String content
) {
}

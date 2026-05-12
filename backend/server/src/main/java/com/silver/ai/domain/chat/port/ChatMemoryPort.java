package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.DomainMessage;

import java.util.List;

/**
 * 聊天记忆管理端口 — 领域层定义，基础设施层实现。
 * 负责分层记忆构建、上下文提取和摘要生成。
 */
public interface ChatMemoryPort {

    /**
     * 构建发送给 LLM 的消息列表（分层记忆版本）。
     */
    List<DomainMessage> buildMessages(Conversation conversation, String systemPrompt, int windowSize);

    /**
     * 提取最近 N 条消息文本作为查询上下文。
     */
    List<String> extractRecentContext(Conversation conversation, int rounds);

    /**
     * 构建用于意图识别和查询规划的上下文。
     * 该上下文应包含摘要与最近历史轮次，但排除当前最后一条用户消息，避免重复改写。
     */
    List<String> buildPlanningContext(Conversation conversation, int rounds);

    /**
     * 生成会话摘要请求文本。
     */
    String buildSummaryPrompt(Conversation conversation);

    /**
     * 判断是否需要摘要压缩。
     */
    boolean needsSummary(Conversation conversation);
}

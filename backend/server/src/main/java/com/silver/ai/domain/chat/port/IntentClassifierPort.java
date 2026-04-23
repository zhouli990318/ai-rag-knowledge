package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.IntentResult;

import java.util.List;

/**
 * 意图分类器端口 — 领域层纯接口，由 infrastructure 提供实现。
 */
public interface IntentClassifierPort {

    /**
     * 对用户消息进行意图分类。
     *
     * @param userMessage       当前用户消息
     * @param conversationContext 近几轮对话上下文（用于指代消解等）
     * @return 意图分类结果
     */
    IntentResult classify(String userMessage, List<String> conversationContext);
}

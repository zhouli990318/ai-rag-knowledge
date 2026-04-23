package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.chat.model.ChatMessage;
import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.provider.port.ChatModelPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 分层记忆管理器 — 近 N 轮完整消息 + 历史摘要。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ChatMemoryManager {

    private final ChatOrchestratorConfig orchestratorConfig;
    private final PromptTemplateEngine promptTemplateEngine;

    private static final int DEFAULT_MAX_CONTEXT_CHARS = 12000;

    /**
     * 构建发送给 LLM 的消息列表（分层记忆版本）。
     * <p>
     * 结构：SystemPrompt → [摘要段落] → 最近 N 轮消息
     */
    public List<Message> buildMessages(Conversation conversation, String systemPrompt, int windowSize) {
        int maxChars = orchestratorConfig != null
                ? orchestratorConfig.getMemoryMaxChars() : DEFAULT_MAX_CONTEXT_CHARS;

        List<Message> messages = new ArrayList<>();

        // 1. 系统提示
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 2. 摘要（如果有）
        if (conversation.getSummary() != null && !conversation.getSummary().isBlank()) {
            messages.add(new SystemMessage(
                    "以下是之前对话的摘要，请结合摘要理解用户的上下文：\n" + conversation.getSummary()));
        }

        // 3. 最近 N 轮完整消息（字符限制内）
        List<ChatMessage> history = conversation.getContextMessages(windowSize);
        Deque<ChatMessage> retained = new ArrayDeque<>();
        int totalChars = 0;

        for (int index = history.size() - 1; index >= 0; index--) {
            ChatMessage message = history.get(index);
            int messageChars = estimateMessageChars(message);
            if (totalChars + messageChars > maxChars && !retained.isEmpty()) {
                break;
            }
            retained.addFirst(message);
            totalChars += messageChars;
        }

        for (ChatMessage message : retained) {
            switch (message.getRole()) {
                case USER -> messages.add(new UserMessage(message.getContent()));
                case ASSISTANT -> messages.add(new AssistantMessage(message.getContent()));
                case SYSTEM -> messages.add(new SystemMessage(message.getContent()));
            }
        }

        return messages;
    }

    /**
     * 提取最近 N 条消息文本作为查询上下文（用于意图识别、重写等）。
     */
    public List<String> extractRecentContext(Conversation conversation, int rounds) {
        List<ChatMessage> history = conversation.getContextMessages(rounds * 2);
        return history.stream()
                .map(msg -> msg.getRole().name() + ": " + msg.getContent())
                .toList();
    }

    /**
     * 生成会话摘要请求文本（由外部调用 LLM 完成摘要）。
     */
    public String buildSummaryPrompt(Conversation conversation) {
        List<ChatMessage> history = conversation.getMessages();
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : history) {
            sb.append(msg.getRole().name()).append(": ").append(msg.getContent()).append("\n");
        }
        return promptTemplateEngine.render(PromptTemplates.CONVERSATION_SUMMARY,
                Map.of("conversation", sb.toString()));
    }

    /**
     * 判断是否需要摘要压缩。
     */
    public boolean needsSummary(Conversation conversation) {
        int threshold = orchestratorConfig != null
                ? orchestratorConfig.getMemorySummaryThreshold() : 20;
        return conversation.needsSummaryCompression(threshold);
    }

    private int estimateMessageChars(ChatMessage message) {
        return message.getContent() == null ? 0 : message.getContent().length() + 32;
    }
}
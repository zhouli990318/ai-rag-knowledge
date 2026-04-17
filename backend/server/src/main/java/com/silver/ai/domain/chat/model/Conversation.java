package com.silver.ai.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话 — 聚合根
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    private Long id;
    private String title;
    private Long providerId;
    private String model;
    /** 关联的知识库 ID（可选，为空则不使用 RAG） */
    private Long knowledgeBaseId;
    @Builder.Default
    private List<Long> mcpServerIds = new ArrayList<>();
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    public void addMessage(MessageRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .conversationId(this.id)
                .role(role)
                .content(content)
                .build();
        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();

        // 自动设置标题（取第一条用户消息的前 50 个字符）
        if (this.title == null && role == MessageRole.USER) {
            this.title = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }
    }

    /**
     * 获取最近的上下文消息（滑动窗口）
     */
    public List<ChatMessage> getContextMessages(int windowSize) {
        if (messages.size() <= windowSize) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(messages.size() - windowSize, messages.size()));
    }

    public void enableRag(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
        this.updatedAt = LocalDateTime.now();
    }

    public void disableRag() {
        this.knowledgeBaseId = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateMcpServers(List<Long> mcpServerIds) {
        this.mcpServerIds = normalizeMcpServerIds(mcpServerIds);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isRagEnabled() {
        return knowledgeBaseId != null;
    }

    public void updateModel(Long providerId, String model) {
        this.providerId = providerId;
        this.model = model;
        this.updatedAt = LocalDateTime.now();
    }

    private List<Long> normalizeMcpServerIds(List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return new ArrayList<>();
        }

        return sourceIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}

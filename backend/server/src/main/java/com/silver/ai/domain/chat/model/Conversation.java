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
    private ToolMode toolMode = ToolMode.AUTO;
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    // ── 记忆摘要 ──
    private String summary;
    private LocalDateTime summaryUpdatedAt;

    // ── 最近意图 ──
    private String lastIntentDomain;
    private String lastIntentCategory;
    private String lastIntentTopic;

    // ── 建议问题缓存快照 ──
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();
    private Integer suggestionVersion;
    private LocalDateTime suggestionUpdatedAt;

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

    public void updateToolMode(ToolMode mode) {
        this.toolMode = mode == null ? ToolMode.AUTO : mode;
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

    /** 更新记忆摘要 */
    public void updateSummary(String summary) {
        this.summary = summary;
        this.summaryUpdatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** 记录最近识别的意图 */
    public void recordIntent(IntentResult intent) {
        if (intent != null) {
            this.lastIntentDomain = intent.getDomain();
            this.lastIntentCategory = intent.getCategory();
            this.lastIntentTopic = intent.getTopic();
            this.updatedAt = LocalDateTime.now();
        }
    }

    /** 是否需要摘要压缩 */
    public boolean needsSummaryCompression(int threshold) {
        return messages.size() > threshold;
    }

    public boolean hasSuggestionsForVersion(int version) {
        return suggestionVersion != null
                && suggestionVersion == version
                && suggestions != null
                && !suggestions.isEmpty();
    }

    public void updateSuggestions(List<String> suggestions, int version) {
        this.suggestions = suggestions == null ? new ArrayList<>() : new ArrayList<>(suggestions);
        this.suggestionVersion = version;
        this.suggestionUpdatedAt = LocalDateTime.now();
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

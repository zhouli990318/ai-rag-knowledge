package com.silver.ai.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 意图树节点 — 实体（支持领域→类目→话题三级结构）
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentNode {

    private Long id;
    private Long parentId;
    private String name;
    private String description;
    /** 0=domain, 1=category, 2=topic */
    private int level;
    /** 逗号分隔的关键词列表 */
    private String keywords;
    @Builder.Default
    private IntentResult.RoutingAdvice routingAdvice = IntentResult.RoutingAdvice.RETRIEVAL;
    @Builder.Default
    private int sortOrder = 0;
    @Builder.Default
    private IntentNodeStatus status = IntentNodeStatus.DRAFT;
    @Builder.Default
    private int version = 1;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 子节点（运行时组装，不持久化） */
    @Builder.Default
    private transient List<IntentNode> children = new ArrayList<>();

    public enum IntentNodeStatus {
        DRAFT, PUBLISHED
    }

    public void publish() {
        this.status = IntentNodeStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInfo(String name, String description, String keywords,
                           IntentResult.RoutingAdvice routingAdvice) {
        this.name = name;
        this.description = description;
        this.keywords = keywords;
        this.routingAdvice = routingAdvice;
        this.updatedAt = LocalDateTime.now();
    }

    /** 获取关键词列表 */
    public List<String> getKeywordList() {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return List.of(keywords.split(","));
    }
}

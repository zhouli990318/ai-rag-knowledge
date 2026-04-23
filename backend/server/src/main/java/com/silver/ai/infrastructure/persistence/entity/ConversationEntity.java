package com.silver.ai.infrastructure.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("conversation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEntity {

    @Id
    private Long id;

    private String title;

    @Column("provider_id")
    private Long providerId;

    private String model;

    @Column("knowledge_base_id")
    private Long knowledgeBaseId;

    private String summary;

    @Column("summary_updated_at")
    private LocalDateTime summaryUpdatedAt;

    @Column("last_intent_domain")
    private String lastIntentDomain;

    @Column("last_intent_category")
    private String lastIntentCategory;

    @Column("last_intent_topic")
    private String lastIntentTopic;

    @Column("suggestion_json")
    private String suggestionJson;

    @Column("suggestion_version")
    private Integer suggestionVersion;

    @Column("suggestion_updated_at")
    private LocalDateTime suggestionUpdatedAt;

    @Column("tool_mode")
    private String toolMode;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}

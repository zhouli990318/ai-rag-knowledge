package com.silver.ai.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "provider_id")
    private Long providerId;

    private String model;

    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "conversation_mcp_server", joinColumns = @JoinColumn(name = "conversation_id"))
    @Column(name = "mcp_server_id")
    @Builder.Default
    private List<Long> mcpServerIds = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ChatMessageEntity> messages = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

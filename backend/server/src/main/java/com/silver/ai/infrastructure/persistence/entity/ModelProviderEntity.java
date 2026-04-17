package com.silver.ai.infrastructure.persistence.entity;

import com.silver.ai.domain.provider.model.ProviderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "model_provider")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType;

    /** AES 加密后的 API Key */
    @Column(name = "api_key", length = 1024)
    private String apiKey;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "default_model")
    private String defaultModel;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "embedding_dimensions")
    private Integer embeddingDimensions;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

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

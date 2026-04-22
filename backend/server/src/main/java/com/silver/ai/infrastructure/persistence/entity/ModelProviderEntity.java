package com.silver.ai.infrastructure.persistence.entity;

import com.silver.ai.domain.provider.model.ProviderType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("model_provider")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProviderEntity {

    @Id
    private Long id;

    private String name;

    @Column("provider_type")
    private ProviderType providerType;

    @Column("api_key")
    private String apiKey;

    @Column("base_url")
    private String baseUrl;

    @Column("default_model")
    private String defaultModel;

    @Column("embedding_model")
    private String embeddingModel;

    @Column("embedding_dimensions")
    private Integer embeddingDimensions;

    @Builder.Default
    private boolean enabled = true;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}

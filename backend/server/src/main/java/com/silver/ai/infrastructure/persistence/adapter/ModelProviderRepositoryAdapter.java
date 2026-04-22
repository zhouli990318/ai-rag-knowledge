package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.infrastructure.persistence.entity.ModelProviderEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcModelProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ModelProviderRepositoryAdapter implements ModelProviderRepository {

    private final R2dbcModelProviderRepository r2dbc;

    @Override
    public Mono<ModelProvider> save(ModelProvider provider) {
        ModelProviderEntity entity = toEntity(provider);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        return r2dbc.save(entity).map(this::toDomain);
    }

    @Override
    public Mono<ModelProvider> findById(Long id) {
        return r2dbc.findById(id).map(this::toDomain);
    }

    @Override
    public Flux<ModelProvider> findAll() {
        return r2dbc.findAll().map(this::toDomain);
    }

    @Override
    public Flux<ModelProvider> findByEnabled(boolean enabled) {
        return r2dbc.findByEnabled(enabled).map(this::toDomain);
    }

    @Override
    public Mono<ModelProvider> findByProviderType(ProviderType type) {
        return r2dbc.findByProviderType(type).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return r2dbc.deleteById(id);
    }

    @Override
    public Mono<Boolean> existsByName(String name) {
        return r2dbc.existsByName(name);
    }

    private ModelProviderEntity toEntity(ModelProvider d) {
        return ModelProviderEntity.builder()
                .id(d.getId())
                .name(d.getName())
                .providerType(d.getProviderType())
                .apiKey(d.getApiKey())
                .baseUrl(d.getBaseUrl())
                .defaultModel(d.getDefaultModel())
                .embeddingModel(d.getEmbeddingModel())
                .embeddingDimensions(d.getEmbeddingDimensions())
                .enabled(d.isEnabled())
                .build();
    }

    private ModelProvider toDomain(ModelProviderEntity e) {
        return ModelProvider.builder()
                .id(e.getId())
                .name(e.getName())
                .providerType(e.getProviderType())
                .apiKey(e.getApiKey())
                .baseUrl(e.getBaseUrl())
                .defaultModel(e.getDefaultModel())
                .embeddingModel(e.getEmbeddingModel())
                .embeddingDimensions(e.getEmbeddingDimensions())
                .enabled(e.isEnabled())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

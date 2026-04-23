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
            entity.setUpdatedAt(now);
            return r2dbc.save(entity).map(this::toDomain);
        }
        // 更新时先读取原记录保留 createdAt
        entity.setUpdatedAt(now);
        return r2dbc.findById(entity.getId())
                .flatMap(existing -> {
                    entity.setCreatedAt(existing.getCreatedAt());
                    return r2dbc.save(entity);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    entity.setCreatedAt(now);
                    return r2dbc.save(entity);
                }))
                .map(this::toDomain);
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
                .healthStatus(d.getHealthStatus() != null ? d.getHealthStatus().name() : "UNKNOWN")
                .lastHealthCheckAt(d.getLastHealthCheckAt())
                .healthFailCount(d.getHealthFailCount())
                .avgFirstTokenMs(d.getAvgFirstTokenMs())
                .priority(d.getPriority())
                .build();
    }

    private ModelProvider toDomain(ModelProviderEntity e) {
        ModelProvider.HealthStatus hs;
        try {
            hs = ModelProvider.HealthStatus.valueOf(e.getHealthStatus());
        } catch (Exception ex) {
            hs = ModelProvider.HealthStatus.UNKNOWN;
        }
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
                .healthStatus(hs)
                .lastHealthCheckAt(e.getLastHealthCheckAt())
                .healthFailCount(e.getHealthFailCount())
                .avgFirstTokenMs(e.getAvgFirstTokenMs())
                .priority(e.getPriority())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

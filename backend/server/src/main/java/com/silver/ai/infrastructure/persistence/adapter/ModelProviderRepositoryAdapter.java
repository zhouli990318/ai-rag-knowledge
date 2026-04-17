package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.infrastructure.persistence.entity.ModelProviderEntity;
import com.silver.ai.infrastructure.persistence.jpa.JpaModelProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ModelProviderRepositoryAdapter implements ModelProviderRepository {

    private final JpaModelProviderRepository jpa;

    @Override
    public ModelProvider save(ModelProvider provider) {
        ModelProviderEntity entity = toEntity(provider);
        entity = jpa.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<ModelProvider> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<ModelProvider> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<ModelProvider> findByEnabled(boolean enabled) {
        return jpa.findByEnabled(enabled).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ModelProvider> findByProviderType(ProviderType type) {
        return jpa.findByProviderType(type).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
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

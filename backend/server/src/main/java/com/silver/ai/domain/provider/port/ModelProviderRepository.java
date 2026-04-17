package com.silver.ai.domain.provider.port;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;

import java.util.List;
import java.util.Optional;

/**
 * 模型提供商持久化端口
 */
public interface ModelProviderRepository {

    ModelProvider save(ModelProvider provider);

    Optional<ModelProvider> findById(Long id);

    List<ModelProvider> findAll();

    List<ModelProvider> findByEnabled(boolean enabled);

    Optional<ModelProvider> findByProviderType(ProviderType type);

    void deleteById(Long id);

    boolean existsByName(String name);
}

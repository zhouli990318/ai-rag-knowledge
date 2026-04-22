package com.silver.ai.domain.provider.port;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ModelProviderRepository {

    Mono<ModelProvider> save(ModelProvider provider);

    Mono<ModelProvider> findById(Long id);

    Flux<ModelProvider> findAll();

    Flux<ModelProvider> findByEnabled(boolean enabled);

    Mono<ModelProvider> findByProviderType(ProviderType type);

    Mono<Void> deleteById(Long id);

    Mono<Boolean> existsByName(String name);
}

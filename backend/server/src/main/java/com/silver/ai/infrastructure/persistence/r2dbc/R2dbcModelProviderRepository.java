package com.silver.ai.infrastructure.persistence.r2dbc;

import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.infrastructure.persistence.entity.ModelProviderEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface R2dbcModelProviderRepository extends ReactiveCrudRepository<ModelProviderEntity, Long> {
    Flux<ModelProviderEntity> findByEnabled(boolean enabled);
    Mono<ModelProviderEntity> findByProviderType(ProviderType type);
    Mono<Boolean> existsByName(String name);
}

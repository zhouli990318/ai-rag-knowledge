package com.silver.ai.infrastructure.persistence.jpa;

import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.infrastructure.persistence.entity.ModelProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaModelProviderRepository extends JpaRepository<ModelProviderEntity, Long> {
    List<ModelProviderEntity> findByEnabled(boolean enabled);
    Optional<ModelProviderEntity> findByProviderType(ProviderType type);
    boolean existsByName(String name);
}

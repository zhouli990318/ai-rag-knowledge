package com.silver.ai.mcpgateway.infrastructure.persistence.jpa;

import com.silver.ai.mcpgateway.infrastructure.persistence.entity.ApiSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaApiSourceRepository extends JpaRepository<ApiSourceEntity, Long> {
    List<ApiSourceEntity> findByActive(boolean active);
}

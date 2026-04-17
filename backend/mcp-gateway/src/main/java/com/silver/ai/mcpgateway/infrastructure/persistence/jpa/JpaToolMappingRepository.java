package com.silver.ai.mcpgateway.infrastructure.persistence.jpa;

import com.silver.ai.mcpgateway.infrastructure.persistence.entity.ToolMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaToolMappingRepository extends JpaRepository<ToolMappingEntity, Long> {
    List<ToolMappingEntity> findByApiSourceId(Long apiSourceId);
    List<ToolMappingEntity> findByEnabled(boolean enabled);
    void deleteByApiSourceId(Long apiSourceId);
}

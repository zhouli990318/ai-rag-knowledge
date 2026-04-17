package com.silver.ai.mcpgateway.infrastructure.persistence.adapter;

import com.silver.ai.mcpgateway.domain.model.ToolMapping;
import com.silver.ai.mcpgateway.domain.port.ToolMappingRepository;
import com.silver.ai.mcpgateway.infrastructure.persistence.entity.ToolMappingEntity;
import com.silver.ai.mcpgateway.infrastructure.persistence.jpa.JpaToolMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ToolMappingRepositoryAdapter implements ToolMappingRepository {

    private final JpaToolMappingRepository jpa;

    @Override
    public ToolMapping save(ToolMapping m) {
        return toDomain(jpa.save(toEntity(m)));
    }

    @Override
    public Optional<ToolMapping> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<ToolMapping> findByApiSourceId(Long apiSourceId) {
        return jpa.findByApiSourceId(apiSourceId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ToolMapping> findByEnabled(boolean enabled) {
        return jpa.findByEnabled(enabled).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByApiSourceId(Long apiSourceId) {
        jpa.deleteByApiSourceId(apiSourceId);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private ToolMappingEntity toEntity(ToolMapping d) {
        return ToolMappingEntity.builder()
                .id(d.getId())
                .apiSourceId(d.getApiSourceId())
                .operationId(d.getOperationId())
                .toolName(d.getToolName())
                .toolDescription(d.getToolDescription())
                .httpMethod(d.getHttpMethod())
                .path(d.getPath())
                .parameterSchema(d.getParameterSchema())
                .responseSchema(d.getResponseSchema())
                .examplePayload(d.getExamplePayload())
                .enabled(d.isEnabled())
                .build();
    }

    private ToolMapping toDomain(ToolMappingEntity e) {
        return ToolMapping.builder()
                .id(e.getId())
                .apiSourceId(e.getApiSourceId())
                .operationId(e.getOperationId())
                .toolName(e.getToolName())
                .toolDescription(e.getToolDescription())
                .httpMethod(e.getHttpMethod())
                .path(e.getPath())
                .parameterSchema(e.getParameterSchema())
                .responseSchema(e.getResponseSchema())
                .examplePayload(e.getExamplePayload())
                .enabled(e.isEnabled())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

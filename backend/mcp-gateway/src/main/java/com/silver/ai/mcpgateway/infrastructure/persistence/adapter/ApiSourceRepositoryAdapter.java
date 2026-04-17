package com.silver.ai.mcpgateway.infrastructure.persistence.adapter;

import com.silver.ai.mcpgateway.domain.model.ApiSource;
import com.silver.ai.mcpgateway.domain.port.ApiSourceRepository;
import com.silver.ai.mcpgateway.infrastructure.persistence.entity.ApiSourceEntity;
import com.silver.ai.mcpgateway.infrastructure.persistence.jpa.JpaApiSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ApiSourceRepositoryAdapter implements ApiSourceRepository {

    private final JpaApiSourceRepository jpa;

    @Override
    public ApiSource save(ApiSource source) {
        return toDomain(jpa.save(toEntity(source)));
    }

    @Override
    public Optional<ApiSource> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<ApiSource> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<ApiSource> findByActive(boolean active) {
        return jpa.findByActive(active).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private ApiSourceEntity toEntity(ApiSource d) {
        return ApiSourceEntity.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .protocolType(d.getProtocolType())
                .baseUrl(d.getBaseUrl())
                .openApiSpec(d.getOpenApiSpec())
                .authType(d.getAuthType())
                .authConfig(d.getAuthConfig())
                .active(d.isActive())
                .build();
    }

    private ApiSource toDomain(ApiSourceEntity e) {
        return ApiSource.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .protocolType(e.getProtocolType())
                .baseUrl(e.getBaseUrl())
                .openApiSpec(e.getOpenApiSpec())
                .authType(e.getAuthType())
                .authConfig(e.getAuthConfig())
                .active(e.isActive())
                .toolMappings(new ArrayList<>())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

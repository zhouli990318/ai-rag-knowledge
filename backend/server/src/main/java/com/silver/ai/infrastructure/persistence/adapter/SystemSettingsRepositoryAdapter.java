package com.silver.ai.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.port.SystemSettingsRepository;
import com.silver.ai.infrastructure.persistence.entity.SystemSettingsEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcSystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SystemSettingsRepositoryAdapter implements SystemSettingsRepository {

    private static final Long SINGLETON_ID = 1L;
    private final R2dbcSystemSettingsRepository r2dbc;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<ChatOrchestratorConfig> load() {
        return r2dbc.findById(SINGLETON_ID)
                .map(this::toDomain)
                .onErrorResume(e -> {
                    log.warn("Failed to load system settings from DB: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<ChatOrchestratorConfig> save(ChatOrchestratorConfig config) {
        return r2dbc.findById(SINGLETON_ID)
                .flatMap(existing -> {
                    existing.setConfig(toJson(config));
                    existing.setUpdatedAt(LocalDateTime.now());
                    return r2dbc.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    SystemSettingsEntity entity = new SystemSettingsEntity();
                    entity.setId(SINGLETON_ID);
                    entity.setConfig(toJson(config));
                    entity.setUpdatedAt(LocalDateTime.now());
                    entity.markNew();
                    return r2dbc.save(entity);
                }))
                .map(this::toDomain);
    }

    private ChatOrchestratorConfig toDomain(SystemSettingsEntity entity) {
        try {
            return objectMapper.readValue(entity.getConfig(), ChatOrchestratorConfig.class);
        } catch (Exception e) {
            log.error("Failed to deserialize system settings JSON, using defaults", e);
            return new ChatOrchestratorConfig();
        }
    }

    private String toJson(ChatOrchestratorConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize system settings", e);
        }
    }
}

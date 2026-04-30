package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.port.SystemSettingsRepository;
import com.silver.ai.infrastructure.config.OrchestratorConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SystemSettingsAppService {

    private final ChatOrchestratorConfig currentConfig;
    private final SystemSettingsRepository settingsRepository;
    private final OrchestratorConfigProperties configProperties;

    public Mono<ChatOrchestratorConfig> getSettings() {
        return Mono.just(currentConfig);
    }

    public Mono<ChatOrchestratorConfig> updateSettings(ChatOrchestratorConfig newConfig) {
        return settingsRepository.save(newConfig)
                .doOnSuccess(saved -> configProperties.applyConfig(saved));
    }
}

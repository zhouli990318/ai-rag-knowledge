package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.port.SystemSettingsRepository;
import com.silver.ai.domain.provider.port.EncryptionPort;
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
    private final EncryptionPort encryptionPort;

    public Mono<ChatOrchestratorConfig> getSettings() {
        return Mono.just(currentConfig);
    }

    public String getMaskedRerankerApiKey(ChatOrchestratorConfig config) {
        if (config == null || config.getRerankerApiKeyEncrypted() == null || config.getRerankerApiKeyEncrypted().isBlank()) {
            return null;
        }
        try {
            String plainApiKey = encryptionPort.decrypt(config.getRerankerApiKeyEncrypted());
            return maskSecret(plainApiKey);
        } catch (Exception ex) {
            return "********";
        }
    }

    public Mono<ChatOrchestratorConfig> updateSettings(ChatOrchestratorConfig newConfig) {
        return updateSettings(newConfig, null, false);
    }

    public Mono<ChatOrchestratorConfig> updateSettings(ChatOrchestratorConfig newConfig, String rerankerApiKey) {
        return updateSettings(newConfig, rerankerApiKey, false);
    }

    public Mono<ChatOrchestratorConfig> updateSettings(ChatOrchestratorConfig newConfig, String rerankerApiKey,
                                                       boolean clearRerankerApiKey) {
        if (clearRerankerApiKey) {
            newConfig.setRerankerApiKeyEncrypted(null);
        } else if (rerankerApiKey != null && !rerankerApiKey.isBlank()) {
            newConfig.setRerankerApiKeyEncrypted(encryptionPort.encrypt(rerankerApiKey));
        } else {
            newConfig.setRerankerApiKeyEncrypted(currentConfig.getRerankerApiKeyEncrypted());
        }
        newConfig.validate();
        return settingsRepository.save(newConfig)
                .doOnSuccess(saved -> configProperties.applyConfig(saved));
    }

    private String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int length = value.length();
        if (length <= 2) {
            return "*".repeat(length);
        }
        if (length <= 8) {
            return value.charAt(0) + "*".repeat(length - 2) + value.charAt(length - 1);
        }
        return value.substring(0, 4) + "*".repeat(length - 8) + value.substring(length - 4);
    }
}

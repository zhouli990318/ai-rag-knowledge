package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.port.SystemSettingsRepository;
import com.silver.ai.domain.provider.port.EncryptionPort;
import com.silver.ai.infrastructure.config.OrchestratorConfigProperties;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemSettingsAppServiceTest {

    @Test
    void updateSettingsShouldPreserveExistingEncryptedRerankerApiKeyWhenNewKeyIsBlank() {
        ChatOrchestratorConfig currentConfig = ChatOrchestratorConfig.builder()
                .rerankerApiKeyEncrypted("encrypted-old-key")
                .build();
        SystemSettingsRepository repository = mock(SystemSettingsRepository.class);
        OrchestratorConfigProperties configProperties = mock(OrchestratorConfigProperties.class);
        EncryptionPort encryptionPort = mock(EncryptionPort.class);
        when(repository.save(any(ChatOrchestratorConfig.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        SystemSettingsAppService service = new SystemSettingsAppService(currentConfig, repository, configProperties, encryptionPort);

        ChatOrchestratorConfig newConfig = ChatOrchestratorConfig.builder()
                .rerankerServiceEnabled(true)
                .rerankerBaseUrl("http://localhost:18080")
                .rerankerModel("bge-reranker-v2-m3")
                .build();

        ChatOrchestratorConfig saved = service.updateSettings(newConfig, "").block();

        assertEquals("encrypted-old-key", saved.getRerankerApiKeyEncrypted());
    }

    @Test
    void updateSettingsShouldEncryptNewRerankerApiKeyBeforeSaving() {
        ChatOrchestratorConfig currentConfig = new ChatOrchestratorConfig();
        SystemSettingsRepository repository = mock(SystemSettingsRepository.class);
        OrchestratorConfigProperties configProperties = mock(OrchestratorConfigProperties.class);
        EncryptionPort encryptionPort = mock(EncryptionPort.class);
        when(encryptionPort.encrypt("plain-reranker-key")).thenReturn("encrypted-new-key");
        when(repository.save(any(ChatOrchestratorConfig.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        SystemSettingsAppService service = new SystemSettingsAppService(currentConfig, repository, configProperties, encryptionPort);

        ChatOrchestratorConfig newConfig = ChatOrchestratorConfig.builder()
                .rerankerServiceEnabled(true)
                .rerankerBaseUrl("http://localhost:18080")
                .rerankerModel("bge-reranker-v2-m3")
                .build();

        ChatOrchestratorConfig saved = service.updateSettings(newConfig, "plain-reranker-key").block();

        assertEquals("encrypted-new-key", saved.getRerankerApiKeyEncrypted());
        verify(encryptionPort).encrypt("plain-reranker-key");
    }

    @Test
    void updateSettingsShouldClearEncryptedRerankerApiKeyWhenExplicitlyRequested() {
        ChatOrchestratorConfig currentConfig = ChatOrchestratorConfig.builder()
                .rerankerApiKeyEncrypted("encrypted-old-key")
                .build();
        SystemSettingsRepository repository = mock(SystemSettingsRepository.class);
        OrchestratorConfigProperties configProperties = mock(OrchestratorConfigProperties.class);
        EncryptionPort encryptionPort = mock(EncryptionPort.class);
        when(repository.save(any(ChatOrchestratorConfig.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        SystemSettingsAppService service = new SystemSettingsAppService(currentConfig, repository, configProperties, encryptionPort);

        ChatOrchestratorConfig newConfig = ChatOrchestratorConfig.builder()
                .rerankerServiceEnabled(true)
                .rerankerBaseUrl("http://localhost:18080")
                .rerankerModel("bge-reranker-v2-m3")
                .build();

        ChatOrchestratorConfig saved = service.updateSettings(newConfig, "plain-reranker-key", true).block();

        assertNull(saved.getRerankerApiKeyEncrypted());
        verify(encryptionPort, never()).encrypt("plain-reranker-key");
    }

        @Test
        void getMaskedRerankerApiKeyShouldMaskMiddleCharacters() {
                ChatOrchestratorConfig currentConfig = new ChatOrchestratorConfig();
                SystemSettingsRepository repository = mock(SystemSettingsRepository.class);
                OrchestratorConfigProperties configProperties = mock(OrchestratorConfigProperties.class);
                EncryptionPort encryptionPort = mock(EncryptionPort.class);
                SystemSettingsAppService service = new SystemSettingsAppService(currentConfig, repository, configProperties, encryptionPort);
                ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                                .rerankerApiKeyEncrypted("encrypted-key")
                                .build();
                when(encryptionPort.decrypt("encrypted-key")).thenReturn("1234567890abcdef");

                String masked = service.getMaskedRerankerApiKey(config);

                assertEquals("1234********cdef", masked);
        }
}
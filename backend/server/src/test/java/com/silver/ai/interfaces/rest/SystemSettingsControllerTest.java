package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.SystemSettingsAppService;
import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.interfaces.dto.SystemSettingsRequest;
import com.silver.ai.interfaces.dto.SystemSettingsResponse;
import com.silver.ai.shared.result.ApiResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemSettingsControllerTest {

    @Test
    void getSettingsShouldExposeConfiguredFlagWithoutReturningApiKey() {
        SystemSettingsAppService service = mock(SystemSettingsAppService.class);
        SystemSettingsController controller = new SystemSettingsController(service);
        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .hydeEnabled(true)
                .rerankerServiceEnabled(true)
                .rerankerBaseUrl("http://localhost:18080")
                .rerankerModel("bge-reranker-v2-m3")
                .rerankerApiKeyEncrypted("encrypted-key")
                .build();
        when(service.getSettings()).thenReturn(Mono.just(config));
                when(service.getMaskedRerankerApiKey(config)).thenReturn("1234********cdef");

        Mono<ApiResponse<SystemSettingsResponse>> responseMono = controller.getSettings();

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                                        assertTrue(response.getData().isHydeEnabled());
                    assertTrue(response.getData().isRerankerApiKeyConfigured());
                    assertEquals("http://localhost:18080", response.getData().getRerankerBaseUrl());
                                        assertEquals("1234********cdef", response.getData().getRerankerApiKeyMasked());
                })
                .verifyComplete();
    }

    @Test
    void updateSettingsShouldForwardPlainApiKeySeparately() {
        SystemSettingsAppService service = mock(SystemSettingsAppService.class);
        SystemSettingsController controller = new SystemSettingsController(service);
        SystemSettingsRequest request = new SystemSettingsRequest();
        request.setHydeEnabled(true);
        request.setRerankerServiceEnabled(true);
        request.setRerankerBaseUrl("http://localhost:18080");
        request.setRerankerModel("bge-reranker-v2-m3");
        request.setRerankerApiKey("plain-key");

        ChatOrchestratorConfig saved = ChatOrchestratorConfig.builder()
                .hydeEnabled(true)
                .rerankerServiceEnabled(true)
                .rerankerBaseUrl("http://localhost:18080")
                .rerankerModel("bge-reranker-v2-m3")
                .rerankerApiKeyEncrypted("encrypted-key")
                .build();
        when(service.updateSettings(any(ChatOrchestratorConfig.class), eq("plain-key"), eq(false))).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.updateSettings(request))
                                .assertNext(response -> {
                                        assertTrue(response.getData().isHydeEnabled());
                                        assertTrue(response.getData().isRerankerApiKeyConfigured());
                                })
                .verifyComplete();

                verify(service).updateSettings(argThat(ChatOrchestratorConfig::isHydeEnabled), eq("plain-key"), eq(false));
    }

    @Test
    void updateSettingsShouldForwardExplicitClearFlag() {
        SystemSettingsAppService service = mock(SystemSettingsAppService.class);
        SystemSettingsController controller = new SystemSettingsController(service);
        SystemSettingsRequest request = new SystemSettingsRequest();
        request.setRerankerServiceEnabled(true);
        request.setRerankerBaseUrl("http://localhost:18080");
        request.setRerankerModel("bge-reranker-v2-m3");
        request.setClearRerankerApiKey(true);

        ChatOrchestratorConfig saved = ChatOrchestratorConfig.builder()
                .rerankerServiceEnabled(true)
                .rerankerBaseUrl("http://localhost:18080")
                .rerankerModel("bge-reranker-v2-m3")
                .build();
        when(service.updateSettings(any(ChatOrchestratorConfig.class), eq(null), eq(true))).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.updateSettings(request))
                .assertNext(response -> assertTrue(!response.getData().isRerankerApiKeyConfigured()))
                .verifyComplete();

        verify(service).updateSettings(any(ChatOrchestratorConfig.class), eq(null), eq(true));
    }
}
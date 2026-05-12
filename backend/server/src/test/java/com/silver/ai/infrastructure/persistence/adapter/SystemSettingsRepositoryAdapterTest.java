package com.silver.ai.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.infrastructure.persistence.entity.SystemSettingsEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcSystemSettingsRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemSettingsRepositoryAdapterTest {

    @Test
    void loadShouldPreserveProvidedDefaultsForFieldsMissingFromPersistedJson() {
        R2dbcSystemSettingsRepository r2dbc = mock(R2dbcSystemSettingsRepository.class);
        SystemSettingsEntity entity = new SystemSettingsEntity();
        entity.setId(1L);
        entity.setConfig("{\"intentEnabled\":false,\"rerankTopK\":8}");
        when(r2dbc.findById(1L)).thenReturn(Mono.just(entity));

        SystemSettingsRepositoryAdapter adapter = new SystemSettingsRepositoryAdapter(r2dbc, new ObjectMapper());
        ChatOrchestratorConfig defaults = new ChatOrchestratorConfig();
        defaults.setIntentEnabled(true);
        defaults.setRerankTopK(5);
        defaults.setRerankerServiceEnabled(true);
        defaults.setRerankerBaseUrl("http://env-reranker:18080");
        defaults.setRerankerModel("env-reranker-model");

        StepVerifier.create(adapter.load(defaults))
                .assertNext(loaded -> {
                    org.junit.jupiter.api.Assertions.assertFalse(loaded.isIntentEnabled());
                    org.junit.jupiter.api.Assertions.assertEquals(8, loaded.getRerankTopK());
                    org.junit.jupiter.api.Assertions.assertTrue(loaded.isRerankerServiceEnabled());
                    org.junit.jupiter.api.Assertions.assertEquals("http://env-reranker:18080", loaded.getRerankerBaseUrl());
                    org.junit.jupiter.api.Assertions.assertEquals("env-reranker-model", loaded.getRerankerModel());
                })
                .verifyComplete();
    }
}
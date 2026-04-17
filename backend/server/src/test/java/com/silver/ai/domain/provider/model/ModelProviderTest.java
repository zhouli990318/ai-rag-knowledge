package com.silver.ai.domain.provider.model;

import com.silver.ai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelProviderTest {

    @Test
    void updateConfigShouldRetainExistingApiKeyWhenBlankAndUseDefaultBaseUrlFallback() {
        ModelProvider provider = ModelProvider.builder()
                .name("openai")
                .providerType(ProviderType.OPENAI)
                .apiKey("encrypted")
                .build();

        provider.updateConfig("openai-new", "", "", "gpt-4.1", "text-embedding-3-small", 1536);

        assertEquals("openai-new", provider.getName());
        assertEquals("encrypted", provider.getApiKey());
        assertEquals(ProviderType.OPENAI.getDefaultBaseUrl(), provider.getEffectiveBaseUrl());
        assertEquals("gpt-4.1", provider.getDefaultModel());
    }

    @Test
    void ensureEnabledShouldRejectDisabledProvider() {
        ModelProvider provider = ModelProvider.builder()
                .name("disabled")
                .providerType(ProviderType.OPENAI)
                .enabled(false)
                .build();

        assertThrows(BusinessException.class, provider::ensureEnabled);
    }
}
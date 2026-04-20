package com.silver.ai.application.service;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.infrastructure.ai.ChatModelRegistry;
import com.silver.ai.infrastructure.ai.EmbeddingModelRegistry;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelProviderAppServiceTest {

    @Test
    void createProviderShouldEncryptApiKeyBeforeSaving() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        ModelProviderAppService service = new ModelProviderAppService(repository, mock(ChatModelPort.class),
                mock(ChatModelRegistry.class), mock(EmbeddingModelRegistry.class));
        ReflectionTestUtils.setField(service, "cryptoSecretKey", "unit-test-key");
        when(repository.existsByName("provider-a")).thenReturn(false);
        when(repository.save(any(ModelProvider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModelProvider provider = service.createProvider("provider-a", ProviderType.OPENAI, "plain-key",
                "https://example.com", "gpt-4.1", "embed", 1536);

        assertFalse("plain-key".equals(provider.getApiKey()));
        assertEquals("plain-key", CryptoUtil.decrypt(provider.getApiKey(), "unit-test-key"));
    }

    @Test
    void toggleProviderShouldFlipStateAndRefreshRegistries() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        ChatModelRegistry chatRegistry = mock(ChatModelRegistry.class);
        EmbeddingModelRegistry embeddingRegistry = mock(EmbeddingModelRegistry.class);
        ModelProviderAppService service = new ModelProviderAppService(repository, mock(ChatModelPort.class), chatRegistry, embeddingRegistry);
        ModelProvider provider = ModelProvider.builder().id(5L).providerType(ProviderType.OPENAI).enabled(true).build();
        when(repository.findById(5L)).thenReturn(Optional.of(provider));

        service.toggleProvider(5L);

        assertFalse(provider.isEnabled());
        verify(repository).save(provider);
        verify(chatRegistry).refresh(5L);
        verify(embeddingRegistry).refresh(5L);
    }

    @Test
    void testConnectionShouldWrapSuccessfulResponse() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ModelProviderAppService service = new ModelProviderAppService(repository, chatModelPort,
                mock(ChatModelRegistry.class), mock(EmbeddingModelRegistry.class));
        ModelProvider provider = ModelProvider.builder().id(2L).providerType(ProviderType.OPENAI).enabled(true).build();
        when(repository.findById(2L)).thenReturn(Optional.of(provider));
        when(chatModelPort.chat(any(), any(), any(), any())).thenReturn("OK");

        String result = service.testConnection(2L);

        assertEquals("连接成功: OK", result);
    }

    @Test
    void getProviderTypesShouldExposeCapabilitiesForAllProviderTypes() {
        ModelProviderAppService service = new ModelProviderAppService(mock(ModelProviderRepository.class), mock(ChatModelPort.class),
                mock(ChatModelRegistry.class), mock(EmbeddingModelRegistry.class));

        List<java.util.Map<String, Object>> result = service.getProviderTypes();

        assertEquals(ProviderType.values().length, result.size());
        assertTrue(result.stream().allMatch(item -> item.containsKey("type") && item.containsKey("supportsChat")));
    }

    @Test
    void createProviderShouldRejectDuplicateName() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        ModelProviderAppService service = new ModelProviderAppService(repository, mock(ChatModelPort.class),
                mock(ChatModelRegistry.class), mock(EmbeddingModelRegistry.class));
        when(repository.existsByName("dup")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.createProvider("dup", ProviderType.OPENAI,
                "key", null, null, null, null));
    }
}
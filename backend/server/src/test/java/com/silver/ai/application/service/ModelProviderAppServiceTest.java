package com.silver.ai.application.service;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.domain.provider.port.EncryptionPort;
import com.silver.ai.domain.provider.port.ModelCachePort;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ModelProviderAppServiceTest {

    @Test
    void createProviderShouldEncryptApiKeyBeforeSaving() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        EncryptionPort encryptionPort = mock(EncryptionPort.class);
        ModelProviderAppService service = new ModelProviderAppService(repository, mock(ChatModelPort.class),
                mock(ModelCachePort.class), encryptionPort);
        when(encryptionPort.encrypt("plain-key")).thenReturn("encrypted-key");
        when(repository.existsByName("provider-a")).thenReturn(Mono.just(false));
        when(repository.save(any(ModelProvider.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ModelProvider provider = service.createProvider("provider-a", ProviderType.OPENAI, "plain-key",
                "https://example.com", "gpt-4.1", "embed", 1536).block();

        assertNotNull(provider);
        assertEquals("encrypted-key", provider.getApiKey());
        verify(encryptionPort).encrypt("plain-key");
    }

    @Test
    void toggleProviderShouldFlipStateAndRefreshCache() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        ModelCachePort modelCachePort = mock(ModelCachePort.class);
        ModelProviderAppService service = new ModelProviderAppService(repository, mock(ChatModelPort.class),
                modelCachePort, mock(EncryptionPort.class));
        ModelProvider provider = ModelProvider.builder().id(5L).providerType(ProviderType.OPENAI).enabled(true).build();
        when(repository.findById(5L)).thenReturn(Mono.just(provider));
        when(repository.save(any(ModelProvider.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        service.toggleProvider(5L).block();

        assertFalse(provider.isEnabled());
        verify(repository).save(provider);
        verify(modelCachePort).refreshCache(5L);
    }

    @Test
    void validateConnectionShouldUseChatModelPortAndWrapSuccessfulResponse() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ModelProviderAppService service = new ModelProviderAppService(repository, chatModelPort,
                mock(ModelCachePort.class), mock(EncryptionPort.class));
        ModelProvider provider = ModelProvider.builder()
                .id(2L)
                .name("provider-a")
                .providerType(ProviderType.OPENAI)
                .enabled(true)
                .defaultModel("gpt-4.1")
                .build();

        when(repository.findById(2L)).thenReturn(Mono.just(provider));
        when(repository.save(any(ModelProvider.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(chatModelPort.chat(eq(2L), eq("gpt-4.1"), any(), any())).thenReturn(Mono.just("OK"));

        String result = service.validateConnection(2L).block();

        assertEquals("\u8fde\u63a5\u6210\u529f: OK", result);
        verify(chatModelPort).chat(eq(2L), eq("gpt-4.1"), any(), any());
        verify(repository).save(provider);
        assertEquals(ModelProvider.HealthStatus.HEALTHY, provider.getHealthStatus());
    }

    @Test
    void getProviderTypesShouldExposeCapabilitiesForAllProviderTypes() {
        ModelProviderAppService service = new ModelProviderAppService(mock(ModelProviderRepository.class), mock(ChatModelPort.class),
                mock(ModelCachePort.class), mock(EncryptionPort.class));

        List<java.util.Map<String, Object>> result = service.getProviderTypes();

        assertEquals(ProviderType.values().length, result.size());
        assertTrue(result.stream().allMatch(item -> item.containsKey("type") && item.containsKey("supportsChat")));
    }

    @Test
    void createProviderShouldRejectDuplicateName() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        EncryptionPort encryptionPort = mock(EncryptionPort.class);
        ModelProviderAppService service = new ModelProviderAppService(repository, mock(ChatModelPort.class),
                mock(ModelCachePort.class), encryptionPort);
        when(encryptionPort.encrypt(any())).thenReturn("encrypted");
        when(repository.save(any(ModelProvider.class)))
                .thenReturn(Mono.error(new org.springframework.dao.DuplicateKeyException("uk_provider_name")));

        StepVerifier.create(service.createProvider("dup", ProviderType.OPENAI, "key", null, null, null, null))
                .expectError(BusinessException.class)
                .verify();
    }
}

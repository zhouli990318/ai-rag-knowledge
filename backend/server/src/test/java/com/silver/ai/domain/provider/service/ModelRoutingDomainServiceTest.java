package com.silver.ai.domain.provider.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelRoutingDomainServiceTest {

    @Test
    void selectProviderShouldReturnPreferredWhenAvailable() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        ChatOrchestratorConfig config = new ChatOrchestratorConfig();
        ModelRoutingDomainService service = new ModelRoutingDomainService(repository, config);

        ModelProvider preferred = ModelProvider.builder()
                .id(1L)
                .name("preferred")
                .providerType(ProviderType.OPENAI)
                .enabled(true)
                .build();

        when(repository.findById(1L)).thenReturn(Mono.just(preferred));

        ModelProvider selected = service.selectProvider(1L);

        assertSame(preferred, selected);
    }

    @Test
    void selectProviderShouldDegradeToPreferredWhenNoFallbackAndPreferredStillEnabled() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        ChatOrchestratorConfig config = new ChatOrchestratorConfig();
        ModelRoutingDomainService service = new ModelRoutingDomainService(repository, config);

        ModelProvider preferred = ModelProvider.builder()
                .id(1L)
                .name("single-provider")
                .providerType(ProviderType.OPENAI)
                .enabled(true)
                .healthStatus(ModelProvider.HealthStatus.UNHEALTHY)
                .build();

        when(repository.findById(1L)).thenReturn(Mono.just(preferred));
        when(repository.findAll()).thenReturn(Flux.just(preferred));

        ModelProvider selected = service.selectProvider(1L);

        assertSame(preferred, selected);
    }

    @Test
    void selectProviderShouldThrowWhenNoFallbackAndPreferredDisabled() {
        ModelProviderRepository repository = mock(ModelProviderRepository.class);
        ChatOrchestratorConfig config = new ChatOrchestratorConfig();
        ModelRoutingDomainService service = new ModelRoutingDomainService(repository, config);

        ModelProvider preferred = ModelProvider.builder()
                .id(1L)
                .name("disabled-provider")
                .providerType(ProviderType.OPENAI)
                .enabled(false)
                .healthStatus(ModelProvider.HealthStatus.UNHEALTHY)
                .build();

        when(repository.findById(1L)).thenReturn(Mono.just(preferred));
        when(repository.findAll()).thenReturn(Flux.just(preferred));

        assertThrows(BusinessException.class, () -> service.selectProvider(1L));
    }
}

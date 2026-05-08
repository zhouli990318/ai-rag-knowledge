package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.DomainMessage;
import com.silver.ai.domain.chat.model.MessageRole;
import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.domain.provider.port.EncryptionPort;
import com.silver.ai.domain.provider.port.ModelCachePort;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelProviderAppService {

    private final ModelProviderRepository providerRepository;
    private final ChatModelPort chatModelPort;
    private final ModelCachePort modelCachePort;
    private final EncryptionPort encryptionPort;

    public Mono<ModelProvider> createProvider(String name, ProviderType providerType, String apiKey,
                                              String baseUrl, String defaultModel,
                                              String embeddingModel, Integer embeddingDimensions) {
        String encryptedKey = (apiKey != null && !apiKey.isBlank())
                ? encryptionPort.encrypt(apiKey) : null;
        ModelProvider provider = ModelProvider.builder()
                .name(name)
                .providerType(providerType)
                .apiKey(encryptedKey)
                .baseUrl(baseUrl)
                .defaultModel(defaultModel)
                .embeddingModel(embeddingModel)
                .embeddingDimensions(embeddingDimensions)
                .build();
        return providerRepository.save(provider)
                .onErrorMap(org.springframework.dao.DuplicateKeyException.class,
                        e -> new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "提供商名称已存在: " + name));
    }

    public Mono<ModelProvider> updateProvider(Long id, String name, String apiKey, String baseUrl,
                                              String defaultModel, String embeddingModel,
                                              Integer embeddingDimensions) {
        return getProvider(id)
                .flatMap(provider -> {
                    String encryptedKey = null;
                    if (apiKey != null && !apiKey.isBlank()) {
                        encryptedKey = encryptionPort.encrypt(apiKey);
                    }
                    provider.updateConfig(name, encryptedKey, baseUrl, defaultModel, embeddingModel, embeddingDimensions);
                    return providerRepository.save(provider);
                })
                .doOnNext(saved -> modelCachePort.refreshCache(id));
    }

    public Mono<ModelProvider> getProvider(Long id) {
        return providerRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PROVIDER_NOT_FOUND)));
    }

    public Flux<ModelProvider> listProviders() {
        return providerRepository.findAll();
    }

    public Mono<Void> deleteProvider(Long id) {
        modelCachePort.refreshCache(id);
        return providerRepository.deleteById(id);
    }

    public Mono<Void> toggleProvider(Long id) {
        return getProvider(id)
                .flatMap(provider -> {
                    if (provider.isEnabled()) provider.disable(); else provider.enable();
                    return providerRepository.save(provider);
                })
                .doOnNext(saved -> modelCachePort.refreshCache(id))
                .then();
    }

    public Mono<String> testConnection(Long id) {
        return getProvider(id).flatMap(provider ->
                chatModelPort.chat(provider.getId(), provider.getDefaultModel(),
                                List.of(new DomainMessage(MessageRole.USER, "Hello, reply with 'OK' only.")),
                                List.of())
                        .flatMap(response -> {
                            provider.markHealthy(0);
                            return providerRepository.save(provider)
                                    .thenReturn("连接成功: " + response);
                        })
                        .onErrorResume(error -> {
                            provider.markUnhealthy();
                            return providerRepository.save(provider)
                                    .then(Mono.error(new BusinessException(
                                            ErrorCode.PROVIDER_CONNECTION_FAILED,
                                            error.getMessage(),
                                            error
                                    )));
                        })
        );
    }

    public List<Map<String, Object>> getProviderTypes() {
        return Arrays.stream(ProviderType.values())
                .map(pt -> Map.<String, Object>of(
                        "type", pt.name(),
                        "displayName", pt.getDisplayName(),
                        "defaultBaseUrl", pt.getDefaultBaseUrl(),
                        "openAiCompatible", pt.isOpenAiCompatible(),
                        "supportsChat", pt.supportsChat(),
                        "supportsEmbedding", pt.supportsEmbedding()
                ))
                .toList();
    }
}

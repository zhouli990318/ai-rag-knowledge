package com.silver.ai.application.service;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.infrastructure.ai.ChatModelRegistry;
import com.silver.ai.infrastructure.ai.EmbeddingModelRegistry;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import com.silver.ai.shared.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelProviderAppService {

    private final ModelProviderRepository providerRepository;
    private final ChatModelPort chatModelPort;
    private final ChatModelRegistry chatModelRegistry;
    private final EmbeddingModelRegistry embeddingModelRegistry;

    @Value("${app.crypto.secret-key:SpringAiRagPlatform2024}")
    private String cryptoSecretKey;

    public Mono<ModelProvider> createProvider(String name, ProviderType providerType, String apiKey,
                                              String baseUrl, String defaultModel,
                                              String embeddingModel, Integer embeddingDimensions) {
        return providerRepository.existsByName(name)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "提供商名称已存在: " + name));
                    }
                    String encryptedKey = (apiKey != null && !apiKey.isBlank())
                            ? CryptoUtil.encrypt(apiKey, cryptoSecretKey) : null;
                    ModelProvider provider = ModelProvider.builder()
                            .name(name)
                            .providerType(providerType)
                            .apiKey(encryptedKey)
                            .baseUrl(baseUrl)
                            .defaultModel(defaultModel)
                            .embeddingModel(embeddingModel)
                            .embeddingDimensions(embeddingDimensions)
                            .build();
                    return providerRepository.save(provider);
                });
    }

    public Mono<ModelProvider> updateProvider(Long id, String name, String apiKey, String baseUrl,
                                              String defaultModel, String embeddingModel,
                                              Integer embeddingDimensions) {
        return getProvider(id)
                .flatMap(provider -> {
                    String encryptedKey = null;
                    if (apiKey != null && !apiKey.isBlank()) {
                        encryptedKey = CryptoUtil.encrypt(apiKey, cryptoSecretKey);
                    }
                    provider.updateConfig(name, encryptedKey, baseUrl, defaultModel, embeddingModel, embeddingDimensions);
                    return providerRepository.save(provider);
                })
                .doOnNext(saved -> {
                    chatModelRegistry.refresh(id);
                    embeddingModelRegistry.refresh(id);
                });
    }

    public Mono<ModelProvider> getProvider(Long id) {
        return providerRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PROVIDER_NOT_FOUND)));
    }

    public Flux<ModelProvider> listProviders() {
        return providerRepository.findAll();
    }

    public Mono<Void> deleteProvider(Long id) {
        chatModelRegistry.refresh(id);
        embeddingModelRegistry.refresh(id);
        return providerRepository.deleteById(id);
    }

    public Mono<Void> toggleProvider(Long id) {
        return getProvider(id)
                .flatMap(provider -> {
                    if (provider.isEnabled()) provider.disable(); else provider.enable();
                    return providerRepository.save(provider);
                })
                .doOnNext(saved -> {
                    chatModelRegistry.refresh(id);
                    embeddingModelRegistry.refresh(id);
                })
                .then();
    }

    public Mono<String> testConnection(Long id) {
        return getProvider(id).flatMap(provider ->
                Mono.fromCallable(() -> {
                            long startTime = System.currentTimeMillis();
                            var chatModel = chatModelRegistry.getWithModel(provider, provider.getDefaultModel());
                            var response = chatModel.call(new Prompt(List.of(new UserMessage("Hello, reply with 'OK' only."))));
                            long duration = System.currentTimeMillis() - startTime;
                            return new ConnectionProbeResult(response.getResult().getOutput().getText(), duration);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(result -> {
                            provider.markHealthy(result.firstTokenMs());
                            return providerRepository.save(provider)
                                    .thenReturn("连接成功: " + result.response());
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

    private record ConnectionProbeResult(String response, long firstTokenMs) {
    }
}

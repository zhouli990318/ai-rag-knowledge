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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public ModelProvider createProvider(String name, ProviderType providerType, String apiKey,
                                        String baseUrl, String defaultModel,
                                        String embeddingModel, Integer embeddingDimensions) {
        if (providerRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "提供商名称已存在: " + name);
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
    }

    public ModelProvider updateProvider(Long id, String name, String apiKey, String baseUrl,
                                        String defaultModel, String embeddingModel,
                                        Integer embeddingDimensions) {
        ModelProvider provider = getProvider(id);

        String encryptedKey = null;
        if (apiKey != null && !apiKey.isBlank()) {
            encryptedKey = CryptoUtil.encrypt(apiKey, cryptoSecretKey);
        }

        provider.updateConfig(name, encryptedKey, baseUrl, defaultModel, embeddingModel, embeddingDimensions);
        ModelProvider saved = providerRepository.save(provider);

        // 刷新缓存
        chatModelRegistry.refresh(id);
        embeddingModelRegistry.refresh(id);

        return saved;
    }

    public ModelProvider getProvider(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    public List<ModelProvider> listProviders() {
        return providerRepository.findAll();
    }

    public void deleteProvider(Long id) {
        chatModelRegistry.refresh(id);
        embeddingModelRegistry.refresh(id);
        providerRepository.deleteById(id);
    }

    public void toggleProvider(Long id) {
        ModelProvider provider = getProvider(id);
        if (provider.isEnabled()) {
            provider.disable();
        } else {
            provider.enable();
        }
        providerRepository.save(provider);
        chatModelRegistry.refresh(id);
        embeddingModelRegistry.refresh(id);
    }

    /**
     * 测试连通性
     */
    public String testConnection(Long id) {
        try {
            String response = chatModelPort.chat(
                    id,
                    null,
                    List.of(new UserMessage("Hello, reply with 'OK' only.")),
                    List.of());
            return "连接成功: " + response;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PROVIDER_CONNECTION_FAILED, e.getMessage());
        }
    }

    /**
     * 获取支持的提供商类型列表
     */
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

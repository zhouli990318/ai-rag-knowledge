package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.shared.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * EmbeddingModel 工厂与注册表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingModelRegistry {

    @Value("${app.crypto.secret-key:SpringAiRagPlatform2024}")
    private String cryptoSecretKey;

    private final ConcurrentHashMap<String, EmbeddingModel> cache = new ConcurrentHashMap<>();

    public EmbeddingModel getOrCreate(ModelProvider provider) {
        String cacheKey = provider.getId() + ":" + provider.getEmbeddingModel();
        return cache.computeIfAbsent(cacheKey, k -> createEmbeddingModel(provider));
    }

    public void refresh(Long providerId) {
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(providerId + ":"));
    }

    private EmbeddingModel createEmbeddingModel(ModelProvider provider) {
        String apiKey = decryptApiKey(provider.getApiKey());
        String baseUrl = normalizeBaseUrl(provider);
        String model = provider.getEmbeddingModel();

        log.info("Creating EmbeddingModel for provider: {} model: {}", provider.getName(), model);

        return switch (provider.getProviderType()) {
            case OLLAMA -> {
                OllamaApi api = new OllamaApi.Builder().baseUrl(baseUrl).build();
                yield OllamaEmbeddingModel.builder()
                        .ollamaApi(api)
                    .defaultOptions(OllamaEmbeddingOptions.builder().model(model).build())
                        .build();
            }
            case OPENAI, DEEPSEEK, DASHSCOPE, QIANFAN, MOONSHOT -> {
                OpenAiApi api = OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
                yield new OpenAiEmbeddingModel(api, MetadataMode.EMBED, OpenAiEmbeddingOptions.builder().model(model).build());
            }
            case ZHIPUAI -> {
                ZhiPuAiApi api = ZhiPuAiApi.builder().apiKey(apiKey).baseUrl(baseUrl).build();
                yield new ZhiPuAiEmbeddingModel(api, MetadataMode.EMBED, ZhiPuAiEmbeddingOptions.builder().model(model).build());
            }
            case ANTHROPIC -> throw new UnsupportedOperationException("Anthropic does not support embedding models");
        };
    }

    private String decryptApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isBlank()) return "";
        try {
            return CryptoUtil.decrypt(encryptedKey, cryptoSecretKey);
        } catch (Exception e) {
            return encryptedKey;
        }
    }

    private String normalizeBaseUrl(ModelProvider provider) {
        String baseUrl = provider.getEffectiveBaseUrl();
        if (provider.getProviderType().isOpenAiCompatible()) {
            return OpenAiCompatibleBaseUrlNormalizer.normalize(baseUrl);
        }
        return baseUrl;
    }
}

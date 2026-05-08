package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.shared.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ChatModel 工厂 — 根据 ProviderType 和配置动态创建 ChatModel 实例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelFactory {

    private final WebClient.Builder webClientBuilder;
    private final RestClient.Builder restClientBuilder;

    @Value("${app.crypto.secret-key}")
    private String cryptoSecretKey;

    public ChatModel createChatModel(ModelProvider provider) {
        String apiKey = decryptApiKey(provider.getApiKey());
        String baseUrl = normalizeBaseUrl(provider);
        String model = provider.getDefaultModel();

        return switch (provider.getProviderType()) {
            case OLLAMA -> createOllamaModel(baseUrl, model);
            case OPENAI -> createOpenAiModel(baseUrl, apiKey, model);
            case ANTHROPIC -> createAnthropicModel(baseUrl, apiKey, model);
            case ZHIPUAI -> createZhiPuAiModel(baseUrl, apiKey, model);
            // DeepSeek/通义千问/文心一言/Moonshot 均兼容 OpenAI 协议
            case DEEPSEEK, DASHSCOPE, QIANFAN, MOONSHOT -> createOpenAiModel(baseUrl, apiKey, model);
        };
    }

    private ChatModel createOllamaModel(String baseUrl, String model) {
        OllamaApi api = new OllamaApi.Builder()
                .baseUrl(baseUrl)
                .webClientBuilder(webClientBuilder.clone())
                .restClientBuilder(restClientBuilder.clone())
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(api)
            .defaultOptions(OllamaChatOptions.builder().model(model).build())
                .build();
    }

    private ChatModel createOpenAiModel(String baseUrl, String apiKey, String model) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .webClientBuilder(webClientBuilder.clone())
                .restClientBuilder(restClientBuilder.clone())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
    }

    private ChatModel createAnthropicModel(String baseUrl, String apiKey, String model) {
        AnthropicApi api = new AnthropicApi.Builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .webClientBuilder(webClientBuilder.clone())
                .restClientBuilder(restClientBuilder.clone())
                .build();
        return AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(AnthropicChatOptions.builder().model(model).build())
                .build();
    }

    private ChatModel createZhiPuAiModel(String baseUrl, String apiKey, String model) {
        ZhiPuAiApi api = ZhiPuAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .webClientBuilder(webClientBuilder.clone())
                .restClientBuilder(restClientBuilder.clone())
                .build();
        return new ZhiPuAiChatModel(api,ZhiPuAiChatOptions.builder().model(model).build());
    }

    private String decryptApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isBlank()) {
            return "";
        }
        try {
            return CryptoUtil.decrypt(encryptedKey, cryptoSecretKey);
        } catch (IllegalStateException e) {
            log.warn("Failed to decrypt API key, using raw value: {}", e.getMessage());
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

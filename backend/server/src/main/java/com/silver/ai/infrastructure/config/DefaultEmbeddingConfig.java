package com.silver.ai.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 提供默认的 EmbeddingModel Bean，作为知识库统一的向量化模型来源。
 */
@Configuration
public class DefaultEmbeddingConfig {

    @Value("${app.default-embedding.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${app.default-embedding.model:nomic-embed-text}")
    private String model;

    @Bean
    @Primary
    public EmbeddingModel defaultEmbeddingModel() {
        OllamaApi api = new OllamaApi.Builder().baseUrl(baseUrl).build();
        return OllamaEmbeddingModel.builder()
                .ollamaApi(api)
                .defaultOptions(OllamaEmbeddingOptions.builder().model(model).build())
                .build();
    }
}

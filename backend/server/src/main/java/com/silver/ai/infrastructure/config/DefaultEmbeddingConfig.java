package com.silver.ai.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 提供默认的 EmbeddingModel Bean，作为知识库统一的向量化模型来源。
 */
@Configuration
public class DefaultEmbeddingConfig {

    @Value("${app.default-embedding.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${app.default-embedding.model:nomic-embed-text}")
    private String model;

    @Value("${app.default-embedding.dimensions:768}")
    private int dimensions;

    @Bean
    @Primary
    public EmbeddingModel defaultEmbeddingModel(WebClient.Builder webClientBuilder, RestClient.Builder restClientBuilder) {
        OllamaApi api = new OllamaApi.Builder()
                .baseUrl(baseUrl)
                .webClientBuilder(webClientBuilder.clone())
                .restClientBuilder(restClientBuilder.clone())
                .build();
        return OllamaEmbeddingModel.builder()
                .ollamaApi(api)
                .defaultOptions(OllamaEmbeddingOptions.builder().model(model).dimensions(dimensions).build())
                .build();
    }
}

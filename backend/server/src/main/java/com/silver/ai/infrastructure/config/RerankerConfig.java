package com.silver.ai.infrastructure.config;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.knowledge.port.RerankerPort;
import com.silver.ai.domain.provider.port.EncryptionPort;
import com.silver.ai.infrastructure.reranker.DynamicRerankerPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RerankerConfig {

    @Bean
    public RerankerPort rerankerPort(
            WebClient.Builder webClientBuilder,
            ChatOrchestratorConfig currentConfig,
            EncryptionPort encryptionPort,
            @Value("${app.reranker.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.reranker.model:bge-reranker-v2-m3}") String model,
            @Value("${app.reranker.api-key:}") String apiKey) {
        return new DynamicRerankerPort(
                webClientBuilder,
                currentConfig,
            encryptionPort,
                apiKey,
                baseUrl,
                model
        );
    }
}
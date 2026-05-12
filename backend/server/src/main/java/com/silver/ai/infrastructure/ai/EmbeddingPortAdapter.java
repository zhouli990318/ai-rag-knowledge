package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.port.EmbeddingPort;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 阻塞式 Embedding 适配器，供文档处理与语义分块使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingPortAdapter implements EmbeddingPort {

    private final ModelProviderRepository modelProviderRepository;
    private final EmbeddingModelRegistry embeddingModelRegistry;
    private final EmbeddingModel defaultEmbeddingModel;

    @Override
    public float[] embed(Long providerId, String text) {
        return resolveModel(providerId).embed(text);
    }

    @Override
    public List<float[]> embedBatch(Long providerId, List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        return resolveModel(providerId).embed(texts);
    }

    private EmbeddingModel resolveModel(Long providerId) {
        if (providerId == null) {
            return defaultEmbeddingModel;
        }

        ModelProvider provider = modelProviderRepository.findById(providerId).block();
        if (provider == null) {
            log.warn("Embedding provider {} not found, falling back to default embedding model", providerId);
            return defaultEmbeddingModel;
        }
        if (provider.getEmbeddingModel() == null || provider.getEmbeddingModel().isBlank()) {
            log.warn("Embedding provider {} has no embedding model configured, falling back to default embedding model", providerId);
            return defaultEmbeddingModel;
        }

        try {
            return embeddingModelRegistry.getOrCreate(provider);
        } catch (Exception ex) {
            log.warn("Failed to initialize embedding provider {}, falling back to default embedding model", providerId, ex);
            return defaultEmbeddingModel;
        }
    }
}
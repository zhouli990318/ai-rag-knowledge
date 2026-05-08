package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.provider.port.ModelCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 模型缓存适配器 — 桥接领域层 ModelCachePort 到 ChatModelRegistry/EmbeddingModelRegistry。
 */
@Component
@RequiredArgsConstructor
public class ModelCacheAdapter implements ModelCachePort {

    private final ChatModelRegistry chatModelRegistry;
    private final EmbeddingModelRegistry embeddingModelRegistry;

    @Override
    public void refreshCache(Long providerId) {
        chatModelRegistry.refresh(providerId);
        embeddingModelRegistry.refresh(providerId);
    }
}

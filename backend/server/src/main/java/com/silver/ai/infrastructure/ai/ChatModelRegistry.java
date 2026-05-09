package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * ChatModel 注册表 — 缓存已创建的 ChatModel 实例，支持热更新
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelRegistry {

    private final ChatModelFactory chatModelFactory;
    private final Cache<Long, ChatModel> modelCache = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    /**
     * 获取或创建 ChatModel
     */
    public ChatModel getOrCreate(ModelProvider provider) {
        return modelCache.get(provider.getId(), id -> {
            log.info("Creating ChatModel for provider: {} ({})", provider.getName(), provider.getProviderType());
            return chatModelFactory.createChatModel(provider);
        });
    }

    /**
     * 使用指定模型覆盖默认模型（临时用）
     */
    public ChatModel getWithModel(ModelProvider provider, String model) {
        if (model == null || model.equals(provider.getDefaultModel())) {
            return getOrCreate(provider);
        }
        // 对于临时指定的模型，不缓存，直接构建
        return chatModelFactory.createChatModel(
                ModelProvider.builder()
                        .id(provider.getId())
                        .providerType(provider.getProviderType())
                        .apiKey(provider.getApiKey())
                        .baseUrl(provider.getBaseUrl())
                        .defaultModel(model)
                        .enabled(provider.isEnabled())
                        .build()
        );
    }

    /**
     * 热更新：移除缓存，下次获取时重新创建
     */
    public void refresh(Long providerId) {
        modelCache.invalidate(providerId);
        log.info("ChatModel cache refreshed for provider: {}", providerId);
    }

    /**
     * 清除所有缓存
     */
    public void clearAll() {
        modelCache.invalidateAll();
    }
}

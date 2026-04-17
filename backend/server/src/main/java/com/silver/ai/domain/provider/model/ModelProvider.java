package com.silver.ai.domain.provider.model;

import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 模型提供商 — 聚合根
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProvider {

    private Long id;
    private String name;
    private ProviderType providerType;
    /** 加密存储的 API Key */
    private String apiKey;
    private String baseUrl;
    private String defaultModel;
    private String embeddingModel;
    private Integer embeddingDimensions;
    @Builder.Default
    private boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void enable() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateConfig(String name, String apiKey, String baseUrl,
                             String defaultModel, String embeddingModel, Integer embeddingDimensions) {
        this.name = name;
        if (apiKey != null && !apiKey.isBlank()) {
            this.apiKey = apiKey;
        }
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.embeddingModel = embeddingModel;
        this.embeddingDimensions = embeddingDimensions;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取实际使用的 base URL（优先使用配置值，否则使用厂商默认值）
     */
    public String getEffectiveBaseUrl() {
        return (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : providerType.getDefaultBaseUrl();
    }

    public void ensureEnabled() {
        if (!enabled) {
            throw new BusinessException(ErrorCode.PROVIDER_DISABLED, name);
        }
    }
}

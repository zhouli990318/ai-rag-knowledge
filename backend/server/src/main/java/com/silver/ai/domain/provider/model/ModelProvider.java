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

    // ── 健康状态与路由 ──
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;
    private LocalDateTime lastHealthCheckAt;
    @Builder.Default
    private int healthFailCount = 0;
    @Builder.Default
    private long avgFirstTokenMs = 0;
    /** 路由优先级，数值越小越优先 */
    @Builder.Default
    private int priority = 0;

    public enum HealthStatus {
        HEALTHY, UNHEALTHY, UNKNOWN
    }

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
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("提供商名称不能为空");
        }
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

    /** 标记健康 */
    public void markHealthy(long firstTokenMs) {
        this.healthStatus = HealthStatus.HEALTHY;
        this.lastHealthCheckAt = LocalDateTime.now();
        this.healthFailCount = 0;
        this.avgFirstTokenMs = firstTokenMs;
        this.updatedAt = LocalDateTime.now();
    }

    /** 标记不健康 */
    public void markUnhealthy() {
        this.healthStatus = HealthStatus.UNHEALTHY;
        this.lastHealthCheckAt = LocalDateTime.now();
        this.healthFailCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /** 是否可用（启用 + 健康） */
    public boolean isAvailable() {
        return enabled && healthStatus != HealthStatus.UNHEALTHY;
    }
}

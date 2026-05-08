package com.silver.ai.domain.provider.port;

/**
 * 模型缓存管理端口 — 领域层定义，基础设施层实现。
 * 用于在 Provider 配置变更后刷新模型实例缓存。
 */
public interface ModelCachePort {

    /**
     * 刷新指定 Provider 的模型缓存。
     */
    void refreshCache(Long providerId);
}

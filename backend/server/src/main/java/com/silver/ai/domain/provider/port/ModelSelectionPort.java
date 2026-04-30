package com.silver.ai.domain.provider.port;

import com.silver.ai.domain.provider.model.ModelProvider;

/**
 * 模型选择端口 — 领域层定义，领域服务实现。
 * 基础设施层通过此端口获取最优提供商，避免直接依赖领域服务。
 */
public interface ModelSelectionPort {

    /**
     * 根据偏好 providerId 选择最佳可用提供商（含健康检查和故障转移）
     */
    ModelProvider selectProvider(Long preferredProviderId);

    /**
     * 记录调用成功
     */
    void recordSuccess(Long providerId, long firstTokenMs);

    /**
     * 记录调用失败
     */
    void recordFailure(Long providerId);
}

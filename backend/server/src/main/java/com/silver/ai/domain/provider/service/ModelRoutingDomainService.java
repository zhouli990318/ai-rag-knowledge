package com.silver.ai.domain.provider.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.domain.provider.port.ModelSelectionPort;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

/**
 * 模型路由领域服务 — 健康检查、故障转移、优先级路由。
 */
@Slf4j
@RequiredArgsConstructor
public class ModelRoutingDomainService implements ModelSelectionPort {

    private final ModelProviderRepository providerRepository;
    private final ChatOrchestratorConfig config;

    /**
     * 选择最佳可用提供商：优先使用指定的 providerId，
     * 如果不可用则按优先级寻找备选。
     */
    @Override
    public ModelProvider selectProvider(Long preferredProviderId) {
        // 1. 尝试首选
        ModelProvider preferred = providerRepository.findById(preferredProviderId)
                .blockOptional()
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        if (preferred.isAvailable()) {
            return preferred;
        }

        log.warn("Preferred provider [{}] is unavailable (healthStatus={}), looking for fallback",
                preferred.getName(), preferred.getHealthStatus());

        // 2. 查找同类型可用备选
        List<ModelProvider> candidates = providerRepository.findAll()
                .filter(p -> p.isAvailable() && !p.getId().equals(preferredProviderId))
                .collectList()
                .blockOptional()
                .orElse(List.of());

        if (candidates.isEmpty()) {
            if (preferred.isEnabled()) {
                log.warn("No healthy fallback found for [{}], degrade to preferred provider for recovery probing",
                        preferred.getName());
                return preferred;
            }
            throw new BusinessException(ErrorCode.PROVIDER_NOT_AVAILABLE,
                    "No available provider found as fallback for " + preferred.getName());
        }

        // 按优先级排序（priority 小的优先），同优先级按首包延迟排序
        ModelProvider fallback = candidates.stream()
                .sorted(Comparator.comparingInt(ModelProvider::getPriority)
                        .thenComparingLong(ModelProvider::getAvgFirstTokenMs))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_AVAILABLE));

        log.info("Fallback provider selected: [{}] (priority={}, avgFirstTokenMs={})",
                fallback.getName(), fallback.getPriority(), fallback.getAvgFirstTokenMs());

        return fallback;
    }

    /**
     * 记录调用成功（更新健康状态和首包延迟）。
     */
    @Override
    public Mono<Void> recordSuccess(Long providerId, long firstTokenMs) {
        return providerRepository.findById(providerId)
                .flatMap(provider -> {
                    provider.markHealthy(firstTokenMs);
                    return providerRepository.save(provider);
                })
                .doOnError(e -> log.warn("Failed to record success for provider {}: {}", providerId, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    /**
     * 记录调用失败（更新故障计数）。
     */
    @Override
    public Mono<Void> recordFailure(Long providerId) {
        return providerRepository.findById(providerId)
                .flatMap(provider -> {
                    provider.markUnhealthy();
                    return providerRepository.save(provider);
                })
                .doOnError(e -> log.warn("Failed to record failure for provider {}: {}", providerId, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}

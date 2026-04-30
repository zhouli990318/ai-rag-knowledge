package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import reactor.core.publisher.Mono;

/**
 * 系统设置仓储端口 — 持久化编排器配置。
 */
public interface SystemSettingsRepository {

    Mono<ChatOrchestratorConfig> load();

    Mono<ChatOrchestratorConfig> save(ChatOrchestratorConfig config);
}

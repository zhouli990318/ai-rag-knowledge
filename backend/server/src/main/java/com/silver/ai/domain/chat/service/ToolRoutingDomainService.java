package com.silver.ai.domain.chat.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.model.ToolIndexEntry;
import com.silver.ai.domain.chat.model.ToolMode;
import com.silver.ai.domain.chat.port.McpToolPort;
import com.silver.ai.domain.chat.model.ToolCallbackHandle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 工具路由领域服务 — 根据意图 + 会话工具模式决定是否注入 MCP 工具。
 *
 * ToolMode 三态：
 * - OFF:      完全不注入
 * - AUTO:     语义检索相关工具（启用时），或注入全部 active 工具（禁用时）
 * - SPECIFIC: 仅注入用户指定的 mcpServerIds
 */
@Slf4j
@RequiredArgsConstructor
public class ToolRoutingDomainService {

    private final McpToolPort mcpToolPort;
    private final ToolIndexDomainService toolIndexService;
    private final ChatOrchestratorConfig config;

    public ToolDecision decide(IntentResult intentResult, ToolMode toolMode,
                               List<Long> mcpServerIds, String userQuery) {
        ToolMode effectiveMode = toolMode == null ? ToolMode.AUTO : toolMode;

        if (effectiveMode == ToolMode.OFF) {
            return ToolDecision.noTool();
        }

        List<ToolCallbackHandle> callbacks;
        if (effectiveMode == ToolMode.SPECIFIC) {
            if (mcpServerIds == null || mcpServerIds.isEmpty()) {
                return ToolDecision.noTool();
            }
            callbacks = mcpToolPort.getToolCallbacks(mcpServerIds);
        } else {
            // AUTO 模式
            callbacks = resolveAutoTools(userQuery);
        }

        if (callbacks.isEmpty()) {
            return ToolDecision.noTool();
        }

        boolean autoExecute = effectiveMode == ToolMode.SPECIFIC
                || intentResult == null
                || intentResult.getConfidence() >= config.getToolAutoExecuteThreshold();
        return new ToolDecision(callbacks, autoExecute);
    }

    /**
     * AUTO 模式工具解析：
     * - 语义检索启用时 → 根据用户查询召回 topK 个相关工具
     * - 语义检索禁用时 → 全量注入（兼容旧行为）
     */
    private List<ToolCallbackHandle> resolveAutoTools(String userQuery) {
        if (config.isToolSemanticRetrievalEnabled() && userQuery != null && !userQuery.isBlank()) {
            List<ToolIndexEntry> relevant = toolIndexService.retrieveRelevantTools(
                    userQuery, config.getToolRetrievalTopK(), config.getToolRetrievalThreshold());

            if (!relevant.isEmpty()) {
                List<Long> toolIds = relevant.stream()
                        .map(ToolIndexEntry::toolId)
                        .toList();
                List<ToolCallbackHandle> callbacks = mcpToolPort.getToolCallbacksByToolIds(toolIds);
                if (!callbacks.isEmpty()) {
                    log.debug("Semantic tool retrieval: {} relevant tools matched from {} candidates",
                            callbacks.size(), relevant.size());
                    return callbacks;
                }
                log.warn("Semantic retrieval found {} tools but getToolCallbacksByToolIds returned empty, " +
                                "toolIds={}. Falling back to all active tools.",
                        relevant.size(), toolIds);
            } else {
                log.debug("Semantic tool retrieval returned no results for query='{}'", userQuery);
            }

            // 语义检索无结果或回调为空 → fallback 到全量活跃工具
            if (config.isToolFallbackEnabled()) {
                List<ToolCallbackHandle> fallback = mcpToolPort.getAllActiveToolCallbacks();
                if (!fallback.isEmpty()) {
                    log.debug("Fallback: injecting all {} active tools", fallback.size());
                    return fallback;
                }
            }
            return List.of();
        }

        // 语义检索禁用 → 全量注入
        return mcpToolPort.getAllActiveToolCallbacks();
    }

    public record ToolDecision(
            List<ToolCallbackHandle> toolCallbacks,
            boolean autoExecute
    ) {
        public static ToolDecision noTool() {
            return new ToolDecision(List.of(), false);
        }

        public boolean hasTools() {
            return toolCallbacks != null && !toolCallbacks.isEmpty();
        }
    }
}

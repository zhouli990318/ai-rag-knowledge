package com.silver.ai.domain.chat.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.model.ToolMode;
import com.silver.ai.infrastructure.mcp.McpToolCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工具路由领域服务 — 根据意图 + 会话工具模式决定是否注入 MCP 工具。
 *
 * ToolMode 三态：
 * - OFF:      完全不注入
 * - AUTO:     注入所有 active 且健康的 MCP 源工具，由模型自行决定是否调用
 * - SPECIFIC: 仅注入用户指定的 mcpServerIds
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolRoutingDomainService {

    private final McpToolCallbackService mcpToolCallbackService;
    private final ChatOrchestratorConfig config;

    public ToolDecision decide(IntentResult intentResult, ToolMode toolMode, List<Long> mcpServerIds) {
        ToolMode effectiveMode = toolMode == null ? ToolMode.AUTO : toolMode;

        if (effectiveMode == ToolMode.OFF) {
            return ToolDecision.noTool();
        }

        List<ToolCallback> callbacks;
        if (effectiveMode == ToolMode.SPECIFIC) {
            if (mcpServerIds == null || mcpServerIds.isEmpty()) {
                return ToolDecision.noTool();
            }
            callbacks = mcpToolCallbackService.getToolCallbacks(mcpServerIds);
        } else {
            // AUTO: 总是注入所有 active 源的工具，让 LLM 自己决定是否调用
            callbacks = mcpToolCallbackService.getAllActiveToolCallbacks();
        }

        if (callbacks.isEmpty()) {
            return ToolDecision.noTool();
        }

        // AUTO/SPECIFIC 都允许自动执行；仅在模型返回工具调用时生效
        boolean autoExecute = effectiveMode == ToolMode.SPECIFIC
                || intentResult == null
                || intentResult.getConfidence() >= config.getToolAutoExecuteThreshold();
        return new ToolDecision(callbacks, autoExecute);
    }

    public record ToolDecision(
            List<ToolCallback> toolCallbacks,
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

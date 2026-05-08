package com.silver.ai.domain.chat.model;

/**
 * MCP 工具定义 — 领域层值对象，替代基础设施层的 McpToolDefinition。
 */
public record ToolDefinition(
        Long id,
        Long apiSourceId,
        String toolName,
        String toolDescription,
        String parameterSchema,
        boolean enabled
) {
}

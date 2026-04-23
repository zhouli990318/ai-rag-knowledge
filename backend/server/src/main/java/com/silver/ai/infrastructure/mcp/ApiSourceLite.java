package com.silver.ai.infrastructure.mcp;

/**
 * MCP 源轻量视图 — 仅携带工具路由所需字段。
 */
public record ApiSourceLite(
        Long id,
        String name,
        Boolean active,
        String healthStatus
) {
}

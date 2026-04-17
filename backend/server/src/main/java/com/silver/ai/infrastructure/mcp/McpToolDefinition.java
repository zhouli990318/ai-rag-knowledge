package com.silver.ai.infrastructure.mcp;

public record McpToolDefinition(
        Long id,
        Long apiSourceId,
        String toolName,
        String toolDescription,
        String parameterSchema,
        boolean enabled
) {
}
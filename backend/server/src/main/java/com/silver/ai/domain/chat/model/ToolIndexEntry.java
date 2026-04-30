package com.silver.ai.domain.chat.model;

/**
 * MCP 工具索引条目 — 用于向量化和语义检索。
 */
public record ToolIndexEntry(
        Long toolId,
        Long apiSourceId,
        String toolName,
        String toolDescription,
        String parameterSchema
) {
    /**
     * 拼接为向量化文本：工具名 + 描述。
     * 不含 parameterSchema（JSON 噪音大，对语义匹配贡献低）。
     */
    public String toEmbeddingText() {
        StringBuilder sb = new StringBuilder();
        if (toolName != null && !toolName.isBlank()) {
            sb.append(toolName);
        }
        if (toolDescription != null && !toolDescription.isBlank()) {
            if (!sb.isEmpty()) sb.append(": ");
            sb.append(toolDescription);
        }
        return sb.isEmpty() ? "unknown tool" : sb.toString();
    }
}

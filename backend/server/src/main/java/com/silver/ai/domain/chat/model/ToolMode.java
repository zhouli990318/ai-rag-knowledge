package com.silver.ai.domain.chat.model;

/**
 * 会话工具模式。
 * - OFF: 完全不注入工具
 * - AUTO: AI 自动发现 — TOOL/HYBRID 意图时注入所有健康的 MCP 源工具（默认）
 * - SPECIFIC: 仅注入用户指定的 MCP 源（使用 Conversation.mcpServerIds）
 */
public enum ToolMode {
    OFF,
    AUTO,
    SPECIFIC;

    public static ToolMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return ToolMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}

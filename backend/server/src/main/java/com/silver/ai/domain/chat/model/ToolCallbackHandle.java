package com.silver.ai.domain.chat.model;

/**
 * 工具回调句柄 — 领域层对 MCP 工具回调的抽象。
 * 具体实现由基础设施层提供，领域层仅传递不消费。
 */
public interface ToolCallbackHandle {

    String getToolName();
}

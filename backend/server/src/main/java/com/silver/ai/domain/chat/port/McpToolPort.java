package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.ToolCallbackHandle;
import com.silver.ai.domain.chat.model.ToolDefinition;

import java.util.List;

/**
 * MCP 工具端口 — 领域层定义，基础设施层实现。
 * 用于获取可注入对话的 MCP 工具回调列表。
 */
public interface McpToolPort {

    /**
     * 获取指定 MCP 源的工具回调列表
     */
    List<ToolCallbackHandle> getToolCallbacks(List<Long> sourceIds);

    /**
     * 获取所有活跃 MCP 源的工具回调列表
     */
    List<ToolCallbackHandle> getAllActiveToolCallbacks();

    /**
     * 获取所有活跃 MCP 源的原始工具定义（用于索引）
     */
    List<ToolDefinition> getAllActiveToolDefinitions();

    /**
     * 按工具 ID 列表获取对应的工具回调（语义检索后精确转换）
     */
    List<ToolCallbackHandle> getToolCallbacksByToolIds(List<Long> toolIds);
}

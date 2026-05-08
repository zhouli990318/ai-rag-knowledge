package com.silver.ai.infrastructure.mcp;

import com.silver.ai.domain.chat.model.ToolCallbackHandle;
import org.springframework.ai.tool.ToolCallback;

/**
 * ToolCallback 句柄适配器 — 将 Spring AI ToolCallback 包装为领域层 ToolCallbackHandle。
 */
public class ToolCallbackHandleAdapter implements ToolCallbackHandle {

    private final ToolCallback delegate;

    public ToolCallbackHandleAdapter(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getToolName() {
        return delegate.getToolDefinition().name();
    }

    /**
     * 解包获取底层 Spring AI ToolCallback，仅供基础设施层使用。
     */
    public ToolCallback unwrap() {
        return delegate;
    }

    /**
     * 批量解包工具列表。
     */
    public static java.util.List<ToolCallback> unwrapAll(java.util.List<ToolCallbackHandle> handles) {
        return handles.stream()
                .filter(h -> h instanceof ToolCallbackHandleAdapter)
                .map(h -> ((ToolCallbackHandleAdapter) h).unwrap())
                .toList();
    }
}

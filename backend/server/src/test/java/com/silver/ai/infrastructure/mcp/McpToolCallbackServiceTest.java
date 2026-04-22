package com.silver.ai.infrastructure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpToolCallbackServiceTest {

    @Test
    void getToolCallbacksShouldSkipFailingSourceAndKeepHealthySources() {
        McpToolGatewayClient gatewayClient = mock(McpToolGatewayClient.class);
        McpToolCallbackService service = new McpToolCallbackService(gatewayClient, new ObjectMapper());
        McpToolDefinition healthyTool = new McpToolDefinition(
                2L,
                200L,
                "healthyTool",
                "Healthy tool",
                "",
                true);
        when(gatewayClient.listTools(100L)).thenThrow(new BusinessException(
                ErrorCode.CHAT_STREAM_ERROR,
                "gateway down"));
        when(gatewayClient.listTools(200L)).thenReturn(List.of(healthyTool));

        List<ToolCallback> callbacks = service.getToolCallbacks(List.of(100L, 200L));

        assertEquals(1, callbacks.size());
        assertEquals("healthyTool", callbacks.get(0).getToolDefinition().name());
    }
}
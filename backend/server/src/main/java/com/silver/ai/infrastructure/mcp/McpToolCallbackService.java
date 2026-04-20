package com.silver.ai.infrastructure.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class McpToolCallbackService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final McpToolGatewayClient mcpToolGatewayClient;
    private final ObjectMapper objectMapper;

    public List<ToolCallback> getToolCallbacks(List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(sourceId -> mcpToolGatewayClient.listTools(sourceId).stream())
                .filter(McpToolDefinition::enabled)
                .map(this::createToolCallback)
                .collect(Collectors.toList());
    }

    private ToolCallback createToolCallback(McpToolDefinition toolDefinition) {
        String inputSchema = normalizeSchema(toolDefinition.parameterSchema());

        return FunctionToolCallback.<Map<String, Object>, String>builder(
                        toolDefinition.toolName(),
                        (arguments, toolContext) -> invokeTool(toolDefinition.id(), arguments, toolContext))
                .description(normalizeDescription(toolDefinition))
                .inputSchema(inputSchema)
                .inputType(MAP_TYPE)
                .build();
    }

    private String invokeTool(Long toolId, Map<String, Object> arguments, ToolContext toolContext) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (arguments != null) {
            payload.putAll(arguments);
        }
        if (toolContext != null && toolContext.getContext() != null && !toolContext.getContext().isEmpty()) {
            payload.put("_toolContext", toolContext.getContext());
        }

        try {
            return mcpToolGatewayClient.invokeTool(toolId, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("MCP 工具参数序列化失败", ex);
        }
    }

    private String normalizeDescription(McpToolDefinition toolDefinition) {
        if (toolDefinition.toolDescription() == null || toolDefinition.toolDescription().isBlank()) {
            return "Invoke MCP tool " + toolDefinition.toolName();
        }
        return toolDefinition.toolDescription();
    }

    private String normalizeSchema(String parameterSchema) {
        if (parameterSchema == null || parameterSchema.isBlank()) {
            return "{\"type\":\"object\",\"properties\":{}}";
        }
        return parameterSchema;
    }
}
package com.silver.ai.infrastructure.mcp;

import com.silver.ai.domain.chat.port.McpToolPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class McpToolCallbackService implements McpToolPort {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final long ACTIVE_CACHE_TTL_MS = 30_000L;

    private final McpToolGatewayClient mcpToolGatewayClient;
    private final ObjectMapper objectMapper;

    private volatile List<ToolCallback> cachedActiveCallbacks = Collections.emptyList();
    private volatile List<McpToolDefinition> cachedActiveDefinitions = Collections.emptyList();
    private volatile long cachedActiveAt = 0L;

    @Override
    public List<ToolCallback> getToolCallbacks(List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(this::loadToolsSafely)
                .filter(McpToolDefinition::enabled)
                .map(this::createToolCallback)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolCallback> getAllActiveToolCallbacks() {
        refreshCacheIfNeeded();
        return cachedActiveCallbacks;
    }

    @Override
    public List<McpToolDefinition> getAllActiveToolDefinitions() {
        refreshCacheIfNeeded();
        return cachedActiveDefinitions;
    }

    @Override
    public List<ToolCallback> getToolCallbacksByToolIds(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return Collections.emptyList();
        }
        var idSet = new java.util.HashSet<>(toolIds);
        refreshCacheIfNeeded();
        return cachedActiveDefinitions.stream()
                .filter(def -> idSet.contains(def.id()))
                .map(this::createToolCallback)
                .collect(Collectors.toList());
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - cachedActiveAt < ACTIVE_CACHE_TTL_MS) {
            return;
        }
        synchronized (this) {
            if (now - cachedActiveAt < ACTIVE_CACHE_TTL_MS) {
                return;
            }
            List<Long> sourceIds = mcpToolGatewayClient.listActiveSourceIds();
            List<McpToolDefinition> definitions = sourceIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .flatMap(this::loadToolsSafely)
                    .filter(McpToolDefinition::enabled)
                    .toList();
            cachedActiveDefinitions = definitions;
            cachedActiveCallbacks = definitions.stream()
                    .map(this::createToolCallback)
                    .collect(Collectors.toList());
            cachedActiveAt = now;
            log.debug("Refreshed global MCP tool cache: {} tools from {} sources",
                    cachedActiveCallbacks.size(), sourceIds.size());
        }
    }

    private Stream<McpToolDefinition> loadToolsSafely(Long sourceId) {
        try {
            return mcpToolGatewayClient.listTools(sourceId).stream();
        } catch (RuntimeException ex) {
            log.warn("Skip MCP source {} because tool loading failed", sourceId, ex);
            return Stream.empty();
        }
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
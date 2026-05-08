package com.silver.ai.infrastructure.mcp;

import com.silver.ai.domain.chat.model.ToolCallbackHandle;
import com.silver.ai.domain.chat.model.ToolDefinition;
import com.silver.ai.domain.chat.port.McpToolPort;
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
public class McpToolCallbackService implements McpToolPort {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final long ACTIVE_CACHE_TTL_MS = 30_000L;

    private final McpToolGatewayClient mcpToolGatewayClient;
    private final ObjectMapper objectMapper;

    private volatile List<ToolCallbackHandle> cachedActiveCallbacks = Collections.emptyList();
    private volatile List<McpToolDefinition> cachedActiveDefinitions = Collections.emptyList();
    private volatile long cachedActiveAt = 0L;
    private final Object cacheLock = new Object();

    @Override
    public List<ToolCallbackHandle> getToolCallbacks(List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(this::loadToolsSafely)
                .filter(McpToolDefinition::enabled)
                .map(this::createToolCallbackHandle)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolCallbackHandle> getAllActiveToolCallbacks() {
        refreshCacheIfNeeded();
        return cachedActiveCallbacks;
    }

    @Override
    public List<ToolDefinition> getAllActiveToolDefinitions() {
        refreshCacheIfNeeded();
        return cachedActiveDefinitions.stream()
                .map(this::toDomainDefinition)
                .toList();
    }

    @Override
    public List<ToolCallbackHandle> getToolCallbacksByToolIds(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return Collections.emptyList();
        }
        var idSet = new java.util.HashSet<>(toolIds);
        refreshCacheIfNeeded();
        List<ToolCallbackHandle> result = cachedActiveDefinitions.stream()
                .filter(def -> idSet.contains(def.id()))
                .map(this::createToolCallbackHandle)
                .collect(Collectors.toList());
        if (result.isEmpty() && !cachedActiveDefinitions.isEmpty()) {
            log.warn("getToolCallbacksByToolIds: requested {} tool IDs {} but no match in cache ({} cached defs: {})",
                    toolIds.size(), toolIds, cachedActiveDefinitions.size(),
                    cachedActiveDefinitions.stream().map(McpToolDefinition::id).toList());
        }
        log.debug("getToolCallbacksByToolIds: requested={}, cacheSize={}, matched={}",
                toolIds.size(), cachedActiveDefinitions.size(), result.size());
        return result;
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - cachedActiveAt < ACTIVE_CACHE_TTL_MS) {
            return;
        }
        synchronized (cacheLock) {
            // Double-check after acquiring lock
            now = System.currentTimeMillis();
            if (now - cachedActiveAt < ACTIVE_CACHE_TTL_MS) {
                return;
            }
            List<Long> sourceIds = mcpToolGatewayClient.listActiveSourceIds();
            log.info("MCP cache refresh: found {} active source IDs: {}", sourceIds.size(), sourceIds);
            List<McpToolDefinition> definitions = sourceIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .flatMap(this::loadToolsSafely)
                    .filter(McpToolDefinition::enabled)
                    .toList();
            cachedActiveDefinitions = definitions;
            cachedActiveCallbacks = definitions.stream()
                    .map(this::createToolCallbackHandle)
                    .collect(Collectors.toList());
            cachedActiveAt = now;
            log.info("MCP cache refresh complete: {} tools from {} sources. Tool IDs: {}",
                    cachedActiveCallbacks.size(), sourceIds.size(),
                    definitions.stream().map(McpToolDefinition::id).toList());
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

    private ToolCallbackHandle createToolCallbackHandle(McpToolDefinition toolDefinition) {
        return new ToolCallbackHandleAdapter(createToolCallback(toolDefinition));
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
        } catch (IllegalArgumentException ex) {
            log.warn("MCP tool {} parameter error: {}", toolId, ex.getMessage());
            return "[MCP 工具参数错误: " + ex.getMessage() + "]";
        } catch (Exception ex) {
            log.warn("MCP tool {} invocation failed: {}", toolId, ex.getMessage());
            return "[MCP 工具调用失败: " + ex.getMessage() + "]";
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

    private ToolDefinition toDomainDefinition(McpToolDefinition def) {
        return new ToolDefinition(def.id(), def.apiSourceId(), def.toolName(),
                def.toolDescription(), def.parameterSchema(), def.enabled());
    }
}
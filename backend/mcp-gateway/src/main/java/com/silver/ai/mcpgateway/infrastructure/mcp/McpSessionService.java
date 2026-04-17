package com.silver.ai.mcpgateway.infrastructure.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.mcpgateway.domain.model.McpAgentSession;
import com.silver.ai.mcpgateway.infrastructure.config.McpSessionProperties;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class McpSessionService {

    private static final String SESSION_MAP_PREFIX = "mcp:gateway:sessions:";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final McpSessionProperties sessionProperties;

    public McpSessionService(RedissonClient redissonClient, ObjectMapper objectMapper,
                             McpSessionProperties sessionProperties) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.sessionProperties = sessionProperties;
    }

    public McpAgentSession provisionSession(Long sourceId, String sessionId, ServerRequest request) {
        if (sourceId == null || sessionId == null || sessionId.isBlank()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Map<String, String> headers = filterHeaders(request.headers().asHttpHeaders().toSingleValueMap());
        RMapCache<String, String> sessionMap = sessionMap(sourceId);
        McpAgentSession session = readSession(sessionMap, sessionId)
                .orElseGet(() -> McpAgentSession.builder()
                        .sourceId(sourceId)
                        .sessionId(sessionId)
                        .createdAt(now)
                        .build());
        session.touch(now, headers);
        writeSession(sessionMap, session);
        return session;
    }

    public McpAgentSession recordToolCall(Long sourceId, String sessionId, String toolName,
                                          Map<String, Object> arguments, McpSyncServerExchange exchange) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        RMapCache<String, String> sessionMap = sessionMap(sourceId);
        McpAgentSession session = readSession(sessionMap, sessionId)
                .orElseGet(() -> McpAgentSession.builder()
                        .sourceId(sourceId)
                        .sessionId(sessionId)
                        .createdAt(now)
                        .build());

        Map<String, String> headers = extractHeaders(exchange.transportContext());
        session.touch(now, headers);
        session.initialize(
                Optional.ofNullable(exchange.getClientInfo()).map(info -> info.name()).orElse(null),
                Optional.ofNullable(exchange.getClientInfo()).map(info -> info.version()).orElse(null),
                writeValue(exchange.getClientCapabilities()),
                now
        );
        session.recordToolCall(toolName, writeValue(arguments == null ? Map.of() : arguments), now);
        writeSession(sessionMap, session);
        return session;
    }

    public Map<String, Object> buildInvocationMetadata(McpAgentSession session) {
        if (session == null) {
            return Map.of();
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sessionId", session.getSessionId());
        metadata.put("sourceId", session.getSourceId());
        metadata.put("clientName", session.getClientName());
        metadata.put("clientVersion", session.getClientVersion());
        metadata.put("toolCallCount", session.getToolCallCount());
        metadata.put("lastToolName", session.getLastToolName());
        metadata.put("lastToolCallAt", session.getLastToolCallAt() == null ? null : session.getLastToolCallAt().toString());
        metadata.put("transportHeaders", session.getTransportHeaders());
        return metadata;
    }

    public void evictSource(Long sourceId) {
        if (sourceId != null) {
            sessionMap(sourceId).delete();
        }
    }

    public void removeSession(Long sourceId, String sessionId) {
        if (sourceId != null && sessionId != null) {
            sessionMap(sourceId).remove(sessionId);
        }
    }

    private RMapCache<String, String> sessionMap(Long sourceId) {
        return redissonClient.getMapCache(SESSION_MAP_PREFIX + sourceId);
    }

    private Optional<McpAgentSession> readSession(RMapCache<String, String> sessionMap, String sessionId) {
        String value = sessionMap.get(sessionId);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, McpAgentSession.class));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to read MCP session {}", sessionId, ex);
            return Optional.empty();
        }
    }

    private void writeSession(RMapCache<String, String> sessionMap, McpAgentSession session) {
        sessionMap.put(session.getSessionId(), writeValue(session), sessionProperties.getTtl().toMillis(), TimeUnit.MILLISECONDS);
    }

    private Map<String, String> extractHeaders(McpTransportContext transportContext) {
        if (transportContext == null) {
            return Map.of();
        }
        Object rawHeaders = transportContext.get(McpTransportMetadataKeys.REQUEST_HEADERS);
        if (!(rawHeaders instanceof Map<?, ?> headerMap)) {
            return Map.of();
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headerMap.forEach((key, value) -> {
            if (key != null && value != null) {
                headers.put(String.valueOf(key), String.valueOf(value));
            }
        });
        return headers;
    }

    private Map<String, String> filterHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> allowList = sessionProperties.getAllowedPassthroughHeaders();
        List<String> blockList = sessionProperties.getBlockedPassthroughHeaders();
        Map<String, String> filtered = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            String lowerName = name.toLowerCase(Locale.ROOT);
            boolean allowed = allowList.stream().anyMatch(item -> item.equalsIgnoreCase(name));
            boolean blocked = blockList.stream().anyMatch(item -> item.equalsIgnoreCase(name));
            if (allowed && !blocked && value != null && !value.isBlank()) {
                filtered.put(lowerName, value);
            }
        });
        return filtered;
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize MCP session payload", ex);
        }
    }
}
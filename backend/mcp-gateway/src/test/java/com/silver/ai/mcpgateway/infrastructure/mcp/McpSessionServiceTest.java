package com.silver.ai.mcpgateway.infrastructure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.mcpgateway.domain.model.McpAgentSession;
import com.silver.ai.mcpgateway.infrastructure.config.McpSessionProperties;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpSessionServiceTest {

    @Test
    void provisionSessionShouldPersistFilteredHeaders() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RMapCache<String, String> sessionCache = mock(RMapCache.class);
        doReturn(sessionCache).when(redissonClient).getMapCache("mcp:gateway:sessions:7");

        McpSessionProperties properties = new McpSessionProperties();
        properties.setTtl(Duration.ofMinutes(30));
        properties.setAllowedPassthroughHeaders(List.of("X-Request-Id", "Traceparent"));
        properties.setBlockedPassthroughHeaders(List.of("Authorization"));

        McpSessionService service = new McpSessionService(redissonClient, new ObjectMapper(), properties);
        ServerRequest request = ServerRequest.create(
                MockServerWebExchange.from(
                        MockServerHttpRequest.post("/api/v1/mcp/sources/7/mcp/message?sessionId=s-1")
                                .header("X-Request-Id", "req-1")
                                .header("Traceparent", "00-abc")
                                .header("Authorization", "ignored")
                                .build()
                ),
                List.<HttpMessageReader<?>>of()
        );

        McpAgentSession session = service.provisionSession(7L, "s-1", request);

        assertNotNull(session);
        assertEquals("s-1", session.getSessionId());
        assertEquals("req-1", session.getTransportHeaders().get("x-request-id"));
        assertEquals("00-abc", session.getTransportHeaders().get("traceparent"));
        verify(sessionCache).put(eq("s-1"), org.mockito.ArgumentMatchers.anyString(), eq(Duration.ofMinutes(30).toMillis()), eq(java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    @Test
    void recordToolCallShouldMergeExchangeMetadataIntoSession() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RMapCache<String, String> sessionCache = mock(RMapCache.class);
        doReturn(sessionCache).when(redissonClient).getMapCache("mcp:gateway:sessions:9");

        McpSessionProperties properties = new McpSessionProperties();
        properties.setTtl(Duration.ofMinutes(10));
        properties.setAllowedPassthroughHeaders(List.of("X-Request-Id"));

        McpSessionService service = new McpSessionService(redissonClient, new ObjectMapper(), properties);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.getClientInfo()).thenReturn(new McpSchema.Implementation("tester", "1.0.0"));
        when(exchange.getClientCapabilities()).thenReturn(mock(McpSchema.ClientCapabilities.class));
        when(exchange.transportContext()).thenReturn(McpTransportContext.create(Map.of(
                McpTransportMetadataKeys.REQUEST_HEADERS, Map.of("x-request-id", "req-9")
        )));

        McpAgentSession session = service.recordToolCall(9L, "session-9", "tool-a", Map.of("id", 1), exchange);

        assertEquals("tester", session.getClientName());
        assertEquals("tool-a", session.getLastToolName());
        assertEquals(1L, session.getToolCallCount());
        assertEquals("req-9", session.getTransportHeaders().get("x-request-id"));
    }

    @Test
    void recordToolCallShouldNotPersistBlockedHeadersFromTransportContext() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RMapCache<String, String> sessionCache = mock(RMapCache.class);
        doReturn(sessionCache).when(redissonClient).getMapCache("mcp:gateway:sessions:10");

        McpSessionProperties properties = new McpSessionProperties();
        properties.setTtl(Duration.ofMinutes(10));
        properties.setAllowedPassthroughHeaders(List.of("X-Request-Id", "Authorization"));
        properties.setBlockedPassthroughHeaders(List.of("Authorization"));

        McpSessionService service = new McpSessionService(redissonClient, new ObjectMapper(), properties);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.getClientInfo()).thenReturn(new McpSchema.Implementation("tester", "1.0.0"));
        when(exchange.getClientCapabilities()).thenReturn(mock(McpSchema.ClientCapabilities.class));
        when(exchange.transportContext()).thenReturn(McpTransportContext.create(Map.of(
                McpTransportMetadataKeys.REQUEST_HEADERS, Map.of(
                        "x-request-id", "req-10",
                        "authorization", "secret-token"
                )
        )));

        McpAgentSession session = service.recordToolCall(10L, "session-10", "tool-a", Map.of("id", 1), exchange);

        assertEquals("req-10", session.getTransportHeaders().get("x-request-id"));
        assertFalse(session.getTransportHeaders().containsKey("authorization"));
    }

    @Test
    void recordToolCallShouldReturnTransientSessionWhenSessionIdMissing() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        McpSessionProperties properties = new McpSessionProperties();
        properties.setTtl(Duration.ofMinutes(10));

        McpSessionService service = new McpSessionService(redissonClient, new ObjectMapper(), properties);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.getClientInfo()).thenReturn(new McpSchema.Implementation("tester", "1.0.0"));
        when(exchange.getClientCapabilities()).thenReturn(mock(McpSchema.ClientCapabilities.class));
        when(exchange.transportContext()).thenReturn(McpTransportContext.create(Map.of(
                McpTransportMetadataKeys.REQUEST_HEADERS, Map.of("x-request-id", "req-11")
        )));

        McpAgentSession session = assertDoesNotThrow(() -> service.recordToolCall(11L, null, "tool-a", Map.of("id", 1), exchange));

        assertNotNull(session);
        assertNull(session.getSessionId());
        assertEquals("tool-a", session.getLastToolName());
        assertEquals(1L, session.getToolCallCount());
        verify(redissonClient, never()).getMapCache("mcp:gateway:sessions:11");
    }
}
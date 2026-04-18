package com.silver.ai.mcpgateway.infrastructure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.mcpgateway.domain.model.ApiSource;
import com.silver.ai.mcpgateway.domain.port.ApiSourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SourceScopedMcpServerRegistryTest {

    @Test
    void routeShouldMatchSourceScopedSsePath() {
    ApiSourceRepository apiSourceRepository = mock(ApiSourceRepository.class);
    DynamicApiToolCallbackProvider toolCallbackProvider = mock(DynamicApiToolCallbackProvider.class);
    when(toolCallbackProvider.getToolCallbacksForSource(1L)).thenReturn(new ToolCallback[0]);

    SourceScopedMcpServerRegistry registry = new SourceScopedMcpServerRegistry(
        apiSourceRepository,
        toolCallbackProvider,
        new ObjectMapper(),
        mock(McpSessionService.class)
    );
    setField(registry, "serverName", "mcp-gateway");
    setField(registry, "serverVersion", "2.0.0");
    setField(registry, "requestTimeout", Duration.ofSeconds(30));
    setField(registry, "sseEndpoint", "/sse");
    setField(registry, "messageEndpoint", "/mcp/message");
    setField(registry, "keepAliveInterval", Duration.ofSeconds(15));

    registry.refreshSource(ApiSource.builder().id(1L).name("demo").active(true).build());

    ServerRequest request = ServerRequest.create(
        MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/mcp/sources/1/sse").build()),
        List.<HttpMessageReader<?>>of()
    );

    HandlerFunction<ServerResponse> handler = registry.route(request).block();

    assertNotNull(handler);
    assertEquals("/api/v1/mcp/sources/1/sse", registry.sourceSsePath(1L));
    assertEquals("/api/v1/mcp/sources/1/mcp/message", registry.sourceMessagePath(1L));
    }

    @Test
    void extractSourceIdShouldAcceptPathWithLeadingSlash() {
        SourceScopedMcpServerRegistry registry = new SourceScopedMcpServerRegistry(
                mock(ApiSourceRepository.class),
                mock(DynamicApiToolCallbackProvider.class),
                new ObjectMapper(),
                mock(McpSessionService.class)
        );

        assertEquals(1L, registry.extractSourceId("/api/v1/mcp/sources/1/sse"));
    }

    @Test
    void extractSourceIdShouldAcceptPathWithoutLeadingSlash() {
        SourceScopedMcpServerRegistry registry = new SourceScopedMcpServerRegistry(
                mock(ApiSourceRepository.class),
                mock(DynamicApiToolCallbackProvider.class),
                new ObjectMapper(),
                mock(McpSessionService.class)
        );

        assertEquals(1L, registry.extractSourceId("api/v1/mcp/sources/1/sse"));
    }

    @Test
    void extractSourceIdShouldRejectNonSourcePath() {
        SourceScopedMcpServerRegistry registry = new SourceScopedMcpServerRegistry(
                mock(ApiSourceRepository.class),
                mock(DynamicApiToolCallbackProvider.class),
                new ObjectMapper(),
                mock(McpSessionService.class)
        );

        assertNull(registry.extractSourceId("/favicon.ico"));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = SourceScopedMcpServerRegistry.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to set field: " + fieldName, ex);
        }
    }
}
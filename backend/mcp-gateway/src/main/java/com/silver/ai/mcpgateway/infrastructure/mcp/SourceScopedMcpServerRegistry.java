package com.silver.ai.mcpgateway.infrastructure.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.mcpgateway.domain.model.ApiSource;
import com.silver.ai.mcpgateway.domain.model.McpAgentSession;
import com.silver.ai.mcpgateway.domain.port.ApiSourceRepository;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerChangeNotificationProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourceScopedMcpServerRegistry {

    private static final Pattern SOURCE_PATH_PATTERN = Pattern.compile("^/?api/v1/mcp/sources/(\\d+)(?:/.*)?$");
    private static final String SOURCE_ID_PLACEHOLDER = "{sourceId}";
    private static final String DEFAULT_INPUT_SCHEMA = "{\"type\":\"object\",\"properties\":{}}";

    private final ApiSourceRepository apiSourceRepository;
    private final DynamicApiToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;
    private final McpSessionService sessionService;

    @Value("${spring.ai.mcp.server.name:mcp-gateway}")
    private String serverName;

    @Value("${spring.ai.mcp.server.version:unknown}")
    private String serverVersion;

    @Value("${spring.ai.mcp.server.request-timeout:PT30S}")
    private Duration requestTimeout;

    @Value("${app.mcp.sse-path:/sse}")
    private String sseEndpoint;

    @Value("${app.mcp.streamable-http-path:/mcp/message}")
    private String messageEndpoint;

    @Value("${spring.ai.mcp.server.sse.keep-alive-interval:PT15S}")
    private Duration keepAliveInterval;

    private final ConcurrentMap<Long, RegisteredSourceServer> servers = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        refreshAll();
    }

    public synchronized void refreshAll() {
        servers.keySet().forEach(this::removeSource);
        apiSourceRepository.findByActive(true).forEach(this::refreshSource);
    }

    public synchronized void refreshSource(ApiSource source) {
        if (source == null || source.getId() == null) {
            return;
        }

        removeSource(source.getId());
        if (!source.isActive()) {
            return;
        }

        servers.put(source.getId(), createServer(source));
        log.info("Registered source-scoped MCP connection for source {}", source.getId());
    }

    public synchronized void removeSource(Long sourceId) {
        if (sourceId == null) {
            return;
        }

        RegisteredSourceServer removed = servers.remove(sourceId);
        if (removed != null) {
            try {
                removed.server().closeGracefully();
            } catch (Exception ex) {
                log.warn("Failed to close MCP server for source {}", sourceId, ex);
            }
        }
        sessionService.evictSource(sourceId);
    }

    public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
        Long sourceId = extractSourceId(request.path());
        if (sourceId == null) {
            return Mono.empty();
        }

        RegisteredSourceServer server = servers.get(sourceId);
        if (server == null) {
            apiSourceRepository.findById(sourceId)
                    .filter(ApiSource::isActive)
                    .ifPresent(this::refreshSource);
            server = servers.get(sourceId);
        }
        if (server == null) {
            return Mono.empty();
        }

        maybeProvisionSession(sourceId, request);

        return server.routerFunction().route(request);
    }

    public String serverNameFor(ApiSource source) {
        return serverName + "-source-" + source.getId();
    }

    public String sourceSsePath(Long sourceId) {
        return buildSourcePath(String.valueOf(sourceId), normalizeEndpoint(sseEndpoint));
    }

    public String sourceMessagePath(Long sourceId) {
        return buildSourcePath(String.valueOf(sourceId), normalizeEndpoint(messageEndpoint));
    }

    public String sourceSsePathTemplate() {
        return buildSourcePath(SOURCE_ID_PLACEHOLDER, normalizeEndpoint(sseEndpoint));
    }

    public String sourceMessagePathTemplate() {
        return buildSourcePath(SOURCE_ID_PLACEHOLDER, normalizeEndpoint(messageEndpoint));
    }

    private RegisteredSourceServer createServer(ApiSource source) {
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper.copy());
        String sourceSsePath = buildSourcePath(String.valueOf(source.getId()), normalizeEndpoint(sseEndpoint));
        String sourceMessagePath = buildSourcePath(String.valueOf(source.getId()), normalizeEndpoint(messageEndpoint));
        WebFluxSseServerTransportProvider transportProvider = WebFluxSseServerTransportProvider.builder()
                .jsonMapper(jsonMapper)
                .basePath("")
                .sseEndpoint(sourceSsePath)
                .messageEndpoint(sourceMessagePath)
                .keepAliveInterval(keepAliveInterval)
            .contextExtractor(request -> buildTransportContext(source.getId(), request))
                .build();

        McpServerAutoConfiguration autoConfiguration = new McpServerAutoConfiguration();
        McpSyncServer server = autoConfiguration.mcpSyncServer(
                transportProvider,
                autoConfiguration.capabilitiesBuilder(),
                buildServerProperties(source),
                new McpServerChangeNotificationProperties(),
                new FixedObjectProvider<>(Arrays.stream(toolCallbackProvider.getToolCallbacksForSource(source.getId()))
                    .map(callback -> toSyncToolSpecification(source.getId(), callback, jsonMapper))
                        .toList()),
                new FixedObjectProvider<>(Collections.emptyList()),
                new FixedObjectProvider<>(Collections.emptyList()),
                new FixedObjectProvider<>(Collections.emptyList()),
                new FixedObjectProvider<>(Collections.emptyList()),
                new EmptyObjectProvider<>(),
                Optional.empty()
        );

        @SuppressWarnings("unchecked")
        RouterFunction<ServerResponse> routerFunction = (RouterFunction<ServerResponse>) transportProvider.getRouterFunction();
        return new RegisteredSourceServer(routerFunction, server);
    }

    private McpServerProperties buildServerProperties(ApiSource source) {
        McpServerProperties properties = new McpServerProperties();
        properties.setEnabled(true);
        properties.setName(serverNameFor(source));
        properties.setVersion(serverVersion);
        properties.setInstructions(source.getDescription());
        properties.setRequestTimeout(requestTimeout);
        return properties;
    }

    private McpServerFeatures.SyncToolSpecification toSyncToolSpecification(Long sourceId, ToolCallback callback,
                                                                            McpJsonMapper jsonMapper) {
        ToolDefinition definition = callback.getToolDefinition();
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(definition.name())
                .description(definition.description())
                .inputSchema(jsonMapper, normalizeSchema(definition.inputSchema()))
                .build();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
            .callHandler((exchange, request) -> invokeCallback(sourceId, definition.name(), callback, exchange, request.arguments()))
                .build();
    }

    private McpSchema.CallToolResult invokeCallback(Long sourceId, String toolName, ToolCallback callback,
                                                    io.modelcontextprotocol.server.McpSyncServerExchange exchange,
                                                    Map<String, Object> arguments) {
        try {
            McpAgentSession session = sessionService.recordToolCall(sourceId, exchange.sessionId(), toolName, arguments, exchange);
            Map<String, Object> payloadMap = new java.util.LinkedHashMap<>();
            if (arguments != null) {
                payloadMap.putAll(arguments);
            }
            payloadMap.put("_mcp", sessionService.buildInvocationMetadata(session));
            String payload = objectMapper.writeValueAsString(payloadMap);
                return McpSchema.CallToolResult.builder()
                    .content(java.util.List.of(new McpSchema.TextContent(callback.call(payload))))
                    .isError(false)
                    .build();
        } catch (JsonProcessingException ex) {
                return McpSchema.CallToolResult.builder()
                    .content(java.util.List.of(new McpSchema.TextContent("MCP 工具参数序列化失败")))
                    .isError(true)
                    .build();
        } catch (Exception ex) {
            String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "MCP 工具执行失败" : ex.getMessage();
                return McpSchema.CallToolResult.builder()
                    .content(java.util.List.of(new McpSchema.TextContent(message)))
                    .isError(true)
                    .build();
        }
    }

    Long extractSourceId(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Matcher matcher = SOURCE_PATH_PATTERN.matcher(path.trim());
        if (!matcher.matches()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }

    private String normalizeSchema(String inputSchema) {
        if (inputSchema == null || inputSchema.isBlank()) {
            return DEFAULT_INPUT_SCHEMA;
        }
        return inputSchema;
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "/";
        }
        return endpoint.startsWith("/") ? endpoint : "/" + endpoint;
    }

    private String buildSourceBasePath(String sourceKey) {
        return "/api/v1/mcp/sources/" + sourceKey;
    }

    private String buildSourcePath(String sourceKey, String endpoint) {
        return buildSourceBasePath(sourceKey) + endpoint;
    }

    private void maybeProvisionSession(Long sourceId, ServerRequest request) {
        if (!request.method().name().equalsIgnoreCase("POST")) {
            return;
        }
        request.queryParam(WebFluxSseServerTransportProvider.SESSION_ID)
                .filter(value -> !value.isBlank())
                .ifPresent(sessionId -> sessionService.provisionSession(sourceId, sessionId, request));
    }

    private McpTransportContext buildTransportContext(Long sourceId, ServerRequest request) {
        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put(McpTransportMetadataKeys.SOURCE_ID, sourceId);
        metadata.put(McpTransportMetadataKeys.REQUEST_METHOD, request.method().name());
        metadata.put(McpTransportMetadataKeys.REQUEST_PATH, request.path());
        request.queryParam(WebFluxSseServerTransportProvider.SESSION_ID)
                .ifPresent(value -> metadata.put(McpTransportMetadataKeys.SESSION_ID, value));
        metadata.put(McpTransportMetadataKeys.REQUEST_HEADERS, normalizeHeaders(request.headers().asHttpHeaders()));
        return McpTransportContext.create(metadata);
    }

    private Map<String, String> normalizeHeaders(HttpHeaders headers) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        headers.forEach((name, values) -> {
            if (values != null && !values.isEmpty()) {
                result.put(name, String.join(",", values));
            }
        });
        return result;
    }

    private record RegisteredSourceServer(RouterFunction<ServerResponse> routerFunction, McpSyncServer server) {
    }

    private static final class FixedObjectProvider<T> implements ObjectProvider<T> {

        private final T value;

        private FixedObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject() {
            return value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public Iterator<T> iterator() {
            return value == null ? Collections.emptyIterator() : Collections.singleton(value).iterator();
        }

        @Override
        public Stream<T> stream() {
            return value == null ? Stream.empty() : Stream.of(value);
        }

        @Override
        public Stream<T> orderedStream() {
            return stream();
        }
    }

    private static final class EmptyObjectProvider<T> implements ObjectProvider<T> {

        @Override
        public T getObject() {
            return null;
        }

        @Override
        public T getObject(Object... args) {
            return null;
        }

        @Override
        public T getIfAvailable() {
            return null;
        }

        @Override
        public T getIfUnique() {
            return null;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }

        @Override
        public Stream<T> orderedStream() {
            return Stream.empty();
        }
    }
}
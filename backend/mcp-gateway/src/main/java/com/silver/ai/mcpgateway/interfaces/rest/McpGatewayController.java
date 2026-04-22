package com.silver.ai.mcpgateway.interfaces.rest;

import com.silver.ai.mcpgateway.application.McpGatewayAppService;
import com.silver.ai.mcpgateway.domain.model.ApiSource;
import com.silver.ai.mcpgateway.domain.model.ToolMapping;
import com.silver.ai.mcpgateway.infrastructure.mcp.SourceScopedMcpServerRegistry;
import com.silver.ai.mcpgateway.interfaces.dto.ApiSourceRequest;
import com.silver.ai.mcpgateway.interfaces.dto.ToolInvokeRequest;
import com.silver.ai.mcpgateway.interfaces.dto.ToolMappingUpdateRequest;
import com.silver.ai.shared.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/mcp")
@RequiredArgsConstructor
public class McpGatewayController {

    private final McpGatewayAppService mcpService;
    private final SourceScopedMcpServerRegistry sourceScopedMcpServerRegistry;

    @Value("${spring.ai.mcp.server.name:mcp-gateway}")
    private String serverName;

    @Value("${spring.ai.mcp.server.version:unknown}")
    private String serverVersion;

    // ===== API Sources =====

    @GetMapping("/sources")
    public Mono<ApiResponse<List<ApiSource>>> listSources() {
        return mcpService.listApiSources()
                .collectList()
                .map(ApiResponse::ok);
    }

    @PostMapping("/sources")
    public Mono<ApiResponse<ApiSource>> createSource(@Valid @RequestBody ApiSourceRequest req) {
        return mcpService.createApiSource(
                        req.getName(), req.getDescription(), req.getBaseUrl(),
                        req.getAuthType(), req.getAuthConfig(), req.getOpenApiSpec())
                .doOnNext(this::refreshRegistrySafely)
                .map(ApiResponse::ok);
    }

    @GetMapping("/sources/{id}")
    public Mono<ApiResponse<ApiSource>> getSource(@PathVariable Long id) {
        return mcpService.getApiSource(id)
                .map(ApiResponse::ok);
    }

    @GetMapping("/connection-info")
    public Mono<ApiResponse<Map<String, String>>> getConnectionInfo(ServerHttpRequest request) {
        return Mono.just(ApiResponse.ok(Map.of(
                "serverName", serverName,
                "version", serverVersion,
                "sseUrl", buildBaseUrl(request) + sourceScopedMcpServerRegistry.sourceSsePathTemplate(),
                "streamableHttpUrl", buildBaseUrl(request) + sourceScopedMcpServerRegistry.sourceMessagePathTemplate()
        )));
    }

    @GetMapping("/sources/{id}/connection-info")
    public Mono<ApiResponse<Map<String, String>>> getSourceConnectionInfo(@PathVariable Long id, ServerHttpRequest request) {
        return mcpService.getApiSource(id)
                .map(source -> {
                    String baseUrl = buildBaseUrl(request);
                    return ApiResponse.ok(Map.of(
                            "serverName", sourceScopedMcpServerRegistry.serverNameFor(source),
                            "version", serverVersion,
                            "sseUrl", baseUrl + sourceScopedMcpServerRegistry.sourceSsePath(id),
                            "streamableHttpUrl", baseUrl + sourceScopedMcpServerRegistry.sourceMessagePath(id)
                    ));
                });
    }

    @PutMapping("/sources/{id}")
    public Mono<ApiResponse<ApiSource>> updateSource(@PathVariable Long id, @Valid @RequestBody ApiSourceRequest req) {
        return mcpService.updateApiSource(id,
                        req.getName(), req.getDescription(), req.getBaseUrl(),
                        req.getAuthType(), req.getAuthConfig())
                .doOnNext(this::refreshRegistrySafely)
                .map(ApiResponse::ok);
    }

    @DeleteMapping("/sources/{id}")
    public Mono<ApiResponse<Void>> deleteSource(@PathVariable Long id) {
        return mcpService.deleteApiSource(id)
                .doOnSuccess(v -> removeFromRegistrySafely(id))
                .then(Mono.fromCallable(ApiResponse::ok));
    }

    // ===== Parse =====

    @PostMapping("/sources/{id}/parse")
    public Mono<ApiResponse<List<ToolMapping>>> parseSpec(@PathVariable Long id, @RequestBody ApiSourceRequest req) {
        Mono<List<ToolMapping>> tools;
        if (req.getOpenApiUrl() != null && !req.getOpenApiUrl().isBlank()) {
            tools = mcpService.parseFromUrl(id, req.getOpenApiUrl());
        } else {
            tools = mcpService.parseOpenApiSpec(id, req.getOpenApiSpec());
        }
        return tools
                .flatMap(result ->
                        mcpService.getApiSource(id)
                                .doOnNext(source -> {
                                    try {
                                        sourceScopedMcpServerRegistry.refreshSource(source);
                                    } catch (RuntimeException ex) {
                                        log.warn("Failed to refresh MCP server registry for source {} after parse", id, ex);
                                    }
                                })
                                .thenReturn(ApiResponse.ok(result))
                );
    }

    // ===== Tool Mappings =====

    @GetMapping("/sources/{id}/tools")
    public Mono<ApiResponse<List<ToolMapping>>> getTools(@PathVariable Long id) {
        return mcpService.getToolMappings(id)
                .collectList()
                .map(ApiResponse::ok);
    }

    @PutMapping("/tools/{id}")
    public Mono<ApiResponse<ToolMapping>> updateTool(@PathVariable Long id, @RequestBody ToolMappingUpdateRequest req) {
        return mcpService.updateToolMapping(id,
                        req.getToolName(), req.getToolDescription(),
                        req.getHttpMethod(), req.getPath(),
                        req.getParameterSchema(), req.getResponseSchema(), req.getExamplePayload(),
                        req.getEnabled())
                .flatMap(mapping ->
                        mcpService.getApiSource(mapping.getApiSourceId())
                                .doOnNext(this::refreshRegistrySafely)
                                .thenReturn(ApiResponse.ok(mapping))
                );
    }

    @PostMapping("/tools/{id}/test")
    public Mono<ApiResponse<String>> testTool(@PathVariable Long id, @RequestBody ToolInvokeRequest req) {
        return mcpService.invokeTool(id, req.getArguments())
                .map(ApiResponse::ok);
    }

    private void refreshRegistrySafely(ApiSource source) {
        try {
            sourceScopedMcpServerRegistry.refreshSource(source);
        } catch (RuntimeException ex) {
            log.warn("Failed to refresh MCP server registry for source {}, will self-heal on next request",
                    source.getId(), ex);
        }
    }

    private void removeFromRegistrySafely(Long sourceId) {
        try {
            sourceScopedMcpServerRegistry.removeSource(sourceId);
        } catch (RuntimeException ex) {
            log.warn("Failed to remove source {} from MCP server registry", sourceId, ex);
        }
    }

    private String buildBaseUrl(ServerHttpRequest request) {
        String scheme = request.getURI().getScheme();
        String host = request.getURI().getHost();
        int port = request.getURI().getPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + host + (defaultPort || port < 0 ? "" : ":" + port);
    }
}

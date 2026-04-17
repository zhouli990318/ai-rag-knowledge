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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ApiResponse<List<ApiSource>> listSources() {
        return ApiResponse.ok(mcpService.listApiSources());
    }

    @PostMapping("/sources")
    public ApiResponse<ApiSource> createSource(@Valid @RequestBody ApiSourceRequest req) {
        ApiSource source = mcpService.createApiSource(
                req.getName(), req.getDescription(), req.getBaseUrl(),
                req.getAuthType(), req.getAuthConfig(), req.getOpenApiSpec());
        sourceScopedMcpServerRegistry.refreshSource(source);
        return ApiResponse.ok(source);
    }

    @GetMapping("/sources/{id}")
    public ApiResponse<ApiSource> getSource(@PathVariable Long id) {
        return ApiResponse.ok(mcpService.getApiSource(id));
    }

    @GetMapping("/connection-info")
    public ApiResponse<Map<String, String>> getConnectionInfo(ServerHttpRequest request) {
        return ApiResponse.ok(Map.of(
                "serverName", serverName,
                "version", serverVersion,
            "sseUrl", buildBaseUrl(request) + sourceScopedMcpServerRegistry.sourceSsePathTemplate(),
            "streamableHttpUrl", buildBaseUrl(request) + sourceScopedMcpServerRegistry.sourceMessagePathTemplate()
        ));
    }

        @GetMapping("/sources/{id}/connection-info")
        public ApiResponse<Map<String, String>> getSourceConnectionInfo(@PathVariable Long id, ServerHttpRequest request) {
        ApiSource source = mcpService.getApiSource(id);
        String baseUrl = buildBaseUrl(request);

        return ApiResponse.ok(Map.of(
            "serverName", sourceScopedMcpServerRegistry.serverNameFor(source),
            "version", serverVersion,
            "sseUrl", baseUrl + sourceScopedMcpServerRegistry.sourceSsePath(id),
            "streamableHttpUrl", baseUrl + sourceScopedMcpServerRegistry.sourceMessagePath(id)
        ));
        }

    @PutMapping("/sources/{id}")
    public ApiResponse<ApiSource> updateSource(@PathVariable Long id, @Valid @RequestBody ApiSourceRequest req) {
        ApiSource source = mcpService.updateApiSource(id,
                req.getName(), req.getDescription(), req.getBaseUrl(),
                req.getAuthType(), req.getAuthConfig());
        sourceScopedMcpServerRegistry.refreshSource(source);
        return ApiResponse.ok(source);
    }

    @DeleteMapping("/sources/{id}")
    public ApiResponse<Void> deleteSource(@PathVariable Long id) {
        mcpService.deleteApiSource(id);
        sourceScopedMcpServerRegistry.removeSource(id);
        return ApiResponse.ok();
    }

    // ===== Parse =====

    @PostMapping("/sources/{id}/parse")
    public ApiResponse<List<ToolMapping>> parseSpec(@PathVariable Long id, @RequestBody ApiSourceRequest req) {
        List<ToolMapping> tools;
        if (req.getOpenApiUrl() != null && !req.getOpenApiUrl().isBlank()) {
            tools = mcpService.parseFromUrl(id, req.getOpenApiUrl());
        } else {
            tools = mcpService.parseOpenApiSpec(id, req.getOpenApiSpec());
        }
        sourceScopedMcpServerRegistry.refreshSource(mcpService.getApiSource(id));
        return ApiResponse.ok(tools);
    }

    // ===== Tool Mappings =====

    @GetMapping("/sources/{id}/tools")
    public ApiResponse<List<ToolMapping>> getTools(@PathVariable Long id) {
        return ApiResponse.ok(mcpService.getToolMappings(id));
    }

    @PutMapping("/tools/{id}")
    public ApiResponse<ToolMapping> updateTool(@PathVariable Long id, @RequestBody ToolMappingUpdateRequest req) {
        ToolMapping mapping = mcpService.updateToolMapping(id,
                req.getToolName(), req.getToolDescription(),
                req.getHttpMethod(), req.getPath(),
                req.getParameterSchema(), req.getResponseSchema(), req.getExamplePayload(),
            req.getEnabled());
        sourceScopedMcpServerRegistry.refreshSource(mcpService.getApiSource(mapping.getApiSourceId()));
        return ApiResponse.ok(mapping);
    }

    @PostMapping("/tools/{id}/test")
    public ApiResponse<String> testTool(@PathVariable Long id, @RequestBody ToolInvokeRequest req) {
        return ApiResponse.ok(mcpService.invokeTool(id, req.getArguments()));
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

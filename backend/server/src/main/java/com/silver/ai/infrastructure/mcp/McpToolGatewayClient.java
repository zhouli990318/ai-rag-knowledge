package com.silver.ai.infrastructure.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class McpToolGatewayClient {

    private final WebClient webClient;

    private final Duration blockTimeout;

    public McpToolGatewayClient(McpGatewayProperties properties, WebClient.Builder webClientBuilder,
                                @Value("${app.mcp-gateway.block-timeout-seconds:10}") int blockTimeoutSeconds) {
        this.blockTimeout = Duration.ofSeconds(blockTimeoutSeconds);
        String baseUrl = properties.baseUrl();
        if (baseUrl != null) {
            baseUrl = baseUrl.trim();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.mcp-gateway.base-url 未配置");
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.webClient = webClientBuilder.clone().baseUrl(baseUrl).build();
    }

    public List<McpToolDefinition> listTools(Long sourceId) {
        McpGatewayResponse<List<McpToolDefinition>> body = webClient.get()
                .uri("/sources/{sourceId}/tools", sourceId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<McpGatewayResponse<List<McpToolDefinition>>>() {})
                .block(blockTimeout);
        if (body == null || body.data() == null) {
            return Collections.emptyList();
        }
        return body.data();
    }

    public List<Long> listActiveSourceIds() {
        try {
            McpGatewayResponse<List<ApiSourceLite>> body = webClient.get()
                    .uri("/sources")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<McpGatewayResponse<List<ApiSourceLite>>>() {})
                    .block(blockTimeout);
            if (body == null || body.data() == null) {
                return Collections.emptyList();
            }
            return body.data().stream()
                    .filter(s -> Boolean.TRUE.equals(s.active()))
                    .filter(s -> !"UNREACHABLE".equalsIgnoreCase(s.healthStatus()))
                    .map(ApiSourceLite::id)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            log.warn("List active MCP sources failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public String invokeTool(Long toolId, String arguments) {
        McpGatewayResponse<String> body = webClient.post()
                .uri("/tools/{toolId}/invoke", toolId)
                .bodyValue(new ToolInvokeRequest(arguments))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<McpGatewayResponse<String>>() {})
                .block(blockTimeout);
        if (body == null) {
            return "[MCP 工具调用失败: 无响应]";
        }
        if (body.code() != 200) {
            return "[MCP 工具调用失败: " + body.message() + "]";
        }
        return body.data() == null ? "" : body.data();
    }
}
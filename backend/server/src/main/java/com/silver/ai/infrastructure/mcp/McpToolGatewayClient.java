package com.silver.ai.infrastructure.mcp;

import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class McpToolGatewayClient {

    private static final ParameterizedTypeReference<McpGatewayResponse<List<McpToolDefinition>>> TOOL_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<McpGatewayResponse<String>> TOOL_RESULT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public McpToolGatewayClient(RestClient.Builder restClientBuilder, McpGatewayProperties properties) {
        String baseUrl = StringUtils.trimWhitespace(properties.baseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("app.mcp-gateway.base-url 未配置");
        }
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public List<McpToolDefinition> listTools(Long sourceId) {
        try {
            McpGatewayResponse<List<McpToolDefinition>> response = restClient.get()
                    .uri("/sources/{id}/tools", sourceId)
                    .retrieve()
                    .body(TOOL_LIST_TYPE);

            if (response == null || response.data() == null) {
                return Collections.emptyList();
            }
            return response.data();
        } catch (Exception ex) {
            log.error("Failed to load MCP tools for source {}", sourceId, ex);
            throw new BusinessException(ErrorCode.CHAT_STREAM_ERROR, "加载 MCP 工具失败: " + ex.getMessage(), ex);
        }
    }

    public String invokeTool(Long toolId, String arguments) {
        try {
            McpGatewayResponse<String> response = restClient.post()
                    .uri("/tools/{id}/test", toolId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ToolInvokeRequest(arguments))
                    .retrieve()
                    .body(TOOL_RESULT_TYPE);

            if (response == null) {
                return "";
            }
            return response.data() == null ? "" : response.data();
        } catch (Exception ex) {
            log.error("Failed to invoke MCP tool {}", toolId, ex);
            throw new BusinessException(ErrorCode.CHAT_STREAM_ERROR, "调用 MCP 工具失败: " + ex.getMessage(), ex);
        }
    }
}
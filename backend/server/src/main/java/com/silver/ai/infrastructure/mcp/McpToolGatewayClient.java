package com.silver.ai.infrastructure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@SuppressWarnings("null")
public class McpToolGatewayClient {

    private final McpGatewayApi api;

    public McpToolGatewayClient(McpGatewayProperties properties, ObjectMapper objectMapper) {
        String baseUrl = properties.baseUrl();
        if (baseUrl != null) {
            baseUrl = baseUrl.trim();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.mcp-gateway.base-url 未配置");
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();

        this.api = retrofit.create(McpGatewayApi.class);
    }

    public List<McpToolDefinition> listTools(Long sourceId) {
        try {
            Response<McpGatewayResponse<List<McpToolDefinition>>> response = api.listTools(sourceId).execute();
            if (!response.isSuccessful()) {
                throw new BusinessException(ErrorCode.CHAT_STREAM_ERROR,
                        "加载 MCP 工具失败: HTTP " + response.code());
            }
            McpGatewayResponse<List<McpToolDefinition>> body = response.body();
            if (body == null || body.data() == null) {
                return Collections.emptyList();
            }
            return body.data();
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("Failed to load MCP tools for source {}", sourceId, ex);
            throw new BusinessException(ErrorCode.CHAT_STREAM_ERROR, "加载 MCP 工具失败: " + ex.getMessage(), ex);
        }
    }

    public String invokeTool(Long toolId, String arguments) {
        try {
            Response<McpGatewayResponse<String>> response =
                    api.invokeTool(toolId, new ToolInvokeRequest(arguments)).execute();
            if (!response.isSuccessful()) {
                throw new BusinessException(ErrorCode.CHAT_STREAM_ERROR,
                        "调用 MCP 工具失败: HTTP " + response.code());
            }
            McpGatewayResponse<String> body = response.body();
            if (body == null) {
                return "";
            }
            return body.data() == null ? "" : body.data();
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("Failed to invoke MCP tool {}", toolId, ex);
            throw new BusinessException(ErrorCode.CHAT_STREAM_ERROR, "调用 MCP 工具失败: " + ex.getMessage(), ex);
        }
    }
}
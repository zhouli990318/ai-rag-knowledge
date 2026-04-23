package com.silver.ai.infrastructure.mcp;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.List;

/**
 * Retrofit2 接口 — MCP Gateway REST API
 */
public interface McpGatewayApi {

    @GET("sources")
    Call<McpGatewayResponse<List<ApiSourceLite>>> listSources();

    @GET("sources/{sourceId}/tools")
    Call<McpGatewayResponse<List<McpToolDefinition>>> listTools(@Path("sourceId") Long sourceId);

    @POST("tools/{toolId}/test")
    Call<McpGatewayResponse<String>> invokeTool(@Path("toolId") Long toolId, @Body ToolInvokeRequest request);
}

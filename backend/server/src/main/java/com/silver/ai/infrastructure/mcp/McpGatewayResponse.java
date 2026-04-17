package com.silver.ai.infrastructure.mcp;

public record McpGatewayResponse<T>(int code, String message, T data, long timestamp) {
}
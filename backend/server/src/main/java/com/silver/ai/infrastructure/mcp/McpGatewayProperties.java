package com.silver.ai.infrastructure.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mcp-gateway")
public record McpGatewayProperties(String baseUrl) {
}
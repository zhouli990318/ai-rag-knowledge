package com.silver.ai.infrastructure.mcp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(McpGatewayProperties.class)
public class McpToolConfiguration {
}
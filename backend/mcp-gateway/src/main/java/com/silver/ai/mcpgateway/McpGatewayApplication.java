package com.silver.ai.mcpgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(excludeName = {
    "org.springframework.ai.mcp.server.autoconfigure.McpServerSseWebFluxAutoConfiguration",
    "org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebFluxAutoConfiguration",
    "org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration",
    "org.springframework.ai.mcp.server.common.autoconfigure.McpServerStatelessAutoConfiguration",
    "org.springframework.ai.mcp.server.common.autoconfigure.ToolCallbackConverterAutoConfiguration",
    "org.springframework.ai.mcp.server.common.autoconfigure.StatelessToolCallbackConverterAutoConfiguration"
})
@ComponentScan(basePackages = {"com.silver.ai.mcpgateway", "com.silver.ai.shared"})
public class McpGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpGatewayApplication.class, args);
    }
}

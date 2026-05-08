package com.silver.ai.mcpgateway.infrastructure.config;

import com.silver.ai.mcpgateway.domain.port.HttpClientPort;
import com.silver.ai.mcpgateway.domain.service.ToolInvocationDomainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public ToolInvocationDomainService toolInvocationDomainService(
            HttpClientPort httpClient, ObjectMapper objectMapper) {
        return new ToolInvocationDomainService(httpClient, objectMapper);
    }
}

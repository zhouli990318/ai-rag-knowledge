package com.silver.ai.infrastructure.config;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将 application.yml 中 app.orchestrator.* 绑定为 ChatOrchestratorConfig Bean。
 */
@Configuration
public class OrchestratorConfigProperties {

    @Bean
    @ConfigurationProperties(prefix = "app.orchestrator")
    public ChatOrchestratorConfig chatOrchestratorConfig() {
        return new ChatOrchestratorConfig();
    }
}

package com.silver.ai.infrastructure.config;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.port.SystemSettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * 编排器配置提供者。
 */
@Slf4j
@Configuration
public class OrchestratorConfigProperties {

    private ChatOrchestratorConfig beanInstance;

    @Autowired
    private SystemSettingsRepository settingsRepository;

    @Bean
    public ChatOrchestratorConfig chatOrchestratorConfig() {
        beanInstance = new ChatOrchestratorConfig();
        return beanInstance;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            ChatOrchestratorConfig dbConfig = settingsRepository.load().block();
            if (dbConfig != null) {
                copyFields(dbConfig, beanInstance);
                log.info("Orchestrator config loaded from DB");
            }
        } catch (Exception e) {
            log.warn("Failed to load orchestrator config from DB: {}", e.getMessage());
        }
    }

    public void reload() {
        try {
            ChatOrchestratorConfig dbConfig = settingsRepository.load().block();
            if (dbConfig != null) {
                copyFields(dbConfig, beanInstance);
                log.info("Orchestrator config reloaded from DB");
            }
        } catch (Exception e) {
            log.warn("Failed to reload orchestrator config: {}", e.getMessage());
        }
    }

    /**
     * 直接用已保存的配置更新内存 bean，避免在 Reactor 线程上调用 .block()。
     */
    public void applyConfig(ChatOrchestratorConfig saved) {
        if (saved != null) {
            copyFields(saved, beanInstance);
            log.info("Orchestrator config applied directly");
        }
    }

    private void copyFields(ChatOrchestratorConfig source, ChatOrchestratorConfig target) {
        target.setIntentConfidenceThreshold(source.getIntentConfidenceThreshold());
        target.setIntentEnabled(source.isIntentEnabled());
        target.setRewriteEnabled(source.isRewriteEnabled());
        target.setRewriteContextRounds(source.getRewriteContextRounds());
        target.setMultiPathRetrievalEnabled(source.isMultiPathRetrievalEnabled());
        target.setRerankTopK(source.getRerankTopK());
        target.setRetrievalTimeoutSeconds(source.getRetrievalTimeoutSeconds());
        target.setDeduplicatePrefixLength(source.getDeduplicatePrefixLength());
        target.setMemoryFullRounds(source.getMemoryFullRounds());
        target.setMemoryMaxChars(source.getMemoryMaxChars());
        target.setMemorySummaryThreshold(source.getMemorySummaryThreshold());
        target.setModelFallbackEnabled(source.isModelFallbackEnabled());
        target.setModelMaxRetries(source.getModelMaxRetries());
        target.setHealthCheckIntervalMinutes(source.getHealthCheckIntervalMinutes());
        target.setToolAutoExecuteThreshold(source.getToolAutoExecuteThreshold());
        target.setToolSemanticRetrievalEnabled(source.isToolSemanticRetrievalEnabled());
        target.setToolRetrievalTopK(source.getToolRetrievalTopK());
        target.setToolRetrievalThreshold(source.getToolRetrievalThreshold());
        target.setToolFallbackEnabled(source.isToolFallbackEnabled());
        target.setTraceSampleRate(source.getTraceSampleRate());
        target.setTraceEnabled(source.isTraceEnabled());
        target.setAuxiliaryProviderId(source.getAuxiliaryProviderId());
        target.setSuggestionPrompt(source.getSuggestionPrompt());
    }
}

package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.SystemSettingsAppService;
import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.interfaces.dto.SystemSettingsRequest;
import com.silver.ai.interfaces.dto.SystemSettingsResponse;
import com.silver.ai.shared.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSettingsAppService settingsAppService;

    @GetMapping
    public Mono<ApiResponse<SystemSettingsResponse>> getSettings() {
        return settingsAppService.getSettings()
                .map(this::toResponse)
                .map(ApiResponse::ok);
    }

    @PutMapping
    public Mono<ApiResponse<SystemSettingsResponse>> updateSettings(@RequestBody SystemSettingsRequest request) {
        return settingsAppService.updateSettings(toDomain(request), request.getRerankerApiKey(), request.isClearRerankerApiKey())
                .map(this::toResponse)
                .map(ApiResponse::ok);
    }

    private SystemSettingsResponse toResponse(ChatOrchestratorConfig config) {
        SystemSettingsResponse response = new SystemSettingsResponse();
        response.setIntentConfidenceThreshold(config.getIntentConfidenceThreshold());
        response.setIntentEnabled(config.isIntentEnabled());
        response.setRewriteEnabled(config.isRewriteEnabled());
        response.setHydeEnabled(config.isHydeEnabled());
        response.setRewriteContextRounds(config.getRewriteContextRounds());
        response.setMultiPathRetrievalEnabled(config.isMultiPathRetrievalEnabled());
        response.setRerankTopK(config.getRerankTopK());
        response.setRetrievalTimeoutSeconds(config.getRetrievalTimeoutSeconds());
        response.setDeduplicatePrefixLength(config.getDeduplicatePrefixLength());
        response.setRerankerServiceEnabled(config.isRerankerServiceEnabled());
        response.setRerankerBaseUrl(config.getRerankerBaseUrl());
        response.setRerankerModel(config.getRerankerModel());
        response.setRerankerApiKeyConfigured(hasText(config.getRerankerApiKeyEncrypted()));
        response.setRerankerApiKeyMasked(settingsAppService.getMaskedRerankerApiKey(config));
        response.setMemoryFullRounds(config.getMemoryFullRounds());
        response.setMemoryMaxChars(config.getMemoryMaxChars());
        response.setMemorySummaryThreshold(config.getMemorySummaryThreshold());
        response.setModelFallbackEnabled(config.isModelFallbackEnabled());
        response.setModelMaxRetries(config.getModelMaxRetries());
        response.setHealthCheckIntervalMinutes(config.getHealthCheckIntervalMinutes());
        response.setToolAutoExecuteThreshold(config.getToolAutoExecuteThreshold());
        response.setToolSemanticRetrievalEnabled(config.isToolSemanticRetrievalEnabled());
        response.setToolRetrievalTopK(config.getToolRetrievalTopK());
        response.setToolRetrievalThreshold(config.getToolRetrievalThreshold());
        response.setToolFallbackEnabled(config.isToolFallbackEnabled());
        response.setTraceSampleRate(config.getTraceSampleRate());
        response.setTraceEnabled(config.isTraceEnabled());
        response.setAuxiliaryProviderId(config.getAuxiliaryProviderId());
        response.setSuggestionPrompt(config.getSuggestionPrompt());
        return response;
    }

    private ChatOrchestratorConfig toDomain(SystemSettingsRequest request) {
        return ChatOrchestratorConfig.builder()
                .intentConfidenceThreshold(request.getIntentConfidenceThreshold())
                .intentEnabled(request.isIntentEnabled())
                .rewriteEnabled(request.isRewriteEnabled())
                .hydeEnabled(request.isHydeEnabled())
                .rewriteContextRounds(request.getRewriteContextRounds())
                .multiPathRetrievalEnabled(request.isMultiPathRetrievalEnabled())
                .rerankTopK(request.getRerankTopK())
                .retrievalTimeoutSeconds(request.getRetrievalTimeoutSeconds())
                .deduplicatePrefixLength(request.getDeduplicatePrefixLength())
                .rerankerServiceEnabled(request.isRerankerServiceEnabled())
                .rerankerBaseUrl(request.getRerankerBaseUrl())
                .rerankerModel(request.getRerankerModel())
                .memoryFullRounds(request.getMemoryFullRounds())
                .memoryMaxChars(request.getMemoryMaxChars())
                .memorySummaryThreshold(request.getMemorySummaryThreshold())
                .modelFallbackEnabled(request.isModelFallbackEnabled())
                .modelMaxRetries(request.getModelMaxRetries())
                .healthCheckIntervalMinutes(request.getHealthCheckIntervalMinutes())
                .toolAutoExecuteThreshold(request.getToolAutoExecuteThreshold())
                .toolSemanticRetrievalEnabled(request.isToolSemanticRetrievalEnabled())
                .toolRetrievalTopK(request.getToolRetrievalTopK())
                .toolRetrievalThreshold(request.getToolRetrievalThreshold())
                .toolFallbackEnabled(request.isToolFallbackEnabled())
                .traceSampleRate(request.getTraceSampleRate())
                .traceEnabled(request.isTraceEnabled())
                .auxiliaryProviderId(request.getAuxiliaryProviderId())
                .suggestionPrompt(request.getSuggestionPrompt())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

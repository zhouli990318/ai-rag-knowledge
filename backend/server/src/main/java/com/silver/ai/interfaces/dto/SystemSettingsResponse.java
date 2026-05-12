package com.silver.ai.interfaces.dto;

import lombok.Data;

@Data
public class SystemSettingsResponse {
    private double intentConfidenceThreshold;
    private boolean intentEnabled;
    private boolean rewriteEnabled;
    private boolean hydeEnabled;
    private int rewriteContextRounds;
    private boolean multiPathRetrievalEnabled;
    private int rerankTopK;
    private int retrievalTimeoutSeconds;
    private int deduplicatePrefixLength;
    private boolean rerankerServiceEnabled;
    private String rerankerBaseUrl;
    private String rerankerModel;
    private boolean rerankerApiKeyConfigured;
    private String rerankerApiKeyMasked;
    private int memoryFullRounds;
    private int memoryMaxChars;
    private int memorySummaryThreshold;
    private boolean modelFallbackEnabled;
    private int modelMaxRetries;
    private int healthCheckIntervalMinutes;
    private double toolAutoExecuteThreshold;
    private boolean toolSemanticRetrievalEnabled;
    private int toolRetrievalTopK;
    private double toolRetrievalThreshold;
    private boolean toolFallbackEnabled;
    private double traceSampleRate;
    private boolean traceEnabled;
    private Long auxiliaryProviderId;
    private String suggestionPrompt;
}
export interface SystemSettings {
  intentConfidenceThreshold: number;
  intentEnabled: boolean;
  rewriteEnabled: boolean;
  rewriteContextRounds: number;
  multiPathRetrievalEnabled: boolean;
  rerankTopK: number;
  retrievalTimeoutSeconds: number;
  deduplicatePrefixLength: number;
  memoryFullRounds: number;
  memoryMaxChars: number;
  memorySummaryThreshold: number;
  modelFallbackEnabled: boolean;
  modelMaxRetries: number;
  healthCheckIntervalMinutes: number;
  toolAutoExecuteThreshold: number;
  toolSemanticRetrievalEnabled: boolean;
  toolRetrievalTopK: number;
  toolRetrievalThreshold: number;
  toolFallbackEnabled: boolean;
  traceSampleRate: number;
  traceEnabled: boolean;
  auxiliaryProviderId: number | null;
  suggestionPrompt: string;
}

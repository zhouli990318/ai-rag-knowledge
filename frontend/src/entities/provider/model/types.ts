export interface Provider {
  id: number;
  name: string;
  providerType: string;
  baseUrl: string;
  defaultModel: string;
  embeddingModel: string;
  embeddingDimensions: number;
  enabled: boolean;
}

export interface ProviderType {
  type: string;
  displayName: string;
  openAiCompatible: boolean;
  defaultBaseUrl: string;
  supportsChat: boolean;
  supportsEmbedding: boolean;
}

export interface ApiResponse<T> {
  code: number;
  data: T;
  message: string | null;
  timestamp?: string;
}

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

export interface Conversation {
  id: number;
  title: string;
  providerId: number;
  model: string;
  knowledgeBaseId: number | null;
  mcpServerIds: number[];
  createdAt: string;
  messages: ChatMessage[];
}

export interface ChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  createdAt: string;
}

export interface SearchResult {
  content: string;
  metadata: Record<string, unknown>;
}

export interface KnowledgeBase {
  id: number;
  name: string;
  description: string;
  embeddingProviderId?: number | null;
  embeddingModel?: string | null;
  chunkStrategy: ChunkStrategy;
  retrievalConfig: RetrievalConfig;
  documentCount: number;
  active: boolean;
}

export interface ChunkStrategy {
  type: string;
  chunkSize: number;
  chunkOverlap: number;
}

export interface RetrievalConfig {
  topK: number;
  similarityThreshold: number;
  filterExpression: string | null;
}

export interface KbDocument {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  status: string;
  chunkCount: number;
  createdAt: string;
}

export interface McpApiSource {
  id: number;
  name: string;
  description: string;
  baseUrl: string;
  authType: string;
  active: boolean;
  toolMappings: McpToolMapping[];
}

export interface McpConnectionInfo {
  serverName: string;
  version: string;
  sseUrl: string;
  streamableHttpUrl: string;
}

export interface McpToolMapping {
  id: number;
  operationId: string;
  toolName: string;
  toolDescription: string;
  httpMethod: string;
  path: string;
  parameterSchema?: string;
  responseSchema?: string;
  examplePayload?: string;
  enabled: boolean;
}

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

export interface SearchResult {
  content: string;
  metadata: Record<string, unknown>;
}

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

export type ChunkType = 'FIXED_SIZE' | 'SENTENCE' | 'PARAGRAPH' | 'RECURSIVE' | 'SEMANTIC';

export interface ChunkStrategy {
  type: ChunkType;
  chunkSize: number;
  chunkOverlap: number;
  semanticThreshold: number;
  childChunkSize: number;
  windowSize: number;
  enableParentChild: boolean;
}

export type RetrievalMode = 'VECTOR' | 'KEYWORD' | 'HYBRID';

export interface RetrievalConfig {
  topK: number;
  similarityThreshold: number;
  filterExpression: string | null;
  retrievalMode: RetrievalMode;
  keywordWeight: number;
  vectorWeight: number;
  rerankerEnabled: boolean;
  rerankerTopK: number;
  windowSize: number;
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

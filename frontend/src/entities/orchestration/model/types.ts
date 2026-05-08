export interface IntentNode {
  id: number;
  parentId: number | null;
  name: string;
  description: string;
  level: number;
  keywords: string;
  routingAdvice: 'RETRIEVAL' | 'TOOL' | 'DIRECT' | 'HYBRID';
  sortOrder: number;
  status: 'DRAFT' | 'PUBLISHED';
  children?: IntentNode[];
}

export interface EtlTask {
  id: number;
  knowledgeBaseId: number;
  documentId: number | null;
  taskType: 'DOCUMENT' | 'GIT_IMPORT' | 'URL_CRAWL';
  currentStage: string;
  progress: number;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TraceSpan {
  spanId: string;
  stage: string;
  startTime: string;
  endTime: string | null;
  durationMs: number;
  success: boolean;
  errorMessage: string | null;
  attributes: Record<string, string>;
}

export interface ChatTrace {
  traceId: string;
  conversationId: number;
  messageId: number | null;
  totalDurationMs: number;
  spans: TraceSpan[];
  createdAt: string;
}

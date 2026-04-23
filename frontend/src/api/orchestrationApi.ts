import api from './client';

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

// ── 意图树 API ──
export const intentTreeApi = {
  getTree: () => api.get('/api/v1/intent-tree'),
  getNode: (id: number) => api.get(`/api/v1/intent-tree/${id}`),
  getChildren: (id: number) => api.get(`/api/v1/intent-tree/${id}/children`),
  create: (node: Partial<IntentNode>) => api.post('/api/v1/intent-tree', node),
  delete: (id: number) => api.delete(`/api/v1/intent-tree/${id}`),
};

// ── ETL 任务 API ──
export const etlTaskApi = {
  getTask: (id: number) => api.get(`/api/v1/etl-tasks/${id}`),
  getByKnowledgeBase: (kbId: number) => api.get(`/api/v1/etl-tasks/knowledge-base/${kbId}`),
};

// ── 链路追踪 API ──
export const traceApi = {
  getTrace: (traceId: string) => api.get(`/api/v1/traces/${traceId}`),
  getByConversation: (conversationId: number) => api.get(`/api/v1/traces/conversation/${conversationId}`),
};

import api from '@/shared/api/client';
import type { ApiResponse } from '@/shared/api/types';
import type { IntentNode, EtlTask, ChatTrace } from '../model/types';

// ── 意图树 API ──
export const intentTreeApi = {
  getTree: () =>
    api.get<ApiResponse<IntentNode[]>>('/api/v1/intent-tree').then((r) => r.data.data),
  create: (node: Partial<IntentNode>) =>
    api.post<ApiResponse<IntentNode>>('/api/v1/intent-tree', node).then((r) => r.data.data),
  delete: (id: number) =>
    api.delete(`/api/v1/intent-tree/${id}`),
};

// ── ETL 任务 API ──
export const etlTaskApi = {
  getByKnowledgeBase: (kbId: number) =>
    api.get<ApiResponse<EtlTask[]>>(`/api/v1/etl-tasks/knowledge-base/${kbId}`).then((r) => r.data.data),
};

// ── 链路追踪 API ──
export const traceApi = {
  getByConversation: (conversationId: number) =>
    api.get<ApiResponse<ChatTrace[]>>(`/api/v1/traces/conversation/${conversationId}`).then((r) => r.data.data),
};

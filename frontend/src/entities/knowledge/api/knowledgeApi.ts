import api from '@/shared/api/client';
import type { ApiResponse } from '@/shared/api/types';
import type { ChunkStrategy, KnowledgeBase, KbDocument, RetrievalConfig, SearchResult } from '../model/types';

const BASE = '/api/v1/knowledge-bases';

export interface CreateKnowledgeBaseRequest {
  name: string;
  description?: string;
  chunkStrategy?: ChunkStrategy;
  retrievalConfig?: RetrievalConfig;
}

export interface UpdateKnowledgeBaseRequest {
  name: string;
  description?: string;
  chunkStrategy?: ChunkStrategy;
  retrievalConfig?: RetrievalConfig;
}

export const knowledgeApi = {
  list: () => api.get<ApiResponse<KnowledgeBase[]>>(BASE).then((r) => r.data.data),
  create: (data: CreateKnowledgeBaseRequest) => api.post<ApiResponse<KnowledgeBase>>(BASE, data).then((r) => r.data.data),
  update: (id: number, data: UpdateKnowledgeBaseRequest) => api.put<ApiResponse<KnowledgeBase>>(`${BASE}/${id}`, data).then((r) => r.data.data),
  delete: (id: number) => api.delete(`${BASE}/${id}`),

  listDocuments: (kbId: number) =>
    api.get<ApiResponse<KbDocument[]>>(`${BASE}/${kbId}/documents`).then((r) => r.data.data),
  uploadDocument: (kbId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.post<ApiResponse<KbDocument>>(`${BASE}/${kbId}/documents`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data.data);
  },
  deleteDocument: (kbId: number, docId: number) =>
    api.delete(`${BASE}/${kbId}/documents/${docId}`),

  rebuildVectors: (kbId: number) =>
    api.post<ApiResponse<string>>(`${BASE}/${kbId}/rebuild-vectors`).then((r) => r.data.data),

  importGit: (kbId: number, data: { repoUrl: string; branch?: string; filePatterns?: string[] }) =>
    api.post<ApiResponse<void>>(`${BASE}/${kbId}/git-import`, data).then((r) => r.data.data),

  search: (kbId: number, query: string, topK?: number, filterExpression?: string) =>
    api.post<ApiResponse<SearchResult[]>>(`${BASE}/${kbId}/search`, { query, topK, filterExpression }).then((r) => r.data.data),
};

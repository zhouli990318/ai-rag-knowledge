import api from '@/shared/api/client';
import type { ApiResponse } from '@/shared/api/types';
import type { Conversation } from '../model/types';

const BASE = '/api/v1/chat';

export interface StreamChatRequest {
  conversationId?: number;
  providerId: number;
  model?: string;
  knowledgeBaseId?: number;
  message: string;
  systemPrompt?: string;
  mcpServerIds?: number[];
  toolMode?: 'OFF' | 'AUTO' | 'SPECIFIC';
  filterExpression?: string;
}

export const chatApi = {
  streamChat: (data: StreamChatRequest, signal?: AbortSignal) => {
    return fetch(`${BASE}/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
      signal,
    });
  },

  chat: (data: StreamChatRequest) =>
    api.post<ApiResponse<string>>(BASE, data).then((r) => r.data.data),

  getConversations: () =>
    api.get<ApiResponse<Conversation[]>>(`${BASE}/conversations`).then((r) => r.data.data),

  getConversation: (id: number) =>
    api.get<ApiResponse<Conversation>>(`${BASE}/conversations/${id}`).then((r) => r.data.data),

  deleteConversation: (id: number) =>
    api.delete(`${BASE}/conversations/${id}`),

  getSuggestions: (id: number) =>
    api.get<ApiResponse<string[]>>(`${BASE}/conversations/${id}/suggestions`).then((r) => {
      const payload = r.data?.data as unknown;
      if (Array.isArray(payload)) {
        return payload;
      }
      if (payload && typeof payload === 'object' && Array.isArray((payload as { data?: unknown }).data)) {
        return (payload as { data: string[] }).data;
      }
      return [];
    }),
};

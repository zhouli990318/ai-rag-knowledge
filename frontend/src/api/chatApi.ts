import api from './client';
import { ApiResponse, Conversation } from './types';

const BASE = '/api/v1/chat';

export interface StreamChatRequest {
  conversationId?: number;
  providerId: number;
  model?: string;
  knowledgeBaseId?: number;
  message: string;
  systemPrompt?: string;
  mcpServerIds?: number[];
}

export const chatApi = {
  streamChat: (data: StreamChatRequest) => {
    return fetch(`${BASE}/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },

  chat: (data: Record<string, unknown>) =>
    api.post<ApiResponse<string>>(BASE, data).then((r) => r.data.data),

  getConversations: () =>
    api.get<ApiResponse<Conversation[]>>(`${BASE}/conversations`).then((r) => r.data.data),

  getConversation: (id: number) =>
    api.get<ApiResponse<Conversation>>(`${BASE}/conversations/${id}`).then((r) => r.data.data),

  deleteConversation: (id: number) =>
    api.delete(`${BASE}/conversations/${id}`),
};

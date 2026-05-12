export interface Conversation {
  id: number;
  title: string;
  providerId: number;
  model: string;
  knowledgeBaseId: number | null;
  toolMode: 'OFF' | 'AUTO' | 'SPECIFIC';
  mcpServerIds: number[];
  filterExpression: string | null;
  createdAt: string;
  messages: ChatMessage[];
}

export interface ChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  createdAt: string;
}

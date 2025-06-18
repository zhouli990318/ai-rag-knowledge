// 基础响应接口
export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

// 聊天响应接口
export interface ChatResponse {
  code: string;
  result?: {
    output: {
      text: string;
    };
    metadata: {
      finishReason: string;
    };
  };
}

// 上传响应接口
export interface UploadResponse extends ApiResponse<string> {
  data: string;
}

// 知识库标签响应接口
export interface RagTagsResponse extends ApiResponse<string[]> {
  data: string[];
}

// 消息类型枚举
export enum MessageType {
  USER = 'user',
  AI = 'ai'
}

// 消息接口
export interface Message {
  id: string;
  content: string;
  type: MessageType;
  timestamp: Date;
  isStreaming?: boolean;
  isThinking?: boolean;
  error?: boolean;
}

// 会话接口
export interface Session {
  id: string;
  name: string;
  messages: Message[];
} 
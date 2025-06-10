export enum MessageType {
  USER = 'user',
  AI = 'ai'
}

export interface Message {
  id: string;
  content: string;
  type: MessageType;
  timestamp: Date;
  isStreaming?: boolean;
  isThinking?: boolean; // 添加思考状态标记
  error?: boolean;
}
export type ChatRole = 'user' | 'assistant' | 'system';

export interface ChatMessage {
  id: string;
  threadId: string;
  userId: string;
  role: ChatRole;
  content: string;
  createdAt: string; // ISO
  meta?: Record<string, any>;
}

export interface ChatThread {
  id: string;
  userId: string;
  title?: string;
  summary?: string;
  createdAt: string;
  updatedAt: string;
  messageCount?: number;
}

import { ChatMessage, ChatThread } from '@/types/chat';
import AsyncStorage from '@react-native-async-storage/async-storage';
import 'react-native-get-random-values';
import { v4 as uuid } from 'uuid';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

interface ChatState {
  threads: ChatThread[];
  messages: Record<string, ChatMessage[]>; // threadId -> messages
  activeThreadId?: string;
  sending: boolean;
  error?: string;
  createThread: (title?: string) => string;
  addLocalMessage: (partial: Omit<ChatMessage, 'id' | 'createdAt'> & { id?: string }) => ChatMessage;
  setActiveThread: (id: string) => void;
  setError: (e?: string) => void;
  replaceAssistantMessage: (threadId: string, tempId: string, content: string) => void;
  reset: () => void;
  updateThread: (id: string, patch: Partial<ChatThread>) => void;
  setThreadMessages: (threadId: string, msgs: ChatMessage[]) => void;
}

export const useChatStore = create<ChatState>()(persist((set, get) => ({
  threads: [],
  messages: {},
  sending: false,
  createThread: (title) => {
    const id = uuid();
    const now = new Date().toISOString();
    const thread: ChatThread = { id, userId: 'local', title, createdAt: now, updatedAt: now };
    set(s => ({ threads: [thread, ...s.threads], activeThreadId: id }));
    return id;
  },
  addLocalMessage: (partial) => {
    const id = partial.id || uuid();
    const threadId = partial.threadId || get().activeThreadId || get().createThread();
    const msg: ChatMessage = {
      id,
      threadId,
      userId: partial.userId || 'local',
      role: partial.role,
      content: partial.content,
      createdAt: new Date().toISOString(),
      meta: partial.meta,
    };
    set(s => ({
      messages: { ...s.messages, [threadId]: [...(s.messages[threadId] || []), msg] },
      activeThreadId: threadId,
    }));
    return msg;
  },
  setActiveThread: (id) => set({ activeThreadId: id }),
  setError: (e) => set({ error: e }),
  replaceAssistantMessage: (threadId, tempId, content) => set(s => {
    const list = s.messages[threadId] || [];
    return {
      messages: {
        ...s.messages,
        [threadId]: list.map(m => m.id === tempId ? { ...m, content } : m)
      }
    };
  }),
  reset: () => set({ threads: [], messages: {}, activeThreadId: undefined }),
  updateThread: (id, patch) => set(s => ({ threads: s.threads.map(t => t.id === id ? { ...t, ...patch, updatedAt: new Date().toISOString() } : t) })),
  setThreadMessages: (threadId, msgs) => set(s => ({ messages: { ...s.messages, [threadId]: msgs } })),
}), {
  name: 'chat-store',
  storage: createJSONStorage(() => AsyncStorage),
  partialize: (state) => ({ threads: state.threads, messages: state.messages, activeThreadId: state.activeThreadId })
}));

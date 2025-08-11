import { ChatMessage } from '@/types/chat';
import Constants from 'expo-constants';
import { supabase } from './supabaseClient';

export interface SendMessageOptions {
  threadId: string;
  messages: { role: 'user' | 'assistant' | 'system'; content: string }[];
  signal?: AbortSignal;
}

const OPENAI_KEY = process.env.EXPO_PUBLIC_OPENAI_API_KEY; // TEMP: no dejar en prod
const OPENAI_MODEL = (Constants.expoConfig?.extra as any)?.OPENAI_MODEL || 'gpt-4o-mini';

export async function sendMessage(options: SendMessageOptions): Promise<ChatMessage> {
  // Primero intentar Edge Function
  try {
    const { data, error } = await supabase.functions.invoke('ai-chat', { body: { messages: options.messages, threadId: options.threadId } });
    if (error) throw error;
    if (data?.content) {
      return {
        id: Math.random().toString(36).slice(2),
        threadId: options.threadId,
        userId: 'ai',
        role: 'assistant',
        content: data.content,
        createdAt: new Date().toISOString(),
      };
    }
  } catch {
    // Edge Function no disponible -> fallback
    if (!OPENAI_KEY) {
      const lastUser = options.messages.filter(m => m.role === 'user').slice(-1)[0]?.content || '';
      return {
        id: Math.random().toString(36).slice(2),
        threadId: options.threadId,
        userId: 'ai',
        role: 'assistant',
        content: `Echo: ${lastUser}`,
        createdAt: new Date().toISOString(),
      };
    }
    try {
      const body = { model: OPENAI_MODEL, messages: options.messages };
      const res = await fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${OPENAI_KEY}` },
        body: JSON.stringify(body),
        signal: options.signal
      });
      if (!res.ok) {
        const txt = await res.text();
        throw new Error('OpenAI fallback error: ' + txt.slice(0,200));
      }
      const json: any = await res.json();
      const content = json.choices?.[0]?.message?.content?.trim() || 'Sin respuesta';
      return {
        id: json.id || Math.random().toString(36).slice(2),
        threadId: options.threadId,
        userId: 'openai',
        role: 'assistant',
        content,
        createdAt: new Date().toISOString(),
      };
    } catch (inner) {
      const lastUser = options.messages.filter(m => m.role === 'user').slice(-1)[0]?.content || '';
      return {
        id: Math.random().toString(36).slice(2),
        threadId: options.threadId,
        userId: 'ai',
        role: 'assistant',
        content: 'Error al obtener respuesta',
        createdAt: new Date().toISOString(),
        meta: { error: String(inner), echo: lastUser }
      } as ChatMessage;
    }
  }
  // Fallback final (no debería llegar)
  return {
    id: Math.random().toString(36).slice(2),
    threadId: options.threadId,
    userId: 'ai',
    role: 'assistant',
    content: 'Sin respuesta',
    createdAt: new Date().toISOString(),
  };
}

export async function ensureRemoteThread(localId: string, title?: string): Promise<{ remoteId: string }> {
  // For simplicity store mapping by using same UUID local (already uuid v4) not guaranteed: if local id not uuid, we always create new.
  // Create remote thread if not exists (search by id not feasible). We'll create a new thread each time we call this if there is none remote stored.
  // Better approach: store remoteId in meta; omitted for brevity.
  if (!supabase) throw new Error('supabase client not ready');
  // Simple strategy: create thread row and return id
  const { data, error } = await supabase.from('ai_threads').insert({ title }).select('id').single();
  if (error) throw error;
  return { remoteId: data.id };
}

export async function fetchThreads(): Promise<any[]> {
  const { data, error } = await supabase.from('v_threads_overview').select('*').order('updated_at', { ascending: false });
  if (error) throw error;
  return data || [];
}

export async function fetchMessages(threadId: string): Promise<ChatMessage[]> {
  const { data, error } = await supabase.from('ai_messages').select('*').eq('thread_id', threadId).order('created_at', { ascending: true });
  if (error) throw error;
  return (data || []).map(r => ({
    id: r.id,
    threadId: r.thread_id,
    userId: r.user_id,
    role: r.role,
    content: r.content,
    createdAt: r.created_at,
    meta: r.meta || undefined
  }));
}

export async function persistUserMessage(threadId: string, content: string): Promise<string> {
  const { data, error } = await supabase.rpc('safe_insert_user_message', { p_thread: threadId, p_content: content });
  if (error) throw error;
  return data as string;
}

export async function insertAssistantMessage(threadId: string, content: string): Promise<string> {
  const { data, error } = await supabase.from('ai_messages').insert({ thread_id: threadId, role: 'assistant', content }).select('id').single();
  if (error) throw error;
  return data.id as string;
}

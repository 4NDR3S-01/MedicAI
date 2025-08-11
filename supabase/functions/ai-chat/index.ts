// Supabase Edge Function: ai-chat
// Deploy: supabase functions deploy ai-chat --project-ref <ref>
// Set secret: supabase secrets set OPENAI_API_KEY=sk-xxxx
// Invoke (client): supabase.functions.invoke('ai-chat', { body: { messages, threadId } })

/// <reference lib="deno.unstable" />
/// <reference lib="dom" />
// @ts-nocheck

import { serve } from 'https://deno.land/std@0.224.0/http/server.ts';

interface InMessage { role: 'user' | 'assistant' | 'system'; content: string }
interface RequestBody { threadId?: string; messages: InMessage[]; model?: string }

const DEFAULT_MODEL = Deno.env.get('OPENAI_MODEL') || 'gpt-5';

serve(async (req: Request): Promise<Response> => {
  const started = Date.now();
  if (req.method !== 'POST') return new Response('Method not allowed', { status: 405 });
  try {
    const apiKey = Deno.env.get('OPENAI_API_KEY');
    if (!apiKey) return new Response(JSON.stringify({ error: 'config_error', detail: 'Missing OPENAI_API_KEY' }), { status: 500 });
    const body = await req.json() as RequestBody;
    if (!body.messages || body.messages.length === 0) {
      return new Response(JSON.stringify({ error: 'validation_error', detail: 'messages required' }), { status: 400 });
    }
    const model = body.model || DEFAULT_MODEL;

    const payload = {
      model,
      messages: body.messages.map(m => ({ role: m.role, content: m.content })),
      temperature: 0.2
    };

    const openaiRes = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${apiKey}` },
      body: JSON.stringify(payload)
    });

    if (!openaiRes.ok) {
      const txt = await openaiRes.text();
      const truncated = txt.slice(0, 500);
      return new Response(JSON.stringify({
        error: 'openai_failed',
        status: openaiRes.status,
        statusText: openaiRes.statusText,
        model,
        detail: truncated
      }), { status: 502 });
    }

    const data = await openaiRes.json();
    const content = data.choices?.[0]?.message?.content?.trim() || '';

    return new Response(JSON.stringify({
      content,
      model,
      usage: data.usage,
      latency_ms: Date.now() - started
    }), { headers: { 'Content-Type': 'application/json' }, status: 200 });
  } catch (e) {
    return new Response(JSON.stringify({ error: 'server_error', detail: String(e), latency_ms: Date.now() - started }), { status: 500 });
  }
});

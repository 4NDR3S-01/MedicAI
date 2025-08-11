import { ChatInputBar } from '@/components/chat/ChatInputBar';
import { ChatMessageBubble } from '@/components/chat/ChatMessageBubble';
import { Palette } from '@/constants/Colors';
import { sendMessage } from '@/services/aiService';
import { useChatStore } from '@/store/chatStore';
import { useSettingsStore } from '@/store/settingsStore';
import type { ChatMessage } from '@/types/chat';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, FlatList, KeyboardAvoidingView, Platform, SafeAreaView, StyleSheet, Text, TouchableOpacity, View, useColorScheme } from 'react-native';

export default function ChatScreen() {
  const { activeThreadId, messages, addLocalMessage, replaceAssistantMessage, createThread } = useChatStore();
  const { dailyAiLimit, accessibility } = useSettingsStore();
  const listRef = useRef<FlatList>(null);
  const [loading, setLoading] = useState(false);
  const scheme = useColorScheme();

  const data = useMemo(() => (activeThreadId ? messages[activeThreadId] || [] : []), [activeThreadId, messages]);
  const userMessagesCount = useMemo(() => data.filter(m => m.role === 'user').length, [data]);
  const hc = accessibility?.highContrast;

  useEffect(() => {
    if (!activeThreadId) {
      createThread('Nuevo chat');
    }
  }, [activeThreadId, createThread]);

  useEffect(() => {
    requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
  }, [data.length]);

  const handleSend = useCallback(async (text: string) => {
    if (!activeThreadId) return;
    const userMsg = addLocalMessage({ threadId: activeThreadId, role: 'user', content: text, userId: 'local' });
    const tempAssistant = addLocalMessage({ threadId: activeThreadId, role: 'assistant', content: '...', userId: 'local' });
    setLoading(true);
    try {
      const reply = await sendMessage({ threadId: activeThreadId, messages: data.concat(userMsg).map((m: ChatMessage) => ({ role: m.role, content: m.content })) });
      replaceAssistantMessage(activeThreadId, tempAssistant.id, reply.content);
    } catch (e: any) {
      console.warn('AI error', e);
      const msg = e?.message ? `Error: ${e.message.substring(0,140)}` : 'Error al obtener respuesta';
      replaceAssistantMessage(activeThreadId, tempAssistant.id, msg);
    } finally {
      setLoading(false);
    }
  }, [activeThreadId, addLocalMessage, replaceAssistantMessage, data]);

  const handleNewThread = () => {
    createThread('Nuevo chat');
  };

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={Platform.OS === 'ios' ? 60 : 0}>
        <View style={styles.container}>
          <View style={[styles.header, hc && { backgroundColor: scheme === 'dark' ? '#000' : '#fff', borderBottomWidth: 2, borderColor: Palette.primary }]}> 
            <TouchableOpacity onPress={handleNewThread} style={styles.newBtn}><Text style={styles.newBtnText}>Nuevo</Text></TouchableOpacity>
            <Text style={styles.counter}>{userMessagesCount}/{dailyAiLimit}</Text>
          </View>
          <FlatList
            ref={listRef}
            data={data}
            keyExtractor={item => item.id}
            renderItem={({ item }) => <ChatMessageBubble message={item} />}
            contentContainerStyle={[styles.list, data.length === 0 && { flex: 1, justifyContent: 'center' }]}
            onContentSizeChange={() => listRef.current?.scrollToEnd({ animated: true })}
            keyboardShouldPersistTaps="handled"
          />
          <ChatInputBar onSend={handleSend} loading={loading} />
          {loading && data.length === 0 && <ActivityIndicator style={{ margin: 16 }} />}
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  list: { paddingVertical: 12, paddingBottom: 8 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 12, paddingVertical: 8, borderBottomWidth: 1, borderColor: '#e2ecee' },
  newBtn: { backgroundColor: Palette.primary, paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20 },
  newBtnText: { color: '#fff', fontWeight: '600' },
  counter: { fontWeight: '600', color: Palette.primary }
});
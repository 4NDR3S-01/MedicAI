import { Colors } from '@/constants/Colors';
import { ChatMessage } from '@/types/chat';
import React from 'react';
import { StyleSheet, Text, useColorScheme, View } from 'react-native';

interface Props { message: ChatMessage; }

export const ChatMessageBubble: React.FC<Props> = ({ message }) => {
  const scheme = useColorScheme();
  const isUser = message.role === 'user';
  let bg: string;
  if (isUser) {
    bg = Colors[scheme || 'light'].tint;
  } else if (scheme === 'dark') {
    bg = '#1f2a30';
  } else {
    bg = '#e8f6f8';
  }
  let color: string;
  if (isUser) {
    color = scheme === 'dark' ? '#10181A' : '#fff';
  } else {
    color = Colors[scheme || 'light'].text;
  }
  return (
    <View style={[styles.container, isUser ? styles.right : styles.left]}>
      <View style={[styles.bubble, { backgroundColor: bg }]}> 
        <Text style={[styles.text, { color }]}>{message.content}</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { marginVertical: 4, paddingHorizontal: 12, width: '100%' },
  left: { alignItems: 'flex-start' },
  right: { alignItems: 'flex-end' },
  bubble: { maxWidth: '85%', borderRadius: 16, padding: 12 },
  text: { fontSize: 16, lineHeight: 20 }
});

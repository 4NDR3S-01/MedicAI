import { Colors } from '@/constants/Colors';
import React, { useState } from 'react';
import { ActivityIndicator, StyleSheet, TextInput, TouchableOpacity, useColorScheme, View } from 'react-native';
import { ThemedText } from '../ThemedText';

interface Props { onSend: (text: string) => Promise<void> | void; loading?: boolean; }

export const ChatInputBar: React.FC<Props> = ({ onSend, loading }) => {
  const [text, setText] = useState('');
  const scheme = useColorScheme();
  const disabled = loading || text.trim().length === 0;

  const handleSend = () => {
    const value = text.trim();
    if (!value) return;
    onSend(value);
    setText('');
  };

  return (
    <View style={[styles.container, { borderColor: Colors[scheme || 'light'].tint }]}> 
      <TextInput
        style={[styles.input, { color: Colors[scheme || 'light'].text }]}
        placeholder="Escribe tu consulta..."
        placeholderTextColor={scheme === 'dark' ? '#889499' : '#7a9ea4'}
        multiline
        value={text}
        onChangeText={setText}
      />
      <TouchableOpacity disabled={disabled} onPress={handleSend} style={[styles.button, disabled && { opacity: 0.5 }]}> 
        {loading ? <ActivityIndicator size="small" color="#fff" /> : <ThemedText style={styles.buttonText}>Enviar</ThemedText>}
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flexDirection: 'row', padding: 8, borderTopWidth: 1, alignItems: 'flex-end' },
  input: { flex: 1, paddingHorizontal: 12, paddingVertical: 8, minHeight: 42, maxHeight: 140 },
  button: { backgroundColor: '#00bbcf', paddingVertical: 10, paddingHorizontal: 16, borderRadius: 12, marginLeft: 8, justifyContent: 'center' },
  buttonText: { color: '#fff', fontWeight: '600' }
});

import { Palette } from '@/constants/Colors';
import { useChatStore } from '@/store/chatStore';
import { useRouter } from 'expo-router';
import React from 'react';
import { Alert, FlatList, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

export default function HistorialScreen() {
  const { threads, setActiveThread, updateThread } = useChatStore();
  const router = useRouter();

  const handleSelect = (id: string) => {
    setActiveThread(id);
    router.push('/(tabs)/chat');
  };

  return (
    <View style={styles.container}>
      <FlatList
        data={threads}
        keyExtractor={t => t.id}
        renderItem={({ item }) => (
          <TouchableOpacity style={styles.item} onPress={() => handleSelect(item.id)} onLongPress={() => {
            Alert.prompt('Renombrar','Nuevo título', text => { if (text?.trim()) updateThread(item.id, { title: text.trim() }); });
          }}>
            <Text style={styles.title}>{item.title || 'Conversación'}</Text>
            <Text style={styles.subtitle}>{new Date(item.createdAt).toLocaleString()}</Text>
          </TouchableOpacity>
        )}
        ListEmptyComponent={<Text style={styles.empty}>Sin conversaciones</Text>}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 12 },
  item: { padding: 14, backgroundColor: '#f1f7f8', borderRadius: 12, marginVertical: 6, borderWidth: 1, borderColor: '#e0ecef' },
  title: { fontSize: 16, fontWeight: '600', color: Palette.primary },
  subtitle: { fontSize: 12, color: '#666', marginTop: 4 },
  empty: { textAlign: 'center', marginTop: 24, color: '#777' }
});
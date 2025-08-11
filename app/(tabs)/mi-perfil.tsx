import { Palette } from '@/constants/Colors';
import { useAuth } from '@/providers/AuthProvider';
import { useChatStore } from '@/store/chatStore';
import { useSettingsStore } from '@/store/settingsStore';
import React, { useMemo } from 'react';
import { ScrollView, StyleSheet, Switch, Text, TouchableOpacity, View, useColorScheme } from 'react-native';

export default function PerfilScreen() {
  const { notificationsEnabled, accessibility, setSetting, dailyAiLimit } = useSettingsStore();
  const { user, signOut } = useAuth();
  const { messages } = useChatStore();
  const scheme = useColorScheme();

  const todayUsage = useMemo(() => {
    const today = new Date().toISOString().slice(0,10);
    let count = 0;
    Object.values(messages).forEach(list => {
      list.forEach(m => {
        if (m.role === 'user' && m.createdAt.startsWith(today)) count += 1;
      });
    });
    return count;
  }, [messages]);

  const hc = accessibility?.highContrast;

  return (
    <ScrollView contentContainerStyle={[styles.container, hc && (scheme === 'dark' ? styles.hcDark : styles.hcLight)]}> 
      <Text style={styles.sectionTitle}>Cuenta</Text>
      <View style={styles.box}> 
        <Text style={styles.label}>Email</Text>
        <Text style={styles.value}>{user?.email}</Text>
        <Text style={styles.meta}>ID: {user?.id.slice(0,8)}…</Text>
      </View>
      <View style={styles.boxRow}> 
        <View style={{ flex: 1 }}>
          <Text style={styles.label}>Uso diario IA</Text>
          <Text style={styles.value}>{todayUsage}/{dailyAiLimit}</Text>
        </View>
      </View>
      <TouchableOpacity style={styles.signOutBtn} onPress={signOut}> 
        <Text style={styles.signOutText}>Cerrar sesión</Text>
      </TouchableOpacity>

      <Text style={styles.sectionTitle}>Preferencias</Text>
      <View style={styles.row}> 
        <Text style={styles.label}>Notificaciones</Text>
        <Switch value={!!notificationsEnabled} onValueChange={v => setSetting('notificationsEnabled', v)} />
      </View>
      <Text style={styles.sectionTitle}>Accesibilidad</Text>
      <View style={styles.row}> 
        <Text style={styles.label}>Texto grande</Text>
        <Switch value={!!accessibility?.largeText} onValueChange={v => setSetting('accessibility', { ...accessibility, largeText: v })} />
      </View>
      <View style={styles.row}> 
        <Text style={styles.label}>Alto contraste</Text>
        <Switch value={!!accessibility?.highContrast} onValueChange={v => setSetting('accessibility', { ...accessibility, highContrast: v })} />
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16 },
  sectionTitle: { fontSize: 18, fontWeight: '600', marginTop: 16, marginBottom: 4 },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 12 },
  label: { fontSize: 16, fontWeight: '500' },
  value: { fontSize: 16, marginTop: 4 },
  meta: { fontSize: 12, color: '#6b7a80', marginTop: 2 },
  box: { backgroundColor: '#f1f7f8', padding: 14, borderRadius: 12, marginTop: 8 },
  boxRow: { flexDirection: 'row', backgroundColor: '#f1f7f8', padding: 14, borderRadius: 12, marginTop: 8, alignItems: 'center' },
  signOutBtn: { backgroundColor: Palette.danger, padding: 14, borderRadius: 12, alignItems: 'center', marginTop: 12 },
  signOutText: { color: '#fff', fontWeight: '600' },
  hcLight: { backgroundColor: '#ffffff' },
  hcDark: { backgroundColor: '#000000' }
});
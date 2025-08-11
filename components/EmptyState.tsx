import { Palette } from '@/constants/Colors';
import React from 'react';
import { StyleSheet, useColorScheme, View } from 'react-native';
import { ThemedText } from './ThemedText';

interface Props { icon?: React.ReactNode; title: string; message?: string; action?: React.ReactNode; }

export const EmptyState: React.FC<Props> = ({ icon, title, message, action }) => {
  const scheme = useColorScheme();
  return (
    <View style={styles.container}>
      {icon}
      <ThemedText type="subtitle" style={{ textAlign: 'center', marginTop: 8 }}>{title}</ThemedText>
      {message && <ThemedText style={{ textAlign: 'center', marginTop: 6, color: scheme === 'dark' ? Palette.textMutedDark : Palette.textMutedLight }}>{message}</ThemedText>}
      {action && <View style={{ marginTop: 16 }}>{action}</View>}
    </View>
  );
};

const styles = StyleSheet.create({
  container: { alignItems: 'center', justifyContent: 'center', padding: 24 }
});

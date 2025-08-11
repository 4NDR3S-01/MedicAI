import { ToastProvider } from '@/components/Toast';
import { useColorScheme } from '@/hooks/useColorScheme';
import { AuthProvider, useAuth } from '@/providers/AuthProvider';
import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import React from 'react';
import { ActivityIndicator, Image, StyleSheet, Text, View } from 'react-native';
import 'react-native-reanimated';

function AppNavigator() {
  const { loading, user } = useAuth();
  const colorScheme = useColorScheme();
  if (loading) {
    return (
      <View style={styles.container}>
        <Image source={require('../assets/images/logo.png')} style={styles.logo} resizeMode="contain" />
        <ActivityIndicator size="large" color="#00bbcf" style={styles.loader} />
        <Text style={styles.text}>Cargando...</Text>
      </View>
    );
  }
  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <Stack>
        {!user && (
          <>
            <Stack.Screen name="login" options={{ headerShown: false }} />
            <Stack.Screen name="register" options={{ title: 'Regresar' }} />
            <Stack.Screen name="forgot-password" options={{ title: 'Regresar' }} />
          </>
        )}
        {user && (
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        )}
        <Stack.Screen name="+not-found" options={{ title: 'No encontrado' }} />
      </Stack>
      <StatusBar style={colorScheme === 'dark' ? 'light' : 'dark'} />
    </ThemeProvider>
  );
}

export default function RootLayout() {
  return (
    <ToastProvider>
      <AuthProvider>
        <AppNavigator />
      </AuthProvider>
    </ToastProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  logo: {
    width: 180,
    height: 180,
    marginBottom: 32,
  },
  loader: {
    marginTop: 16,
  },
  text: {
    marginTop: 16,
    fontSize: 18,
    color: '#00bbcf',
    fontWeight: 'bold',
  },
});

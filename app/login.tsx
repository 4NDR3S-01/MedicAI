import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { Alert, Image, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, TextInput, TouchableOpacity, useColorScheme, View } from 'react-native';
import { ThemedText } from '../components/ThemedText';
import { Colors } from '../constants/Colors';
import { supabase } from '../services/supabaseClient';



export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const router = useRouter();
  const colorScheme = useColorScheme();

  useEffect(() => {
    supabase.auth.getSession().then(({ data }) => {
      if (data.session) {
        router.replace('/(tabs)/chat');
      }
    });
    const { data: listener } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session) {
        router.replace('/(tabs)/chat');
      }
    });
    return () => {
      listener?.subscription.unsubscribe();
    };
  }, [router]);

  const handleLogin = async () => {
    setLoading(true);
    const { error } = await supabase.auth.signInWithPassword({ email, password });
    setLoading(false);
    if (error) {
      Alert.alert('Error', error.message);
    } else {
      router.replace('/(tabs)/chat');
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 60 : 0}
    >
      <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: 'center', alignItems: 'center', minHeight: '100%' }} keyboardShouldPersistTaps="handled">
        <View style={[styles.card, { backgroundColor: Colors[colorScheme || 'light'].background }]}>
          <Image source={require('../assets/images/logo.png')} style={styles.logo} resizeMode="contain" />
          <ThemedText
            type="title"
            style={{
              color: Colors[colorScheme || 'light'].tint,
              fontFamily: 'SpaceMono',
              marginBottom: 32,
              letterSpacing: 0.5,
            }}
          >
            Iniciar Sesión
          </ThemedText>
          <View style={styles.inputWrapper}>
            <TextInput
              style={[
                styles.input,
                {
                  color: colorScheme === 'dark' ? Colors.dark.text : Colors.light.text,
                  borderColor: Colors[colorScheme || 'light'].tint,
                  backgroundColor: colorScheme === 'dark' ? '#162125' : '#fff',
                  fontFamily: 'SpaceMono',
                },
              ]}
              placeholder="Correo electrónico"
              placeholderTextColor={colorScheme === 'dark' ? Colors.dark.icon : Colors.light.icon}
              autoCapitalize="none"
              keyboardType="email-address"
              value={email}
              onChangeText={setEmail}
            />
          </View>
          <View style={styles.inputWrapper}>
            <TextInput
              style={[
                styles.input,
                {
                  color: colorScheme === 'dark' ? Colors.dark.text : Colors.light.text,
                  borderColor: Colors[colorScheme || 'light'].tint,
                  backgroundColor: colorScheme === 'dark' ? '#162125' : '#fff',
                  fontFamily: 'SpaceMono',
                },
              ]}
              placeholder="Contraseña"
              placeholderTextColor={colorScheme === 'dark' ? Colors.dark.icon : Colors.light.icon}
              secureTextEntry
              value={password}
              onChangeText={setPassword}
            />
          </View>
          <TouchableOpacity
            style={[
              styles.buttonContainer,
              colorScheme === 'dark' ? styles.buttonDark : styles.buttonLight,
              loading && { opacity: 0.7 },
            ]}
            onPress={handleLogin}
            disabled={loading}
            activeOpacity={0.85}
          >
            <ThemedText
              style={{
                color: colorScheme === 'dark' ? '#10181A' : '#fff',
                fontWeight: 'bold',
                textAlign: 'center',
                fontSize: 16,
                paddingVertical: 14,
                fontFamily: 'SpaceMono',
                letterSpacing: 1,
                textTransform: 'uppercase',
              }}
            >
              {loading ? 'Cargando...' : 'Entrar'}
            </ThemedText>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => router.push('/register')} style={styles.linkContainer}>
            <ThemedText style={[styles.link, { color: Colors[colorScheme || 'light'].tint }]}>¿No tienes cuenta? Regístrate</ThemedText>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => router.push('/forgot-password')} style={styles.linkContainer}>
            <ThemedText style={[styles.link, { color: Colors[colorScheme || 'light'].tint }]}>¿Olvidaste tu contraseña?</ThemedText>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  card: {
    alignSelf: 'center',
    justifyContent: 'flex-start',
    alignItems: 'center',
    padding: 24,
    borderRadius: 24,
    marginVertical: 48,
    marginHorizontal: 16,
    maxWidth: 380,
    width: '90%',
    minWidth: 280,
    shadowColor: '#00bbcf',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.18,
    shadowRadius: 24,
    elevation: 8,
  },
  logo: {
    width: 120,
    height: 120,
    marginBottom: 24,
    borderRadius: 0,
    shadowColor: 'transparent',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0,
    shadowRadius: 0,
    elevation: 0,
    backgroundColor: 'transparent',
  },
  inputWrapper: {
    width: '100%',
    marginBottom: 16,
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: 'transparent',
  },
  input: {
    width: '100%',
    height: 48,
    borderWidth: 1.5,
    borderRadius: 12,
    paddingHorizontal: 16,
    fontSize: 16,
    backgroundColor: 'transparent',
  },
  buttonContainer: {
    width: '100%',
    marginTop: 8,
    borderRadius: 16,
    overflow: 'hidden',
    marginBottom: 8,
    shadowColor: '#00bbcf',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.18,
    shadowRadius: 12,
    elevation: 4,
  },
  buttonLight: {
    backgroundColor: Colors.light.tint,
    borderColor: Colors.light.tint,
  },
  buttonDark: {
    backgroundColor: Colors.dark.tint,
    borderColor: Colors.dark.tint,
  },
  linkContainer: {
    marginTop: 12,
  },
  link: {
    fontSize: 15,
    textDecorationLine: 'underline',
    textAlign: 'center',
    letterSpacing: 0.5,
    fontWeight: '600',
  },
});

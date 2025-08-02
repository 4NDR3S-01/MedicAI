import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Alert, Image, KeyboardAvoidingView, Platform, StyleSheet, TextInput, TouchableOpacity, useColorScheme, View } from 'react-native';
import { ThemedText } from '../components/ThemedText';
import { Colors } from '../constants/Colors';
import { supabase } from '../services/supabaseClient';

export default function ForgotPasswordScreen() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const colorScheme = useColorScheme();
  const router = useRouter();

  const handleReset = async () => {
    setLoading(true);
    const { error } = await supabase.auth.resetPasswordForEmail(email);
    setLoading(false);
    if (error) {
      Alert.alert('Error', error.message);
    } else {
      Alert.alert('Éxito', 'Se ha enviado un enlace de restablecimiento a tu correo electrónico.');
      router.push('/login');
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 60 : 0}
    >
      <View
        style={{
          flex: 1,
          justifyContent: 'center',
          alignItems: 'center',
          paddingTop: 32,
          paddingBottom: 48,
        }}
      >
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
            Recuperar contraseña
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
          <TouchableOpacity
            style={[
              styles.buttonContainer,
              colorScheme === 'dark' ? styles.buttonDark : styles.buttonLight,
              loading && { opacity: 0.7 },
            ]}
            onPress={handleReset}
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
              {loading ? 'Enviando...' : 'Enviar enlace'}
            </ThemedText>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => router.push('/login')} style={styles.linkContainer}>
            <ThemedText style={[styles.link, { color: Colors[colorScheme || 'light'].tint }]}>¿Recordaste tu contraseña? Inicia sesión</ThemedText>
          </TouchableOpacity>
        </View>
      </View>
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

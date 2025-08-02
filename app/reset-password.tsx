import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, StyleSheet, TextInput, TouchableOpacity, useColorScheme, View } from 'react-native';
import { ThemedText } from '../components/ThemedText';
import { Colors } from '../constants/Colors';
import { supabase } from '../services/supabaseClient';

export default function ResetPasswordUpdateScreen() {
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const colorScheme = useColorScheme();
  const router = useRouter();

  // El usuario ya está autenticado temporalmente por el link de Supabase
  const handleUpdatePassword = async () => {
    if (!password || password.length < 6) {
      Alert.alert('Error', 'La contraseña debe tener al menos 6 caracteres.');
      return;
    }
    setLoading(true);
    const { error } = await supabase.auth.updateUser({ password });
    setLoading(false);
    if (error) {
      Alert.alert('Error', error.message);
    } else {
      Alert.alert('Éxito', 'Tu contraseña ha sido actualizada. Inicia sesión con tu nueva contraseña.');
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
          <ThemedText
            type="title"
            style={{
              color: Colors[colorScheme || 'light'].tint,
              fontFamily: 'SpaceMono',
              marginBottom: 32,
              letterSpacing: 0.5,
            }}
          >
            Nueva contraseña
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
              placeholder="Nueva contraseña"
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
            onPress={handleUpdatePassword}
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
              {loading ? 'Actualizando...' : 'Actualizar contraseña'}
            </ThemedText>
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
});

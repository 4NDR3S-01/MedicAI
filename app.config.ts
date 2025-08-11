import type { ExpoConfig } from '@expo/config-types';
import 'dotenv/config';

const SUPABASE_URL = process.env.EXPO_PUBLIC_SUPABASE_URL || process.env.SUPABASE_URL;
const SUPABASE_ANON_KEY = process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY || process.env.SUPABASE_ANON_KEY;
const OPENAI_API_KEY = process.env.EXPO_PUBLIC_OPENAI_API_KEY; // WARNING: solo para pruebas, no dejar en producción
const OPENAI_MODEL = process.env.EXPO_PUBLIC_OPENAI_MODEL || 'gpt-4o-mini';

const config: ExpoConfig = {
  name: 'MedicAI',
  slug: 'MedicAI',
  scheme: 'appmedicai',
  version: '1.0.0',
  orientation: 'portrait',
  userInterfaceStyle: 'automatic',
  newArchEnabled: true,
  icon: './assets/images/icon.png',
  ios: { supportsTablet: true },
  android: {
    adaptiveIcon: {
      foregroundImage: './assets/images/icon.png',
      backgroundColor: '#ffffff'
    },
    edgeToEdgeEnabled: true
  },
  web: {
    bundler: 'metro',
    output: 'static',
    favicon: './assets/images/icon.png'
  },
  plugins: [
    'expo-router',
    [
      'expo-splash-screen',
      {
        image: './assets/images/icon.png',
        imageWidth: 200,
        resizeMode: 'contain',
        backgroundColor: '#ffffff'
      }
    ]
  ],
  experiments: { typedRoutes: true },
  extra: {
    SUPABASE_URL,
    SUPABASE_ANON_KEY,
    OPENAI_MODEL,
    hasPublicOpenAIKey: !!OPENAI_API_KEY,
    eas: { projectId: process.env.EAS_PROJECT_ID }
  },
  runtimeVersion: { policy: 'appVersion' }
};

export default config;

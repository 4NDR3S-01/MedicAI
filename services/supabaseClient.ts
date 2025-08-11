import { createClient } from '@supabase/supabase-js';
import Constants from 'expo-constants';

const SUPABASE_URL = (Constants.expoConfig?.extra as any)?.SUPABASE_URL;
const SUPABASE_ANON_KEY = (Constants.expoConfig?.extra as any)?.SUPABASE_ANON_KEY;

if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {
  throw new Error('Faltan SUPABASE_URL o SUPABASE_ANON_KEY (revisa .env o app.config.ts)');
}

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

import { ProfileSettings } from '@/types/profile';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

interface SettingsState extends ProfileSettings {
  setSetting: <K extends keyof ProfileSettings>(key: K, value: ProfileSettings[K]) => void;
  hydrate: (data: ProfileSettings) => void;
}

export const useSettingsStore = create<SettingsState>()(persist((set) => ({
  theme: 'system',
  dailyAiLimit: 20,
  notificationsEnabled: true,
  locale: 'es',
  accessibility: { largeText: false, highContrast: false },
  setSetting: (key, value) => set(s => ({ ...s, [key]: value })),
  hydrate: (data) => set({ ...data })
}), {
  name: 'settings-store',
  storage: createJSONStorage(() => AsyncStorage)
}));

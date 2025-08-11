import { Medication, Reminder, ReminderLog } from '@/types/medications';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { v4 as uuid } from 'uuid';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

interface ReminderState {
  medications: Medication[];
  reminders: Reminder[];
  logs: ReminderLog[];
  addMedication: (data: Omit<Medication, 'id' | 'createdAt'>) => Medication;
  updateMedication: (id: string, patch: Partial<Medication>) => void;
  deleteMedication: (id: string) => void;
  addReminder: (data: Omit<Reminder, 'id' | 'createdAt'>) => Reminder;
  updateReminder: (id: string, patch: Partial<Reminder>) => void;
  deleteReminder: (id: string) => void;
  addLog: (data: Omit<ReminderLog, 'id'>) => ReminderLog;
  toggleReminder: (id: string, enabled: boolean) => void;
}

export const useReminderStore = create<ReminderState>()(persist((set) => ({
  medications: [],
  reminders: [],
  logs: [],
  addMedication: (data) => {
    const item: Medication = { id: uuid(), createdAt: new Date().toISOString(), ...data };
    set(s => ({ medications: [item, ...s.medications] }));
    return item;
  },
  updateMedication: (id, patch) => set(s => ({ medications: s.medications.map(m => m.id === id ? { ...m, ...patch } : m) })),
  deleteMedication: (id) => set(s => ({ medications: s.medications.filter(m => m.id !== id), reminders: s.reminders.filter(r => r.medicationId !== id) })),
  addReminder: (data) => {
    const item: Reminder = { id: uuid(), createdAt: new Date().toISOString(), ...data };
    set(s => ({ reminders: [item, ...s.reminders] }));
    return item;
  },
  updateReminder: (id, patch) => set(s => ({ reminders: s.reminders.map(r => r.id === id ? { ...r, ...patch } : r) })),
  deleteReminder: (id) => set(s => ({ reminders: s.reminders.filter(r => r.id !== id), logs: s.logs.filter(l => l.reminderId !== id) })),
  addLog: (data) => {
    const item: ReminderLog = { id: uuid(), ...data };
    set(s => ({ logs: [item, ...s.logs] }));
    return item;
  },
  toggleReminder: (id, enabled) => set(s => ({ reminders: s.reminders.map(r => r.id === id ? { ...r, enabled } : r) }))
}), {
  name: 'reminder-store',
  storage: createJSONStorage(() => AsyncStorage)
}));

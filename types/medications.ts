export type ScheduleType = 'FIXED_TIMES' | 'INTERVAL' | 'PRN';

export interface Medication {
  id: string;
  userId: string;
  name: string;
  dose: number;
  unit: string; // mg, ml, etc
  instructions?: string;
  createdAt: string;
}

export interface Reminder {
  id: string;
  userId: string;
  medicationId: string;
  scheduleType: ScheduleType;
  times?: string[]; // e.g. ['08:00','14:00'] for FIXED_TIMES
  intervalHours?: number; // for INTERVAL
  timezone: string;
  nextRunAt?: string; // ISO
  enabled: boolean;
  createdAt: string;
}

export interface ReminderLog {
  id: string;
  userId: string;
  reminderId: string;
  status: 'taken' | 'skipped' | 'missed';
  takenAt: string; // ISO
  note?: string;
}

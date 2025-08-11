export interface UserProfile {
  userId: string;
  fullName?: string;
  avatarUrl?: string;
  settings?: ProfileSettings;
  createdAt: string;
}

export interface ProfileSettings {
  theme?: 'light' | 'dark' | 'system';
  dailyAiLimit?: number;
  notificationsEnabled?: boolean;
  locale?: string; // 'es'
  accessibility?: {
    largeText?: boolean;
    highContrast?: boolean;
  };
}

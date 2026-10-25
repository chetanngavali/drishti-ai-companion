export type AppRoute =
  | 'home'
  | 'schemes'
  | 'companion'
  | 'schedule'
  | 'scan'
  | 'navigate'
  | 'describe'
  | 'nearby'
  | 'emergency'
  | 'settings'
  | 'safety'
  | 'privacy'
  | 'permissions';

export type AppLanguage = 'en' | 'hi' | 'mr';

export type CoreState =
  | 'IDLE'
  | 'LISTENING'
  | 'PROCESSING'
  | 'SPEAKING'
  | 'ATTENTION'
  | 'ERROR'
  | 'EMERGENCY';

export type SchemeCategory =
  | 'all'
  | 'central'
  | 'maharashtra'
  | 'scholarship'
  | 'assistive_devices'
  | 'pension';

export interface GovernmentScheme {
  id: string;
  name: string;
  nativeNames: {
    en: string;
    hi: string;
    mr: string;
  };
  shortDescription: {
    en: string;
    hi: string;
    mr: string;
  };
  authority: string;
  jurisdiction: 'Central (Government of India)' | 'State (Maharashtra)' | 'National / DEPwD';
  category: 'central' | 'maharashtra' | 'scholarship' | 'assistive_devices' | 'pension';
  eligibility: string[];
  benefits: string[];
  documentsRequired: string[];
  applicationOpeningDate?: string;
  applicationClosingDate?: string;
  officialUrl: string;
  applicationInstructions: string[];
  lastVerified: string;
  isFeatured?: boolean;
}

export interface SchemeNotification {
  id: string;
  schemeId: string;
  schemeName: string;
  title: string;
  message: string;
  date: string;
  isRead: boolean;
  type: 'opening' | 'deadline' | 'update' | 'new';
  officialUrl: string;
}

export type ReminderCategory =
  | 'medication'
  | 'doctor_appointment'
  | 'medical_test'
  | 'health_routine'
  | 'personal';

export type ReminderRepeat = 'none' | 'daily' | 'weekly' | 'weekdays';

export type ReminderStatus = 'active' | 'completed' | 'missed' | 'snoozed';

export interface MedicalReminder {
  id: string;
  title: string;
  category: ReminderCategory;
  date: string; // YYYY-MM-DD
  time: string; // HH:mm (24hr)
  repeat: ReminderRepeat;
  notes?: string;
  status: ReminderStatus;
  createdAt: string;
  updatedAt: string;
  snoozedUntil?: string;
}

export interface CompanionMessage {
  id: string;
  sender: 'user' | 'drishti';
  text: string;
  timestamp: string;
  language: AppLanguage;
  isCrisis?: boolean;
}

export interface AccessibilitySettings {
  largeText: boolean;
  highContrast: boolean;
  voiceSpeed: number; // 0.75 - 1.5
  voiceVolume: number; // 0.5 - 1.0
  voiceWakeEnabled: boolean;
  spokenAlertsEnabled: boolean;
  soundEffectsEnabled: boolean;
}

export interface UserAccessibilityProfile {
  name?: string;
  age?: number;
  disabilityPercentage?: number;
  hasUdidCard?: boolean;
  state?: string;
  annualIncomeBracket?: 'below_1_lakh' | '1_to_3_lakh' | 'above_3_lakh';
  isStudent?: boolean;
}

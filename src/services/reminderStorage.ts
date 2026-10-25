import { MedicalReminder, ReminderCategory, ReminderRepeat, ReminderStatus, AppLanguage } from '../types';
import { speechService } from './speechService';

const REMINDERS_KEY = 'drishti_medical_reminders_v1';

// Seed reminders so queries like "What is on my schedule today?" or "Read my medical reminders" work immediately
export const INITIAL_REMINDERS: MedicalReminder[] = [
  {
    id: 'rem-1',
    title: 'Morning Eye Drops (Carboxymethylcellulose)',
    category: 'medication',
    date: new Date().toISOString().split('T')[0],
    time: '08:00',
    repeat: 'daily',
    notes: '2 drops in both eyes after breakfast for lubrication',
    status: 'active',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 'rem-2',
    title: 'Appointment with Dr. Arvind Kulkarni (Ophthalmologist)',
    category: 'doctor_appointment',
    date: (() => {
      const d = new Date();
      d.setDate(d.getDate() + 1);
      return d.toISOString().split('T')[0];
    })(),
    time: '11:00',
    repeat: 'none',
    notes: 'National Eye Care Clinic, Room 4, carry previous prescription and UDID card',
    status: 'active',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 'rem-3',
    title: 'Evening Blood Pressure Routine Check',
    category: 'health_routine',
    date: new Date().toISOString().split('T')[0],
    time: '18:00',
    repeat: 'daily',
    notes: 'Rest for 5 minutes before cuff measurement, log reading in notebook',
    status: 'active',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 'rem-4',
    title: 'Night Medicine (Vitamin A & Calcium tablet)',
    category: 'medication',
    date: new Date().toISOString().split('T')[0],
    time: '20:00',
    repeat: 'daily',
    notes: 'Take with warm water after dinner',
    status: 'active',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

class ReminderStorage {
  private listeners: ((reminders: MedicalReminder[]) => void)[] = [];
  private alertListeners: ((reminder: MedicalReminder) => void)[] = [];
  private checkIntervalId: any = null;
  private alertedReminderIds: Set<string> = new Set();

  constructor() {
    if (typeof window !== 'undefined') {
      this.initStorage();
      this.startBackgroundMonitor();
    }
  }

  private initStorage(): void {
    try {
      const raw = localStorage.getItem(REMINDERS_KEY);
      if (!raw) {
        localStorage.setItem(REMINDERS_KEY, JSON.stringify(INITIAL_REMINDERS));
      }
    } catch (e) {
      console.error('Failed to init reminder storage:', e);
    }
  }

  public getAll(): MedicalReminder[] {
    if (typeof window === 'undefined') return INITIAL_REMINDERS;
    try {
      const raw = localStorage.getItem(REMINDERS_KEY);
      if (!raw) return INITIAL_REMINDERS;
      return JSON.parse(raw);
    } catch (e) {
      return INITIAL_REMINDERS;
    }
  }

  public saveAll(reminders: MedicalReminder[]): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(REMINDERS_KEY, JSON.stringify(reminders));
      this.notifyListeners(reminders);
    } catch (e) {
      console.error('Failed to save reminders:', e);
    }
  }

  public addReminder(item: Omit<MedicalReminder, 'id' | 'createdAt' | 'updatedAt' | 'status'>): MedicalReminder {
    const list = this.getAll();
    const newReminder: MedicalReminder = {
      ...item,
      id: 'rem-' + Date.now() + '-' + Math.random().toString(36).substring(2, 6),
      status: 'active',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    list.unshift(newReminder);
    this.saveAll(list);
    return newReminder;
  }

  public updateReminder(id: string, updates: Partial<MedicalReminder>): MedicalReminder | null {
    const list = this.getAll();
    const index = list.findIndex((r) => r.id === id);
    if (index === -1) return null;

    const updated = {
      ...list[index],
      ...updates,
      updatedAt: new Date().toISOString(),
    };
    list[index] = updated;
    this.saveAll(list);
    return updated;
  }

  public deleteReminder(id: string): boolean {
    const list = this.getAll();
    const filtered = list.filter((r) => r.id !== id);
    if (filtered.length !== list.length) {
      this.saveAll(filtered);
      return true;
    }
    return false;
  }

  public markCompleted(id: string): MedicalReminder | null {
    return this.updateReminder(id, { status: 'completed' });
  }

  public snoozeReminder(id: string, minutes: number = 10): MedicalReminder | null {
    const now = new Date();
    now.setMinutes(now.getMinutes() + minutes);
    const snoozeTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
    const snoozeDate = now.toISOString().split('T')[0];

    return this.updateReminder(id, {
      status: 'snoozed',
      time: snoozeTime,
      date: snoozeDate,
      snoozedUntil: now.toISOString(),
    });
  }

  // Subscribe to changes
  public subscribe(cb: (reminders: MedicalReminder[]) => void): () => void {
    this.listeners.push(cb);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== cb);
    };
  }

  public onAlertTriggered(cb: (reminder: MedicalReminder) => void): () => void {
    this.alertListeners.push(cb);
    return () => {
      this.alertListeners = this.alertListeners.filter((l) => l !== cb);
    };
  }

  private notifyListeners(reminders: MedicalReminder[]): void {
    this.listeners.forEach((cb) => cb(reminders));
  }

  // Background monitor: checks every 10 seconds for due reminders
  private startBackgroundMonitor(): void {
    if (this.checkIntervalId) return;

    this.checkIntervalId = setInterval(() => {
      this.checkDueReminders();
    }, 10000);
  }

  private checkDueReminders(): void {
    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];
    const currentHourMin = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

    const reminders = this.getAll();
    for (const r of reminders) {
      if (r.status === 'completed' || r.status === 'missed') continue;

      // Check if scheduled date/time matches now
      const isDateMatch = r.date === todayStr || (r.repeat === 'daily') || (r.repeat === 'weekdays' && now.getDay() >= 1 && now.getDay() <= 5);
      const isTimeMatch = r.time === currentHourMin;

      const alertKey = `${r.id}-${todayStr}-${currentHourMin}`;

      if (isDateMatch && isTimeMatch && !this.alertedReminderIds.has(alertKey)) {
        this.alertedReminderIds.add(alertKey);
        this.triggerAlert(r);
      }
    }
  }

  public triggerAlert(reminder: MedicalReminder): void {
    // 1. Play auditory alarm tone
    speechService.playTone('alert');

    // 2. Speak reminder aloud
    const spokenMessage = `Drishti Reminder. ${reminder.title}. Scheduled for ${this.formatTimeForSpeech(reminder.time)}.${reminder.notes ? ' ' + reminder.notes : ''}`;
    setTimeout(() => {
      speechService.speak(spokenMessage);
    }, 600);

    // 3. Web Notification (if permission granted)
    if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification(`Drishti Reminder: ${reminder.title}`, {
          body: `Category: ${reminder.category}. Time: ${reminder.time}. ${reminder.notes || ''}`,
          icon: '/assets/drishti/ic_drishti_logo.png',
        });
      } catch (_) {}
    }

    // 4. Notify in-app alert listeners (modal dialog)
    this.alertListeners.forEach((cb) => cb(reminder));
  }

  public formatTimeForSpeech(time24: string): string {
    const [h, m] = time24.split(':').map(Number);
    const period = h >= 12 ? 'P M' : 'A M';
    const h12 = h % 12 === 0 ? 12 : h % 12;
    return m === 0 ? `${h12} ${period}` : `${h12} ${m} ${period}`;
  }

  // Answer voice queries on schedule and speak all reminders as requested
  public querySchedule(queryText: string, lang: AppLanguage = 'en'): { summary: string; reminders: MedicalReminder[] } {
    const lower = queryText.toLowerCase().trim();
    const reminders = this.getAll();
    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];

    const tomorrow = new Date(now);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];

    const dayOfWeek = now.getDay();
    const isWeekday = dayOfWeek >= 1 && dayOfWeek <= 5;

    // Check if user is asking for the entire/full schedule
    const isEntireSchedule =
      lower.includes('entire') ||
      lower.includes('full') ||
      lower.includes('all') ||
      lower.includes('whole') ||
      lower.includes('read my schedule') ||
      lower.includes('read schedule') ||
      lower.includes('tell me my schedule') ||
      lower.includes('what is my schedule') ||
      lower.includes('माझे वेळापत्रक') ||
      lower.includes('संपूर्ण वेळापत्रक') ||
      lower.includes('मेरा शेड्यूल') ||
      lower.includes('पूरा शेड्यूल');

    const isTomorrowOnly = lower.includes('tomorrow') || lower.includes('कल') || lower.includes('उद्या');

    // 1. If entire schedule requested:
    if (isEntireSchedule && !isTomorrowOnly) {
      const activeReminders = reminders.filter((r) => r.status !== 'completed');

      if (activeReminders.length === 0) {
        if (lang === 'hi') {
          return { summary: 'आपकी समयसारणी में वर्तमान में कोई सक्रिय रिमाइंडर नहीं है।', reminders: [] };
        }
        if (lang === 'mr') {
          return { summary: 'तुमच्या वेळापत्रकात सध्या कोणतेही सक्रिय स्मरणपत्र नाही.', reminders: [] };
        }
        return { summary: 'You currently have no scheduled reminders on your agenda.', reminders: [] };
      }

      // Group into Today and Upcoming
      const todayReminders = activeReminders.filter((r) => {
        return r.date === todayStr || r.repeat === 'daily' || (r.repeat === 'weekdays' && isWeekday);
      });
      todayReminders.sort((a, b) => a.time.localeCompare(b.time));

      const upcomingReminders = activeReminders.filter((r) => {
        return !todayReminders.some((tr) => tr.id === r.id);
      });
      upcomingReminders.sort((a, b) => (a.date + a.time).localeCompare(b.date + b.time));

      if (lang === 'hi') {
        let text = `यहाँ आपकी पूरी समयसारणी है। `;
        if (todayReminders.length > 0) {
          text += `आज के लिए कुल ${todayReminders.length} रिमाइंडर हैं: `;
          todayReminders.forEach((r, idx) => {
            text += `${idx + 1}. ${this.formatTimeForSpeech(r.time)} पर ${r.title}${r.notes ? ` (${r.notes})` : ''}। `;
          });
        } else {
          text += `आज के लिए कोई तत्काल रिमाइंडर नहीं है। `;
        }

        if (upcomingReminders.length > 0) {
          text += `आगामी कार्यक्रमों में: `;
          upcomingReminders.forEach((r, idx) => {
            const dayLabel = r.date === tomorrowStr ? 'कल' : r.date;
            text += `${dayLabel} को ${this.formatTimeForSpeech(r.time)} पर ${r.title}। `;
          });
        }
        text += `आपकी संपूर्ण समयसारणी समाप्त हुई।`;
        return { summary: text, reminders: activeReminders };
      }

      if (lang === 'mr') {
        let text = `येथे तुमचे संपूर्ण वेळापत्रक आहे. `;
        if (todayReminders.length > 0) {
          text += `आजसाठी एकूण ${todayReminders.length} स्मरणपत्रे आहेत: `;
          todayReminders.forEach((r, idx) => {
            text += `${idx + 1}. ${this.formatTimeForSpeech(r.time)} वाजता ${r.title}${r.notes ? ` (${r.notes})` : ''}. `;
          });
        } else {
          text += `आजसाठी कोणतेही तातडीचे रिमाइंडर नाही. `;
        }

        if (upcomingReminders.length > 0) {
          text += `पुढील नियोजित कार्यक्रमात: `;
          upcomingReminders.forEach((r) => {
            const dayLabel = r.date === tomorrowStr ? 'उद्या' : r.date;
            text += `${dayLabel} ${this.formatTimeForSpeech(r.time)} वाजता ${r.title}. `;
          });
        }
        text += `हे तुमचे संपूर्ण वेळापत्रक पूर्ण झाले आहे.`;
        return { summary: text, reminders: activeReminders };
      }

      // English summary
      let text = `Here is your entire schedule. `;
      if (todayReminders.length > 0) {
        text += `Today you have ${todayReminders.length} reminder${todayReminders.length > 1 ? 's' : ''}: `;
        todayReminders.forEach((r, idx) => {
          const ordinal = idx === 0 ? 'First' : idx === 1 ? 'Second' : idx === 2 ? 'Third' : `Number ${idx + 1}`;
          text += `${ordinal}, at ${this.formatTimeForSpeech(r.time)}, ${r.title}${r.notes ? ` - notes: ${r.notes}` : ''}. `;
        });
      } else {
        text += `You have no reminders due today. `;
      }

      if (upcomingReminders.length > 0) {
        text += `For upcoming days: `;
        upcomingReminders.forEach((r) => {
          const dayLabel = r.date === tomorrowStr ? 'Tomorrow' : `On ${r.date}`;
          text += `${dayLabel} at ${this.formatTimeForSpeech(r.time)}, ${r.title}. `;
        });
      }

      text += `That concludes your entire schedule.`;
      return { summary: text, reminders: activeReminders };
    }

    // 2. Specific date queries (e.g. today or tomorrow)
    let targetDate = todayStr;
    let isToday = true;

    if (isTomorrowOnly) {
      targetDate = tomorrowStr;
      isToday = false;
    }

    const filtered = reminders.filter((r) => {
      if (r.status === 'completed') return false;
      if (lower.includes('medication') || lower.includes('medicine') || lower.includes('दवा') || lower.includes('औषध')) {
        if (r.category !== 'medication') return false;
      }
      if (lower.includes('appointment') || lower.includes('doctor') || lower.includes('डॉक्टर') || lower.includes('भेट')) {
        if (r.category !== 'doctor_appointment') return false;
      }

      if (r.date === targetDate) return true;
      if (r.repeat === 'daily') return true;
      if (r.repeat === 'weekdays' && (isToday ? isWeekday : true)) return true;
      return false;
    });

    filtered.sort((a, b) => a.time.localeCompare(b.time));

    if (filtered.length === 0) {
      if (lang === 'hi') {
        return {
          summary: isToday
            ? 'आज के लिए आपका कोई निर्धारित रिमाइंडर नहीं है। आपकी समयसारणी पूरी तरह खाली है।'
            : 'कल के लिए आपका कोई निर्धारित रिमाइंडर नहीं है।',
          reminders: [],
        };
      }
      if (lang === 'mr') {
        return {
          summary: isToday
            ? 'आजसाठी तुमचे कोणतेही नियोजित रिमाइंडर नाही. तुमचे वेळापत्रक रिकामे आहे.'
            : 'उद्यासाठी तुमचे कोणतेही नियोजित रिमाइंडर नाही.',
          reminders: [],
        };
      }
      return {
        summary: isToday
          ? 'You have no scheduled reminders for today. Your schedule is all clear!'
          : 'You have no scheduled reminders for tomorrow. You are all set!',
        reminders: [],
      };
    }

    if (lang === 'hi') {
      const count = filtered.length;
      const details = filtered
        .map((r, i) => `${i + 1}. ${this.formatTimeForSpeech(r.time)} पर ${r.title}${r.notes ? ` (${r.notes})` : ''}`)
        .join('; ');
      return {
        summary: `आज आपके कुल ${count} रिमाइंडर हैं: ${details}।`,
        reminders: filtered,
      };
    }

    if (lang === 'mr') {
      const count = filtered.length;
      const details = filtered
        .map((r, i) => `${i + 1}. ${this.formatTimeForSpeech(r.time)} वाजता ${r.title}${r.notes ? ` (${r.notes})` : ''}`)
        .join('; ');
      return {
        summary: `आज तुमची एकूण ${count} स्मरणपत्रे आहेत: ${details}.`,
        reminders: filtered,
      };
    }

    const count = filtered.length;
    const countWord = count === 1 ? 'one reminder' : `${count} reminders`;
    const details = filtered
      .map((r, idx) => {
        const orderWord = idx === 0 ? 'First' : idx === 1 ? 'Second' : idx === 2 ? 'Third' : `Number ${idx + 1}`;
        const timeSpeech = this.formatTimeForSpeech(r.time);
        const categoryLabel = r.category === 'medication' ? 'medicine reminder' : r.category === 'doctor_appointment' ? 'doctor appointment' : 'reminder';
        return `${orderWord}, your ${categoryLabel} for "${r.title}" is at ${timeSpeech}${r.notes ? ` (${r.notes})` : ''}`;
      })
      .join('. ');

    const summary = `You have ${countWord} scheduled for ${isToday ? 'today' : 'tomorrow'}. ${details}.`;
    return { summary, reminders: filtered };
  }
}

export const reminderStorage = new ReminderStorage();

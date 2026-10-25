import React, { useState, useEffect } from 'react';
import {
  Calendar as CalendarIcon,
  Clock,
  Plus,
  Mic,
  Volume2,
  Check,
  RotateCcw,
  Trash2,
  Bell,
  Pill,
  Stethoscope,
  HeartPulse,
  Sparkles,
  Info,
  CalendarDays,
  CheckCircle2,
  Repeat,
  AlertCircle,
  X,
} from 'lucide-react';
import { MedicalReminder, ReminderCategory, ReminderRepeat, AppLanguage, CoreState } from '../types';
import { reminderStorage } from '../services/reminderStorage';
import { speechService } from '../services/speechService';
import { DrishtiCoreOrb } from '../components/DrishtiCoreOrb';

interface ScheduleScreenProps {
  language: AppLanguage;
  initialVoiceQuery?: string;
  initialSpokenSummary?: string;
  onClearInitialVoiceQuery?: () => void;
  onFinishedSpokenSummary?: () => void;
}

export const ScheduleScreen: React.FC<ScheduleScreenProps> = ({
  language,
  initialVoiceQuery,
  initialSpokenSummary,
  onClearInitialVoiceQuery,
  onFinishedSpokenSummary,
}) => {
  const [reminders, setReminders] = useState<MedicalReminder[]>(() => reminderStorage.getAll());
  const [selectedView, setSelectedView] = useState<'daily' | 'weekly' | 'monthly'>('daily');
  const [selectedCategory, setSelectedCategory] = useState<ReminderCategory | 'all'>('all');
  const [showAddModal, setShowAddModal] = useState(false);

  // Voice Interaction State
  const [isVoiceActive, setIsVoiceActive] = useState(false);
  const [coreState, setCoreState] = useState<CoreState>('IDLE');
  const [voiceStep, setVoiceStep] = useState<'idle' | 'greeting' | 'listening_details' | 'confirming_save'>('idle');
  const [pendingVoiceReminder, setPendingVoiceReminder] = useState<any | null>(null);
  const [voiceDialogueText, setVoiceDialogueText] = useState<string>('');

  // Handle incoming voice queries or spoken schedule requests
  useEffect(() => {
    if (initialSpokenSummary) {
      setVoiceDialogueText(initialSpokenSummary);
      setCoreState('SPEAKING');
      speechService.speak(initialSpokenSummary, {
        language,
        onEnd: () => {
          setCoreState('IDLE');
          onFinishedSpokenSummary?.();
        },
        onError: () => {
          setCoreState('IDLE');
          onFinishedSpokenSummary?.();
        },
      });
      onClearInitialVoiceQuery?.();
    } else if (initialVoiceQuery) {
      processScheduleVoiceInput(initialVoiceQuery);
      onClearInitialVoiceQuery?.();
    }
  }, [initialVoiceQuery, initialSpokenSummary]);

  // Form State for manual creation
  const [formTitle, setFormTitle] = useState('');
  const [formCategory, setFormCategory] = useState<ReminderCategory>('medication');
  const [formDate, setFormDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [formTime, setFormTime] = useState('08:00');
  const [formRepeat, setFormRepeat] = useState<ReminderRepeat>('daily');
  const [formNotes, setFormNotes] = useState('');

  // Subscribe to storage updates
  useEffect(() => {
    const unsubscribe = reminderStorage.subscribe((updated) => {
      setReminders([...updated]);
    });
    return () => unsubscribe();
  }, []);

  // Request Notification permission
  useEffect(() => {
    if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission().catch(() => {});
    }
  }, []);

  // Primary activation: "Hey Drishti Schedule"
  const startVoiceSchedulingFlow = () => {
    speechService.stop();
    setVoiceStep('greeting');
    setCoreState('SPEAKING');
    setIsVoiceActive(true);

    const greeting =
      language === 'hi'
        ? "मैं सुन रहा हूँ। आप क्या शेड्यूल करना चाहते हैं?"
        : language === 'mr'
        ? "मी ऐकत आहे. तुम्हाला काय शेड्युल करायचे आहे?"
        : "I'm listening. What would you like to schedule?";

    setVoiceDialogueText(greeting);

    speechService.speak(greeting, {
      language,
      onEnd: () => {
        // Step 2: Listen for user instruction
        listenForScheduleInstruction();
      },
    });
  };

  const listenForScheduleInstruction = () => {
    setVoiceStep('listening_details');
    setCoreState('LISTENING');
    speechService.playTone('listen_start');

    speechService.startListening({
      language,
      continuous: false,
      onResult: async (transcript, isFinal) => {
        if (isFinal) {
          setVoiceDialogueText(`"${transcript}"`);
          await processScheduleVoiceInput(transcript);
        }
      },
      onError: (err) => {
        setCoreState('IDLE');
        setVoiceStep('idle');
        speechService.speak("I couldn't hear that clearly. Please tap the microphone to try again.", { language });
      },
      onEnd: () => {
        if (voiceStep === 'listening_details') {
          setCoreState('IDLE');
        }
      },
    });
  };

  const processScheduleVoiceInput = async (transcript: string) => {
    setCoreState('PROCESSING');
    speechService.playTone('processing');

    const lower = transcript.toLowerCase().trim();

    // Fast-path: Direct today's schedule voice query (speaks all reminders immediately)
    const isDirectScheduleQuery =
      lower.includes('today') ||
      lower.includes('todays') ||
      lower.includes("today's") ||
      lower.includes('what') ||
      lower.includes('tell') ||
      lower.includes('read') ||
      lower.includes('check') ||
      lower.includes('appointments') ||
      lower.includes('list') ||
      lower.includes('agenda') ||
      lower.includes('क्या') ||
      lower.includes('बताओ') ||
      lower.includes('सांगा') ||
      lower.includes('सांग');

    if (isDirectScheduleQuery) {
      const scheduleResult = reminderStorage.querySchedule(transcript, language);
      setCoreState('SPEAKING');
      setVoiceDialogueText(scheduleResult.summary);
      speechService.speak(scheduleResult.summary, {
        language,
        onEnd: () => {
          setCoreState('IDLE');
          setVoiceStep('idle');
        },
        onError: () => {
          setCoreState('IDLE');
          setVoiceStep('idle');
        },
      });
      return;
    }

    try {
      const res = await fetch('/api/schedule/parse', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          transcript,
          currentTimeIso: new Date().toISOString(),
          language,
        }),
      });

      const data = await res.json();

      // Action: Query Schedule
      if (data.action === 'query') {
        const scheduleResult = reminderStorage.querySchedule(transcript, language);
        setCoreState('SPEAKING');
        setVoiceDialogueText(scheduleResult.summary);
        speechService.speak(scheduleResult.summary, {
          language,
          onEnd: () => {
            setCoreState('IDLE');
            setVoiceStep('idle');
          },
          onError: () => {
            setCoreState('IDLE');
            setVoiceStep('idle');
          },
        });
        return;
      }

      // Action: Cancel
      if (data.action === 'cancel') {
        const found = reminders.find(
          (r) =>
            r.title.toLowerCase().includes(data.title.toLowerCase()) ||
            (data.time && r.time === data.time)
        );
        if (found) {
          reminderStorage.deleteReminder(found.id);
          const confirmText = `Your reminder for "${found.title}" has been cancelled.`;
          setVoiceDialogueText(confirmText);
          setCoreState('SPEAKING');
          speechService.speak(confirmText, {
            language,
            onEnd: () => {
              setCoreState('IDLE');
              setVoiceStep('idle');
            },
          });
        } else {
          const notFoundText = "I couldn't find a matching reminder to cancel on your schedule.";
          setVoiceDialogueText(notFoundText);
          speechService.speak(notFoundText, { language });
          setCoreState('IDLE');
          setVoiceStep('idle');
        }
        return;
      }

      // Action: Create Reminder (Requires spoken confirmation)
      if (data.action === 'create' || !data.action) {
        setPendingVoiceReminder(data);
        setVoiceStep('confirming_save');
        setCoreState('SPEAKING');

        const prompt =
          data.spokenPrompt ||
          `You want a reminder: "${data.title}" at ${reminderStorage.formatTimeForSpeech(
            data.time || '08:00'
          )}. Should I save it?`;

        setVoiceDialogueText(prompt);

        speechService.speak(prompt, {
          language,
          onEnd: () => {
            // Listen for user's confirmation ("Yes", "Save it", "Cancel")
            listenForConfirmation(data);
          },
        });
      }
    } catch (e) {
      console.error('Schedule processing error:', e);
      setCoreState('IDLE');
      setVoiceStep('idle');
    }
  };

  const listenForConfirmation = (pendingData: any) => {
    setCoreState('LISTENING');
    speechService.playTone('listen_start');

    speechService.startListening({
      language,
      continuous: false,
      onResult: (transcript, isFinal) => {
        if (isFinal) {
          const lower = transcript.toLowerCase();
          if (
            lower.includes('yes') ||
            lower.includes('save') ||
            lower.includes('confirm') ||
            lower.includes('हाँ') ||
            lower.includes('हो')
          ) {
            // Save reminder
            executeSaveReminder(pendingData);
          } else {
            // Cancelled
            speechService.stopListening();
            setCoreState('SPEAKING');
            const cancelText = "Reminder was not saved.";
            setVoiceDialogueText(cancelText);
            speechService.speak(cancelText, {
              language,
              onEnd: () => {
                setCoreState('IDLE');
                setVoiceStep('idle');
                setPendingVoiceReminder(null);
              },
            });
          }
        }
      },
      onError: () => {
        setCoreState('IDLE');
        setVoiceStep('idle');
      },
      onEnd: () => {
        if (voiceStep === 'confirming_save') {
          setCoreState('IDLE');
        }
      },
    });
  };

  const executeSaveReminder = (data: any) => {
    reminderStorage.addReminder({
      title: data.title || 'Medical Reminder',
      category: data.category || 'medication',
      date: data.date || new Date().toISOString().split('T')[0],
      time: data.time || '08:00',
      repeat: data.repeat || 'none',
      notes: data.notes || '',
    });

    speechService.playTone('success');
    setCoreState('SPEAKING');

    const finalConfirm = `Your reminder for "${data.title}" has been saved for ${reminderStorage.formatTimeForSpeech(
      data.time || '08:00'
    )}.`;

    setVoiceDialogueText(finalConfirm);

    speechService.speak(finalConfirm, {
      language,
      onEnd: () => {
        setCoreState('IDLE');
        setVoiceStep('idle');
        setPendingVoiceReminder(null);
        setIsVoiceActive(false);
      },
    });
  };

  const handleManualAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formTitle.trim()) return;

    const saved = reminderStorage.addReminder({
      title: formTitle.trim(),
      category: formCategory,
      date: formDate,
      time: formTime,
      repeat: formRepeat,
      notes: formNotes.trim(),
    });

    speechService.playTone('success');
    speechService.speak(`Reminder saved for ${saved.title} at ${reminderStorage.formatTimeForSpeech(saved.time)}.`);

    setFormTitle('');
    setFormNotes('');
    setShowAddModal(false);
  };

  const handleReadRemindersAloud = () => {
    speechService.stop();
    speechService.playTone('processing');
    const result = reminderStorage.querySchedule('what is on my schedule today', language);
    setVoiceDialogueText(result.summary);
    setCoreState('SPEAKING');
    speechService.speak(result.summary, {
      language,
      onEnd: () => setCoreState('IDLE'),
      onError: () => setCoreState('IDLE'),
    });
  };

  const getCategoryMeta = (cat: ReminderCategory) => {
    switch (cat) {
      case 'medication':
        return {
          label: 'Medication',
          icon: <Pill className="w-4 h-4 text-blue-600" />,
          color: 'bg-blue-50 text-blue-700 border-blue-200',
        };
      case 'doctor_appointment':
        return {
          label: 'Doctor Appointment',
          icon: <Stethoscope className="w-4 h-4 text-indigo-600" />,
          color: 'bg-indigo-50 text-indigo-700 border-indigo-200',
        };
      case 'medical_test':
        return {
          label: 'Medical Test',
          icon: <HeartPulse className="w-4 h-4 text-rose-600" />,
          color: 'bg-rose-50 text-rose-700 border-rose-200',
        };
      case 'health_routine':
        return {
          label: 'Health Routine',
          icon: <Sparkles className="w-4 h-4 text-emerald-600" />,
          color: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        };
      default:
        return {
          label: 'Personal',
          icon: <Clock className="w-4 h-4 text-slate-600" />,
          color: 'bg-slate-50 text-slate-700 border-slate-200',
        };
    }
  };

  // Filtered by Category and View
  const todayStr = new Date().toISOString().split('T')[0];
  const filteredReminders = reminders.filter((r) => {
    if (selectedCategory !== 'all' && r.category !== selectedCategory) return false;

    if (selectedView === 'daily') {
      return r.date === todayStr || r.repeat === 'daily';
    } else if (selectedView === 'weekly') {
      return true;
    }
    return true;
  });

  return (
    <div className="w-full min-h-screen bg-slate-50/70 pb-28 pt-4 px-4 sm:px-6">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Main Schedule Banner */}
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/90 shadow-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className="text-[11px] font-black uppercase tracking-widest text-blue-700 bg-blue-50 border border-blue-200/60 px-3 py-1 rounded-full">
                  "Hey Drishti Schedule" Active
                </span>
                <span className="text-[11px] text-slate-500 font-medium">
                  Audible Reminders &amp; Calendar
                </span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
                Medical Schedule &amp; Reminders
              </h1>
              <p className="text-xs sm:text-sm text-slate-600 mt-1 max-w-xl">
                Manage medications, doctor appointments, check-ups, and health routines with spoken voice confirmations.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={handleReadRemindersAloud}
                aria-label="Read today's schedule aloud"
                className="px-4 py-2.5 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold text-xs flex items-center gap-2 transition-all cursor-pointer"
              >
                <Volume2 className="w-4 h-4 text-blue-600" />
                <span>Read Today's Schedule</span>
              </button>

              <button
                onClick={() => setShowAddModal(true)}
                aria-label="Add new reminder manually"
                className="px-4 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs flex items-center gap-1.5 shadow-xs transition-all cursor-pointer"
              >
                <Plus className="w-4 h-4" />
                <span>Add Reminder</span>
              </button>
            </div>
          </div>

          {/* Voice Assistant Interaction Stage */}
          <div className="mt-6 p-5 sm:p-6 rounded-3xl bg-blue-50/60 border-2 border-blue-200 flex flex-col sm:flex-row items-center gap-5">
            <div className="shrink-0">
              <DrishtiCoreOrb
                state={coreState}
                size="compact"
                onClick={startVoiceSchedulingFlow}
                ariaLabel="Trigger Hey Drishti Schedule voice assistant"
              />
            </div>

            <div className="flex-1 text-center sm:text-left space-y-2">
              <div className="flex items-center justify-center sm:justify-start gap-2">
                <span className="w-2 h-2 rounded-full bg-blue-600 animate-ping" />
                <span className="text-xs font-black uppercase tracking-wider text-blue-800">
                  Voice Scheduling Assistant
                </span>
              </div>
              <p className="text-base font-bold text-slate-900">
                {voiceDialogueText || (
                  <span>
                    Tap the orb or speak <span className="text-blue-600 font-extrabold">"Hey Drishti Schedule"</span> to create or check reminders.
                  </span>
                )}
              </p>
              <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-1">
                {[
                  'Remind me to take my medicine at 8 PM',
                  'What is on my schedule today?',
                  'Schedule doctor appointment tomorrow at 11 AM',
                  'Read my medical reminders',
                ].map((sample, idx) => (
                  <button
                    key={idx}
                    onClick={() => processScheduleVoiceInput(sample)}
                    className="text-[11px] font-semibold text-slate-700 bg-white border border-blue-200/80 px-2.5 py-1 rounded-lg hover:bg-blue-50 hover:text-blue-600 transition-colors cursor-pointer"
                  >
                    "{sample}"
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* View Switcher (Daily, Weekly, Monthly) & Category Filter */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          {/* Calendar View Tabs */}
          <div className="flex items-center p-1 bg-white rounded-2xl border border-slate-200 shadow-xs self-start">
            {[
              { id: 'daily', label: "Today's Agenda" },
              { id: 'weekly', label: '7-Day View' },
              { id: 'monthly', label: 'All Events' },
            ].map((v) => (
              <button
                key={v.id}
                onClick={() => setSelectedView(v.id as any)}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  selectedView === v.id
                    ? 'bg-blue-600 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {v.label}
              </button>
            ))}
          </div>

          {/* Category Filter Pills */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
            {[
              { id: 'all', label: 'All' },
              { id: 'medication', label: 'Medications' },
              { id: 'doctor_appointment', label: 'Appointments' },
              { id: 'medical_test', label: 'Tests' },
              { id: 'health_routine', label: 'Routines' },
            ].map((cat) => (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.id as any)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors cursor-pointer ${
                  selectedCategory === cat.id
                    ? 'bg-slate-900 text-white font-bold'
                    : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>

        {/* Reminders List */}
        <div className="space-y-3">
          {filteredReminders.length === 0 ? (
            <div className="p-12 text-center bg-white rounded-3xl border border-slate-200 space-y-2">
              <CalendarDays className="w-10 h-10 text-slate-400 mx-auto" />
              <h3 className="text-base font-bold text-slate-900">No scheduled reminders found</h3>
              <p className="text-xs text-slate-500">
                You can say "Hey Drishti Schedule, remind me..." or tap "Add Reminder".
              </p>
            </div>
          ) : (
            filteredReminders.map((reminder) => {
              const meta = getCategoryMeta(reminder.category);
              const isCompleted = reminder.status === 'completed';

              return (
                <div
                  key={reminder.id}
                  className={`p-5 rounded-3xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4 ${
                    isCompleted
                      ? 'bg-slate-50/80 border-slate-200 opacity-60'
                      : 'bg-white border-slate-200/90 shadow-xs hover:border-slate-300'
                  }`}
                >
                  <div className="flex items-start gap-4">
                    {/* Time Box */}
                    <div className="w-16 h-16 rounded-2xl bg-blue-50 border border-blue-200 flex flex-col items-center justify-center shrink-0">
                      <span className="text-lg font-black text-blue-900">
                        {reminder.time}
                      </span>
                      <span className="text-[10px] font-bold text-blue-600 uppercase">
                        {parseInt(reminder.time.split(':')[0]) >= 12 ? 'PM' : 'AM'}
                      </span>
                    </div>

                    {/* Details */}
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded-md border flex items-center gap-1 ${meta.color}`}
                        >
                          {meta.icon}
                          <span>{meta.label}</span>
                        </span>

                        {reminder.repeat !== 'none' && (
                          <span className="text-[10px] font-semibold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md flex items-center gap-1">
                            <Repeat className="w-3 h-3" />
                            <span>{reminder.repeat}</span>
                          </span>
                        )}

                        <span className="text-[11px] text-slate-400 font-medium">{reminder.date}</span>
                      </div>

                      <h3
                        className={`text-base font-bold text-slate-900 ${
                          isCompleted ? 'line-through text-slate-400' : ''
                        }`}
                      >
                        {reminder.title}
                      </h3>

                      {reminder.notes && (
                        <p className="text-xs text-slate-500 leading-relaxed">{reminder.notes}</p>
                      )}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 self-end sm:self-center">
                    {/* Hear reminder button */}
                    <button
                      onClick={() => {
                        const speakMsg = `${reminder.title} at ${reminderStorage.formatTimeForSpeech(
                          reminder.time
                        )}.${reminder.notes ? ' ' + reminder.notes : ''}`;
                        speechService.speak(speakMsg, { language });
                      }}
                      aria-label={`Hear ${reminder.title} aloud`}
                      className="p-2.5 rounded-xl text-blue-600 hover:bg-blue-50 border border-transparent hover:border-blue-200 transition-colors cursor-pointer"
                    >
                      <Volume2 className="w-4 h-4" />
                    </button>

                    {/* Complete button */}
                    <button
                      onClick={() => reminderStorage.markCompleted(reminder.id)}
                      disabled={isCompleted}
                      aria-label={`Mark ${reminder.title} as completed`}
                      className={`px-3.5 py-2 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
                        isCompleted
                          ? 'bg-slate-100 text-slate-400 cursor-default'
                          : 'bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100'
                      }`}
                    >
                      <Check className="w-3.5 h-3.5" />
                      <span>{isCompleted ? 'Done' : 'Complete'}</span>
                    </button>

                    {/* Delete button */}
                    <button
                      onClick={() => {
                        if (confirm(`Cancel and delete reminder for "${reminder.title}"?`)) {
                          reminderStorage.deleteReminder(reminder.id);
                        }
                      }}
                      aria-label={`Delete ${reminder.title}`}
                      className="p-2.5 rounded-xl text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors cursor-pointer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Browser Background Scheduling Notice */}
        <div className="p-4 rounded-2xl bg-slate-100/80 border border-slate-200 text-slate-600 text-xs flex items-start gap-2.5">
          <Info className="w-4 h-4 text-slate-500 shrink-0 mt-0.5" />
          <p>
            <strong>Browser Audio &amp; Timer Limitations:</strong> In browser environments, background alerts function reliably while this tab remains open or backgrounded. When an alarm is due, Drishti sounds a chime and speaks the reminder aloud. Please grant browser notification permissions for lock-screen alerts.
          </p>
        </div>
      </div>

      {/* Manual Add Reminder Modal */}
      {showAddModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 animate-fadeIn"
          onClick={() => setShowAddModal(false)}
        >
          <div
            className="bg-white rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl border border-slate-200 space-y-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="text-lg font-bold text-slate-900">Add Schedule Reminder</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-1.5 rounded-full hover:bg-slate-100 text-slate-500"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleManualAddSubmit} className="space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-800 mb-1">Reminder / Event Title *</label>
                <input
                  type="text"
                  required
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder="e.g. Eye drops, Doctor check-up, Blood test"
                  className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:bg-white focus:border-blue-600 focus-visible:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-800 mb-1">Category</label>
                  <select
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value as ReminderCategory)}
                    className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:bg-white focus:border-blue-600 focus-visible:outline-none"
                  >
                    <option value="medication">Medication</option>
                    <option value="doctor_appointment">Doctor Appointment</option>
                    <option value="medical_test">Medical Test</option>
                    <option value="health_routine">Health Routine</option>
                    <option value="personal">Personal</option>
                  </select>
                </div>

                <div>
                  <label className="block font-bold text-slate-800 mb-1">Repeat</label>
                  <select
                    value={formRepeat}
                    onChange={(e) => setFormRepeat(e.target.value as ReminderRepeat)}
                    className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:bg-white focus:border-blue-600 focus-visible:outline-none"
                  >
                    <option value="none">Once only</option>
                    <option value="daily">Daily</option>
                    <option value="weekdays">Weekdays</option>
                    <option value="weekly">Weekly</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-800 mb-1">Date</label>
                  <input
                    type="date"
                    required
                    value={formDate}
                    onChange={(e) => setFormDate(e.target.value)}
                    className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:bg-white focus:border-blue-600 focus-visible:outline-none"
                  >
                  </input>
                </div>

                <div>
                  <label className="block font-bold text-slate-800 mb-1">Time (24hr)</label>
                  <input
                    type="time"
                    required
                    value={formTime}
                    onChange={(e) => setFormTime(e.target.value)}
                    className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:bg-white focus:border-blue-600 focus-visible:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-800 mb-1">Notes / Instructions (Optional)</label>
                <textarea
                  rows={2}
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  placeholder="e.g. Take with warm water after breakfast"
                  className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:bg-white focus:border-blue-600 focus-visible:outline-none"
                />
              </div>

              <div className="flex items-center gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 py-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-xs"
                >
                  Save Reminder
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

import React from 'react';
import { Bell, Check, Clock, X, Pill, Stethoscope, HeartPulse, Sparkles } from 'lucide-react';
import { MedicalReminder } from '../types';
import { reminderStorage } from '../services/reminderStorage';

interface ActiveReminderModalProps {
  reminder: MedicalReminder | null;
  onClose: () => void;
}

export const ActiveReminderModal: React.FC<ActiveReminderModalProps> = ({ reminder, onClose }) => {
  if (!reminder) return null;

  const handleComplete = () => {
    reminderStorage.markCompleted(reminder.id);
    onClose();
  };

  const handleSnooze = () => {
    reminderStorage.snoozeReminder(reminder.id, 10);
    onClose();
  };

  const getCategoryIcon = () => {
    switch (reminder.category) {
      case 'medication':
        return <Pill className="w-8 h-8 text-blue-600" />;
      case 'doctor_appointment':
        return <Stethoscope className="w-8 h-8 text-indigo-600" />;
      case 'medical_test':
        return <HeartPulse className="w-8 h-8 text-rose-600" />;
      case 'health_routine':
        return <Sparkles className="w-8 h-8 text-emerald-600" />;
      default:
        return <Bell className="w-8 h-8 text-blue-600" />;
    }
  };

  return (
    <div
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="alarm-title"
      aria-describedby="alarm-desc"
      className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn"
    >
      <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl border-2 border-blue-500 animate-scaleUp text-center">
        {/* Pulsing Alarm Icon */}
        <div className="w-20 h-20 mx-auto rounded-3xl bg-blue-50 border-2 border-blue-200 flex items-center justify-center mb-4 shadow-sm animate-bounce">
          {getCategoryIcon()}
        </div>

        <span className="text-xs font-black uppercase tracking-widest text-blue-600 bg-blue-100 px-3 py-1 rounded-full">
          Drishti Medical Reminder
        </span>

        <h2 id="alarm-title" className="text-2xl font-black text-slate-900 mt-3 leading-snug">
          {reminder.title}
        </h2>

        <p id="alarm-desc" className="text-sm font-semibold text-slate-600 mt-2">
          Scheduled Time: {reminderStorage.formatTimeForSpeech(reminder.time)}
          {reminder.repeat !== 'none' && ` • Repeats ${reminder.repeat}`}
        </p>

        {reminder.notes && (
          <div className="mt-3 p-3 rounded-2xl bg-slate-50 border border-slate-200 text-xs text-slate-700 font-medium">
            <strong>Notes:</strong> {reminder.notes}
          </div>
        )}

        {/* Big Accessible Buttons */}
        <div className="mt-6 space-y-3">
          <button
            onClick={handleComplete}
            className="w-full py-4 px-6 rounded-2xl bg-emerald-600 text-white font-extrabold text-base sm:text-lg flex items-center justify-center gap-3 hover:bg-emerald-700 active:scale-98 shadow-md transition-all cursor-pointer focus-visible:ring-4 focus-visible:ring-emerald-600"
          >
            <Check className="w-6 h-6 stroke-[3]" />
            <span>Mark as Done</span>
          </button>

          <button
            onClick={handleSnooze}
            className="w-full py-3.5 px-6 rounded-2xl bg-slate-100 text-slate-800 font-bold text-sm sm:text-base flex items-center justify-center gap-2 hover:bg-slate-200 active:scale-98 transition-all cursor-pointer focus-visible:ring-4 focus-visible:ring-slate-600"
          >
            <Clock className="w-5 h-5 text-slate-600" />
            <span>Snooze for 10 Minutes</span>
          </button>

          <button
            onClick={onClose}
            className="w-full py-2.5 text-xs font-semibold text-slate-500 hover:text-slate-800 transition-colors flex items-center justify-center gap-1 cursor-pointer"
          >
            <X className="w-4 h-4" /> Dismiss alert
          </button>
        </div>
      </div>
    </div>
  );
};

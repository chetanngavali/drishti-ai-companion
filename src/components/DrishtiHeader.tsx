import React, { useState } from 'react';
import { Menu, Globe, AlertTriangle, Sun, Moon, Volume2, Bell, Mic, MicOff } from 'lucide-react';
import { AppLanguage, CoreState } from '../types';
import { speechService } from '../services/speechService';

interface DrishtiHeaderProps {
  onOpenDrawer: () => void;
  language: AppLanguage;
  onLanguageChange: (lang: AppLanguage) => void;
  onOpenSos: () => void;
  onOpenNotifications: () => void;
  unreadNotificationsCount: number;
  coreState: CoreState;
  highContrast: boolean;
  onToggleHighContrast: () => void;
  isWakeWordActive?: boolean;
  onToggleWakeWord?: () => void;
}

export const DrishtiHeader: React.FC<DrishtiHeaderProps> = ({
  onOpenDrawer,
  language,
  onLanguageChange,
  onOpenSos,
  onOpenNotifications,
  unreadNotificationsCount,
  coreState,
  highContrast,
  onToggleHighContrast,
  isWakeWordActive = false,
  onToggleWakeWord,
}) => {
  const [showLanguageModal, setShowLanguageModal] = useState(false);

  const getLanguageLabel = () => {
    switch (language) {
      case 'hi':
        return 'Auto • हिन्दी';
      case 'mr':
        return 'Auto • मराठी';
      default:
        return 'Auto • English';
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full px-4 sm:px-6 py-3 bg-white/95 backdrop-blur-md border-b border-slate-200/80 shadow-xs transition-colors">
      <div className="max-w-6xl mx-auto flex items-center justify-between gap-3">
        {/* Left: Circular White Elevated Menu Drawer Trigger */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              speechService.playTone('listen_start');
              onOpenDrawer();
            }}
            aria-label="Open Navigation Menu"
            className="w-11 h-11 rounded-full bg-white border border-slate-200 shadow-sm flex items-center justify-center text-blue-600 hover:bg-slate-50 active:scale-95 transition-all cursor-pointer focus-visible:ring-4 focus-visible:ring-blue-600"
          >
            <Menu className="w-5 h-5 text-blue-600" />
          </button>

          {/* Drishti Brand Title & Voice State Indicator */}
          <div className="flex items-center gap-2 ml-1">
            <img
              src="/assets/drishti/ic_drishti_logo.png"
              alt="Drishti Logo"
              className="w-7 h-7 object-contain rounded-md"
            />
            <span className="text-lg font-bold tracking-tight text-slate-900 hidden xs:inline">
              Drishti<span className="text-blue-600">AI</span>
            </span>

            {/* Voice status indicator */}
            {coreState !== 'IDLE' && (
              <span
                role="status"
                aria-live="polite"
                className={`text-xs font-semibold px-2.5 py-0.5 rounded-full uppercase tracking-wider flex items-center gap-1.5 ${
                  coreState === 'LISTENING'
                    ? 'bg-blue-100 text-blue-700 animate-pulse'
                    : coreState === 'SPEAKING'
                    ? 'bg-sky-100 text-sky-700'
                    : coreState === 'PROCESSING'
                    ? 'bg-purple-100 text-purple-700'
                    : coreState === 'EMERGENCY'
                    ? 'bg-red-100 text-red-700 animate-pulse'
                    : 'bg-amber-100 text-amber-700'
                }`}
              >
                <Volume2 className="w-3 h-3 animate-spin" />
                {coreState}
              </span>
            )}
          </div>
        </div>

        {/* Center: Language & Wake Word Controls */}
        <div className="flex items-center gap-2">
          {/* "Hey Drishti" Voice Wake Trigger Button */}
          {onToggleWakeWord && (
            <button
              onClick={onToggleWakeWord}
              aria-label={
                isWakeWordActive
                  ? "Wake word active: Say 'Hey Drishti' anytime. Tap to disable"
                  : "Tap to enable 'Hey Drishti' voice wake word listener"
              }
              className={`h-11 px-3 sm:px-3.5 rounded-full border shadow-sm flex items-center gap-1.5 transition-all cursor-pointer text-xs font-bold focus-visible:ring-4 focus-visible:ring-blue-600 ${
                isWakeWordActive
                  ? 'bg-emerald-50 border-emerald-300 text-emerald-800'
                  : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              {isWakeWordActive ? (
                <>
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-ping shrink-0" />
                  <Mic className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                  <span className="hidden sm:inline">"Hey Drishti" Active</span>
                </>
              ) : (
                <>
                  <MicOff className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                  <span className="hidden sm:inline">Enable "Hey Drishti"</span>
                </>
              )}
            </button>
          )}

          <button
            onClick={() => setShowLanguageModal(true)}
            aria-label={`Current language: ${getLanguageLabel()}. Tap to switch language`}
            className="h-11 px-3.5 rounded-full bg-white border border-slate-200 shadow-sm flex items-center gap-1.5 text-slate-900 hover:bg-slate-50 active:scale-95 transition-all cursor-pointer text-xs sm:text-sm font-semibold focus-visible:ring-4 focus-visible:ring-blue-600"
          >
            <Globe className="w-4 h-4 text-blue-600 shrink-0" />
            <span className="truncate max-w-[110px] sm:max-w-none">{getLanguageLabel()}</span>
          </button>

          {/* Scheme Notifications Bell */}
          <button
            onClick={onOpenNotifications}
            aria-label={`Government Scheme Notifications. ${unreadNotificationsCount} unread.`}
            className="relative w-11 h-11 rounded-full bg-white border border-slate-200 shadow-sm flex items-center justify-center text-slate-700 hover:bg-slate-50 active:scale-95 transition-all cursor-pointer focus-visible:ring-4 focus-visible:ring-blue-600"
          >
            <Bell className="w-4 h-4 text-slate-700" />
            {unreadNotificationsCount > 0 && (
              <span className="absolute -top-1 -right-1 min-w-[20px] h-5 px-1.5 bg-blue-600 text-white rounded-full text-[11px] font-bold flex items-center justify-center shadow-xs">
                {unreadNotificationsCount}
              </span>
            )}
          </button>

          {/* High Contrast Toggle */}
          <button
            onClick={onToggleHighContrast}
            aria-label={highContrast ? 'Switch to Standard Theme' : 'Enable High Contrast Accessibility Theme'}
            className={`w-11 h-11 rounded-full border shadow-sm flex items-center justify-center active:scale-95 transition-all cursor-pointer focus-visible:ring-4 focus-visible:ring-blue-600 ${
              highContrast ? 'bg-black border-white text-yellow-300' : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
            }`}
          >
            {highContrast ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>
        </div>

        {/* Right: Emergency SOS Quick Action Button */}
        <div>
          <button
            onClick={() => {
              speechService.playTone('alert');
              onOpenSos();
            }}
            aria-label="Trigger Emergency SOS and Alert Guardians"
            className="h-11 px-4 rounded-full bg-red-50 border border-red-200 text-red-600 shadow-sm flex items-center gap-1.5 font-bold text-xs sm:text-sm hover:bg-red-100 active:scale-95 transition-all cursor-pointer focus-visible:ring-4 focus-visible:ring-red-600"
          >
            <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
            <span>SOS</span>
          </button>
        </div>
      </div>

      {/* Language Selection Modal */}
      {showLanguageModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="lang-modal-title"
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 animate-fadeIn"
          onClick={() => setShowLanguageModal(false)}
        >
          <div
            className="bg-white rounded-3xl p-6 max-w-sm w-full shadow-2xl border border-slate-200"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="lang-modal-title" className="text-xl font-bold text-slate-900 mb-1">
              Choose Your Language
            </h2>
            <p className="text-xs text-slate-600 mb-5">
              Select your preferred language for the Drishti interface, speech guidance, and voice companion.
            </p>

            <div className="space-y-3">
              {[
                { code: 'en', native: 'English', sub: 'English' },
                { code: 'hi', native: 'हिन्दी', sub: 'Hindi' },
                { code: 'mr', native: 'मराठी', sub: 'Marathi' },
              ].map((item) => {
                const isSelected = language === item.code;
                return (
                  <button
                    key={item.code}
                    onClick={() => {
                      onLanguageChange(item.code as AppLanguage);
                      speechService.playTone('success');
                      speechService.speak(
                        item.code === 'hi'
                          ? 'भाषा हिन्दी में सेट की गई है।'
                          : item.code === 'mr'
                          ? 'भाषा मराठीमध्ये सेट केली आहे.'
                          : 'Language set to English.',
                        { language: item.code as AppLanguage }
                      );
                      setShowLanguageModal(false);
                    }}
                    className={`w-full p-4 rounded-2xl border text-left flex items-center justify-between transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-blue-50 border-blue-600 text-blue-700 shadow-sm'
                        : 'bg-slate-50 border-slate-200 text-slate-900 hover:bg-slate-100'
                    }`}
                  >
                    <div>
                      <div className="font-bold text-base">{item.native}</div>
                      <div className="text-xs text-slate-500">{item.sub}</div>
                    </div>
                    {isSelected && (
                      <div className="w-6 h-6 rounded-full bg-blue-600 text-white flex items-center justify-center text-xs font-bold">
                        ✓
                      </div>
                    )}
                  </button>
                );
              })}
            </div>

            <button
              onClick={() => setShowLanguageModal(false)}
              className="mt-6 w-full py-3 rounded-xl bg-slate-100 text-slate-700 font-semibold text-sm hover:bg-slate-200 transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </header>
  );
};

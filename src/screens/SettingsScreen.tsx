import React, { useState } from 'react';
import {
  Settings as SettingsIcon,
  Globe,
  Volume2,
  Sliders,
  Eye,
  Type,
  Sun,
  Moon,
  Shield,
  RotateCcw,
  Radio,
  Bell,
  Users,
  Info,
} from 'lucide-react';
import { AppLanguage, AccessibilitySettings } from '../types';
import { speechService } from '../services/speechService';

interface SettingsScreenProps {
  language: AppLanguage;
  onLanguageChange: (lang: AppLanguage) => void;
  accessibilitySettings: AccessibilitySettings;
  onUpdateAccessibilitySettings: (settings: Partial<AccessibilitySettings>) => void;
  onNavigateToSafety: () => void;
  onNavigateToPrivacy: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  language,
  onLanguageChange,
  accessibilitySettings,
  onUpdateAccessibilitySettings,
  onNavigateToSafety,
  onNavigateToPrivacy,
}) => {
  const [pairingCode, setPairingCode] = useState<string | null>(null);

  const handleGeneratePairingCode = () => {
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    setPairingCode(code);
    speechService.playTone('success');
    speechService.speak(`Your 6 digit guardian pairing code is ${code.split('').join(' ')}. Valid for 15 minutes.`, { language });
  };

  const handleResetSettings = () => {
    if (confirm('Reset all voice, speech, and accessibility settings to default values?')) {
      onUpdateAccessibilitySettings({
        largeText: false,
        highContrast: false,
        voiceSpeed: 1.0,
        voiceVolume: 1.0,
        voiceWakeEnabled: false,
        spokenAlertsEnabled: true,
        soundEffectsEnabled: true,
      });
      speechService.playTone('success');
      speechService.speak('Preferences have been reset to defaults.', { language });
    }
  };

  return (
    <div className="w-full min-h-screen bg-slate-50/70 pb-28 pt-4 px-4 sm:px-6">
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/90 shadow-xs space-y-6">
          <div>
            <span className="text-xs font-black uppercase tracking-widest text-blue-700 bg-blue-50 px-3 py-1 rounded-full">
              Drishti Configuration
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 mt-2 tracking-tight">
              Settings &amp; Accessibility
            </h1>
            <p className="text-xs sm:text-sm text-slate-600">
              Personalize speech rate, contrast, language, guardian pairing, and voice services.
            </p>
          </div>

          {/* Section 1: Voice & Language */}
          <div className="space-y-4 pt-2">
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-500 flex items-center gap-2">
              <Globe className="w-4 h-4 text-blue-600" />
              <span>Voice &amp; Language</span>
            </h2>

            {/* Language Selection */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Application Language</h3>
                  <p className="text-xs text-slate-500">Interface, guidance, and spoken responses</p>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { code: 'en', label: 'English' },
                  { code: 'hi', label: 'हिन्दी' },
                  { code: 'mr', label: 'मराठी' },
                ].map((l) => (
                  <button
                    key={l.code}
                    onClick={() => {
                      onLanguageChange(l.code as AppLanguage);
                      speechService.playTone('success');
                    }}
                    className={`py-2.5 px-3 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                      language === l.code
                        ? 'bg-blue-600 text-white shadow-xs'
                        : 'bg-white border border-slate-200 text-slate-800 hover:bg-slate-100'
                    }`}
                  >
                    {l.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Voice Speed Slider */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Voice Speech Speed</h3>
                  <p className="text-xs text-slate-500">Adjust the pace of spoken assistance</p>
                </div>
                <span className="text-xs font-black text-blue-600 bg-blue-100 px-2 py-0.5 rounded-md">
                  {accessibilitySettings.voiceSpeed}x
                </span>
              </div>
              <input
                type="range"
                min="0.75"
                max="1.5"
                step="0.05"
                value={accessibilitySettings.voiceSpeed}
                onChange={(e) => onUpdateAccessibilitySettings({ voiceSpeed: parseFloat(e.target.value) })}
                aria-label="Adjust voice speech speed"
                className="w-full accent-blue-600 cursor-pointer"
              />
              <button
                onClick={() => speechService.speak('This is how Drishti will speak to you at this speed.', { language, rate: accessibilitySettings.voiceSpeed })}
                className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1 pt-1"
              >
                <Volume2 className="w-3.5 h-3.5" /> Test Voice Speed
              </button>
            </div>
          </div>

          {/* Section 2: Accessibility Preferences */}
          <div className="space-y-4 pt-2">
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-500 flex items-center gap-2">
              <Eye className="w-4 h-4 text-blue-600" />
              <span>Accessibility &amp; Contrast</span>
            </h2>

            {/* High Contrast Mode Toggle */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-900">High Contrast Palette</h3>
                <p className="text-xs text-slate-500">High-contrast black &amp; white mode with bright accents</p>
              </div>
              <button
                onClick={() => onUpdateAccessibilitySettings({ highContrast: !accessibilitySettings.highContrast })}
                className={`w-12 h-6 flex items-center rounded-full p-1 transition-colors cursor-pointer ${
                  accessibilitySettings.highContrast ? 'bg-blue-600 justify-end' : 'bg-slate-300 justify-start'
                }`}
              >
                <span className="bg-white w-4 h-4 rounded-full shadow-md" />
              </button>
            </div>

            {/* Large Text Size Toggle */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-900">Large Readable Text</h3>
                <p className="text-xs text-slate-500">Scale up typography across all cards and menus</p>
              </div>
              <button
                onClick={() => onUpdateAccessibilitySettings({ largeText: !accessibilitySettings.largeText })}
                className={`w-12 h-6 flex items-center rounded-full p-1 transition-colors cursor-pointer ${
                  accessibilitySettings.largeText ? 'bg-blue-600 justify-end' : 'bg-slate-300 justify-start'
                }`}
              >
                <span className="bg-white w-4 h-4 rounded-full shadow-md" />
              </button>
            </div>

            {/* Spoken Alerts on Reminders */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-900">Spoken Medical Alerts</h3>
                <p className="text-xs text-slate-500">Announce reminder titles aloud when scheduled time arrives</p>
              </div>
              <button
                onClick={() => onUpdateAccessibilitySettings({ spokenAlertsEnabled: !accessibilitySettings.spokenAlertsEnabled })}
                className={`w-12 h-6 flex items-center rounded-full p-1 transition-colors cursor-pointer ${
                  accessibilitySettings.spokenAlertsEnabled ? 'bg-blue-600 justify-end' : 'bg-slate-300 justify-start'
                }`}
              >
                <span className="bg-white w-4 h-4 rounded-full shadow-md" />
              </button>
            </div>
          </div>

          {/* Section 3: Guardian & Family Pairing */}
          <div className="space-y-4 pt-2">
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-500 flex items-center gap-2">
              <Users className="w-4 h-4 text-blue-600" />
              <span>Guardian &amp; Family Pairing</span>
            </h2>

            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
              <div>
                <h3 className="text-sm font-bold text-slate-900">Secure Parent / Guardian Link</h3>
                <p className="text-xs text-slate-500">
                  Allow your approved guardian to view SOS alerts and battery telemetry. Access requires manual approval.
                </p>
              </div>

              {pairingCode ? (
                <div className="p-4 rounded-2xl bg-blue-100/60 border border-blue-300 text-center space-y-1">
                  <span className="text-[10px] font-bold text-blue-700 uppercase tracking-widest">
                    Active 6-Digit Pairing Code
                  </span>
                  <div className="text-3xl font-black text-blue-900 tracking-widest">{pairingCode}</div>
                  <p className="text-[11px] text-slate-500">Expires in 15 minutes • Single-use only</p>
                </div>
              ) : (
                <button
                  onClick={handleGeneratePairingCode}
                  className="w-full py-3 px-4 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-xs transition-colors cursor-pointer"
                >
                  Generate 6-Digit Pairing Code
                </button>
              )}
            </div>
          </div>

          {/* Section 4: Safety & Policy Links */}
          <div className="space-y-2 pt-2 border-t border-slate-100">
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-500 mb-2">
              Safety &amp; Compliance Disclosures
            </h2>
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={onNavigateToSafety}
                className="p-3 rounded-xl bg-slate-50 hover:bg-slate-100 text-left text-xs font-bold text-slate-800 border border-slate-200"
              >
                Safety &amp; Assistive Notice →
              </button>
              <button
                onClick={onNavigateToPrivacy}
                className="p-3 rounded-xl bg-slate-50 hover:bg-slate-100 text-left text-xs font-bold text-slate-800 border border-slate-200"
              >
                Privacy &amp; Data Policy →
              </button>
            </div>
          </div>

          {/* Reset Defaults Button */}
          <div className="pt-2">
            <button
              onClick={handleResetSettings}
              className="w-full py-3 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs flex items-center justify-center gap-2 transition-colors cursor-pointer"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Reset All Preferences to Default</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

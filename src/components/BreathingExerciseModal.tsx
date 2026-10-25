import React, { useEffect, useState } from 'react';
import { X, Volume2, VolumeX, Play, Pause, RotateCcw } from 'lucide-react';
import { speechService } from '../services/speechService';
import { AppLanguage } from '../types';

interface BreathingExerciseModalProps {
  isOpen: boolean;
  onClose: () => void;
  language: AppLanguage;
}

export const BreathingExerciseModal: React.FC<BreathingExerciseModalProps> = ({
  isOpen,
  onClose,
  language,
}) => {
  if (!isOpen) return null;

  const [phase, setPhase] = useState<'inhale' | 'hold' | 'exhale' | 'ready'>('ready');
  const [countdown, setCountdown] = useState(4);
  const [cycle, setCycle] = useState(1);
  const [isActive, setIsActive] = useState(true);
  const [voiceGuidance, setVoiceGuidance] = useState(true);

  // 4-4-4 Box Breathing Cycle (4s Inhale, 4s Hold, 4s Exhale)
  useEffect(() => {
    if (!isActive) return;

    let currentPhase: 'inhale' | 'hold' | 'exhale' = 'inhale';
    let currentSeconds = 4;
    setPhase('inhale');
    setCountdown(4);

    const speakPhase = (p: 'inhale' | 'hold' | 'exhale') => {
      if (!voiceGuidance) return;
      speechService.stop();
      if (p === 'inhale') {
        speechService.speak(
          language === 'hi' ? 'गहरी साँस अंदर लें... चार... तीन... दो... एक' : language === 'mr' ? 'दीर्घ श्वास आत घ्या... चार... तीन... दोन... एक' : 'Breathe in slowly... four... three... two... one',
          { language, rate: 0.85 }
        );
      } else if (p === 'hold') {
        speechService.speak(
          language === 'hi' ? 'साँस रोकें...' : language === 'mr' ? 'श्वास रोखून धरा...' : 'Hold your breath gently...',
          { language, rate: 0.85 }
        );
      } else if (p === 'exhale') {
        speechService.speak(
          language === 'hi' ? 'धीरे-धीरे साँस बाहर छोड़ें...' : language === 'mr' ? 'हळूच श्वास बाहेर सोडा...' : 'Breathe out slowly and let all tension go...',
          { language, rate: 0.85 }
        );
      }
    };

    speakPhase('inhale');

    const interval = setInterval(() => {
      currentSeconds -= 1;
      if (currentSeconds <= 0) {
        currentSeconds = 4;
        if (currentPhase === 'inhale') {
          currentPhase = 'hold';
        } else if (currentPhase === 'hold') {
          currentPhase = 'exhale';
        } else {
          currentPhase = 'inhale';
          setCycle((c) => c + 1);
        }
        setPhase(currentPhase);
        speakPhase(currentPhase);
      }
      setCountdown(currentSeconds);
    }, 1000);

    return () => {
      clearInterval(interval);
      speechService.stop();
    };
  }, [isActive, voiceGuidance, language]);

  const getPhaseText = () => {
    switch (phase) {
      case 'inhale':
        return {
          en: 'Inhale Slowly',
          hi: 'गहरी साँस अंदर लें',
          mr: 'दीर्घ श्वास आत घ्या',
        }[language] || 'Inhale Slowly';
      case 'hold':
        return {
          en: 'Hold Gently',
          hi: 'साँस रोकें',
          mr: 'श्वास रोखून धरा',
        }[language] || 'Hold Gently';
      case 'exhale':
        return {
          en: 'Exhale Completely',
          hi: 'धीरे-धीरे साँस छोड़ें',
          mr: 'हळूच श्वास सोडा',
        }[language] || 'Exhale Completely';
      default:
        return 'Ready';
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="breathing-title"
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl p-6 sm:p-8 max-w-sm w-full shadow-2xl border border-slate-200 text-center flex flex-col items-center"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top Control Bar */}
        <div className="w-full flex items-center justify-between mb-2">
          <button
            onClick={() => setVoiceGuidance(!voiceGuidance)}
            aria-label={voiceGuidance ? 'Mute spoken guidance' : 'Enable spoken guidance'}
            className="p-2 rounded-full text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
          >
            {voiceGuidance ? <Volume2 className="w-5 h-5 text-blue-600" /> : <VolumeX className="w-5 h-5" />}
          </button>
          <span className="text-xs font-bold text-slate-500 uppercase tracking-widest">
            Cycle {cycle} of 4
          </span>
          <button
            onClick={() => {
              speechService.stop();
              onClose();
            }}
            aria-label="Close breathing exercise"
            className="p-2 rounded-full text-slate-500 hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <h2 id="breathing-title" className="text-xl font-extrabold text-slate-900 mt-2">
          Calming 4-4-4 Breath
        </h2>
        <p className="text-xs text-slate-500 mb-6">
          Follow the rhythmic pulse to relax your body and quiet your mind.
        </p>

        {/* Dynamic Expanding/Contracting Breathing Orb */}
        <div className="relative w-52 h-52 flex items-center justify-center my-4">
          {/* Animated concentric rings */}
          <div
            className={`absolute rounded-full transition-all duration-1000 ease-in-out ${
              phase === 'inhale'
                ? 'w-48 h-48 bg-blue-100/70 border-2 border-blue-400 scale-110'
                : phase === 'hold'
                ? 'w-48 h-48 bg-sky-100/70 border-2 border-sky-400 scale-105 animate-pulse'
                : 'w-24 h-24 bg-blue-50/50 border-2 border-blue-200 scale-90'
            }`}
          />

          {/* Central Counter Orb */}
          <div
            className={`relative z-10 rounded-full flex flex-col items-center justify-center text-white shadow-xl transition-all duration-1000 ${
              phase === 'inhale'
                ? 'w-36 h-36 bg-gradient-to-br from-blue-600 to-indigo-700 scale-105'
                : phase === 'hold'
                ? 'w-36 h-36 bg-gradient-to-br from-sky-500 to-blue-600'
                : 'w-28 h-28 bg-gradient-to-br from-blue-500 to-blue-800 scale-95'
            }`}
          >
            <span className="text-3xl font-black">{countdown}</span>
            <span className="text-[11px] font-bold uppercase tracking-wider text-blue-100">
              {phase}
            </span>
          </div>
        </div>

        {/* Phase Instructions */}
        <div className="mt-4 mb-6">
          <p className="text-lg font-bold text-slate-900" role="status" aria-live="polite">
            {getPhaseText()}
          </p>
          <p className="text-xs text-slate-500 mt-1">
            {phase === 'inhale'
              ? 'Draw deep air into your belly'
              : phase === 'hold'
              ? 'Stay still and calm'
              : 'Release gently through mouth'}
          </p>
        </div>

        {/* Controls */}
        <div className="flex items-center gap-3 w-full">
          <button
            onClick={() => setIsActive(!isActive)}
            className="flex-1 py-3 px-4 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm flex items-center justify-center gap-2 transition-all cursor-pointer"
          >
            {isActive ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
            <span>{isActive ? 'Pause' : 'Resume'}</span>
          </button>
          <button
            onClick={() => {
              setCycle(1);
              setPhase('inhale');
              setCountdown(4);
            }}
            aria-label="Restart exercise"
            className="p-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors cursor-pointer"
          >
            <RotateCcw className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};

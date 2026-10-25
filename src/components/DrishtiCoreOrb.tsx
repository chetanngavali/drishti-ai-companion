import React from 'react';
import { Mic, Volume2, Loader2, AlertTriangle, Sparkles, ShieldAlert } from 'lucide-react';
import { CoreState } from '../types';

interface DrishtiCoreOrbProps {
  state: CoreState;
  onClick: () => void;
  isListening?: boolean;
  size?: 'normal' | 'large' | 'compact';
  ariaLabel?: string;
}

export const DrishtiCoreOrb: React.FC<DrishtiCoreOrbProps> = ({
  state,
  onClick,
  isListening = false,
  size = 'normal',
  ariaLabel,
}) => {
  // Dimension scale
  const dimensions =
    size === 'large'
      ? { outer: 'w-64 h-64 sm:w-72 sm:h-72', core: 'w-40 h-40 sm:w-48 sm:h-48', icon: 'w-16 h-16' }
      : size === 'compact'
      ? { outer: 'w-44 h-44', core: 'w-28 h-28', icon: 'w-9 h-9' }
      : { outer: 'w-56 h-56 sm:w-64 sm:h-64', core: 'w-36 h-36 sm:w-40 sm:h-40', icon: 'w-12 h-12' };

  // Color theme according to DrishtiCore.kt state
  const getStateColors = () => {
    switch (state) {
      case 'LISTENING':
        return {
          glow: 'from-blue-400/30 via-sky-300/20 to-transparent',
          ring: 'border-blue-500',
          coreBg: 'from-blue-600 via-blue-700 to-indigo-900',
          particle: '#38BDF8',
          text: 'Listening...',
        };
      case 'PROCESSING':
        return {
          glow: 'from-purple-500/30 via-fuchsia-300/20 to-transparent',
          ring: 'border-purple-500 animate-spin',
          coreBg: 'from-purple-600 via-purple-700 to-indigo-950',
          particle: '#C084FC',
          text: 'Thinking...',
        };
      case 'SPEAKING':
        return {
          glow: 'from-sky-400/35 via-blue-300/20 to-transparent',
          ring: 'border-sky-400',
          coreBg: 'from-sky-500 via-blue-600 to-blue-900',
          particle: '#38BDF8',
          text: 'Speaking...',
        };
      case 'ATTENTION':
        return {
          glow: 'from-amber-400/35 via-yellow-300/20 to-transparent',
          ring: 'border-amber-500',
          coreBg: 'from-amber-500 via-yellow-600 to-amber-900',
          particle: '#F59E0B',
          text: 'Attention',
        };
      case 'EMERGENCY':
        return {
          glow: 'from-red-500/40 via-red-300/25 to-transparent',
          ring: 'border-red-600 animate-pulse',
          coreBg: 'from-red-600 via-red-700 to-rose-950',
          particle: '#DC2626',
          text: 'Emergency',
        };
      default: // IDLE
        return {
          glow: 'from-sky-300/25 via-blue-200/15 to-transparent',
          ring: 'border-blue-300/60',
          coreBg: 'from-blue-600 via-blue-700 to-indigo-900',
          particle: '#38BDF8',
          text: 'Tap to Talk',
        };
    }
  };

  const colors = getStateColors();

  return (
    <div className="relative flex flex-col items-center justify-center select-none py-2">
      {/* Outer Atmospheric Aura (Concentric animated halo) */}
      <div
        className={`absolute rounded-full bg-gradient-to-r ${colors.glow} ${dimensions.outer} ${
          state === 'LISTENING' || state === 'EMERGENCY'
            ? 'scale-110 animate-pulse duration-700'
            : 'animate-pulse-glow'
        }`}
        aria-hidden="true"
      />

      {/* Orbiting concentric soft rings */}
      <div
        className={`absolute rounded-full border border-dashed ${colors.ring} ${dimensions.outer} opacity-40 ${
          state === 'PROCESSING' ? 'animate-spin' : ''
        }`}
        style={{ animationDuration: state === 'PROCESSING' ? '3s' : '16s' }}
        aria-hidden="true"
      />

      {/* Interactive Core Orb Button */}
      <button
        onClick={onClick}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onClick();
          }
        }}
        role="button"
        tabIndex={0}
        aria-label={ariaLabel || `Drishti Voice Assistant. Status: ${colors.text}. Tap to speak or activate.`}
        className={`relative z-10 rounded-full shadow-2xl flex flex-col items-center justify-center transition-all cursor-pointer transform active:scale-95 focus-visible:ring-4 focus-visible:ring-blue-600 focus-visible:outline-none ${dimensions.core} bg-gradient-to-br ${colors.coreBg}`}
        style={{
          boxShadow:
            state === 'EMERGENCY'
              ? '0 0 50px rgba(220, 38, 38, 0.55), inset 0 2px 10px rgba(255,255,255,0.4)'
              : state === 'LISTENING'
              ? '0 0 45px rgba(37, 99, 235, 0.5), inset 0 2px 10px rgba(255,255,255,0.4)'
              : '0 12px 35px rgba(37, 99, 235, 0.35), inset 0 2px 8px rgba(255,255,255,0.3)',
        }}
      >
        {/* Optical inner glass highlight */}
        <div
          className="absolute top-2 w-3/4 h-1/3 rounded-full bg-white/20 blur-[1px] pointer-events-none"
          aria-hidden="true"
        />

        {/* Central Icon depending on state */}
        <div className="text-white flex items-center justify-center drop-shadow-md">
          {state === 'LISTENING' ? (
            <div className="flex items-center gap-1.5 h-10">
              <span className="w-1.5 h-7 bg-white rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
              <span className="w-1.5 h-10 bg-white rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
              <span className="w-1.5 h-5 bg-white rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
              <span className="w-1.5 h-9 bg-white rounded-full animate-bounce" style={{ animationDelay: '75ms' }} />
              <span className="w-1.5 h-6 bg-white rounded-full animate-bounce" style={{ animationDelay: '225ms' }} />
            </div>
          ) : state === 'PROCESSING' ? (
            <Loader2 className={`${dimensions.icon} animate-spin`} />
          ) : state === 'SPEAKING' ? (
            <Volume2 className={`${dimensions.icon} animate-pulse`} />
          ) : state === 'EMERGENCY' ? (
            <ShieldAlert className={`${dimensions.icon} animate-bounce`} />
          ) : state === 'ATTENTION' ? (
            <AlertTriangle className={`${dimensions.icon} animate-pulse`} />
          ) : (
            <Mic className={`${dimensions.icon} transition-transform group-hover:scale-110`} />
          )}
        </div>

        {/* Accessible Micro-Label below icon */}
        <span className="text-[11px] font-bold text-white/90 tracking-wider uppercase mt-1 drop-shadow-sm">
          {isListening ? 'Listening...' : colors.text}
        </span>
      </button>
    </div>
  );
};

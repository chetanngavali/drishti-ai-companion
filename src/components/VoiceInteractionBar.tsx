import React, { useEffect, useState } from 'react';
import { Sparkles, Activity } from 'lucide-react';
import { CoreState } from '../types';

interface VoiceInteractionBarProps {
  coreState: CoreState;
  activeTranscript: string;
  onSuggestionClick: (suggestion: string) => void;
  language?: string;
}

const DEFAULT_SUGGESTIONS = [
  'Hey Drishti',
  'Tell me government scheme',
  'Read my schedule',
  'Be my companion',
  'Read out notifications',
  'Remind me to take my medicine at 8 PM',
  'Tell me about Maharashtra disability schemes',
  'Tell me something positive',
];

export const VoiceInteractionBar: React.FC<VoiceInteractionBarProps> = ({
  coreState,
  activeTranscript,
  onSuggestionClick,
}) => {
  const [suggestionIndex, setSuggestionIndex] = useState(0);

  useEffect(() => {
    if (coreState !== 'IDLE') return;

    const timer = setInterval(() => {
      setSuggestionIndex((prev) => (prev + 1) % DEFAULT_SUGGESTIONS.length);
    }, 4500);

    return () => clearInterval(timer);
  }, [coreState]);

  const currentSuggestion = DEFAULT_SUGGESTIONS[suggestionIndex];

  return (
    <div className="w-full max-w-xl mx-auto px-4 py-2">
      <div
        className="bg-white/95 backdrop-blur-md rounded-2xl p-3 border border-slate-200/90 shadow-sm flex items-center gap-3 transition-all hover:border-blue-300"
      >
        <div
          className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 transition-colors ${
            coreState !== 'IDLE' ? 'bg-blue-600 text-white' : 'bg-blue-50 text-blue-600'
          }`}
        >
          {coreState !== 'IDLE' ? (
            <Activity className="w-4 h-4 animate-pulse" />
          ) : (
            <Sparkles className="w-4 h-4" />
          )}
        </div>

        <div className="flex-1 min-w-0">
          {coreState !== 'IDLE' ? (
            <div className="flex items-center gap-2">
              <span
                className={`text-xs font-bold uppercase tracking-wider shrink-0 ${
                  coreState === 'ERROR'
                    ? 'text-red-600'
                    : coreState === 'LISTENING'
                    ? 'text-blue-600'
                    : coreState === 'PROCESSING'
                    ? 'text-purple-600'
                    : 'text-sky-600'
                }`}
              >
                {coreState === 'LISTENING'
                  ? 'Hearing:'
                  : coreState === 'PROCESSING'
                  ? 'Thinking:'
                  : coreState === 'ERROR'
                  ? 'Notice:'
                  : 'Speaking:'}
              </span>
              <p
                className={`text-sm font-semibold truncate ${
                  coreState === 'ERROR' ? 'text-red-700' : 'text-slate-900'
                }`}
              >
                {activeTranscript || (coreState === 'ERROR' ? 'Microphone issue. Please tap again.' : 'Listening to your voice...')}
              </p>
            </div>
          ) : (
            <button
              onClick={() => onSuggestionClick(currentSuggestion)}
              className="text-left w-full group cursor-pointer focus-visible:outline-none"
              aria-label={`Suggestion: Try saying, ${currentSuggestion}. Tap to execute.`}
            >
              <span className="text-xs text-slate-600 font-medium mr-1.5">Try saying:</span>
              <span className="text-xs sm:text-sm font-semibold text-slate-900 group-hover:text-blue-600 transition-colors">
                "{currentSuggestion}"
              </span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

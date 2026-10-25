import React from 'react';
import {
  FileText,
  HeartHandshake,
  Calendar,
  Eye,
  Navigation,
  Sparkles,
  MapPin,
  AlertTriangle,
  ArrowRight,
} from 'lucide-react';
import { AppRoute, AppLanguage, CoreState } from '../types';
import { DrishtiCoreOrb } from '../components/DrishtiCoreOrb';
import { VoiceInteractionBar } from '../components/VoiceInteractionBar';
import { speechService } from '../services/speechService';

interface HomeScreenProps {
  onNavigate: (route: AppRoute) => void;
  language: AppLanguage;
  coreState: CoreState;
  onCoreClick: () => void;
  activeTranscript: string;
  unreadNotificationsCount: number;
  onVoiceQuery?: (query: string) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  onNavigate,
  language,
  coreState,
  onCoreClick,
  activeTranscript,
  unreadNotificationsCount,
  onVoiceQuery,
}) => {
  const getGreeting = () => {
    switch (language) {
      case 'hi':
        return {
          hello: 'नमस्ते,',
          askMe: 'मुझसे पूछें ',
          anything: 'जो भी आपको चाहिए',
        };
      case 'mr':
        return {
          hello: 'नमस्कार,',
          askMe: 'मला विचारा ',
          anything: 'तुम्हाला जे हवे आहे',
        };
      default:
        return {
          hello: 'Hello,',
          askMe: 'Ask me ',
          anything: 'anything you need',
        };
    }
  };

  const greeting = getGreeting();

  const handleSuggestionClick = (suggestion: string) => {
    speechService.playTone('listen_start');
    if (onVoiceQuery) {
      onVoiceQuery(suggestion);
    } else {
      const lower = suggestion.toLowerCase();
      if (lower.includes('scheme') || lower.includes('scholarship') || lower.includes('योजना')) {
        onNavigate('schemes');
      } else if (lower.includes('schedule') || lower.includes('remind') || lower.includes('appointment')) {
        onNavigate('schedule');
      } else if (lower.includes('lonely') || lower.includes('relax') || lower.includes('breathing')) {
        onNavigate('companion');
      } else {
        onCoreClick();
      }
    }
  };

  return (
    <div className="relative w-full min-h-screen overflow-hidden bg-[#FAFCFF] pb-28 pt-4">
      {/* Background Flowing Wave Ribbons & Ambient Glow Canvas (matching HomeScreen.kt Canvas) */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden" aria-hidden="true">
        {/* Soft Radial Ambient Glow */}
        <div
          className={`absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[580px] h-[580px] rounded-full blur-3xl transition-all duration-1000 ${
            coreState === 'LISTENING'
              ? 'bg-blue-400/25 scale-110'
              : coreState === 'PROCESSING'
              ? 'bg-purple-400/25 scale-105'
              : coreState === 'SPEAKING'
              ? 'bg-sky-400/25'
              : coreState === 'EMERGENCY'
              ? 'bg-red-400/35 animate-pulse'
              : 'bg-sky-200/35'
          }`}
        />

        {/* Top undulating light ribbon */}
        <div className="absolute -top-16 -left-20 w-[120%] h-64 bg-gradient-to-r from-transparent via-sky-100/40 to-blue-50/20 rounded-full blur-2xl transform rotate-3" />
        {/* Bottom undulating light ribbon */}
        <div className="absolute bottom-16 -right-20 w-[120%] h-72 bg-gradient-to-l from-transparent via-blue-100/30 to-sky-50/20 rounded-full blur-2xl transform -rotate-3" />
      </div>

      <div className="relative z-10 max-w-4xl mx-auto px-4 sm:px-6 flex flex-col items-center">
        {/* 1. Center-Aligned Greeting Typography */}
        <div className="text-center mt-3 sm:mt-6 mb-4 space-y-1">
          <p className="text-2xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
            {greeting.hello}
          </p>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-slate-900">
            <span className="text-blue-600">{greeting.askMe}</span>
            {greeting.anything}
          </h1>
        </div>

        {/* 2. Centralized Drishti Voice Core Orb */}
        <div className="my-3 sm:my-5 flex flex-col items-center">
          <DrishtiCoreOrb
            state={coreState}
            isListening={coreState === 'LISTENING'}
            onClick={onCoreClick}
            size="large"
            ariaLabel="Drishti Core voice assistant button. Double tap or click to speak."
          />
        </div>

        {/* 3. Bottom Glassmorphism Suggestion Ticker */}
        <div className="w-full mb-6">
          <VoiceInteractionBar
            coreState={coreState}
            activeTranscript={activeTranscript}
            onSuggestionClick={handleSuggestionClick}
          />
        </div>

        {/* 4. Primary Accessible Feature Cards Grid */}
        <div className="w-full space-y-3">
          <div className="flex items-center justify-between px-1">
            <h2 className="text-xs font-black uppercase tracking-widest text-slate-500">
              Assistance &amp; Life Companion Services
            </h2>
            <span className="text-xs text-blue-600 font-bold">Quick Touch Targets</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
            {/* Feature 1 Card: Government Schemes */}
            <button
              onClick={() => onNavigate('schemes')}
              className="group p-5 bg-white rounded-3xl border border-blue-200 shadow-xs hover:border-blue-400 hover:shadow-md transition-all text-left flex flex-col justify-between cursor-pointer focus-visible:ring-4 focus-visible:ring-blue-600"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <div className="w-12 h-12 rounded-2xl bg-blue-600 text-white flex items-center justify-center shadow-xs group-hover:scale-105 transition-transform">
                    <FileText className="w-6 h-6" />
                  </div>
                  {unreadNotificationsCount > 0 ? (
                    <span className="text-[11px] font-black bg-blue-100 text-blue-700 px-2.5 py-0.5 rounded-full">
                      {unreadNotificationsCount} New Alerts
                    </span>
                  ) : (
                    <span className="text-[10px] font-bold bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md">
                      Verified .gov
                    </span>
                  )}
                </div>

                <h3 className="text-lg font-bold text-slate-900 group-hover:text-blue-600 transition-colors">
                  Government Schemes
                </h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  Financial pensions, ADIP smart canes, DAISY audio players, and Maharashtra state scholarships.
                </p>
              </div>

              <div className="flex items-center gap-1.5 text-xs font-bold text-blue-600 mt-4 pt-3 border-t border-slate-100">
                <span>Browse &amp; Listen Schemes</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
              </div>
            </button>

            {/* Feature 2 Card: Voice Companion */}
            <button
              onClick={() => onNavigate('companion')}
              className="group p-5 bg-white rounded-3xl border border-sky-200 shadow-xs hover:border-sky-400 hover:shadow-md transition-all text-left flex flex-col justify-between cursor-pointer focus-visible:ring-4 focus-visible:ring-sky-600"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-sky-500 to-blue-600 text-white flex items-center justify-center shadow-xs group-hover:scale-105 transition-transform">
                    <HeartHandshake className="w-6 h-6" />
                  </div>
                  <span className="text-[10px] font-bold bg-sky-50 text-sky-700 px-2 py-0.5 rounded-md">
                    Trilingual AI
                  </span>
                </div>

                <h3 className="text-lg font-bold text-slate-900 group-hover:text-sky-600 transition-colors">
                  Voice Companion
                </h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  Empathetic friend for emotional support, daily reflection, and guided 4-4-4 relaxation breathing.
                </p>
              </div>

              <div className="flex items-center gap-1.5 text-xs font-bold text-sky-600 mt-4 pt-3 border-t border-slate-100">
                <span>Start Conversation</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
              </div>
            </button>

            {/* Feature 3 Card: Medical Schedule */}
            <button
              onClick={() => onNavigate('schedule')}
              className="group p-5 bg-white rounded-3xl border border-indigo-200 shadow-xs hover:border-indigo-400 hover:shadow-md transition-all text-left flex flex-col justify-between cursor-pointer focus-visible:ring-4 focus-visible:ring-indigo-600"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-indigo-600 to-purple-600 text-white flex items-center justify-center shadow-xs group-hover:scale-105 transition-transform">
                    <Calendar className="w-6 h-6" />
                  </div>
                  <span className="text-[10px] font-bold bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-md">
                    Voice Scheduling
                  </span>
                </div>

                <h3 className="text-lg font-bold text-slate-900 group-hover:text-indigo-600 transition-colors">
                  Medical Schedule
                </h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  Say "Hey Drishti Schedule" to manage medications, doctor visits, and check-ups with spoken alerts.
                </p>
              </div>

              <div className="flex items-center gap-1.5 text-xs font-bold text-indigo-600 mt-4 pt-3 border-t border-slate-100">
                <span>Open Calendar Agenda</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
              </div>
            </button>
          </div>

          {/* Secondary Vision & Mobility Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
            <button
              onClick={() => onNavigate('scan')}
              className="p-4 bg-white rounded-2xl border border-slate-200 hover:border-blue-300 text-left transition-all cursor-pointer flex flex-col items-start gap-2"
            >
              <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <Eye className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-slate-900">Scan Obstacles</h4>
                <p className="text-[11px] text-slate-500">Live camera HUD</p>
              </div>
            </button>

            <button
              onClick={() => onNavigate('navigate')}
              className="p-4 bg-white rounded-2xl border border-slate-200 hover:border-blue-300 text-left transition-all cursor-pointer flex flex-col items-start gap-2"
            >
              <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <Navigation className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-slate-900">Outdoor GPS</h4>
                <p className="text-[11px] text-slate-500">Walking turns</p>
              </div>
            </button>

            <button
              onClick={() => onNavigate('describe')}
              className="p-4 bg-white rounded-2xl border border-slate-200 hover:border-blue-300 text-left transition-all cursor-pointer flex flex-col items-start gap-2"
            >
              <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <Sparkles className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-slate-900">Describe Scene</h4>
                <p className="text-[11px] text-slate-500">Visual perception</p>
              </div>
            </button>

            <button
              onClick={() => onNavigate('nearby')}
              className="p-4 bg-white rounded-2xl border border-slate-200 hover:border-blue-300 text-left transition-all cursor-pointer flex flex-col items-start gap-2"
            >
              <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <MapPin className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-slate-900">Nearby Transit</h4>
                <p className="text-[11px] text-slate-500">Bus &amp; pharmacy</p>
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

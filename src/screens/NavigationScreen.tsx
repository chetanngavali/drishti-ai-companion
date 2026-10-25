import React, { useState } from 'react';
import { Navigation as NavIcon, Compass, MapPin, Volume2, ArrowUp, ArrowRight, CornerUpRight, RotateCcw } from 'lucide-react';
import { speechService } from '../services/speechService';
import { AppLanguage } from '../types';

interface NavigationScreenProps {
  language: AppLanguage;
}

export const NavigationScreen: React.FC<NavigationScreenProps> = ({ language }) => {
  const [destination, setDestination] = useState('Central Metro Station');
  const [currentStepIndex, setCurrentStepIndex] = useState(0);

  const steps = [
    { instruction: 'Walk straight ahead for 45 meters towards North exit', clock: "12 o'clock", dist: '45m' },
    { instruction: 'Turn right at the pedestrian crossing ramp', clock: "3 o'clock", dist: '15m' },
    { instruction: 'Continue on tactile footpath past General Hospital', clock: "12 o'clock", dist: '120m' },
    { instruction: 'Destination Central Metro Station Entrance is on your left', clock: "9 o'clock", dist: 'Arrival' },
  ];

  const currentStep = steps[currentStepIndex];

  const speakStep = (step: (typeof steps)[0]) => {
    speechService.playTone('listen_start');
    speechService.speak(`Navigation: ${step.instruction}. Turn towards ${step.clock}. Distance ${step.dist}.`, { language });
  };

  return (
    <div className="w-full min-h-screen bg-slate-50/70 pb-28 pt-4 px-4 sm:px-6">
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/90 shadow-xs space-y-4">
          <div className="flex items-center gap-2">
            <span className="text-xs font-black uppercase tracking-widest text-blue-700 bg-blue-50 px-3 py-1 rounded-full">
              Pedestrian Turn-by-Turn GPS
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Outdoor Navigation
          </h1>
          <p className="text-xs sm:text-sm text-slate-600">
            Clock-face directions, tactile paving orientation, and audio guidance for safe walking.
          </p>

          {/* Active Navigation Card */}
          <div className="p-6 rounded-3xl bg-blue-600 text-white shadow-xl shadow-blue-600/20 space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-blue-200 flex items-center gap-1.5">
                <Compass className="w-4 h-4 animate-spin" style={{ animationDuration: '8s' }} />
                Step {currentStepIndex + 1} of {steps.length}
              </span>
              <span className="text-lg font-black bg-white/20 px-3 py-0.5 rounded-full">
                {currentStep.dist}
              </span>
            </div>

            <div className="flex items-start gap-4">
              <div className="w-14 h-14 rounded-2xl bg-white/20 flex items-center justify-center shrink-0">
                <CornerUpRight className="w-7 h-7 text-white" />
              </div>
              <div className="space-y-1">
                <h2 className="text-lg sm:text-xl font-bold leading-snug">
                  {currentStep.instruction}
                </h2>
                <p className="text-xs text-blue-100 font-semibold">
                  Orientation: Head toward <strong>{currentStep.clock}</strong>
                </p>
              </div>
            </div>

            <div className="pt-2 flex items-center gap-2">
              <button
                onClick={() => speakStep(currentStep)}
                className="flex-1 py-3 px-4 rounded-xl bg-white text-blue-600 font-bold text-xs flex items-center justify-center gap-2 shadow-sm hover:bg-blue-50 cursor-pointer"
              >
                <Volume2 className="w-4 h-4" />
                <span>Repeat Voice Step</span>
              </button>

              <button
                onClick={() => {
                  const next = (currentStepIndex + 1) % steps.length;
                  setCurrentStepIndex(next);
                  speakStep(steps[next]);
                }}
                className="py-3 px-4 rounded-xl bg-blue-700 hover:bg-blue-800 text-white font-bold text-xs flex items-center gap-1 cursor-pointer"
              >
                <span>Next Step</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* All Steps Overview */}
          <div className="space-y-2 pt-2">
            <h3 className="text-sm font-bold text-slate-800">Route Overview to {destination}</h3>
            <div className="space-y-2">
              {steps.map((st, i) => (
                <div
                  key={i}
                  onClick={() => {
                    setCurrentStepIndex(i);
                    speakStep(st);
                  }}
                  className={`p-3.5 rounded-2xl border text-xs flex items-center justify-between cursor-pointer transition-colors ${
                    i === currentStepIndex
                      ? 'bg-blue-50 border-blue-300 text-blue-900 font-bold'
                      : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span className="w-6 h-6 rounded-full bg-white border border-slate-200 flex items-center justify-center font-black text-slate-700">
                      {i + 1}
                    </span>
                    <span>{st.instruction}</span>
                  </div>
                  <span className="text-[11px] text-slate-500 font-semibold shrink-0">{st.dist}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

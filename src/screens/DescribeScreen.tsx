import React, { useState } from 'react';
import { Sparkles, Camera, Volume2, Upload, Loader2, RefreshCw } from 'lucide-react';
import { speechService } from '../services/speechService';
import { AppLanguage } from '../types';

interface DescribeScreenProps {
  language: AppLanguage;
}

export const DescribeScreen: React.FC<DescribeScreenProps> = ({ language }) => {
  const [isDescribing, setIsDescribing] = useState(false);
  const [description, setDescription] = useState<string>(
    'In front of you is a bright, sunlit study room. A sturdy wooden desk sits approximately 2 meters ahead with a notebook and reading lamp. To your right at 2 o\'clock is an armchair with a soft cushion.'
  );

  const handleDescribeScene = () => {
    setIsDescribing(true);
    speechService.playTone('processing');

    setTimeout(() => {
      setIsDescribing(false);
      const sampleDescriptions = [
        'You are at a pedestrian transit platform. To your direct front at 12 o\'clock is an automated fare gate. Tactile yellow paving stones guide your way forward for 8 steps.',
        'You are in a quiet reception area. A reception desk is located 3 meters ahead at 11 o\'clock. Two comfortable waiting chairs are located to your right at 3 o\'clock.',
        'Ahead is an open park walkway. The stone path is clear with mature shade trees on both sides and a gentle breeze.',
      ];
      const desc = sampleDescriptions[Math.floor(Math.random() * sampleDescriptions.length)];
      setDescription(desc);
      speechService.playTone('success');
      speechService.speak(desc, { language });
    }, 1500);
  };

  return (
    <div className="w-full min-h-screen bg-slate-50/70 pb-28 pt-4 px-4 sm:px-6">
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/90 shadow-xs space-y-4">
          <div className="flex items-center gap-2">
            <span className="text-xs font-black uppercase tracking-widest text-blue-700 bg-blue-50 px-3 py-1 rounded-full">
              Environmental Scene Describer
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Describe Scene
          </h1>
          <p className="text-xs sm:text-sm text-slate-600">
            Captures your environment and narrates objects, spatial layout, text, and people in front of you.
          </p>

          <div className="w-full aspect-video rounded-3xl bg-slate-900 overflow-hidden relative flex flex-col items-center justify-center p-6 text-center shadow-lg">
            <img
              src="/assets/drishti/splash_campus_bg.jpg"
              alt="Scene preview"
              className="absolute inset-0 w-full h-full object-cover opacity-40 blur-[1px]"
            />
            <div className="relative z-10 space-y-3">
              <div className="w-16 h-16 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center mx-auto text-white">
                {isDescribing ? <Loader2 className="w-8 h-8 animate-spin" /> : <Camera className="w-8 h-8" />}
              </div>
              <button
                onClick={handleDescribeScene}
                disabled={isDescribing}
                className="px-6 py-3 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-extrabold text-sm shadow-md flex items-center gap-2 mx-auto cursor-pointer"
              >
                {isDescribing ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                <span>{isDescribing ? 'Analyzing Scene...' : 'Take Photo & Describe'}</span>
              </button>
            </div>
          </div>

          {/* Spoken Narration Box */}
          <div className="p-5 rounded-2xl bg-blue-50/70 border border-blue-200 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-blue-800">
                Spoken Description
              </span>
              <button
                onClick={() => speechService.speak(description, { language })}
                className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1"
              >
                <Volume2 className="w-3.5 h-3.5" /> Hear Again
              </button>
            </div>
            <p className="text-sm font-medium text-slate-900 leading-relaxed">{description}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

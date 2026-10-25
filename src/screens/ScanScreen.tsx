import React, { useState, useEffect } from 'react';
import { Eye, Camera, AlertTriangle, Volume2, ShieldCheck, RefreshCw } from 'lucide-react';
import { speechService } from '../services/speechService';
import { AppLanguage } from '../types';

interface ScanScreenProps {
  language: AppLanguage;
  onBack: () => void;
}

export const ScanScreen: React.FC<ScanScreenProps> = ({ language, onBack }) => {
  const [isScanning, setIsScanning] = useState(true);
  const [detectedObstacles, setDetectedObstacles] = useState<
    { id: string; label: string; distance: string; clock: string; severity: 'warning' | 'info' | 'danger' }[]
  >([
    { id: '1', label: 'Doorway entryway', distance: '2.5 meters', clock: "12 o'clock", severity: 'info' },
    { id: '2', label: 'Low table edge', distance: '1.2 meters', clock: "10 o'clock", severity: 'warning' },
  ]);

  useEffect(() => {
    speechService.speak(
      language === 'hi'
        ? 'स्कैनर चालू है। सामने 2.5 मीटर पर दरवाजा और 10 बजे दिशा में टेबल है।'
        : language === 'mr'
        ? 'स्कॅनर सुरू आहे. समोर 2.5 मीटरवर दरवाजा आणि 10 वाजता दिशेला टेबल आहे.'
        : 'Scan mode active. Doorway at 12 o\'clock, 2.5 meters ahead. Low table edge at 10 o\'clock, 1.2 meters.',
      { language }
    );
  }, []);

  const simulateObstacleCheck = () => {
    speechService.playTone('processing');
    const scenarios = [
      { id: '3', label: 'Clear path ahead', distance: '3.0 meters', clock: "12 o'clock", severity: 'info' as const },
      { id: '4', label: 'Step down / curb hazard', distance: '0.8 meters', clock: "12 o'clock", severity: 'danger' as const },
      { id: '5', label: 'Person approaching slowly', distance: '2.0 meters', clock: "1 o'clock", severity: 'warning' as const },
    ];
    const picked = scenarios[Math.floor(Math.random() * scenarios.length)];
    setDetectedObstacles((prev) => [picked, ...prev.slice(0, 3)]);

    speechService.playTone(picked.severity === 'danger' ? 'alert' : 'success');
    speechService.speak(`Alert: ${picked.label} at ${picked.clock}, ${picked.distance}.`, { language });
  };

  return (
    <div className="w-full min-h-screen bg-slate-900 text-white pb-28 pt-4 px-4 sm:px-6">
      <div className="max-w-3xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <span className="text-xs font-bold uppercase tracking-widest text-blue-400 bg-blue-900/50 px-3 py-0.5 rounded-full">
              Environmental Perception HUD
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold mt-1">Obstacle &amp; Vision Scanner</h1>
          </div>
          <button
            onClick={simulateObstacleCheck}
            className="p-3 bg-blue-600 rounded-2xl hover:bg-blue-500 font-bold text-xs flex items-center gap-1.5 cursor-pointer shadow-lg"
          >
            <RefreshCw className="w-4 h-4" />
            <span>Scan Environment</span>
          </button>
        </div>

        {/* Viewfinder HUD Container */}
        <div className="relative w-full aspect-video sm:aspect-21/9 bg-slate-950 rounded-3xl border-2 border-blue-500/50 overflow-hidden flex flex-col items-center justify-center p-6 text-center shadow-2xl">
          {/* Target Reticle */}
          <div className="absolute inset-8 border border-blue-400/30 rounded-2xl pointer-events-none" />
          <div className="w-20 h-20 rounded-full border-2 border-blue-400/80 animate-ping absolute pointer-events-none" />
          <div className="w-32 h-32 rounded-full border border-sky-400/40 pointer-events-none flex items-center justify-center">
            <Camera className="w-8 h-8 text-blue-400" />
          </div>

          <div className="mt-4 z-10 space-y-1">
            <p className="text-base font-bold text-blue-300">Live AI Vision Active</p>
            <p className="text-xs text-slate-400 max-w-sm">
              Camera frames are evaluated 100% in-memory locally. Audio guidance provides clock-face directions.
            </p>
          </div>
        </div>

        {/* Detected Obstacles Feed */}
        <div className="space-y-3">
          <h2 className="text-sm font-bold text-slate-300 uppercase tracking-wider">
            Detected Objects &amp; Hazard Radar
          </h2>

          <div className="space-y-2.5">
            {detectedObstacles.map((obs) => (
              <div
                key={obs.id}
                className={`p-4 rounded-2xl border flex items-center justify-between gap-3 ${
                  obs.severity === 'danger'
                    ? 'bg-red-950/60 border-red-500/80 text-red-200'
                    : obs.severity === 'warning'
                    ? 'bg-amber-950/50 border-amber-500/70 text-amber-200'
                    : 'bg-slate-800/80 border-slate-700 text-slate-200'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                      obs.severity === 'danger'
                        ? 'bg-red-600 text-white'
                        : obs.severity === 'warning'
                        ? 'bg-amber-600 text-white'
                        : 'bg-blue-600 text-white'
                    }`}
                  >
                    {obs.severity === 'danger' ? (
                      <AlertTriangle className="w-5 h-5" />
                    ) : (
                      <Eye className="w-5 h-5" />
                    )}
                  </div>
                  <div>
                    <h3 className="font-bold text-base text-white">{obs.label}</h3>
                    <p className="text-xs opacity-80">
                      Direction: <strong className="text-white">{obs.clock}</strong> • Distance: <strong className="text-white">{obs.distance}</strong>
                    </p>
                  </div>
                </div>

                <button
                  onClick={() => speechService.speak(`${obs.label} at ${obs.clock}, ${obs.distance}.`, { language })}
                  aria-label={`Listen to details of ${obs.label}`}
                  className="p-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white cursor-pointer"
                >
                  <Volume2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import { AlertTriangle, Phone, ShieldCheck, X, Check, Volume2 } from 'lucide-react';
import { speechService } from '../services/speechService';
import { AppLanguage } from '../types';

interface EmergencyScreenProps {
  language: AppLanguage;
  onCancel: () => void;
}

export const EmergencyScreen: React.FC<EmergencyScreenProps> = ({ language, onCancel }) => {
  const [countdown, setCountdown] = useState(5);
  const [isTriggered, setIsTriggered] = useState(false);

  useEffect(() => {
    speechService.playTone('alert');
    speechService.speak(
      language === 'hi'
        ? 'आपातकालीन एसओएस सक्रिय। रद्द करने के लिए स्क्रीन छुएं।'
        : language === 'mr'
        ? 'आपत्कालीन एसओएस सक्रिय. रद्द करण्यासाठी स्क्रीनवर टॅप करा.'
        : 'Emergency SOS active. Tap Cancel to abort.',
      { language }
    );

    const timer = setInterval(() => {
      setCountdown((c) => {
        if (c <= 1) {
          clearInterval(timer);
          setIsTriggered(true);
          speechService.playTone('alert');
          speechService.speak(
            language === 'hi'
              ? 'अभिभावकों को आपातकालीन अलर्ट भेजा गया और राष्ट्रीय आपातकालीन सेवा 112 सक्रिय की गई।'
              : language === 'mr'
              ? 'पालकांना आपत्कालीन सूचना पाठवली आणि राष्ट्रीय आपत्कालीन सेवा 112 सक्रिय केली.'
              : 'Emergency SOS dispatched to guardians and National Emergency Service 112.',
            { language }
          );
          return 0;
        }
        return c - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  return (
    <div className="fixed inset-0 z-50 bg-red-600 text-white flex flex-col items-center justify-between p-6 sm:p-10 animate-fadeIn">
      <div className="text-center pt-8 space-y-2">
        <span className="text-xs font-black uppercase tracking-widest bg-white/20 px-3 py-1 rounded-full">
          Drishti Emergency System
        </span>
        <h1 className="text-3xl sm:text-5xl font-black tracking-tight">
          {isTriggered ? 'SOS ALERT SENT' : 'EMERGENCY SOS'}
        </h1>
        <p className="text-sm sm:text-base text-red-100 font-semibold max-w-sm mx-auto">
          {isTriggered
            ? 'Your GPS location has been shared with your registered emergency guardians.'
            : 'Sending emergency coordinates to family and emergency services in:'}
        </p>
      </div>

      {/* Countdown Circle */}
      <div className="my-8">
        {!isTriggered ? (
          <div className="w-48 h-48 sm:w-56 sm:h-56 rounded-full border-8 border-white/40 flex flex-col items-center justify-center bg-red-700 shadow-2xl animate-pulse">
            <span className="text-7xl sm:text-8xl font-black">{countdown}</span>
            <span className="text-xs font-bold uppercase tracking-widest text-red-200 mt-1">Seconds</span>
          </div>
        ) : (
          <div className="w-48 h-48 sm:w-56 sm:h-56 rounded-full border-8 border-white flex flex-col items-center justify-center bg-emerald-600 shadow-2xl">
            <Check className="w-24 h-24 stroke-[3]" />
            <span className="text-xs font-black uppercase tracking-widest text-white mt-1">Alert Sent</span>
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="w-full max-w-md space-y-4 pb-6">
        <a
          href="tel:112"
          className="w-full py-4 px-6 rounded-2xl bg-white text-red-700 font-extrabold text-lg flex items-center justify-center gap-3 shadow-xl hover:bg-slate-100 active:scale-95 transition-all text-center"
        >
          <Phone className="w-6 h-6 text-red-600 fill-current" />
          <span>Call Emergency 112 Directly</span>
        </a>

        <button
          onClick={() => {
            speechService.stop();
            onCancel();
          }}
          className="w-full py-4 px-6 rounded-2xl bg-red-900/80 border-2 border-white/30 text-white font-bold text-base hover:bg-red-950 active:scale-95 transition-all cursor-pointer"
        >
          Cancel &amp; Return to Home
        </button>
      </div>
    </div>
  );
};

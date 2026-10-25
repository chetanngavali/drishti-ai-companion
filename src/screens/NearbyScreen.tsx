import React, { useState } from 'react';
import { MapPin, Bus, Pill, Building, Coffee, Volume2, Navigation } from 'lucide-react';
import { speechService } from '../services/speechService';
import { AppLanguage } from '../types';

interface NearbyScreenProps {
  language: AppLanguage;
  onNavigateToWalk: () => void;
}

export const NearbyScreen: React.FC<NearbyScreenProps> = ({ language, onNavigateToWalk }) => {
  const [selectedFilter, setSelectedFilter] = useState<'all' | 'transit' | 'pharmacy' | 'clinic'>('all');

  const places = [
    { id: '1', name: 'Central Bus Stand (MSRTC Depot)', category: 'transit', dist: '120m', clock: "11 o'clock", address: 'Station Road, Platform 2' },
    { id: '2', name: 'Apollo 24x7 Pharmacy', category: 'pharmacy', dist: '85m', clock: "2 o'clock", address: 'Shop 4, Ground Floor' },
    { id: '3', name: 'Sanjivani Eye Care Clinic', category: 'clinic', dist: '240m', clock: "1 o'clock", address: 'Medical Enclave, 1st Floor' },
    { id: '4', name: 'Metro Line 1 Underground Gate B', category: 'transit', dist: '350m', clock: "12 o'clock", address: 'Tactile Path Exit 3' },
  ];

  const filtered = places.filter((p) => selectedFilter === 'all' || p.category === selectedFilter);

  const speakPlace = (p: (typeof places)[0]) => {
    speechService.playTone('listen_start');
    speechService.speak(`${p.name}. Located ${p.dist} away at ${p.clock}. Address: ${p.address}.`, { language });
  };

  return (
    <div className="w-full min-h-screen bg-slate-50/70 pb-28 pt-4 px-4 sm:px-6">
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/90 shadow-xs space-y-4">
          <div className="flex items-center gap-2">
            <span className="text-xs font-black uppercase tracking-widest text-blue-700 bg-blue-50 px-3 py-1 rounded-full">
              Nearby Transit &amp; Medical Facilities
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Nearby Places
          </h1>
          <p className="text-xs sm:text-sm text-slate-600">
            Discover accessible bus stops, pharmacies, hospitals, and transit hubs with clock directions.
          </p>

          <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
            {[
              { id: 'all', label: 'All Places' },
              { id: 'transit', label: 'Bus & Metro' },
              { id: 'pharmacy', label: 'Pharmacies' },
              { id: 'clinic', label: 'Hospitals' },
            ].map((f) => (
              <button
                key={f.id}
                onClick={() => setSelectedFilter(f.id as any)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
                  selectedFilter === f.id
                    ? 'bg-blue-600 text-white'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>

          <div className="space-y-3 pt-2">
            {filtered.map((p) => (
              <div
                key={p.id}
                className="p-4 rounded-2xl bg-white border border-slate-200 hover:border-blue-300 shadow-xs flex items-center justify-between gap-3 transition-colors"
              >
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                    {p.category === 'transit' ? (
                      <Bus className="w-5 h-5" />
                    ) : p.category === 'pharmacy' ? (
                      <Pill className="w-5 h-5" />
                    ) : (
                      <Building className="w-5 h-5" />
                    )}
                  </div>
                  <div>
                    <h3 className="font-bold text-sm text-slate-900">{p.name}</h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {p.dist} away • Direction: <strong className="text-slate-800">{p.clock}</strong>
                    </p>
                    <p className="text-[11px] text-slate-400 mt-0.5">{p.address}</p>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  <button
                    onClick={() => speakPlace(p)}
                    aria-label={`Listen details for ${p.name}`}
                    className="p-2.5 rounded-xl text-blue-600 hover:bg-blue-50 cursor-pointer"
                  >
                    <Volume2 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={onNavigateToWalk}
                    aria-label={`Start walking guidance to ${p.name}`}
                    className="px-3 py-2 rounded-xl bg-blue-600 text-white font-bold text-xs flex items-center gap-1 hover:bg-blue-700 cursor-pointer"
                  >
                    <Navigation className="w-3.5 h-3.5" />
                    <span>Navigate</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

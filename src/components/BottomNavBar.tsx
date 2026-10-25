import React from 'react';
import { Home, FileText, HeartHandshake, Calendar, Settings } from 'lucide-react';
import { AppRoute, AppLanguage } from '../types';

interface BottomNavBarProps {
  currentRoute: AppRoute;
  onNavigate: (route: AppRoute) => void;
  language: AppLanguage;
  unreadNotificationsCount: number;
}

export const BottomNavBar: React.FC<BottomNavBarProps> = ({
  currentRoute,
  onNavigate,
  language,
  unreadNotificationsCount,
}) => {
  const tabs = [
    {
      route: 'home' as AppRoute,
      label: { en: 'Home', hi: 'होम', mr: 'होम' },
      icon: <Home className="w-5 h-5" />,
    },
    {
      route: 'schemes' as AppRoute,
      label: { en: 'Schemes', hi: 'योजनाएं', mr: 'योजना' },
      icon: <FileText className="w-5 h-5" />,
      badge: unreadNotificationsCount,
    },
    {
      route: 'companion' as AppRoute,
      label: { en: 'Companion', hi: 'साथी', mr: 'साथी' },
      icon: <HeartHandshake className="w-5 h-5" />,
    },
    {
      route: 'schedule' as AppRoute,
      label: { en: 'Schedule', hi: 'शेड्यूल', mr: 'वेळापत्रक' },
      icon: <Calendar className="w-5 h-5" />,
    },
    {
      route: 'settings' as AppRoute,
      label: { en: 'Settings', hi: 'सेटिंग्स', mr: 'सेटिंग्ज' },
      icon: <Settings className="w-5 h-5" />,
    },
  ];

  return (
    <nav
      aria-label="Bottom Quick Navigation"
      className="fixed bottom-0 left-0 right-0 z-30 bg-white/95 backdrop-blur-md border-t border-slate-200/90 shadow-lg px-2 sm:px-4 py-2 transition-colors"
    >
      <div className="max-w-md mx-auto grid grid-cols-5 gap-1">
        {tabs.map((tab) => {
          const isSelected = currentRoute === tab.route;
          const label = tab.label[language] || tab.label.en;

          return (
            <button
              key={tab.route}
              onClick={() => onNavigate(tab.route)}
              aria-label={label}
              className={`relative flex flex-col items-center justify-center py-1.5 px-1 rounded-2xl transition-all cursor-pointer ${
                isSelected
                  ? 'text-blue-600 font-bold scale-105'
                  : 'text-slate-600 hover:text-slate-900 font-medium'
              }`}
            >
              <div
                className={`w-10 h-8 flex items-center justify-center rounded-xl transition-colors ${
                  isSelected ? 'bg-blue-50 text-blue-600' : 'text-slate-600'
                }`}
              >
                {tab.icon}
                {tab.badge !== undefined && tab.badge > 0 && (
                  <span className="absolute top-1 right-2 min-w-[18px] h-4 px-1 bg-blue-600 text-white rounded-full text-[10px] font-black flex items-center justify-center">
                    {tab.badge}
                  </span>
                )}
              </div>
              <span className="text-[11px] truncate tracking-tight mt-0.5">{label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};

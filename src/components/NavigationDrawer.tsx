import React from 'react';
import {
  X,
  Home,
  FileText,
  HeartHandshake,
  Calendar,
  Eye,
  Navigation,
  Sparkles,
  MapPin,
  Settings,
  AlertTriangle,
  Shield,
  Lock,
} from 'lucide-react';
import { AppRoute, AppLanguage } from '../types';

interface NavigationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  currentRoute: AppRoute;
  onNavigate: (route: AppRoute) => void;
  language: AppLanguage;
  unreadNotificationsCount: number;
}

export const NavigationDrawer: React.FC<NavigationDrawerProps> = ({
  isOpen,
  onClose,
  currentRoute,
  onNavigate,
  language,
  unreadNotificationsCount,
}) => {
  if (!isOpen) return null;

  const navItems: {
    route: AppRoute;
    title: { en: string; hi: string; mr: string };
    subtitle: { en: string; hi: string; mr: string };
    icon: React.ReactNode;
    badge?: number;
    highlight?: boolean;
  }[] = [
    {
      route: 'home',
      title: { en: 'Home Dashboard', hi: 'मुख्य पृष्ठ', mr: 'मुख्य डॅशबोर्ड' },
      subtitle: { en: 'Primary voice core & overview', hi: 'आवाज नियंत्रण केंद्र', mr: 'व्हॉइस नियंत्रण केंद्र' },
      icon: <Home className="w-5 h-5" />,
    },
    {
      route: 'schemes',
      title: { en: 'Government Schemes', hi: 'सरकारी योजनाएं', mr: 'शासकीय योजना' },
      subtitle: { en: 'Verified welfare, scholarships & aids', hi: 'छात्रवृत्ति व उपकरण सहायता', mr: 'शिष्यवृत्ती, उपकरणे व निवृत्तीवेतन' },
      icon: <FileText className="w-5 h-5" />,
      badge: unreadNotificationsCount,
      highlight: true,
    },
    {
      route: 'companion',
      title: { en: 'Drishti Voice Companion', hi: 'दृष्टि वॉइस साथी', mr: 'दृष्टी व्हॉइस साथी' },
      subtitle: { en: 'Empathetic emotional support & calm', hi: 'मानसिक कल्याण व विश्राम', mr: 'भावनिक आधार व श्वासोच्छ्वास' },
      icon: <HeartHandshake className="w-5 h-5" />,
      highlight: true,
    },
    {
      route: 'schedule',
      title: { en: 'Medical Schedule & Reminders', hi: 'दवा व डॉक्टर समयसारणी', mr: 'औषध व डॉक्टर वेळापत्रक' },
      subtitle: { en: '"Hey Drishti Schedule" voice manager', hi: 'आवाज आधारित स्मरणपत्र', mr: 'व्हॉइस आधारित स्मरणपत्र' },
      icon: <Calendar className="w-5 h-5" />,
      highlight: true,
    },
    {
      route: 'scan',
      title: { en: 'Scan & Obstacle Vision', hi: 'स्कैन व बाधा पहचान', mr: 'स्कॅन व अडथळा शोध' },
      subtitle: { en: 'Real-time hazard detection HUD', hi: 'पर्यावरण बाधा विश्लेषण', mr: 'वातावरणातील अडथळे' },
      icon: <Eye className="w-5 h-5" />,
    },
    {
      route: 'navigate',
      title: { en: 'Outdoor GPS Navigation', hi: 'आउटडोर नेविगेशन', mr: 'रस्ता दिशादर्शन' },
      subtitle: { en: 'Pedestrian turn-by-turn guidance', hi: 'मार्गदर्शन व सुरक्षित रस्ता', mr: 'वळणावळणाची माहिती' },
      icon: <Navigation className="w-5 h-5" />,
    },
    {
      route: 'describe',
      title: { en: 'Describe Scene', hi: 'दृश्य विवरण', mr: 'परिसराचे वर्णन' },
      subtitle: { en: 'High-detail scene perception', hi: 'पर्यावरण का सजीव वर्णन', mr: 'परिसराचे सविस्तर वर्णन' },
      icon: <Sparkles className="w-5 h-5" />,
    },
    {
      route: 'nearby',
      title: { en: 'Nearby Places & Transit', hi: 'आसपास के स्थान', mr: 'जवळपासची ठिकाणे' },
      subtitle: { en: 'Bus stops, hospitals & pharmacies', hi: 'बस स्टॉप व दवाखाने', mr: 'बस थांबे व रुग्णालये' },
      icon: <MapPin className="w-5 h-5" />,
    },
    {
      route: 'settings',
      title: { en: 'Accessibility & Settings', hi: 'सेटिंग्स व सुगमता', mr: 'सेटिंग्ज व सुलभता' },
      subtitle: { en: 'Speech speed, contrast, voice wake', hi: 'आवाज व सुगमता विकल्प', mr: 'आवाज व सुलभता पर्याय' },
      icon: <Settings className="w-5 h-5" />,
    },
  ];

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Navigation Menu"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="w-full max-w-xs sm:max-w-sm bg-white h-full shadow-2xl flex flex-col justify-between overflow-y-auto border-r border-slate-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Drawer Header with Drishti branding */}
        <div className="p-6 border-b border-slate-100 bg-slate-50/70">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-white border border-slate-200 shadow-xs flex items-center justify-center p-2">
                <img
                  src="/assets/drishti/drishti_logo_eye.png"
                  alt="Drishti"
                  className="w-full h-full object-contain"
                />
              </div>
              <div>
                <h1 className="text-xl font-extrabold tracking-tight text-slate-900">
                  Drishti<span className="text-blue-600">AI</span>
                </h1>
                <p className="text-xs text-slate-500 font-medium">Assistive Companion 2.0</p>
              </div>
            </div>

            <button
              onClick={onClose}
              aria-label="Close Navigation Drawer"
              className="w-10 h-10 rounded-full flex items-center justify-center text-slate-500 hover:bg-slate-200/70 active:scale-95 transition-all cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Navigation Items List */}
        <div className="p-4 space-y-1.5 flex-1 overflow-y-auto">
          {navItems.map((item) => {
            const isSelected = currentRoute === item.route;
            const title = item.title[language] || item.title.en;
            const subtitle = item.subtitle[language] || item.subtitle.en;

            return (
              <button
                key={item.route}
                onClick={() => {
                  onNavigate(item.route);
                  onClose();
                }}
                className={`w-full p-3.5 rounded-2xl flex items-center gap-3.5 text-left transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                    : item.highlight
                    ? 'bg-blue-50/70 text-slate-900 hover:bg-blue-100/70 border border-blue-200/60'
                    : 'text-slate-800 hover:bg-slate-100/80'
                }`}
              >
                <div
                  className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                    isSelected
                      ? 'bg-white/20 text-white'
                      : item.highlight
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-100 text-slate-700'
                  }`}
                >
                  {item.icon}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-1">
                    <span className="font-bold text-sm truncate">{title}</span>
                    {item.badge !== undefined && item.badge > 0 && (
                      <span
                        className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full ${
                          isSelected ? 'bg-white text-blue-600' : 'bg-blue-600 text-white'
                        }`}
                      >
                        {item.badge}
                      </span>
                    )}
                  </div>
                  <p className={`text-xs truncate ${isSelected ? 'text-blue-100' : 'text-slate-500'}`}>
                    {subtitle}
                  </p>
                </div>
              </button>
            );
          })}
        </div>

        {/* Drawer Footer Actions (SOS & Safety Notices) */}
        <div className="p-4 border-t border-slate-100 bg-slate-50/50 space-y-2">
          <button
            onClick={() => {
              onNavigate('emergency');
              onClose();
            }}
            className="w-full py-3.5 px-4 rounded-xl bg-red-50 hover:bg-red-100 text-red-700 font-bold text-sm border border-red-200 flex items-center justify-center gap-2 transition-all cursor-pointer"
          >
            <AlertTriangle className="w-4 h-4 text-red-600" />
            <span>Emergency SOS Trigger</span>
          </button>

          <div className="flex items-center justify-between text-[11px] text-slate-500 pt-2 px-1">
            <button
              onClick={() => {
                onNavigate('safety');
                onClose();
              }}
              className="hover:underline flex items-center gap-1"
            >
              <Shield className="w-3 h-3" /> Safety Notice
            </button>
            <button
              onClick={() => {
                onNavigate('privacy');
                onClose();
              }}
              className="hover:underline flex items-center gap-1"
            >
              <Lock className="w-3 h-3" /> Privacy Policy
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

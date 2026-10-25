import React, { useState } from 'react';
import {
  Volume2,
  VolumeX,
  ExternalLink,
  Search,
  Mic,
  CheckCircle2,
  Calendar,
  FileCheck,
  Building2,
  Award,
  Bell,
  Check,
  Info,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import {
  VERIFIED_GOVERNMENT_SCHEMES,
  getSavedNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
} from '../services/schemesData';
import { GovernmentScheme, SchemeCategory, SchemeNotification, AppLanguage } from '../types';
import { speechService } from '../services/speechService';

interface GovernmentSchemesScreenProps {
  language: AppLanguage;
  onNavigateToCompanion: () => void;
  autoReadNotifications?: boolean;
  onFinishedReadingNotifications?: () => void;
  autoReadSchemes?: boolean;
  onFinishedReadingSchemes?: () => void;
}

export const GovernmentSchemesScreen: React.FC<GovernmentSchemesScreenProps> = ({
  language,
  autoReadNotifications = false,
  onFinishedReadingNotifications,
  autoReadSchemes = false,
  onFinishedReadingSchemes,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<SchemeCategory>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [notifications, setNotifications] = useState<SchemeNotification[]>(() => getSavedNotifications());
  const [selectedScheme, setSelectedScheme] = useState<GovernmentScheme | null>(null);
  const [expandedSchemeId, setExpandedSchemeId] = useState<string | null>(null);
  const [currentlySpeakingId, setCurrentlySpeakingId] = useState<string | null>(null);
  const [isVoiceSearching, setIsVoiceSearching] = useState(false);
  const [voiceQueryAnswer, setVoiceQueryAnswer] = useState<string | null>(null);
  const [isReadingAllNotifications, setIsReadingAllNotifications] = useState(false);
  const [isReadingAllSchemes, setIsReadingAllSchemes] = useState(false);

  const autoReadProcessedRef = React.useRef(false);
  const autoReadSchemesProcessedRef = React.useRef(false);

  // Category filter tabs
  const categories: { id: SchemeCategory; label: { en: string; hi: string; mr: string } }[] = [
    { id: 'all', label: { en: 'All Schemes', hi: 'सभी योजनाएं', mr: 'सर्व योजना' } },
    { id: 'central', label: { en: 'Central Govt', hi: 'केंद्र सरकार', mr: 'केंद्र शासन' } },
    { id: 'maharashtra', label: { en: 'Maharashtra State', hi: 'महाराष्ट्र राज्य', mr: 'महाराष्ट्र शासन' } },
    { id: 'scholarship', label: { en: 'Scholarships', hi: 'छात्रवृत्तियां', mr: 'शिष्यवृत्ती' } },
    { id: 'assistive_devices', label: { en: 'Assistive Devices', hi: 'सहायक उपकरण', mr: 'सहाय्यक उपकरणे' } },
    { id: 'pension', label: { en: 'Pensions', hi: 'पेंशन योजनाएं', mr: 'निवृत्तीवेतन' } },
  ];

  // Filtered schemes
  const filteredSchemes = VERIFIED_GOVERNMENT_SCHEMES.filter((scheme) => {
    const matchesCategory = selectedCategory === 'all' || scheme.category === selectedCategory;
    const query = searchQuery.toLowerCase().trim();
    if (!query) return matchesCategory;

    const matchesName =
      scheme.name.toLowerCase().includes(query) ||
      scheme.nativeNames.hi.toLowerCase().includes(query) ||
      scheme.nativeNames.mr.toLowerCase().includes(query);
    const matchesDesc =
      scheme.shortDescription.en.toLowerCase().includes(query) ||
      scheme.shortDescription.hi.toLowerCase().includes(query) ||
      scheme.shortDescription.mr.toLowerCase().includes(query);
    const matchesAuthority = scheme.authority.toLowerCase().includes(query);

    return matchesCategory && (matchesName || matchesDesc || matchesAuthority);
  });

  // Read scheme aloud using speech synthesis
  const handleReadScheme = (scheme: GovernmentScheme) => {
    if (currentlySpeakingId === scheme.id) {
      speechService.stop();
      setCurrentlySpeakingId(null);
      return;
    }

    setCurrentlySpeakingId(scheme.id);
    speechService.playTone('listen_start');

    const desc = scheme.shortDescription[language] || scheme.shortDescription.en;
    const benefits = scheme.benefits.slice(0, 2).join('. ');
    const speechText = `${scheme.name}. ${desc}. Benefits include: ${benefits}. Applications can be submitted at ${scheme.authority}.`;

    speechService.speak(speechText, {
      language,
      rate: 0.95,
      onEnd: () => setCurrentlySpeakingId(null),
      onError: () => setCurrentlySpeakingId(null),
    });
  };

  // Read notification aloud
  const handleReadNotification = (notif: SchemeNotification) => {
    speechService.playTone('listen_start');
    const text = `Notification: ${notif.title}. ${notif.message}`;
    speechService.speak(text, { language });
    handleMarkNotificationRead(notif.id);
  };

  const handleMarkNotificationRead = (id: string) => {
    const updated = markNotificationAsRead(id);
    setNotifications([...updated]);
  };

  const handleMarkAllRead = () => {
    const updated = markAllNotificationsAsRead();
    setNotifications([...updated]);
    speechService.playTone('success');
  };

  // Read ALL government scheme notifications aloud in sequence
  const handleReadAllNotificationsAloud = () => {
    if (isReadingAllNotifications) {
      speechService.stop();
      setIsReadingAllNotifications(false);
      return;
    }

    const currentNotifs = getSavedNotifications();
    if (!currentNotifs || currentNotifs.length === 0) {
      const emptyText =
        language === 'hi'
          ? 'वर्तमान में कोई नई सरकारी योजना सूचनाएं नहीं हैं।'
          : language === 'mr'
          ? 'सध्या कोणतीही नवीन शासकीय योजना सूचना नाही.'
          : 'There are currently no government scheme notifications.';
      speechService.speak(emptyText, { language });
      return;
    }

    // Mark all as read
    const updated = markAllNotificationsAsRead();
    setNotifications([...updated]);

    setIsReadingAllNotifications(true);
    speechService.playTone('listen_start');

    // Build comprehensive spoken announcement
    let fullScript = '';
    if (language === 'hi') {
      fullScript = `सरकारी योजनाओं की सभी सूचनाएं पढ़ रहे हैं। कुल ${currentNotifs.length} सूचनाएं हैं। `;
      currentNotifs.forEach((notif, index) => {
        fullScript += `सूचना संख्या ${index + 1}: ${notif.title}। संबंधित योजना: ${notif.schemeName}। ${notif.message}। `;
      });
      fullScript += `सभी सरकारी योजना सूचनाएं समाप्त हुईं।`;
    } else if (language === 'mr') {
      fullScript = `शासकीय योजनांच्या सर्व सूचना वाचत आहे. एकूण ${currentNotifs.length} सूचना उपलब्ध आहेत. `;
      currentNotifs.forEach((notif, index) => {
        fullScript += `सूचना क्रमांक ${index + 1}: ${notif.title}. योजना: ${notif.schemeName}. ${notif.message}. `;
      });
      fullScript += `सर्व शासकीय योजना सूचना वाचून पूर्ण झाल्या आहेत.`;
    } else {
      fullScript = `Reading all verified government scheme notifications. You have ${currentNotifs.length} alerts. `;
      currentNotifs.forEach((notif, index) => {
        fullScript += `Notification ${index + 1}: ${notif.title}. Scheme: ${notif.schemeName}. ${notif.message}. `;
      });
      fullScript += `That concludes all government scheme notifications.`;
    }

    speechService.speak(fullScript, {
      language,
      rate: 0.95,
      onStart: () => setIsReadingAllNotifications(true),
      onEnd: () => {
        setIsReadingAllNotifications(false);
        onFinishedReadingNotifications?.();
      },
      onError: () => {
        setIsReadingAllNotifications(false);
        onFinishedReadingNotifications?.();
      },
    });
  };

  // Read ALL verified government schemes aloud
  const handleReadAllSchemesAloud = () => {
    if (isReadingAllSchemes) {
      speechService.stop();
      setIsReadingAllSchemes(false);
      return;
    }

    setIsReadingAllSchemes(true);
    speechService.playTone('listen_start');

    let script = '';
    if (language === 'hi') {
      script = `दृष्टिहीन नागरिकों के लिए प्रमुख सत्यापित सरकारी योजनाएं पढ़ रहे हैं। `;
      script += `पहला: एडीआईपी योजना (ADIP Scheme), केंद्र सरकार द्वारा निःशुल्क स्मार्ट केन, डेज़ी ऑडियो प्लेयर, और लैपटॉप सहायता। `;
      script += `दूसरा: इंदिरा गांधी राष्ट्रीय दिव्यांग पेंशन योजना, प्रत्यक्ष मासिक वित्तीय सहायता। `;
      script += `तीसरा: दिव्यांगजन स्वावलंबन योजना, उच्च शिक्षा और स्वरोजगार हेतु रियायती ऋण। `;
      script += `चौथा: महाराष्ट्र दिव्यांग स्वाधार व शिष्यवृत्ती योजना, दृष्टिहीन छात्रों के लिए मासिक निर्वाह भत्ता और छात्रवृत्ति। `;
      script += `पाँचवाँ: यूडीआईडी कार्ड, पूरे भारत में बस व रेल यात्रा छूट हेतु एकल पहचान पत्र। `;
      script += `आप किसी भी योजना कार्ड पर टैप करके पात्रता और आवश्यक दस्तावेज सुन सकते हैं।`;
    } else if (language === 'mr') {
      script = `दृष्टिहीन व्यक्तींसाठी प्रमुख शासकीय कल्याणकारी योजना वाचत आहे. `;
      script += `क्रमांक १: एडीआयपी योजना (ADIP Scheme), केंद्र शासनातर्फे मोफत स्मार्ट केन, डेझी ऑडिओ प्लेयर आणि लॅपटॉप. `;
      script += `क्रमांक २: इंदिरा गांधी राष्ट्रीय दिव्यांग निवृत्तीवेतन योजना, थेट मासिक आर्थिक पेन्शन. `;
      script += `क्रमांक ३: दिव्यांगजन स्वावलंबन योजना, उच्च शिक्षण व स्वयंरोजगारासाठी सवलतीचे कर्ज. `;
      script += `क्रमांक ४: महाराष्ट्र दिव्यांग स्वाधार व शिष्यवृत्ती योजना, दृष्टिहीन विद्यार्थ्यांसाठी मासिक भत्ता व शिष्यवृत्ती. `;
      script += `क्रमांक ५: युडीआयडी कार्ड, देशभरात बस व रेल्वे प्रवास सवलतीसाठी ओळखपत्र. `;
      script += `सविस्तर माहितीसाठी कोणत्याही योजनेवर स्पर्श करा.`;
    } else {
      script = `Reading verified government welfare schemes for visually impaired citizens. `;
      script += `First: ADIP Scheme by the Central Ministry of Social Justice. Provides free assistive devices including Smart Canes, Daisy audio players, refreshable Braille displays, and screen-reading laptops. `;
      script += `Second: Indira Gandhi National Disability Pension Scheme. Provides monthly direct financial pension assistance. `;
      script += `Third: Divyangjan Swavalamban Yojana. Concessional financial loans up to 50 lakh rupees for higher education, vocational training, and self-employment. `;
      script += `Fourth: Maharashtra Divyang Swadhar and Scholarship Scheme. Monthly hostel stipend and educational aid for blind students. `;
      script += `Fifth: Unique Disability ID (UDID) Card. Single verified smart card offering free bus and railway travel concessions across India. `;
      script += `You can tap any scheme card to hear full eligibility and required document checklists.`;
    }

    speechService.speak(script, {
      language,
      rate: 0.95,
      onStart: () => setIsReadingAllSchemes(true),
      onEnd: () => {
        setIsReadingAllSchemes(false);
        onFinishedReadingSchemes?.();
      },
      onError: () => {
        setIsReadingAllSchemes(false);
        onFinishedReadingSchemes?.();
      },
    });
  };

  // Automatically read all schemes when directed by voice
  React.useEffect(() => {
    if (autoReadSchemes) {
      if (!autoReadSchemesProcessedRef.current) {
        autoReadSchemesProcessedRef.current = true;
        handleReadAllSchemesAloud();
      }
    } else {
      autoReadSchemesProcessedRef.current = false;
    }
  }, [autoReadSchemes]);

  // Automatically read all notifications when directed by voice
  React.useEffect(() => {
    if (autoReadNotifications) {
      if (!autoReadProcessedRef.current) {
        autoReadProcessedRef.current = true;
        handleReadAllNotificationsAloud();
      }
    } else {
      autoReadProcessedRef.current = false;
    }
  }, [autoReadNotifications]);

  // Voice Search / Query for Schemes
  const handleVoiceSearch = () => {
    if (isVoiceSearching) {
      speechService.stopListening();
      setIsVoiceSearching(false);
      return;
    }

    setIsVoiceSearching(true);
    speechService.startListening({
      language,
      onResult: async (transcript, isFinal) => {
        if (isFinal) {
          setIsVoiceSearching(false);
          setSearchQuery(transcript);
          await querySchemesAi(transcript);
        }
      },
      onError: (err) => {
        setIsVoiceSearching(false);
        speechService.speak(err, { language });
      },
      onEnd: () => {
        setIsVoiceSearching(false);
      },
    });
  };

  const querySchemesAi = async (query: string) => {
    try {
      speechService.playTone('processing');
      const res = await fetch('/api/schemes/query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query, language }),
      });
      const data = await res.json();
      if (data.answer) {
        setVoiceQueryAnswer(data.answer);
        speechService.speak(data.answer, { language });
      }
    } catch (e) {
      console.error('Schemes query failed:', e);
    }
  };

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return (
    <div className="w-full min-h-screen bg-slate-50/70 pb-28 pt-4 px-4 sm:px-6">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Screen Header */}
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/90 shadow-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className="text-[11px] font-extrabold uppercase tracking-widest text-blue-700 bg-blue-50 border border-blue-200/60 px-3 py-1 rounded-full">
                  Verified Government Portals
                </span>
                <span className="text-[11px] text-slate-500 font-medium">
                  Updated October 2026
                </span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
                Government Schemes
              </h1>
              <p className="text-sm text-slate-600 mt-1 max-w-xl">
                Official financial assistance, assistive device programs, scholarships, and pensions for visually impaired citizens in India &amp; Maharashtra.
              </p>
            </div>

            {/* Quick Listen All Button */}
            <button
              onClick={handleReadAllSchemesAloud}
              aria-label={isReadingAllSchemes ? "Stop reading government schemes" : "Read overview of verified government schemes aloud"}
              className={`self-start sm:self-center px-4 py-2.5 rounded-2xl border font-bold text-xs flex items-center gap-2 transition-all cursor-pointer focus-visible:ring-4 focus-visible:ring-blue-600 ${
                isReadingAllSchemes
                  ? 'bg-red-50 text-red-600 border-red-200 animate-pulse'
                  : 'bg-blue-50 border-blue-200 text-blue-700 hover:bg-blue-100'
              }`}
            >
              {isReadingAllSchemes ? (
                <>
                  <VolumeX className="w-4 h-4 text-red-600" />
                  <span>Stop Reading Schemes</span>
                </>
              ) : (
                <>
                  <Volume2 className="w-4 h-4 text-blue-600" />
                  <span>Read Schemes Aloud</span>
                </>
              )}
            </button>
          </div>

          {/* Search & Voice Query Bar */}
          <div className="mt-6 flex items-center gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute left-4 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search scheme name, Smart Cane, scholarship, pension..."
                aria-label="Search government schemes"
                className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-sm font-medium text-slate-900 placeholder:text-slate-400 focus:bg-white focus:border-blue-600 focus-visible:outline-none transition-all"
              />
            </div>

            <button
              onClick={handleVoiceSearch}
              aria-label={isVoiceSearching ? 'Listening... Tap to stop' : 'Ask question about government schemes by voice'}
              className={`h-12 px-4 rounded-2xl border flex items-center gap-2 font-bold text-xs sm:text-sm transition-all cursor-pointer ${
                isVoiceSearching
                  ? 'bg-blue-600 text-white border-blue-600 animate-pulse'
                  : 'bg-white border-slate-200 text-slate-800 hover:bg-slate-50'
              }`}
            >
              <Mic className={`w-4 h-4 ${isVoiceSearching ? 'text-white' : 'text-blue-600'}`} />
              <span className="hidden sm:inline">{isVoiceSearching ? 'Listening...' : 'Ask by Voice'}</span>
            </button>
          </div>

          {/* AI Spoken Answer Banner if available */}
          {voiceQueryAnswer && (
            <div className="mt-4 p-4 rounded-2xl bg-blue-50 border border-blue-200 flex items-start justify-between gap-3 animate-fadeIn">
              <div className="space-y-1">
                <span className="text-[11px] font-bold text-blue-700 uppercase tracking-wider">
                  Drishti Voice Answer
                </span>
                <p className="text-sm text-slate-900 font-medium leading-relaxed">
                  {voiceQueryAnswer}
                </p>
              </div>
              <button
                onClick={() => setVoiceQueryAnswer(null)}
                aria-label="Dismiss answer"
                className="text-xs text-blue-600 font-bold hover:underline shrink-0"
              >
                Clear
              </button>
            </div>
          )}
        </div>

        {/* Scheme Notifications Section */}
        {notifications.length > 0 && (
          <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/90 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-blue-100 flex items-center justify-center text-blue-600">
                  <Bell className="w-4 h-4" />
                </div>
                <div>
                  <h2 className="text-base font-bold text-slate-900">
                    Scheme Notifications &amp; Deadlines
                  </h2>
                  <p className="text-xs text-slate-500 font-medium">
                    {unreadCount > 0 ? `${unreadCount} unread alert${unreadCount > 1 ? 's' : ''}` : 'All notifications caught up'}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={handleReadAllNotificationsAloud}
                  aria-label={
                    isReadingAllNotifications
                      ? 'Stop reading notifications aloud'
                      : 'Read all government scheme notifications aloud'
                  }
                  className={`px-3.5 py-1.5 rounded-xl border text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
                    isReadingAllNotifications
                      ? 'bg-red-50 text-red-600 border-red-200 animate-pulse'
                      : 'bg-blue-50 text-blue-700 border-blue-200 hover:bg-blue-100'
                  }`}
                >
                  {isReadingAllNotifications ? (
                    <>
                      <VolumeX className="w-3.5 h-3.5 text-red-600" />
                      <span>Stop Reading</span>
                    </>
                  ) : (
                    <>
                      <Volume2 className="w-3.5 h-3.5 text-blue-600" />
                      <span>Read All Aloud</span>
                    </>
                  )}
                </button>

                {unreadCount > 0 && (
                  <button
                    onClick={handleMarkAllRead}
                    className="text-xs font-bold text-slate-500 hover:text-blue-700 hover:underline flex items-center gap-1 cursor-pointer px-2 py-1.5"
                  >
                    <Check className="w-3.5 h-3.5" />
                    <span>Mark Read</span>
                  </button>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {notifications.map((notif) => (
                <div
                  key={notif.id}
                  className={`p-4 rounded-2xl border transition-all text-left space-y-2 ${
                    notif.isRead
                      ? 'bg-slate-50/80 border-slate-200/80 opacity-80'
                      : 'bg-blue-50/40 border-blue-200 shadow-xs'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <span
                      className={`text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-md ${
                        notif.type === 'opening'
                          ? 'bg-emerald-100 text-emerald-800'
                          : notif.type === 'deadline'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-blue-100 text-blue-800'
                      }`}
                    >
                      {notif.type}
                    </span>
                    <span className="text-[11px] text-slate-400 font-medium">{notif.date}</span>
                  </div>

                  <h3 className="text-sm font-bold text-slate-900 leading-snug">
                    {notif.title}
                  </h3>
                  <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                    {notif.message}
                  </p>

                  <div className="flex items-center gap-2 pt-1">
                    <button
                      onClick={() => handleReadNotification(notif)}
                      className="px-3 py-1.5 rounded-xl bg-white border border-slate-200 text-blue-600 text-xs font-bold flex items-center gap-1 hover:bg-blue-50 active:scale-95 transition-all cursor-pointer"
                    >
                      <Volume2 className="w-3.5 h-3.5" />
                      <span>Listen</span>
                    </button>

                    <a
                      href={notif.officialUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-3 py-1.5 rounded-xl bg-white border border-slate-200 text-slate-700 text-xs font-semibold flex items-center gap-1 hover:bg-slate-50 transition-all"
                    >
                      <span>Website</span>
                      <ExternalLink className="w-3 h-3 text-slate-400" />
                    </a>

                    {!notif.isRead && (
                      <button
                        onClick={() => handleMarkNotificationRead(notif.id)}
                        className="ml-auto text-[11px] font-medium text-slate-500 hover:text-slate-800"
                      >
                        Dismiss
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Category Filters */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
          {categories.map((cat) => {
            const isSelected = selectedCategory === cat.id;
            const label = cat.label[language] || cat.label.en;
            return (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.id)}
                className={`px-4 py-2 rounded-2xl text-xs sm:text-sm font-bold whitespace-nowrap transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-blue-600 text-white shadow-sm shadow-blue-500/20'
                    : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
                }`}
              >
                {label}
              </button>
            );
          })}
        </div>

        {/* Scheme Cards Grid */}
        <div className="space-y-4">
          {filteredSchemes.length === 0 ? (
            <div className="p-12 text-center bg-white rounded-3xl border border-slate-200">
              <Info className="w-10 h-10 text-slate-400 mx-auto mb-3" />
              <h3 className="text-base font-bold text-slate-900">No matching schemes found</h3>
              <p className="text-xs text-slate-500 mt-1">
                Try switching categories or clearing your search keywords.
              </p>
              <button
                onClick={() => {
                  setSelectedCategory('all');
                  setSearchQuery('');
                }}
                className="mt-4 px-4 py-2 rounded-xl bg-blue-600 text-white font-bold text-xs"
              >
                Reset Filters
              </button>
            </div>
          ) : (
            filteredSchemes.map((scheme) => {
              const isSpeaking = currentlySpeakingId === scheme.id;
              const isExpanded = expandedSchemeId === scheme.id;
              const title = scheme.nativeNames[language] || scheme.name;
              const desc = scheme.shortDescription[language] || scheme.shortDescription.en;

              return (
                <div
                  key={scheme.id}
                  className="bg-white rounded-3xl border border-slate-200/90 shadow-xs p-5 sm:p-7 transition-all hover:border-slate-300"
                >
                  {/* Card Header */}
                  <div className="flex items-start justify-between gap-3">
                    <div className="space-y-1.5">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-[10px] font-black uppercase tracking-wider px-2.5 py-0.5 rounded-md bg-blue-50 text-blue-700 border border-blue-200/60">
                          {scheme.jurisdiction}
                        </span>
                        {scheme.isFeatured && (
                          <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-md bg-amber-50 text-amber-700">
                            ★ High Priority
                          </span>
                        )}
                        <span className="text-[11px] text-slate-600 font-medium">
                          {scheme.lastVerified}
                        </span>
                      </div>
                      <h2 className="text-lg sm:text-xl font-bold text-slate-900 leading-snug">
                        {title}
                      </h2>
                    </div>

                    {/* Listen Icon Button */}
                    <button
                      onClick={() => handleReadScheme(scheme)}
                      aria-label={isSpeaking ? 'Stop reading scheme details' : `Read ${title} aloud`}
                      className={`w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 border transition-all cursor-pointer ${
                        isSpeaking
                          ? 'bg-blue-600 text-white border-blue-600 shadow-sm animate-pulse'
                          : 'bg-slate-50 border-slate-200 text-blue-600 hover:bg-blue-50'
                      }`}
                    >
                      {isSpeaking ? <VolumeX className="w-5 h-5" /> : <Volume2 className="w-5 h-5" />}
                    </button>
                  </div>

                  {/* Description */}
                  <p className="text-sm text-slate-700 mt-3 leading-relaxed">
                    {desc}
                  </p>

                  {/* Highlights Grid (Eligibility & Benefits) */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-4 pt-3 border-t border-slate-100 text-xs">
                    <div className="space-y-1.5 p-3 rounded-2xl bg-slate-50 border border-slate-200/60">
                      <span className="font-bold text-slate-900 flex items-center gap-1.5">
                        <FileCheck className="w-3.5 h-3.5 text-blue-600" />
                        Eligibility Highlights:
                      </span>
                      <ul className="text-slate-600 space-y-1 list-disc pl-4">
                        {scheme.eligibility.slice(0, 2).map((item, idx) => (
                          <li key={idx} className="line-clamp-2">{item}</li>
                        ))}
                      </ul>
                    </div>

                    <div className="space-y-1.5 p-3 rounded-2xl bg-blue-50/50 border border-blue-200/60">
                      <span className="font-bold text-slate-900 flex items-center gap-1.5">
                        <Award className="w-3.5 h-3.5 text-blue-600" />
                        Assistance Provided:
                      </span>
                      <ul className="text-slate-700 space-y-1 list-disc pl-4 font-medium">
                        {scheme.benefits.slice(0, 2).map((item, idx) => (
                          <li key={idx} className="line-clamp-2">{item}</li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  {/* Collapsible Expanded Details */}
                  {isExpanded && (
                    <div className="mt-5 pt-4 border-t border-slate-200 space-y-4 text-xs animate-fadeIn">
                      <div>
                        <h4 className="font-bold text-slate-900 mb-1 flex items-center gap-1.5">
                          <Building2 className="w-3.5 h-3.5 text-blue-600" /> Administering Authority:
                        </h4>
                        <p className="text-slate-600 pl-5">{scheme.authority}</p>
                      </div>

                      <div>
                        <h4 className="font-bold text-slate-900 mb-1 flex items-center gap-1.5">
                          <FileCheck className="w-3.5 h-3.5 text-blue-600" /> Required Documents:
                        </h4>
                        <ul className="text-slate-600 space-y-1 pl-5 list-disc">
                          {scheme.documentsRequired.map((doc, idx) => (
                            <li key={idx}>{doc}</li>
                          ))}
                        </ul>
                      </div>

                      <div>
                        <h4 className="font-bold text-slate-900 mb-1 flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-blue-600" /> Application Timeline:
                        </h4>
                        <p className="text-slate-600 pl-5">
                          {scheme.applicationOpeningDate || 'Ongoing'} &nbsp;to&nbsp;{' '}
                          {scheme.applicationClosingDate || 'Open indefinitely'}
                        </p>
                      </div>

                      <div>
                        <h4 className="font-bold text-slate-900 mb-1 flex items-center gap-1.5">
                          <CheckCircle2 className="w-3.5 h-3.5 text-blue-600" /> How to Apply:
                        </h4>
                        <ol className="text-slate-600 space-y-1 pl-5 list-decimal">
                          {scheme.applicationInstructions.map((step, idx) => (
                            <li key={idx}>{step}</li>
                          ))}
                        </ol>
                      </div>
                    </div>
                  )}

                  {/* Card Action Buttons */}
                  <div className="flex flex-wrap items-center justify-between gap-3 mt-5 pt-4 border-t border-slate-100">
                    <button
                      onClick={() => setExpandedSchemeId(isExpanded ? null : scheme.id)}
                      className="text-xs font-bold text-blue-600 hover:text-blue-700 flex items-center gap-1 cursor-pointer"
                    >
                      <span>{isExpanded ? 'Hide Details' : 'Read Full Details & Checklist'}</span>
                      {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                    </button>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setSelectedScheme(scheme)}
                        className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition-all cursor-pointer"
                      >
                        Document Checklist
                      </button>

                      <a
                        href={scheme.officialUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold flex items-center gap-1.5 shadow-xs transition-all cursor-pointer"
                      >
                        <span>Official Website</span>
                        <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Modal Dialog for Document Checklist */}
      {selectedScheme && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 animate-fadeIn"
          onClick={() => setSelectedScheme(null)}
        >
          <div
            className="bg-white rounded-3xl p-6 sm:p-8 max-w-lg w-full shadow-2xl border border-slate-200 space-y-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <span className="text-[10px] font-black uppercase tracking-wider text-blue-600 bg-blue-50 px-2.5 py-0.5 rounded-md">
                  Official Checklist
                </span>
                <h3 className="text-xl font-bold text-slate-900 mt-1">
                  {selectedScheme.name}
                </h3>
              </div>
              <button
                onClick={() => setSelectedScheme(null)}
                className="w-9 h-9 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 hover:bg-slate-200"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4 text-xs text-slate-700">
              <div className="p-4 rounded-2xl bg-blue-50/60 border border-blue-200/80 space-y-2">
                <h4 className="font-bold text-blue-900 text-sm">Required Documents Checklist</h4>
                <p className="text-slate-600 text-xs">
                  Prepare original copies and digital scanned PDFs before beginning online registration:
                </p>
                <ul className="space-y-1.5 pt-1">
                  {selectedScheme.documentsRequired.map((doc, idx) => (
                    <li key={idx} className="flex items-start gap-2 text-slate-800 font-medium">
                      <span className="w-4 h-4 rounded-full bg-blue-600 text-white flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">
                        ✓
                      </span>
                      <span>{doc}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="space-y-2">
                <h4 className="font-bold text-slate-900 text-sm">Official Portal Link</h4>
                <a
                  href={selectedScheme.officialUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full py-3 px-4 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs flex items-center justify-center gap-2 transition-all cursor-pointer"
                >
                  <span>Open {selectedScheme.officialUrl}</span>
                  <ExternalLink className="w-4 h-4" />
                </a>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

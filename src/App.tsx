/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import {
  AppRoute,
  AppLanguage,
  CoreState,
  AccessibilitySettings,
  MedicalReminder,
} from './types';
import { DrishtiHeader } from './components/DrishtiHeader';
import { NavigationDrawer } from './components/NavigationDrawer';
import { BottomNavBar } from './components/BottomNavBar';
import { ActiveReminderModal } from './components/ActiveReminderModal';
import { HomeScreen } from './screens/HomeScreen';
import { GovernmentSchemesScreen } from './screens/GovernmentSchemesScreen';
import { VoiceCompanionScreen } from './screens/VoiceCompanionScreen';
import { ScheduleScreen } from './screens/ScheduleScreen';
import { ScanScreen } from './screens/ScanScreen';
import { NavigationScreen } from './screens/NavigationScreen';
import { DescribeScreen } from './screens/DescribeScreen';
import { NearbyScreen } from './screens/NearbyScreen';
import { EmergencyScreen } from './screens/EmergencyScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { InfoScreens } from './screens/InfoScreens';
import { speechService } from './services/speechService';
import { reminderStorage } from './services/reminderStorage';
import { getSavedNotifications } from './services/schemesData';

const SETTINGS_KEY = 'drishti_accessibility_settings_v1';
const LANGUAGE_KEY = 'drishti_app_language_v1';

export default function App() {
  const [currentRoute, setCurrentRoute] = useState<AppRoute>('home');
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [coreState, setCoreState] = useState<CoreState>('IDLE');
  const [activeTranscript, setActiveTranscript] = useState('');
  const [activeAlarmReminder, setActiveAlarmReminder] = useState<MedicalReminder | null>(null);
  const [needsAudioUnlock, setNeedsAudioUnlock] = useState(false);

  // App Language
  const [language, setLanguage] = useState<AppLanguage>(() => {
    if (typeof window === 'undefined') return 'en';
    const saved = localStorage.getItem(LANGUAGE_KEY);
    return (saved as AppLanguage) || 'en';
  });

  // Accessibility Settings
  const [accessibilitySettings, setAccessibilitySettings] = useState<AccessibilitySettings>(() => {
    if (typeof window === 'undefined') {
      return {
        largeText: false,
        highContrast: false,
        voiceSpeed: 1.0,
        voiceVolume: 1.0,
        voiceWakeEnabled: true,
        spokenAlertsEnabled: true,
        soundEffectsEnabled: true,
      };
    }
    try {
      const raw = localStorage.getItem(SETTINGS_KEY);
      if (raw) return JSON.parse(raw);
    } catch (_) {}
    return {
      largeText: false,
      highContrast: false,
      voiceSpeed: 1.0,
      voiceVolume: 1.0,
      voiceWakeEnabled: true,
      spokenAlertsEnabled: true,
      soundEffectsEnabled: true,
    };
  });

  // Unread scheme notifications count
  const [unreadCount, setUnreadCount] = useState<number>(() => {
    const notifs = getSavedNotifications();
    return notifs.filter((n) => !n.isRead).length;
  });

  // Sync Language
  const handleLanguageChange = (lang: AppLanguage) => {
    setLanguage(lang);
    if (typeof window !== 'undefined') {
      localStorage.setItem(LANGUAGE_KEY, lang);
    }
  };

  // Sync Accessibility Settings
  const handleUpdateAccessibilitySettings = (updates: Partial<AccessibilitySettings>) => {
    setAccessibilitySettings((prev) => {
      const updated = { ...prev, ...updates };
      if (typeof window !== 'undefined') {
        localStorage.setItem(SETTINGS_KEY, JSON.stringify(updated));
      }
      return updated;
    });
  };

  // Toggle High Contrast
  const handleToggleHighContrast = () => {
    handleUpdateAccessibilitySettings({ highContrast: !accessibilitySettings.highContrast });
  };

  // Listen for scheduled medical reminder alarms
  useEffect(() => {
    const unsubscribe = reminderStorage.onAlertTriggered((reminder) => {
      setActiveAlarmReminder(reminder);
    });
    return () => unsubscribe();
  }, []);

  // Unlock browser audio context and speech synthesis on first user interaction (Vercel / Production Autoplay)
  useEffect(() => {
    const unlock = () => {
      speechService.unlockAudio();
      setNeedsAudioUnlock(false);
    };
    window.addEventListener('click', unlock, { passive: true });
    window.addEventListener('touchstart', unlock, { passive: true });
    window.addEventListener('keydown', unlock, { passive: true });
    return () => {
      window.removeEventListener('click', unlock);
      window.removeEventListener('touchstart', unlock);
      window.removeEventListener('keydown', unlock);
    };
  }, []);

  // Global Central Core Voice Handler
  const handleGlobalVoiceCore = async () => {
    if (coreState === 'LISTENING') {
      speechService.stopListening();
      setCoreState('IDLE');
      return;
    }

    speechService.stop();

    // Check for insecure origin (HTTP on non-localhost IP addresses)
    if (
      typeof window !== 'undefined' &&
      window.location.protocol === 'http:' &&
      window.location.hostname !== 'localhost' &&
      window.location.hostname !== '127.0.0.1'
    ) {
      setCoreState('ERROR');
      setActiveTranscript('Speech recognition requires HTTPS or http://localhost:3000');
      speechService.playTone('error');
      setTimeout(() => setCoreState('IDLE'), 4500);
      return;
    }

    // Proactively verify / request browser microphone permission
    const perm = await speechService.requestMicPermission();
    if (!perm.granted) {
      setCoreState('ERROR');
      setActiveTranscript(perm.error || 'Microphone blocked. Please allow mic access in your browser address bar.');
      speechService.playTone('error');
      setTimeout(() => setCoreState('IDLE'), 4500);
      return;
    }

    setCoreState('LISTENING');
    speechService.playTone('listen_start');

    speechService.startListening({
      language,
      continuous: false,
      onResult: (transcript, isFinal) => {
        setActiveTranscript(transcript);
        if (isFinal) {
          handleVoiceRouting(transcript);
        }
      },
      onError: (err) => {
        setCoreState('ERROR');
        setActiveTranscript(err);
        speechService.playTone('error');
        setTimeout(() => setCoreState('IDLE'), 4000);
      },
      onEnd: () => {
        setCoreState((prev) => (prev === 'LISTENING' ? 'IDLE' : prev));
      },
    });
  };

  // Schedule, Schemes, and Companion screen voice handoffs
  const [scheduleInitialQuery, setScheduleInitialQuery] = useState<string | null>(null);
  const [scheduleSpokenSummary, setScheduleSpokenSummary] = useState<string | null>(null);
  const [autoStartCompanion, setAutoStartCompanion] = useState(false);
  const [autoReadNotifications, setAutoReadNotifications] = useState(false);
  const [autoReadSchemes, setAutoReadSchemes] = useState(false);
  const [isWakeWordActive, setIsWakeWordActive] = useState(false);

  // Background Wake Word ("Hey Drishti") Listener
  useEffect(() => {
    if (!accessibilitySettings.voiceWakeEnabled) {
      speechService.stopWakeWordListener(true);
      setIsWakeWordActive(false);
      return;
    }

    // Do not run background wake listening when actively speaking, processing, or in companion mode
    if (coreState !== 'IDLE' || currentRoute === 'companion') {
      speechService.stopWakeWordListener(false);
      return;
    }

    const started = speechService.startWakeWordListener({
      language,
      onWake: (remainingCommand) => {
        setIsWakeWordActive(true);
        if (!speechService.getIsAudioUnlocked()) {
          setNeedsAudioUnlock(true);
        }
        speechService.playTone('listen_start');

        const cleanCommand = remainingCommand.trim();
        // If a command was attached directly (e.g. "Hey Drishti tell me government scheme")
        if (cleanCommand.length > 2) {
          handleVoiceRouting(cleanCommand);
        } else {
          // Only wake phrase was spoken ("Hey Drishti") -> wake up listening trigger!
          setCoreState('SPEAKING');
          const wakeAck =
            language === 'hi'
              ? 'हाँ, मैं सुन रही हूँ।'
              : language === 'mr'
              ? 'हो, मी ऐकत आहे.'
              : 'Yes, I am listening.';

          setActiveTranscript(wakeAck);
          speechService.speak(wakeAck, {
            language,
            onEnd: () => {
              // Open listening trigger to receive the subsequent voice command!
              handleGlobalVoiceCore();
            },
            onError: () => {
              handleGlobalVoiceCore();
            },
          });
        }
      },
      onError: () => {
        setIsWakeWordActive(false);
      },
    });

    setIsWakeWordActive(started);

    return () => {
      speechService.stopWakeWordListener(false);
    };
  }, [accessibilitySettings.voiceWakeEnabled, coreState, currentRoute, language]);

  // Ensure first click on page initializes wake listener if browser policy requires a gesture
  useEffect(() => {
    const handleFirstGesture = () => {
      if (accessibilitySettings.voiceWakeEnabled && !isWakeWordActive && coreState === 'IDLE') {
        const started = speechService.startWakeWordListener({
          language,
          onWake: (remainingCommand) => {
            setIsWakeWordActive(true);
            speechService.playTone('listen_start');
            const cleanCommand = remainingCommand.trim();
            if (cleanCommand.length > 2) {
              handleVoiceRouting(cleanCommand);
            } else {
              setCoreState('SPEAKING');
              const wakeAck =
                language === 'hi'
                  ? 'हाँ, मैं सुन रही हूँ।'
                  : language === 'mr'
                  ? 'हो, मी ऐकत आहे.'
                  : 'Yes, I am listening.';
              setActiveTranscript(wakeAck);
              speechService.speak(wakeAck, {
                language,
                onEnd: () => handleGlobalVoiceCore(),
                onError: () => handleGlobalVoiceCore(),
              });
            }
          },
        });
        if (started) {
          setIsWakeWordActive(true);
          // Successfully started — remove listener
          window.removeEventListener('click', handleFirstGesture);
          window.removeEventListener('touchstart', handleFirstGesture);
        }
        // If not started (e.g., still speaking), keep the listener active to retry on next gesture
      }
    };
    window.addEventListener('click', handleFirstGesture, { passive: true });
    window.addEventListener('touchstart', handleFirstGesture, { passive: true });
    return () => {
      window.removeEventListener('click', handleFirstGesture);
      window.removeEventListener('touchstart', handleFirstGesture);
    };
  }, [accessibilitySettings.voiceWakeEnabled, isWakeWordActive, coreState, language]);

  // Toggle Wake Word mode
  const handleToggleWakeWord = () => {
    const nextState = !accessibilitySettings.voiceWakeEnabled;
    handleUpdateAccessibilitySettings({ voiceWakeEnabled: nextState });
    if (nextState) {
      speechService.playTone('listen_start');
      speechService.speak(
        language === 'hi'
          ? "हे दृष्टि वेक वर्ड चालू हो गया है। आप कभी भी 'हे दृष्टि' बोल सकते हैं।"
          : language === 'mr'
          ? "हे दृष्टी वेक वर्ड सुरू झाले आहे. तुम्ही कधीही 'हे दृष्टी' बोलू शकता."
          : "Hey Drishti wake word enabled. Say 'Hey Drishti' anytime.",
        { language }
      );
    } else {
      speechService.playTone('listen_stop');
      speechService.speak(
        language === 'hi'
          ? "वेक वर्ड बंद कर दिया गया है।"
          : language === 'mr'
          ? "वेक वर्ड बंद केले आहे."
          : "Wake word disabled.",
        { language }
      );
    }
  };

  // Natural Language Voice Routing
  const handleVoiceRouting = async (transcript: string) => {
    // Immediately stop active microphone listening so no ambient audio interferes
    speechService.stopListening();
    const lower = transcript.toLowerCase().trim();
    setCoreState('PROCESSING');
    speechService.playTone('processing');

    // Strip out leading wake phrases if present in the routed transcript
    const strippedLower = lower
      .replace(/^hey drishti\s*,?\s*/i, '')
      .replace(/^drishti\s*,?\s*/i, '')
      .replace(/^हे दृष्टि\s*,?\s*/i, '')
      .replace(/^दृष्टि\s*,?\s*/i, '')
      .replace(/^हे दृष्टी\s*,?\s*/i, '')
      .replace(/^दृष्टी\s*,?\s*/i, '')
      .trim();

    const queryToEvaluate = strippedLower || lower;

    // 0. Only Wake Trigger Query ("hey drishti", "drishti", "wake up", "wake up trigger", "listening trigger")
    if (
      queryToEvaluate === 'hey drishti' ||
      queryToEvaluate === 'drishti' ||
      queryToEvaluate === 'wake up' ||
      queryToEvaluate === 'wake' ||
      queryToEvaluate === 'listening trigger' ||
      queryToEvaluate === 'listening' ||
      queryToEvaluate === 'listen' ||
      queryToEvaluate === 'सुनो' ||
      queryToEvaluate === 'ऐका' ||
      queryToEvaluate === ''
    ) {
      setCoreState('SPEAKING');
      const wakeAck =
        language === 'hi'
          ? 'हाँ, मैं सुन रही हूँ। कहिए क्या सहायता करूँ?'
          : language === 'mr'
          ? 'हो, मी ऐकत आहे. सांगा मी कशी मदत करू?'
          : 'Yes, I am listening. What can I help you with?';

      setActiveTranscript(wakeAck);
      speechService.speak(wakeAck, {
        language,
        onEnd: () => handleGlobalVoiceCore(),
        onError: () => handleGlobalVoiceCore(),
      });
      return;
    }

    // 1. Read Out Government Scheme Notifications ("read out notification", "reda out notification", etc.)
    const isNotificationQuery =
      queryToEvaluate.includes('read out notification') ||
      queryToEvaluate.includes('reda out notification') ||
      queryToEvaluate.includes('read out notifications') ||
      queryToEvaluate.includes('reda out notifications') ||
      queryToEvaluate.includes('read notification') ||
      queryToEvaluate.includes('reda notification') ||
      queryToEvaluate.includes('read notifications') ||
      queryToEvaluate.includes('reda notifications') ||
      queryToEvaluate.includes('read all notification') ||
      queryToEvaluate.includes('read all notifications') ||
      queryToEvaluate.includes('tell me notification') ||
      queryToEvaluate.includes('tell me notifications') ||
      queryToEvaluate.includes('tell notification') ||
      queryToEvaluate.includes('speak notification') ||
      queryToEvaluate.includes('what are my notifications') ||
      queryToEvaluate.includes('check notifications') ||
      queryToEvaluate.includes('all notification') ||
      queryToEvaluate.includes('all notifications') ||
      (queryToEvaluate.includes('notification') &&
        (queryToEvaluate.includes('read') ||
          queryToEvaluate.includes('reda') ||
          queryToEvaluate.includes('speak') ||
          queryToEvaluate.includes('tell') ||
          queryToEvaluate.includes('all') ||
          queryToEvaluate.includes('out'))) ||
      queryToEvaluate.includes('सूचनाएं पढ़ो') ||
      queryToEvaluate.includes('नोटिफिकेशन पढ़ो') ||
      queryToEvaluate.includes('नोटिफिकेशन सुनाओ') ||
      queryToEvaluate.includes('नोटिफिकेशन वाचा') ||
      queryToEvaluate.includes('सर्व सूचना वाचा');

    if (isNotificationQuery) {
      speechService.stop();
      setAutoReadNotifications(true);
      setCurrentRoute('schemes');
      setCoreState('SPEAKING');
      return;
    }

    // 2. Read Out Government Schemes ("tell me government scheme", "read government scheme", "what schemes are there", etc.)
    const isSchemeQuery =
      queryToEvaluate.includes('tell me government scheme') ||
      queryToEvaluate.includes('tell me government schemes') ||
      queryToEvaluate.includes('tell government scheme') ||
      queryToEvaluate.includes('tell government schemes') ||
      queryToEvaluate.includes('tell me scheme') ||
      queryToEvaluate.includes('tell me schemes') ||
      queryToEvaluate.includes('tell scheme') ||
      queryToEvaluate.includes('tell schemes') ||
      queryToEvaluate.includes('read government scheme') ||
      queryToEvaluate.includes('read government schemes') ||
      queryToEvaluate.includes('read out government scheme') ||
      queryToEvaluate.includes('read out government schemes') ||
      queryToEvaluate.includes('read scheme') ||
      queryToEvaluate.includes('read schemes') ||
      queryToEvaluate.includes('speak government scheme') ||
      queryToEvaluate.includes('speak government schemes') ||
      queryToEvaluate.includes('what is government scheme') ||
      queryToEvaluate.includes('what are government schemes') ||
      queryToEvaluate.includes('government scheme') ||
      queryToEvaluate.includes('government schemes') ||
      queryToEvaluate.includes('scholarship') ||
      queryToEvaluate.includes('pension') ||
      queryToEvaluate.includes('assistive') ||
      queryToEvaluate.includes('smart cane') ||
      queryToEvaluate.includes('adip') ||
      queryToEvaluate.includes('maharashtra scheme') ||
      queryToEvaluate.includes('सरकारी योजना बताओ') ||
      queryToEvaluate.includes('सरकारी योजनाएं बताओ') ||
      queryToEvaluate.includes('सरकारी योजना') ||
      queryToEvaluate.includes('शासकीय योजना सांगा') ||
      queryToEvaluate.includes('शासकीय योजना') ||
      queryToEvaluate.includes('योजना');

    if (isSchemeQuery) {
      speechService.stop();
      setAutoReadSchemes(true);
      setCurrentRoute('schemes');
      setCoreState('SPEAKING');
      return;
    }

    // 3. Voice Companion ("be my companion", "be my campanion", "talk with companion", etc.)
    const isCompanionQuery =
      queryToEvaluate.includes('be my companion') ||
      queryToEvaluate.includes('be my campanion') ||
      queryToEvaluate.includes('be my companian') ||
      queryToEvaluate.includes('be companion') ||
      queryToEvaluate.includes('be campanion') ||
      queryToEvaluate.includes('campanion') ||
      queryToEvaluate.includes('companion') ||
      queryToEvaluate.includes('friendly conversation') ||
      queryToEvaluate.includes('start friendly conversation') ||
      queryToEvaluate.includes('talk with') ||
      queryToEvaluate.includes('talk to') ||
      queryToEvaluate.includes('want to talk') ||
      queryToEvaluate.includes('start conversation') ||
      queryToEvaluate.includes('have a chat') ||
      queryToEvaluate.includes('feeling lonely') ||
      queryToEvaluate.includes('lonely') ||
      queryToEvaluate.includes('साथी बनो') ||
      queryToEvaluate.includes('बात करनी है') ||
      queryToEvaluate.includes('बात करो') ||
      queryToEvaluate.includes('साथी से बात') ||
      queryToEvaluate.includes('कंपेनियन') ||
      queryToEvaluate.includes('माझा मित्र हो') ||
      queryToEvaluate.includes('माझा सोबती हो') ||
      queryToEvaluate.includes('मित्राशी बोला') ||
      queryToEvaluate.includes('संभाषण सुरू');

    if (isCompanionQuery) {
      speechService.stop();
      setAutoStartCompanion(true);
      setCurrentRoute('companion');
      setCoreState('SPEAKING');
      return;
    }

    // 4. "Read my schedule" / Medical Reminders / Entire Schedule
    const isScheduleQuery =
      queryToEvaluate.includes('read my schedule') ||
      queryToEvaluate.includes('read schedule') ||
      queryToEvaluate.includes('read out my schedule') ||
      queryToEvaluate.includes('read out my entire schedule') ||
      queryToEvaluate.includes('read my entire schedule') ||
      queryToEvaluate.includes('entire schedule') ||
      queryToEvaluate.includes('whole schedule') ||
      queryToEvaluate.includes('full schedule') ||
      queryToEvaluate.includes('all schedule') ||
      queryToEvaluate.includes('tell me my schedule') ||
      queryToEvaluate.includes('what is my schedule') ||
      queryToEvaluate.includes('tell me todays schedule') ||
      queryToEvaluate.includes('tell me today schedule') ||
      queryToEvaluate.includes('what is on my schedule today') ||
      queryToEvaluate.includes("what's on my schedule today") ||
      queryToEvaluate.includes('check schedule') ||
      queryToEvaluate.includes('schedule') ||
      queryToEvaluate.includes('remind') ||
      queryToEvaluate.includes('medicine') ||
      queryToEvaluate.includes('appointment') ||
      queryToEvaluate.includes('doctor') ||
      queryToEvaluate.includes('मेरा पूरा शेड्यूल पढ़ो') ||
      queryToEvaluate.includes('मेरा शेड्यूल पढ़ो') ||
      queryToEvaluate.includes('शेड्यूल बताओ') ||
      queryToEvaluate.includes('माझे संपूर्ण वेळापत्रक वाचा') ||
      queryToEvaluate.includes('वेळापत्रक वाचा') ||
      queryToEvaluate.includes('वेळापत्रक सांगा') ||
      queryToEvaluate.includes('वेळापत्रक');

    if (isScheduleQuery) {
      // Query schedule and hand off to ScheduleScreen to speak entire schedule aloud
      const result = reminderStorage.querySchedule(queryToEvaluate, language);
      setCurrentRoute('schedule');
      setScheduleSpokenSummary(result.summary);
      setActiveTranscript(result.summary);
      setCoreState('SPEAKING');
      return;
    }

    // 5. Vision / Scan commands
    if (
      queryToEvaluate.includes('scan') ||
      queryToEvaluate.includes('obstacle') ||
      queryToEvaluate.includes('देखें') ||
      queryToEvaluate.includes('बघा')
    ) {
      setCurrentRoute('scan');
      setCoreState('IDLE');
      return;
    }

    // 6. Outdoor Navigation commands
    if (
      queryToEvaluate.includes('navigate') ||
      queryToEvaluate.includes('direction') ||
      queryToEvaluate.includes('रास्ता') ||
      queryToEvaluate.includes('मार्ग')
    ) {
      setCurrentRoute('navigate');
      setCoreState('IDLE');
      return;
    }

    // 7. Describe Scene commands
    if (
      queryToEvaluate.includes('describe') ||
      queryToEvaluate.includes('what is in front of me') ||
      queryToEvaluate.includes('विवरण')
    ) {
      setCurrentRoute('describe');
      setCoreState('IDLE');
      return;
    }

    // 8. Nearby commands
    if (
      queryToEvaluate.includes('nearby') ||
      queryToEvaluate.includes('bus') ||
      queryToEvaluate.includes('hospital') ||
      queryToEvaluate.includes('पास')
    ) {
      setCurrentRoute('nearby');
      setCoreState('IDLE');
      return;
    }

    // 9. Emergency SOS
    if (
      queryToEvaluate.includes('emergency') ||
      queryToEvaluate.includes('sos') ||
      queryToEvaluate.includes('help') ||
      queryToEvaluate.includes('मदद')
    ) {
      setCurrentRoute('emergency');
      setCoreState('IDLE');
      return;
    }

    // Default: Provide helpful guidance
    setCoreState('SPEAKING');
    const helpfulSpeech =
      language === 'hi'
        ? "मैं दृष्टि हूँ। आप मुझसे कह सकते हैं: 'सरकारी योजनाएं बताओ', 'मेरा शेड्यूल पढ़ो', या 'मेरे साथी बनो'।"
        : language === 'mr'
        ? "मी दृष्टी आहे. तुम्ही मला म्हणू शकता: 'शासकीय योजना सांगा', 'माझे वेळापत्रक वाचा', किंवा 'माझे सोबती व्हा'."
        : "I am Drishti. You can say: 'Tell me government scheme', 'Read my schedule', or 'Be my companion'.";

    setActiveTranscript(helpfulSpeech);
    speechService.speak(helpfulSpeech, {
      language,
      onEnd: () => setCoreState('IDLE'),
      onError: () => setCoreState('IDLE'),
    });
  };

  return (
    <div
      className={`min-h-screen flex flex-col font-sans transition-colors ${
        accessibilitySettings.highContrast ? 'high-contrast-mode' : ''
      } ${accessibilitySettings.largeText ? 'text-lg' : 'text-base'}`}
    >
      {/* Accessible skip link for screen readers */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:px-4 focus:py-2 focus:bg-blue-600 focus:text-white focus:rounded-xl focus:shadow-lg focus:outline-none"
      >
        Skip to main content
      </a>

      {/* Audio Autoplay Policy Unlock Banner */}
      {needsAudioUnlock && (
        <button
          onClick={() => {
            speechService.unlockAudio();
            setNeedsAudioUnlock(false);
            speechService.playTone('success');
            const welcomeMsg =
              language === 'hi'
                ? 'दृष्टि ऑडियो सक्रिय हो गया है।'
                : language === 'mr'
                ? 'दृष्टी ऑडिओ सक्रिय झाला आहे.'
                : 'Drishti voice and audio enabled.';
            speechService.speak(welcomeMsg, { language });
          }}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3.5 px-4 text-center flex items-center justify-center gap-2 shadow-lg transition-all cursor-pointer animate-pulse z-50 sticky top-0"
          role="alert"
          aria-live="assertive"
        >
          <span className="text-xl">🔊</span>
          <span className="text-sm sm:text-base font-extrabold underline decoration-white/60">
            {language === 'hi'
              ? 'आवाज सुनने के लिए यहाँ स्क्रीन पर एक बार टैप करें'
              : language === 'mr'
              ? 'आवाज ऐकण्यासाठी स्क्रीनवर एकदा टॅप करा'
              : 'Tap anywhere on screen to enable voice output & sound'}
          </span>
        </button>
      )}

      {/* Top Header */}
      <DrishtiHeader
        onOpenDrawer={() => setIsDrawerOpen(true)}
        language={language}
        onLanguageChange={handleLanguageChange}
        onOpenSos={() => setCurrentRoute('emergency')}
        onOpenNotifications={() => setCurrentRoute('schemes')}
        unreadNotificationsCount={unreadCount}
        coreState={coreState}
        highContrast={accessibilitySettings.highContrast}
        onToggleHighContrast={handleToggleHighContrast}
        isWakeWordActive={isWakeWordActive}
        onToggleWakeWord={handleToggleWakeWord}
      />

      {/* Main Content Area */}
      <main id="main-content" className="flex-1 w-full" tabIndex={-1}>
        {currentRoute === 'home' && (
          <HomeScreen
            onNavigate={(route) => setCurrentRoute(route)}
            language={language}
            coreState={coreState}
            onCoreClick={handleGlobalVoiceCore}
            activeTranscript={activeTranscript}
            unreadNotificationsCount={unreadCount}
            onVoiceQuery={(query) => handleVoiceRouting(query)}
          />
        )}

        {currentRoute === 'schemes' && (
          <GovernmentSchemesScreen
            language={language}
            onNavigateToCompanion={() => {
              setAutoStartCompanion(true);
              setCurrentRoute('companion');
            }}
            autoReadNotifications={autoReadNotifications}
            onFinishedReadingNotifications={() => {
              setAutoReadNotifications(false);
              setCoreState('IDLE');
              setUnreadCount(0);
            }}
            autoReadSchemes={autoReadSchemes}
            onFinishedReadingSchemes={() => {
              setAutoReadSchemes(false);
              setCoreState('IDLE');
            }}
          />
        )}

        {currentRoute === 'companion' && (
          <VoiceCompanionScreen
            language={language}
            onLanguageChange={handleLanguageChange}
            autoStartConversation={autoStartCompanion}
            onEndConversation={() => {
              setAutoStartCompanion(false);
              setCoreState('IDLE');
            }}
          />
        )}

        {currentRoute === 'schedule' && (
          <ScheduleScreen
            language={language}
            initialVoiceQuery={scheduleInitialQuery || undefined}
            initialSpokenSummary={scheduleSpokenSummary || undefined}
            onClearInitialVoiceQuery={() => {
              setScheduleInitialQuery(null);
              setScheduleSpokenSummary(null);
            }}
            onFinishedSpokenSummary={() => {
              setCoreState('IDLE');
            }}
          />
        )}

        {currentRoute === 'scan' && (
          <ScanScreen language={language} onBack={() => setCurrentRoute('home')} />
        )}

        {currentRoute === 'navigate' && (
          <NavigationScreen language={language} />
        )}

        {currentRoute === 'describe' && (
          <DescribeScreen language={language} />
        )}

        {currentRoute === 'nearby' && (
          <NearbyScreen language={language} onNavigateToWalk={() => setCurrentRoute('navigate')} />
        )}

        {currentRoute === 'emergency' && (
          <EmergencyScreen language={language} onCancel={() => setCurrentRoute('home')} />
        )}

        {currentRoute === 'settings' && (
          <SettingsScreen
            language={language}
            onLanguageChange={handleLanguageChange}
            accessibilitySettings={accessibilitySettings}
            onUpdateAccessibilitySettings={handleUpdateAccessibilitySettings}
            onNavigateToSafety={() => setCurrentRoute('safety')}
            onNavigateToPrivacy={() => setCurrentRoute('privacy')}
          />
        )}

        {currentRoute === 'safety' && (
          <InfoScreens type="safety" onBack={() => setCurrentRoute('settings')} />
        )}

        {currentRoute === 'privacy' && (
          <InfoScreens type="privacy" onBack={() => setCurrentRoute('settings')} />
        )}

        {currentRoute === 'permissions' && (
          <InfoScreens type="permissions" onBack={() => setCurrentRoute('settings')} />
        )}
      </main>

      {/* Slide-over Navigation Drawer */}
      <NavigationDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        currentRoute={currentRoute}
        onNavigate={(route) => setCurrentRoute(route)}
        language={language}
        unreadNotificationsCount={unreadCount}
      />

      {/* Accessible Bottom Quick Navigation Bar (always visible except on Emergency full screen) */}
      {currentRoute !== 'emergency' && (
        <BottomNavBar
          currentRoute={currentRoute}
          onNavigate={(route) => setCurrentRoute(route)}
          language={language}
          unreadNotificationsCount={unreadCount}
        />
      )}

      {/* Real-time Alarm / Reminder Trigger Modal */}
      <ActiveReminderModal
        reminder={activeAlarmReminder}
        onClose={() => setActiveAlarmReminder(null)}
      />
    </div>
  );
}

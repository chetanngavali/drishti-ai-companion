import React, { useState, useEffect, useRef } from 'react';
import {
  HeartHandshake,
  Volume2,
  VolumeX,
  PhoneCall,
  Wind,
  Trash2,
  ShieldAlert,
  Mic,
  MicOff,
  Sparkles,
  Info,
  Radio,
  RotateCcw,
  Check,
} from 'lucide-react';
import { AppLanguage, CompanionMessage, CoreState } from '../types';
import { speechService } from '../services/speechService';
import { DrishtiCoreOrb } from '../components/DrishtiCoreOrb';
import { BreathingExerciseModal } from '../components/BreathingExerciseModal';

interface VoiceCompanionScreenProps {
  language: AppLanguage;
  onLanguageChange: (lang: AppLanguage) => void;
  autoStartConversation?: boolean;
  onEndConversation?: () => void;
}

const COMPANION_STORAGE_KEY = 'drishti_companion_history_v1';

export const VoiceCompanionScreen: React.FC<VoiceCompanionScreenProps> = ({
  language,
  onLanguageChange,
  autoStartConversation = false,
  onEndConversation,
}) => {
  const [messages, setMessages] = useState<CompanionMessage[]>(() => {
    if (typeof window === 'undefined') return [];
    try {
      const raw = localStorage.getItem(COMPANION_STORAGE_KEY);
      if (raw) return JSON.parse(raw);
    } catch (_) {}
    return [
      {
        id: 'msg-welcome',
        sender: 'drishti',
        text:
          language === 'hi'
            ? 'नमस्ते, मैं दृष्टि हूँ, आपका आत्मीय साथी। आज आप कैसा महसूस कर रहे हैं? जब भी आपका बात करने का मन हो, मैं सुनने के लिए उपस्थित हूँ।'
            : language === 'mr'
            ? 'नमस्कार, मी दृष्टी आहे, तुमचा मित्र. आज तुम्हाला कसे वाटत आहे? तुम्हाला जेव्हाही बोलावेसे वाटेल, मी ऐकायला तयार आहे.'
            : "Hello, I am Drishti, your companion. How are you feeling today? Whenever you need someone to talk to, I am right here listening.",
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        language,
      },
    ];
  });

  const [coreState, setCoreState] = useState<CoreState>('IDLE');
  const [activeTranscript, setActiveTranscript] = useState('');
  const [isWakePhraseMode, setIsWakePhraseMode] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [isBreathingModalOpen, setIsBreathingModalOpen] = useState(false);
  const [showCrisisBanner, setShowCrisisBanner] = useState(false);
  const [saveHistoryConsent, setSaveHistoryConsent] = useState(true);
  const [lastDrishtiReply, setLastDrishtiReply] = useState<string>('');
  const [isContinuousMode, setIsContinuousMode] = useState(false);

  const isContinuousRef = useRef(false);
  const isEndingRef = useRef(false);
  const autoStartedRef = useRef(false);
  const chatBottomRef = useRef<HTMLDivElement>(null);

  // Auto-scroll chat to bottom
  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, activeTranscript]);

  // Save history to localStorage if consented
  useEffect(() => {
    if (typeof window === 'undefined') return;
    if (saveHistoryConsent) {
      try {
        localStorage.setItem(COMPANION_STORAGE_KEY, JSON.stringify(messages.slice(-25)));
      } catch (_) {}
    } else {
      localStorage.removeItem(COMPANION_STORAGE_KEY);
    }
  }, [messages, saveHistoryConsent]);

  // Clean cleanup on unmount
  useEffect(() => {
    return () => {
      isContinuousRef.current = false;
      isEndingRef.current = true;
      speechService.stop();
      speechService.stopListening();
    };
  }, []);

  // Helper to detect if user wants to terminate conversation
  const isEndTrigger = (text: string): boolean => {
    const clean = text.toLowerCase().trim().replace(/[.,!?;:]/g, '');
    if (!clean) return false;

    // Exact matches
    const exactEnds = [
      'end',
      'stop',
      'exit',
      'bye',
      'goodbye',
      'quit',
      'finish',
      'done',
      'close',
      'end conversation',
      'stop conversation',
      'stop talking',
      'exit conversation',
      'थांबा',
      'समाप्त',
      'बंद करा',
      'अलविदा',
      'बस',
      'खत्म',
      'बंद करो',
      'पूर्ण',
    ];
    if (exactEnds.includes(clean)) return true;

    // Word check
    const words = clean.split(/\s+/);
    if (
      words.includes('end') ||
      words.includes('stop') ||
      words.includes('थांबा') ||
      words.includes('समाप्त') ||
      words.includes('अलविदा')
    ) {
      if (
        words.length <= 4 &&
        (words[0] === 'end' ||
          words[words.length - 1] === 'end' ||
          words.includes('stop') ||
          words.includes('bye') ||
          words.includes('थांबा') ||
          words.includes('समाप्त'))
      ) {
        return true;
      }
    }

    return (
      clean.includes('end conversation') ||
      clean.includes('stop conversation') ||
      clean.includes('finish conversation') ||
      clean.includes('close conversation') ||
      clean.includes('बातचीत समाप्त') ||
      clean.includes('संभाषण संपवा') ||
      clean.includes('संभाषण थांबवा')
    );
  };

  // Continuous speech recognition loop
  const startContinuousListening = () => {
    if (!isContinuousRef.current || isEndingRef.current) return;
    if (speechService.isSpeaking()) {
      // Never activate microphone while Drishti is actively speaking
      return;
    }

    speechService.stopListening();
    setCoreState('LISTENING');
    speechService.playTone('listen_start');

    speechService.startListening({
      language,
      continuous: false,
      onResult: (transcript, isFinal) => {
        // Discard any sound picked up if speech synthesis is in progress
        if (speechService.isSpeaking()) return;

        setActiveTranscript(transcript);
        if (isFinal) {
          const cleanText = transcript.trim();
          if (isEndTrigger(cleanText)) {
            handleEndContinuousConversation();
          } else {
            handleUserMessage(cleanText);
          }
        }
      },
      onError: (err) => {
        // If continuous mode is active, gracefully re-listen ONLY if speech is not actively playing
        if (isContinuousRef.current && !isEndingRef.current && !speechService.isSpeaking()) {
          setTimeout(() => {
            if (isContinuousRef.current && !isEndingRef.current && !speechService.isSpeaking()) {
              startContinuousListening();
            }
          }, 800);
        } else if (!speechService.isSpeaking()) {
          setCoreState('IDLE');
          setActiveTranscript('');
        }
      },
      onEnd: () => {
        // Resume listening only if continuous mode is active and Drishti is NOT speaking
        if (isContinuousRef.current && !isEndingRef.current) {
          setCoreState((prev) => {
            if (prev === 'LISTENING') {
              setTimeout(() => {
                if (isContinuousRef.current && !isEndingRef.current && !speechService.isSpeaking()) {
                  startContinuousListening();
                }
              }, 400);
            }
            return prev;
          });
        } else if (!speechService.isSpeaking()) {
          setCoreState((prev) => (prev === 'LISTENING' ? 'IDLE' : prev));
        }
      },
    });
  };

  // Handle ending the conversation gracefully
  const handleEndContinuousConversation = () => {
    isContinuousRef.current = false;
    isEndingRef.current = true;
    setIsContinuousMode(false);
    speechService.stopListening();

    const goodbyeText =
      language === 'hi'
        ? 'बातचीत समाप्त कर रहे हैं। जब भी आपका बात करने का मन हो, मैं हमेशा यहीं हूँ। आपका दिन शुभ हो!'
        : language === 'mr'
        ? 'संभाषण समाप्त करत आहे. तुम्हाला जेव्हाही बोलावेसे वाटेल, मी येथेच आहे. तुमचा दिवस चांगला जावो!'
        : 'Ending our conversation. I am always right here whenever you want to talk. Have a wonderful day!';

    setActiveTranscript(goodbyeText);
    setCoreState('SPEAKING');

    speechService.speak(goodbyeText, {
      language,
      onEnd: () => {
        setCoreState('IDLE');
        setActiveTranscript('');
        onEndConversation?.();
      },
      onError: () => {
        setCoreState('IDLE');
        setActiveTranscript('');
        onEndConversation?.();
      },
    });
  };

  // Start continuous conversational mode
  const startContinuousMode = () => {
    isEndingRef.current = false;
    isContinuousRef.current = true;
    setIsContinuousMode(true);

    const greetingText =
      language === 'hi'
        ? "नमस्ते मेरे प्यारे दोस्त! आपका साथी बनकर मुझे बहुत खुशी हो रही है। आप अभी कैसा महसूस कर रहे हैं? मुझसे खुलकर बात कीजिए, और जब तक आप 'end' या 'समाप्त' नहीं कहेंगे, हमारी यह प्यारी बातचीत लगातार चलती रहेगी।"
        : language === 'mr'
        ? "नमस्कार माझ्या प्रिय मित्रा! तुमचा सोबती बनून मला खूप आनंद होत आहे. तुम्हाला आता कसे वाटत आहे? माझ्याशी मनमोकळेपणाने बोला, आणि जोपर्यंत तुम्ही 'end' किंवा 'थांबा' म्हणत नाही, तोपर्यंत आपले हे सुंदर संभाषण सुरूच राहील."
        : "Hello my dear friend! I am so glad to be your companion. How are you feeling today? Speak freely with me, and we will keep our friendly conversation going until you say 'end' or 'stop'.";

    setActiveTranscript(greetingText);
    setCoreState('SPEAKING');
    speechService.stopListening();

    speechService.speak(greetingText, {
      language,
      rate: 0.95,
      onStart: () => setCoreState('SPEAKING'),
      onEnd: () => {
        if (isContinuousRef.current && !isEndingRef.current) {
          setTimeout(() => {
            if (isContinuousRef.current && !isEndingRef.current && !speechService.isSpeaking()) {
              startContinuousListening();
            }
          }, 450);
        } else {
          setCoreState('IDLE');
        }
      },
      onError: () => {
        if (isContinuousRef.current && !isEndingRef.current) {
          setTimeout(() => {
            if (isContinuousRef.current && !isEndingRef.current && !speechService.isSpeaking()) {
              startContinuousListening();
            }
          }, 500);
        } else {
          setCoreState('IDLE');
        }
      },
    });
  };

  // Trigger continuous conversation automatically when requested via voice
  useEffect(() => {
    if (autoStartConversation) {
      startContinuousMode();
    }
  }, [autoStartConversation]);

  // Handle user speech input
  const handleUserMessage = async (text: string) => {
    if (!text || text.trim() === '') return;

    speechService.stop();
    const cleanUserText = text.trim();

    // Check if user says end
    if (isEndTrigger(cleanUserText)) {
      handleEndContinuousConversation();
      return;
    }

    // Check for special local voice commands
    const lower = cleanUserText.toLowerCase();

    if (
      lower.includes('stop talking') ||
      lower.includes('be quiet') ||
      lower.includes('चुप रहो') ||
      lower.includes('थांबा')
    ) {
      speechService.stop();
      if (isContinuousRef.current) {
        handleEndContinuousConversation();
      } else {
        setCoreState('IDLE');
      }
      return;
    }

    if (
      lower.includes('repeat that') ||
      lower.includes('repeat') ||
      lower.includes('फिर से बोलो') ||
      lower.includes('पुन्हा सांगा')
    ) {
      if (lastDrishtiReply) {
        speakResponse(lastDrishtiReply);
      }
      return;
    }

    if (
      lower.includes('breathing exercise') ||
      lower.includes('help me relax') ||
      lower.includes('श्वास व्यायाम') ||
      lower.includes('रिलैक्स')
    ) {
      setIsBreathingModalOpen(true);
      return;
    }

    // Add user message to state
    const userMsg: CompanionMessage = {
      id: 'usr-' + Date.now(),
      sender: 'user',
      text: cleanUserText,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      language,
    };

    setMessages((prev) => [...prev, userMsg]);
    setActiveTranscript('');
    setCoreState('PROCESSING');
    speechService.playTone('processing');

    try {
      const historyPayload = messages.slice(-5).map((m) => ({
        role: m.sender === 'user' ? 'user' : 'model',
        text: m.text,
      }));

      const res = await fetch('/api/companion/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: cleanUserText,
          language,
          history: historyPayload,
        }),
      });

      const data = await res.json();
      const replyText =
        data.reply ||
        (language === 'hi'
          ? 'मैं यहीं आपके साथ हूँ। आज आपका दिन कैसा रहा, मुझे बताएँ।'
          : language === 'mr'
          ? 'मी तुमच्या सोबतच आहे. आजचा दिवस कसा गेला ते मला सांगा.'
          : "I am right here with you. How has your day been so far?");

      setLastDrishtiReply(replyText);

      if (data.isCrisis) {
        setShowCrisisBanner(true);
      }

      const drishtiMsg: CompanionMessage = {
        id: 'drishti-' + Date.now(),
        sender: 'drishti',
        text: replyText,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        language,
        isCrisis: data.isCrisis,
      };

      setMessages((prev) => [...prev, drishtiMsg]);

      // Speak response aloud if not muted
      speakResponse(replyText);
    } catch (err) {
      console.error('Companion error:', err);
      setCoreState('IDLE');
      const fallbackText =
        language === 'hi'
          ? 'मैं आपकी बात ध्यान से सुन रहा हूँ। आप मुझसे अपने मन की कोई भी बात कह सकते हैं।'
          : language === 'mr'
          ? 'मी तुमचे बोलणे लक्षपूर्वक ऐकत आहे. तुम्ही मला काहीही मनमोकळेपणे सांगू शकता.'
          : "I'm right here listening to you. Tell me, how has your day been so far?";
      speakResponse(fallbackText);
    }
  };

  const speakResponse = (text: string) => {
    if (isMuted) {
      setCoreState('IDLE');
      if (isContinuousRef.current && !isEndingRef.current) {
        setTimeout(() => startContinuousListening(), 500);
      }
      return;
    }

    speechService.stopListening();
    setCoreState('SPEAKING');

    speechService.speak(text, {
      language,
      rate: 0.95,
      onStart: () => setCoreState('SPEAKING'),
      onEnd: () => {
        // If continuous mode is active, automatically listen for user's next reply after audio clears
        if (isContinuousRef.current && !isEndingRef.current) {
          setTimeout(() => {
            if (isContinuousRef.current && !isEndingRef.current && !speechService.isSpeaking()) {
              startContinuousListening();
            }
          }, 450);
        } else if (isWakePhraseMode) {
          setCoreState('IDLE');
          startWakeListening();
        } else {
          setCoreState('IDLE');
        }
      },
      onError: () => {
        if (isContinuousRef.current && !isEndingRef.current) {
          setTimeout(() => {
            if (isContinuousRef.current && !isEndingRef.current && !speechService.isSpeaking()) {
              startContinuousListening();
            }
          }, 500);
        } else {
          setCoreState('IDLE');
        }
      },
    });
  };

  // Toggle push-to-talk voice recording
  const handleToggleVoice = () => {
    if (isContinuousMode) {
      handleEndContinuousConversation();
      return;
    }

    if (coreState === 'LISTENING') {
      speechService.stopListening();
      setCoreState('IDLE');
      return;
    }

    speechService.stop();
    setCoreState('LISTENING');
    speechService.playTone('listen_start');

    speechService.startListening({
      language,
      continuous: false,
      onResult: (transcript, isFinal) => {
        setActiveTranscript(transcript);
        if (isFinal) {
          handleUserMessage(transcript);
        }
      },
      onError: () => {
        setCoreState('ERROR');
        setActiveTranscript('');
        speechService.playTone('error');
        setTimeout(() => setCoreState('IDLE'), 2000);
      },
      onEnd: () => {
        setCoreState((prev) => (prev === 'LISTENING' ? 'IDLE' : prev));
      },
    });
  };

  // Continuous wake phrase listening
  const startWakeListening = () => {
    speechService.startListening({
      language,
      continuous: true,
      onResult: (transcript, isFinal) => {
        const lower = transcript.toLowerCase();
        if (lower.includes('hey drishti') || lower.includes('हे दृष्टी')) {
          speechService.stopListening();
          speechService.playTone('listen_start');
          setCoreState('LISTENING');
          const stripped = transcript.replace(/hey drishti|हे दृष्टी/gi, '').trim();
          if (stripped.length > 2 && isFinal) {
            handleUserMessage(stripped);
          } else {
            speechService.speak(
              language === 'hi' ? 'हाँ, मैं सुन रहा हूँ। कहिए।' : language === 'mr' ? 'हो, मी ऐकतो आहे. बोला.' : "I'm listening. I'm here with you.",
              {
                language,
                onEnd: () => handleToggleVoice(),
              }
            );
          }
        }
      },
      onError: () => {
        setIsWakePhraseMode(false);
      },
      onEnd: () => {},
    });
  };

  // Toggle wake-phrase mode
  const handleToggleWakeMode = () => {
    if (isWakePhraseMode) {
      speechService.stopListening();
      setIsWakePhraseMode(false);
      speechService.speak('Wake phrase mode turned off.', { language });
    } else {
      setIsWakePhraseMode(true);
      speechService.speak("Wake phrase mode active. Say 'Hey Drishti' anytime while this tab is active.", { language });
      startWakeListening();
    }
  };

  const handleInterrupt = () => {
    speechService.stop();
    setCoreState('IDLE');
  };

  const handleClearHistory = () => {
    setMessages([]);
    localStorage.removeItem(COMPANION_STORAGE_KEY);
    speechService.playTone('success');
  };

  return (
    <div className="w-full min-h-screen bg-slate-50/70 pb-28 pt-4 px-4 sm:px-6">
      <div className="max-w-3xl mx-auto space-y-6">
        {/* Companion Header Banner */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="text-[11px] font-extrabold uppercase tracking-widest text-blue-700 bg-blue-50 border border-blue-200/60 px-3 py-0.5 rounded-full flex items-center gap-1.5">
                <HeartHandshake className="w-3.5 h-3.5 text-blue-600" />
                Drishti Emotional Well-being
              </span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Voice Companion
            </h1>
            <p className="text-xs sm:text-sm text-slate-600">
              A gentle, supportive friend designed to listen, encourage, and guide your daily peace of mind.
            </p>
          </div>

          {/* Action Toolbar */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Continuous Conversation Toggle Button */}
            <button
              onClick={isContinuousMode ? handleEndContinuousConversation : startContinuousMode}
              aria-label={
                isContinuousMode
                  ? "End continuous conversation mode"
                  : "Start hands-free continuous conversation mode"
              }
              className={`px-4 py-2.5 rounded-2xl border text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer shadow-xs ${
                isContinuousMode
                  ? 'bg-red-600 text-white border-red-600 animate-pulse'
                  : 'bg-blue-50 text-blue-700 border-blue-200 hover:bg-blue-100'
              }`}
            >
              {isContinuousMode ? (
                <>
                  <MicOff className="w-4 h-4" />
                  <span>End Conversation</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 text-blue-600" />
                  <span>Start Conversation (Hands-Free)</span>
                </>
              )}
            </button>

            {/* Mute Voice Toggle */}
            <button
              onClick={() => {
                if (!isMuted) speechService.stop();
                setIsMuted(!isMuted);
              }}
              aria-label={isMuted ? 'Unmute companion voice output' : 'Mute companion voice output'}
              className={`p-3 rounded-2xl border transition-all cursor-pointer ${
                isMuted
                  ? 'bg-red-50 text-red-600 border-red-200'
                  : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
              }`}
            >
              {isMuted ? <VolumeX className="w-5 h-5" /> : <Volume2 className="w-5 h-5" />}
            </button>

            {/* Wake Word Toggle */}
            <button
              onClick={handleToggleWakeMode}
              aria-label={isWakePhraseMode ? "Disable 'Hey Drishti' wake phrase" : "Enable 'Hey Drishti' wake phrase"}
              className={`px-3.5 py-2.5 rounded-2xl border text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
                isWakePhraseMode
                  ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                  : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
              }`}
            >
              <Radio className={`w-4 h-4 ${isWakePhraseMode ? 'animate-pulse' : ''}`} />
              <span>{isWakePhraseMode ? "'Hey Drishti' Active" : "Wake Phrase"}</span>
            </button>

            {/* Breathing Exercise Trigger */}
            <button
              onClick={() => setIsBreathingModalOpen(true)}
              aria-label="Start interactive breathing relaxation exercise"
              className="px-3.5 py-2.5 rounded-2xl bg-sky-50 border border-sky-200 text-sky-700 hover:bg-sky-100 font-bold text-xs flex items-center gap-1.5 transition-all cursor-pointer"
            >
              <Wind className="w-4 h-4" />
              <span>Breathe</span>
            </button>
          </div>
        </div>

        {/* Continuous Conversation Active Banner */}
        {isContinuousMode && (
          <div className="p-4 sm:p-5 rounded-3xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-md flex items-center justify-between gap-4 animate-fadeIn">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-white/20 backdrop-blur-xs flex items-center justify-center shrink-0">
                <Mic className="w-5 h-5 text-white animate-bounce" />
              </div>
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
                  <span className="font-extrabold text-sm tracking-wide">
                    Hands-Free Conversation Active
                  </span>
                </div>
                <p className="text-xs text-blue-100">
                  Drishti is listening and replying continuously. Say <strong className="text-white underline font-black">&apos;end&apos;</strong> or <strong className="text-white underline font-black">&apos;समाप्त&apos;</strong> anytime to stop.
                </p>
              </div>
            </div>

            <button
              onClick={handleEndContinuousConversation}
              className="px-3.5 py-2 bg-white text-blue-700 hover:bg-blue-50 font-extrabold text-xs rounded-xl shadow-xs transition-all shrink-0 cursor-pointer"
            >
              End Conversation
            </button>
          </div>
        )}

        {/* Mental Health Safety Helpline Drawer (Active if distress detected or clicked) */}
        {showCrisisBanner && (
          <div className="p-5 rounded-3xl bg-red-50 border-2 border-red-300 shadow-sm space-y-3 animate-fadeIn">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-2 text-red-700 font-extrabold text-sm sm:text-base">
                <ShieldAlert className="w-5 h-5 text-red-600 shrink-0" />
                <span>Immediate Support &amp; Indian Crisis Helplines</span>
              </div>
              <button
                onClick={() => setShowCrisisBanner(false)}
                className="text-xs text-red-500 font-bold hover:underline"
              >
                Dismiss
              </button>
            </div>
            <p className="text-xs text-slate-700 leading-relaxed font-medium">
              You are never alone. Professional, free, and completely confidential mental health counselors are available right now:
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
              <a
                href="tel:14416"
                className="p-3 bg-white rounded-2xl border border-red-200 text-slate-900 flex items-center justify-between hover:bg-red-50/50 transition-colors"
              >
                <div>
                  <div className="font-bold text-xs sm:text-sm">Tele-MANAS (Govt of India)</div>
                  <div className="text-[11px] text-red-600 font-semibold">Toll-Free 24x7: 14416 / 1800-891-4416</div>
                </div>
                <PhoneCall className="w-4 h-4 text-red-600" />
              </a>

              <a
                href="tel:18005990019"
                className="p-3 bg-white rounded-2xl border border-red-200 text-slate-900 flex items-center justify-between hover:bg-red-50/50 transition-colors"
              >
                <div>
                  <div className="font-bold text-xs sm:text-sm">KIRAN Mental Health Helpline</div>
                  <div className="text-[11px] text-red-600 font-semibold">Toll-Free 24x7: 1800-599-0019</div>
                </div>
                <PhoneCall className="w-4 h-4 text-red-600" />
              </a>
            </div>
          </div>
        )}

        {/* Central Drishti Core Voice Stage */}
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/90 shadow-xs flex flex-col items-center justify-center text-center space-y-4">
          <DrishtiCoreOrb
            state={coreState}
            isListening={coreState === 'LISTENING'}
            onClick={handleToggleVoice}
            size="large"
            ariaLabel="Drishti Voice Companion Core. Tap to speak with Drishti, or say 'Hey Drishti'."
          />

          <div className="space-y-1">
            <h2 className="text-lg font-bold text-slate-900">
              {coreState === 'LISTENING'
                ? 'I am listening to you...'
                : coreState === 'PROCESSING'
                ? 'Reflecting and thinking...'
                : coreState === 'SPEAKING'
                ? 'Speaking with you...'
                : 'Tap the orb or speak to begin'}
            </h2>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              {coreState === 'LISTENING'
                ? activeTranscript || 'Speak naturally. I am listening attentively.'
                : "You can talk about your day, ask for relaxation, or just share what's on your mind."}
            </p>
          </div>

          {/* Quick Voice Interruption / Stop Talking Button */}
          {coreState === 'SPEAKING' && (
            <button
              onClick={handleInterrupt}
              className="px-5 py-2 rounded-2xl bg-red-100 hover:bg-red-200 text-red-700 font-bold text-xs flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <VolumeX className="w-4 h-4" />
              <span>Stop Talking (Interrupt)</span>
            </button>
          )}

          {/* Quick Supportive Prompt Pills */}
          <div className="flex flex-wrap items-center justify-center gap-2 pt-2 max-w-lg">
            {[
              { text: "Hey Drishti, I am feeling lonely", label: "I'm feeling lonely" },
              { text: "Tell me something positive", label: "Something positive" },
              { text: "Drishti, help me relax", label: "Help me relax" },
              { text: "Let's do a breathing exercise", label: "Breathing exercise" },
              { text: "How can I reflect on my day?", label: "Reflect on my day" },
            ].map((prompt, idx) => (
              <button
                key={idx}
                onClick={() => handleUserMessage(prompt.text)}
                className="px-3.5 py-1.5 rounded-full bg-slate-100 hover:bg-blue-50 hover:text-blue-600 text-slate-700 text-xs font-semibold border border-slate-200/60 transition-all cursor-pointer"
              >
                {prompt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Conversation Transcript Stream */}
        <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/90 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-blue-600" />
              <span>Conversation Transcript</span>
            </h3>

            <div className="flex items-center gap-3">
              <label className="text-xs text-slate-500 flex items-center gap-1.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={saveHistoryConsent}
                  onChange={(e) => setSaveHistoryConsent(e.target.checked)}
                  className="rounded text-blue-600 focus:ring-blue-500"
                />
                <span>Save History</span>
              </label>

              {messages.length > 0 && (
                <button
                  onClick={handleClearHistory}
                  aria-label="Clear conversation transcript"
                  className="text-xs text-slate-400 hover:text-red-600 flex items-center gap-1 transition-colors cursor-pointer"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Clear</span>
                </button>
              )}
            </div>
          </div>

          <div
            tabIndex={0}
            role="region"
            aria-label="Conversation history"
            className="space-y-3 max-h-96 overflow-y-auto pr-1 focus-visible:outline-none"
          >
            {messages.map((msg) => {
              const isUser = msg.sender === 'user';
              return (
                <div
                  key={msg.id}
                  className={`flex flex-col ${isUser ? 'items-end' : 'items-start'}`}
                >
                  <div
                    className={`max-w-[85%] rounded-3xl p-4 text-sm leading-relaxed ${
                      isUser
                        ? 'bg-blue-600 text-white rounded-br-xs shadow-xs font-medium'
                        : msg.isCrisis
                        ? 'bg-red-50 border-2 border-red-300 text-red-950 rounded-bl-xs'
                        : 'bg-slate-100 text-slate-900 rounded-bl-xs'
                    }`}
                  >
                    <p>{msg.text}</p>

                    {!isUser && (
                      <div className="mt-2 pt-2 border-t border-black/10 flex items-center gap-2">
                        <button
                          onClick={() => speakResponse(msg.text)}
                          className="text-[11px] font-bold text-blue-600 hover:underline flex items-center gap-1"
                        >
                          <Volume2 className="w-3 h-3" />
                          <span>Hear again</span>
                        </button>
                      </div>
                    )}
                  </div>
                  <span className="text-[10px] text-slate-400 mt-1 px-1">{msg.timestamp}</span>
                </div>
              );
            })}
            <div ref={chatBottomRef} />
          </div>
        </div>

        {/* Mental Health Professional Care Disclaimer */}
        <div className="p-4 rounded-2xl bg-slate-100/80 border border-slate-200 text-slate-600 text-xs flex items-start gap-2.5">
          <Info className="w-4 h-4 text-slate-500 shrink-0 mt-0.5" />
          <p>
            <strong>Accessibility Notice:</strong> Drishti Voice Companion provides emotional support, mindfulness exercises, and compassionate listening. It is not a licensed physician, clinical psychologist, or medical therapy service. For urgent medical emergencies or severe distress, please dial 112 or Tele-MANAS at 14416.
          </p>
        </div>
      </div>

      {/* Breathing Exercise Modal */}
      <BreathingExerciseModal
        isOpen={isBreathingModalOpen}
        onClose={() => setIsBreathingModalOpen(false)}
        language={language}
      />
    </div>
  );
};

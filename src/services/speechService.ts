import { AppLanguage } from '../types';

export class SpeechService {
  private synth: SpeechSynthesis | null = null;
  private currentSessionId: number = 0;
  private isActivelySpeaking: boolean = false;
  private activeUtterances: SpeechSynthesisUtterance[] = [];
  private speechCooldownTimer: any = null;
  private chunkWatchdogTimer: any = null;

  private recognition: any = null;
  private isListening: boolean = false;

  private wakeRecognition: any = null;
  private isWakeListening: boolean = false;
  private wakeRestartTimer: any = null;
  private onWakeWordDetectedCallback: ((remainingCommand: string, fullTranscript: string) => void) | null = null;
  private wakeLanguage: AppLanguage = 'en';

  private audioContext: AudioContext | null = null;
  private cachedVoices: SpeechSynthesisVoice[] = [];
  private isAudioUnlocked: boolean = false;

  constructor() {
    if (typeof window !== 'undefined') {
      if ('speechSynthesis' in window) {
        this.synth = window.speechSynthesis;
        this.updateVoices();
        if (this.synth) {
          this.synth.onvoiceschanged = () => this.updateVoices();
        }
      }
    }
  }

  private updateVoices(): void {
    if (!this.synth) return;
    try {
      const v = this.synth.getVoices();
      if (v && v.length > 0) {
        this.cachedVoices = v;
      }
    } catch (_) {}
  }

  // Unlock browser audio context and speech synthesis on user interaction
  public unlockAudio(): void {
    if (this.audioContext && this.audioContext.state === 'suspended') {
      this.audioContext.resume().catch(() => {});
    }
    if (this.synth) {
      if (this.synth.paused) {
        try {
          this.synth.resume();
        } catch (_) {}
      }
      this.updateVoices();
      if (!this.isAudioUnlocked) {
        this.isAudioUnlocked = true;
        try {
          const primer = new SpeechSynthesisUtterance(' ');
          primer.volume = 0.001;
          this.synth.speak(primer);
        } catch (_) {}
      }
    }
  }

  private getAudioContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    if (!this.audioContext) {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (AudioCtx) {
        this.audioContext = new AudioCtx();
      }
    }
    if (this.audioContext && this.audioContext.state === 'suspended') {
      this.audioContext.resume().catch(() => {});
    }
    return this.audioContext;
  }

  // Synthesize accessible audio feedback tones using Web Audio API
  public playTone(type: 'listen_start' | 'listen_stop' | 'success' | 'alert' | 'processing' | 'error') {
    try {
      const ctx = this.getAudioContext();
      if (!ctx) return;

      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.connect(gain);
      gain.connect(ctx.destination);

      if (type === 'listen_start') {
        osc.type = 'sine';
        osc.frequency.setValueAtTime(440, now);
        osc.frequency.exponentialRampToValueAtTime(880, now + 0.12);
        gain.gain.setValueAtTime(0.18, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.16);
        osc.start(now);
        osc.stop(now + 0.16);
      } else if (type === 'listen_stop') {
        osc.type = 'sine';
        osc.frequency.setValueAtTime(660, now);
        osc.frequency.exponentialRampToValueAtTime(440, now + 0.1);
        gain.gain.setValueAtTime(0.15, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.14);
        osc.start(now);
        osc.stop(now + 0.14);
      } else if (type === 'success') {
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(523.25, now);
        osc.frequency.setValueAtTime(659.25, now + 0.08);
        osc.frequency.setValueAtTime(783.99, now + 0.16);
        gain.gain.setValueAtTime(0.2, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.4);
        osc.start(now);
        osc.stop(now + 0.4);
      } else if (type === 'alert') {
        osc.type = 'sine';
        osc.frequency.setValueAtTime(880, now);
        osc.frequency.setValueAtTime(1174.66, now + 0.15);
        osc.frequency.setValueAtTime(880, now + 0.3);
        gain.gain.setValueAtTime(0.28, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.55);
        osc.start(now);
        osc.stop(now + 0.55);
      } else if (type === 'processing') {
        osc.type = 'sine';
        osc.frequency.setValueAtTime(587.33, now);
        gain.gain.setValueAtTime(0.08, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.08);
        osc.start(now);
        osc.stop(now + 0.08);
      } else if (type === 'error') {
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(320, now);
        osc.frequency.exponentialRampToValueAtTime(160, now + 0.2);
        gain.gain.setValueAtTime(0.18, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
        osc.start(now);
        osc.stop(now + 0.25);
      }
    } catch (e) {
      console.warn('Audio tone synthesis bypassed:', e);
    }
  }

  /**
   * Group text into natural, coherent speech chunks (~120-220 chars).
   * This preserves full thoughts per chunk (like individual scheme descriptions)
   * while completely preventing Chrome's 15-second speech synthesis garbage collection timeout.
   */
  private splitIntoChunks(text: string, maxChunkLength: number = 220): string[] {
    const cleanText = text.replace(/<[^>]+>/g, '').trim();
    if (!cleanText) return [];

    // Split on sentence terminators: '.', '!', '?', '।', '\n'
    const sentenceRegex = /[^.!?।\n]+[.!?।\n]+|[^.!?।\n]+$/g;
    const rawSentences = cleanText.match(sentenceRegex) || [cleanText];

    const chunks: string[] = [];
    let currentChunk = '';

    for (const raw of rawSentences) {
      const sentence = raw.trim();
      if (!sentence) continue;

      if (!currentChunk) {
        currentChunk = sentence;
      } else if (currentChunk.length + sentence.length + 1 <= maxChunkLength) {
        currentChunk += ' ' + sentence;
      } else {
        chunks.push(currentChunk);
        currentChunk = sentence;
      }
    }

    if (currentChunk) {
      chunks.push(currentChunk);
    }

    return chunks.length > 0 ? chunks : [cleanText];
  }

  /**
   * Resilient voice selector with automatic Devanagari fallback.
   * On Windows/Android, Marathi (mr-IN) voice is almost never installed.
   * If mr-IN is passed without a voice, Chrome produces complete silence!
   * Falling back to Hindi (hi-IN) voice allows Devanagari script to be spoken audibly.
   */
  private getVoiceSelection(lang: AppLanguage): { voice: SpeechSynthesisVoice | null; langTag: string } {
    this.updateVoices();
    const voices = this.cachedVoices.length > 0 ? this.cachedVoices : (this.synth?.getVoices() || []);
    const targetTag = lang === 'hi' ? 'hi-IN' : lang === 'mr' ? 'mr-IN' : 'en-IN';

    if (!voices || voices.length === 0) {
      return { voice: null, langTag: lang === 'mr' ? 'hi-IN' : targetTag };
    }

    // 1. Exact match for target language tag
    let voice = voices.find(
      (v) => v.lang.replace('_', '-').toLowerCase() === targetTag.toLowerCase()
    );
    if (voice) {
      return { voice, langTag: voice.lang || targetTag };
    }

    // 2. Prefix match (e.g. 'hi', 'en', 'mr')
    voice = voices.find((v) => v.lang.toLowerCase().startsWith(lang));
    if (voice) {
      return { voice, langTag: voice.lang || targetTag };
    }

    // 3. For Marathi ('mr'), fallback to Hindi ('hi') voice (Devanagari pronunciation)
    if (lang === 'mr') {
      const hindiVoice = voices.find((v) => v.lang.toLowerCase().startsWith('hi'));
      if (hindiVoice) {
        return { voice: hindiVoice, langTag: hindiVoice.lang || 'hi-IN' };
      }
    }

    // 4. Indian English voice fallback
    const indianVoice = voices.find((v) => v.lang.replace('_', '-').toLowerCase() === 'en-in');
    if (indianVoice) {
      return { voice: indianVoice, langTag: indianVoice.lang || 'en-IN' };
    }

    // 5. Any English voice
    const englishVoice = voices.find((v) => v.lang.toLowerCase().startsWith('en'));
    if (englishVoice) {
      return { voice: englishVoice, langTag: englishVoice.lang || 'en-US' };
    }

    // 6. Default system voice
    const defaultVoice = voices.find((v) => v.default) || voices[0] || null;
    return { voice: defaultVoice, langTag: defaultVoice?.lang || (lang === 'mr' ? 'hi-IN' : targetTag) };
  }

  /**
   * Robust, uninterrupted Text-To-Speech with sequential asynchronous chunking.
   * Guarantees complete continuous playback without stalling, mid-speech truncation,
   * or Chrome queue deadlock.
   */
  public speak(
    text: string,
    options?: {
      language?: AppLanguage;
      rate?: number;
      volume?: number;
      pitch?: number;
      onStart?: () => void;
      onEnd?: () => void;
      onError?: (err: any) => void;
    }
  ): void {
    if (!this.synth) {
      console.warn('Speech synthesis not available in this browser');
      options?.onEnd?.();
      return;
    }

    const wasSpeaking = this.isSpeaking();

    // Cancel any previous speech session
    this.stop(false);

    if (!text || text.trim() === '') {
      options?.onEnd?.();
      return;
    }

    // Split text into coherent chunks
    const chunks = this.splitIntoChunks(text, 220);
    if (chunks.length === 0) {
      options?.onEnd?.();
      return;
    }

    // Increment session ID to discard superseded sessions
    this.currentSessionId++;
    const sessionId = this.currentSessionId;

    // Immediately mark active speaking and pause wake word listening so
    // microphone does NOT pick up speaker sound and self-interrupt!
    this.isActivelySpeaking = true;
    this.temporarilySuspendWakeWord();

    const lang = options?.language || 'en';
    const { voice: selectedVoice, langTag: resolvedLangTag } = this.getVoiceSelection(lang);

    let currentIndex = 0;
    let hasStartedFired = false;

    const clearChunkWatchdog = () => {
      if (this.chunkWatchdogTimer) {
        clearTimeout(this.chunkWatchdogTimer);
        this.chunkWatchdogTimer = null;
      }
    };

    const playNextChunk = () => {
      clearChunkWatchdog();
      if (this.currentSessionId !== sessionId) return;

      if (currentIndex >= chunks.length) {
        // All chunks finished speaking!
        this.activeUtterances = [];

        // Allow 350ms acoustic cooldown so audio hardware reverb clears
        clearTimeout(this.speechCooldownTimer);
        this.speechCooldownTimer = setTimeout(() => {
          if (this.currentSessionId === sessionId) {
            this.isActivelySpeaking = false;
            this.resumeWakeWordIfConfigured();
            options?.onEnd?.();
          }
        }, 350);
        return;
      }

      const chunkText = chunks[currentIndex];
      const utterance = new SpeechSynthesisUtterance(chunkText);
      utterance.lang = resolvedLangTag;
      utterance.rate = options?.rate || 0.95;
      utterance.volume = options?.volume !== undefined ? options.volume : 1.0;
      utterance.pitch = options?.pitch || 1.0;
      if (selectedVoice) {
        utterance.voice = selectedVoice;
      }

      // Keep utterance in memory array so V8 GC cannot garbage-collect it
      this.activeUtterances = [utterance];

      utterance.onstart = () => {
        if (this.currentSessionId !== sessionId) return;
        if (!hasStartedFired) {
          hasStartedFired = true;
          options?.onStart?.();
        }
      };

      let chunkHandled = false;
      const advance = () => {
        if (chunkHandled) return;
        chunkHandled = true;
        clearChunkWatchdog();

        if (this.currentSessionId !== sessionId) return;

        currentIndex++;
        // CRITICAL FIX: Asynchronous 60ms delay between chunks allows Chromium's
        // native C++ speech engine to cleanly tear down the finished utterance
        // before starting the next one. Calling speak() synchronously inside onend
        // causes Chrome to hang or ignore subsequent chunks.
        setTimeout(() => {
          if (this.currentSessionId === sessionId) {
            playNextChunk();
          }
        }, 60);
      };

      utterance.onend = () => {
        advance();
      };

      utterance.onerror = (e: any) => {
        console.warn(`Speech chunk ${currentIndex} notification:`, e?.error);
        if (this.currentSessionId !== sessionId) return;
        advance();
      };

      // Watchdog timer: In case browser drops the onend event, automatically advance
      const expectedDurationMs = Math.max(5000, chunkText.length * 150);
      this.chunkWatchdogTimer = setTimeout(() => {
        if (this.currentSessionId === sessionId && !chunkHandled) {
          console.warn(`Watchdog advancing speech chunk ${currentIndex}`);
          advance();
        }
      }, expectedDurationMs);

      try {
        if (this.synth?.paused) {
          this.synth.resume();
        }
        this.synth?.speak(utterance);
      } catch (err) {
        console.error('TTS speak invocation error:', err);
        advance();
      }
    };

    // If synth was previously speaking, wait 50ms for asynchronous cancel to clear native HAL
    if (wasSpeaking) {
      setTimeout(() => {
        if (this.currentSessionId === sessionId) {
          playNextChunk();
        }
      }, 50);
    } else {
      playNextChunk();
    }
  }

  // Interrupt and immediately cancel speech
  public stop(stopWakeListeningPermanent: boolean = false): void {
    this.currentSessionId++;
    if (this.chunkWatchdogTimer) {
      clearTimeout(this.chunkWatchdogTimer);
      this.chunkWatchdogTimer = null;
    }
    clearTimeout(this.speechCooldownTimer);
    this.activeUtterances = [];
    this.isActivelySpeaking = false;

    if (this.synth) {
      try {
        this.synth.cancel();
      } catch (_) {}
    }

    if (stopWakeListeningPermanent) {
      this.stopWakeWordListener(true);
    }
  }

  public isSpeaking(): boolean {
    return this.isActivelySpeaking || !!(this.synth && (this.synth.speaking || this.synth.pending));
  }

  // ---------------------------------------------------------
  // Wake Word ("Hey Drishti") Background Detection
  // ---------------------------------------------------------

  private temporarilySuspendWakeWord(): void {
    clearTimeout(this.wakeRestartTimer);
    if (this.wakeRecognition) {
      try {
        this.wakeRecognition.abort();
      } catch (_) {}
    }
  }

  private resumeWakeWordIfConfigured(): void {
    if (this.isWakeListening && !this.isListening && !this.isSpeaking()) {
      clearTimeout(this.wakeRestartTimer);
      this.wakeRestartTimer = setTimeout(() => {
        if (this.isWakeListening && !this.isListening && !this.isSpeaking()) {
          try {
            this.wakeRecognition?.start();
            console.log('[Drishti Wake] Resumed wake listener after speech/listening ended');
          } catch (e) {
            console.warn('[Drishti Wake] Resume failed:', e);
            // Retry once more after a longer delay
            clearTimeout(this.wakeRestartTimer);
            this.wakeRestartTimer = setTimeout(() => {
              if (this.isWakeListening && !this.isListening && !this.isSpeaking()) {
                try {
                  this.wakeRecognition?.start();
                } catch (_) {}
              }
            }, 1500);
          }
        }
      }, 700);
    }
  }

  /**
   * Evaluates incoming recognition transcripts for "Hey Drishti" wake phrases.
   * Protects against partial words (e.g. "दृष्टिहीन" meaning blind) to prevent false-triggers.
   */
  private checkWakeMatch(transcript: string): { matched: boolean; remainingCommand: string } {
    const lower = transcript.toLowerCase().trim();

    // 1. Explicit multi-word wake phrases (highest confidence)
    const phrasePatterns = [
      'hey drishti',
      'hey dristi',
      'hey drishty',
      'hey drashti',
      'हे दृष्टि',
      'हे दृष्टी',
      'सुनो दृष्टि',
      'सुनो दृष्टी',
      'ऐका दृष्टी',
      'ऐका दृष्टि',
      'हॅलो दृष्टी',
      'नमस्ते दृष्टि',
    ];

    for (const phrase of phrasePatterns) {
      const idx = lower.indexOf(phrase);
      if (idx !== -1) {
        return {
          matched: true,
          remainingCommand: transcript.slice(idx + phrase.length).trim(),
        };
      }
    }

    // 2. Standalone English word "drishti" or "dristi" with word boundaries
    const englishWordMatch = lower.match(/\b(drishti|dristi|drishty|drashti)\b/i);
    if (englishWordMatch && englishWordMatch.index !== undefined) {
      return {
        matched: true,
        remainingCommand: transcript.slice(englishWordMatch.index + englishWordMatch[0].length).trim(),
      };
    }

    // 3. Standalone Devanagari "दृष्टि" or "दृष्टी"
    // Negative lookahead (?!(?:हीन|बाधित|कोण|गोचर|दोष)) prevents triggering on words like "दृष्टिहीन" (visually impaired)
    const devanagariMatch = lower.match(/(?:^|\s)(दृष्टि|दृष्टी)(?!(?:हीन|बाधित|कोण|गोचर|दोष))(?:\s|$|[।,?!])/);
    if (devanagariMatch && devanagariMatch.index !== undefined) {
      const matchWord = devanagariMatch[1];
      const matchPos = lower.indexOf(matchWord);
      return {
        matched: true,
        remainingCommand: transcript.slice(matchPos + matchWord.length).trim(),
      };
    }

    return { matched: false, remainingCommand: '' };
  }

  public startWakeWordListener(options: {
    language?: AppLanguage;
    onWake: (remainingCommand: string, fullTranscript: string) => void;
    onError?: (err: any) => void;
  }): boolean {
    if (typeof window === 'undefined') return false;

    const SpeechRec = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRec) return false;

    this.onWakeWordDetectedCallback = options.onWake;
    this.wakeLanguage = options.language || 'en';
    this.isWakeListening = true;

    try {
      this.temporarilySuspendWakeWord();

      const recognition = new SpeechRec();
      this.wakeRecognition = recognition;

      const langTag = this.wakeLanguage === 'hi' ? 'hi-IN' : this.wakeLanguage === 'mr' ? 'mr-IN' : 'en-IN';
      recognition.lang = langTag;
      recognition.interimResults = true;
      recognition.continuous = true;
      recognition.maxAlternatives = 1;

      recognition.onresult = (event: any) => {
        // Drop any microphone results if speech synthesis is playing to prevent acoustic feedback loop!
        if (this.isListening || this.isSpeaking() || this.isActivelySpeaking) {
          return;
        }

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          const transcript = event.results[i][0].transcript;
          const matchResult = this.checkWakeMatch(transcript);

          if (matchResult.matched) {
            console.log('[Drishti Wake] Wake word detected!', transcript);
            // Wake word matched! Temporarily halt wake listening
            this.temporarilySuspendWakeWord();
            if (this.onWakeWordDetectedCallback) {
              this.onWakeWordDetectedCallback(matchResult.remainingCommand, transcript);
            }
            break;
          }
        }
      };

      recognition.onerror = (e: any) => {
        if (e.error === 'not-allowed' || e.error === 'service-not-allowed') {
          this.isWakeListening = false;
          options.onError?.(e);
        }
        // For transient errors (no-speech, network, aborted, audio-capture),
        // let onend handle the auto-restart instead of silently dying
      };

      recognition.onend = () => {
        // Auto-restart wake listener if user enabled it and we're not speaking/listening
        if (this.isWakeListening && !this.isListening && !this.isSpeaking() && !this.isActivelySpeaking) {
          clearTimeout(this.wakeRestartTimer);
          this.wakeRestartTimer = setTimeout(() => {
            if (this.isWakeListening && !this.isListening && !this.isSpeaking() && !this.isActivelySpeaking) {
              try {
                this.wakeRecognition?.start();
                console.log('[Drishti Wake] Auto-restarted wake listener');
              } catch (restartErr) {
                // Recognition may already be started or in a bad state — recreate after a delay
                console.warn('[Drishti Wake] Restart failed, will retry:', restartErr);
                clearTimeout(this.wakeRestartTimer);
                this.wakeRestartTimer = setTimeout(() => {
                  if (this.isWakeListening && !this.isListening && !this.isSpeaking()) {
                    try {
                      this.wakeRecognition?.start();
                    } catch (_) {}
                  }
                }, 1500);
              }
            }
          }, 600);
        }
      };

      if (!this.isSpeaking() && !this.isActivelySpeaking) {
        recognition.start();
      }
      return true;
    } catch (e) {
      this.isWakeListening = false;
      return false;
    }
  }

  public stopWakeWordListener(permanent: boolean = true): void {
    if (permanent) {
      this.isWakeListening = false;
    }
    clearTimeout(this.wakeRestartTimer);
    if (this.wakeRecognition) {
      try {
        this.wakeRecognition.abort();
      } catch (_) {}
      this.wakeRecognition = null;
    }
  }

  public isWakeWordActive(): boolean {
    return this.isWakeListening;
  }

  // Check and proactively request browser microphone permission
  public async requestMicPermission(): Promise<{ granted: boolean; error?: string }> {
    if (typeof window === 'undefined') return { granted: true };

    if (!navigator.mediaDevices?.getUserMedia) {
      return { granted: true };
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach((track) => track.stop());
      return { granted: true };
    } catch (e: any) {
      console.warn('Microphone permission check failed:', e);
      let error = 'Microphone permission blocked. Please allow microphone access in your browser settings.';
      if (e.name === 'NotAllowedError' || e.name === 'PermissionDeniedError') {
        error = 'Microphone blocked. Click the lock/camera icon next to the address bar to allow microphone.';
      } else if (e.name === 'NotFoundError' || e.name === 'DevicesNotFoundError') {
        error = 'No microphone device found on this computer. Please connect a microphone.';
      }
      return { granted: false, error };
    }
  }

  public startListening(options: {
    language?: AppLanguage;
    onResult: (transcript: string, isFinal: boolean) => void;
    onError: (error: string) => void;
    onEnd: () => void;
    continuous?: boolean;
  }): boolean {
    if (typeof window === 'undefined') return false;

    // Immediately stop wake recognition while active voice recognition is running
    this.temporarilySuspendWakeWord();

    // If currently speaking, stop synthesis cleanly before listening
    if (this.isSpeaking()) {
      this.stop(false);
    }

    const SpeechRec = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRec) {
      options.onError('Speech recognition is not supported in this browser. Please use Chrome or Edge.');
      return false;
    }

    try {
      this.stopListening();

      const recognition = new SpeechRec();
      this.recognition = recognition;
      this.isListening = true;

      const lang = options.language || 'en';
      recognition.lang = lang === 'hi' ? 'hi-IN' : lang === 'mr' ? 'mr-IN' : 'en-IN';
      recognition.interimResults = true;
      recognition.maxAlternatives = 1;
      recognition.continuous = options.continuous ?? false;

      recognition.onstart = () => {
        this.playTone('listen_start');
      };

      recognition.onresult = (event: any) => {
        // If synthesis became active, discard microphone audio
        if (this.isSpeaking() || this.isActivelySpeaking) {
          return;
        }

        let interimTranscript = '';
        let finalTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          const item = event.results[i];
          if (item.isFinal) {
            finalTranscript += item[0].transcript;
          } else {
            interimTranscript += item[0].transcript;
          }
        }

        const text = finalTranscript || interimTranscript;
        if (text) {
          options.onResult(text, !!finalTranscript);
        }
      };

      recognition.onerror = (event: any) => {
        this.isListening = false;
        // Ignore deliberate aborts
        if (event.error === 'aborted') {
          return;
        }

        let errMsg = 'Microphone or speech recognition error.';
        if (event.error === 'not-allowed') {
          errMsg = 'Microphone access blocked. Click the lock/tune icon in your address bar to allow microphone.';
        } else if (event.error === 'service-not-allowed') {
          errMsg = 'Speech service blocked. In Brave browser, enable Google Services in settings, or use Chrome/Edge.';
        } else if (event.error === 'network') {
          errMsg = 'Voice recognition network error. Please check your internet connection and try again.';
        } else if (event.error === 'no-speech') {
          errMsg = 'No speech detected. Please speak closer to the microphone and try again.';
        } else if (event.error === 'audio-capture') {
          errMsg = 'No microphone detected. Please check that your microphone is plugged in and enabled.';
        } else if (event.error === 'language-not-supported') {
          errMsg = 'Language not supported for speech recognition in this browser. Please try English or Hindi.';
        }

        this.playTone('error');
        options.onError(errMsg);
      };

      recognition.onend = () => {
        this.isListening = false;
        options.onEnd();
        this.resumeWakeWordIfConfigured();
      };

      recognition.start();
      return true;
    } catch (e: any) {
      this.isListening = false;
      options.onError(e.message || 'Unable to start speech recognition.');
      this.resumeWakeWordIfConfigured();
      return false;
    }
  }

  public stopListening(): void {
    if (this.recognition) {
      try {
        this.recognition.abort();
      } catch (_) {}
      this.recognition = null;
    }
    this.isListening = false;
  }

  public isCurrentlyListening(): boolean {
    return this.isListening;
  }
}

export const speechService = new SpeechService();

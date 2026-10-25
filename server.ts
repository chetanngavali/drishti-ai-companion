import express from "express";
import { GoogleGenAI, Type } from "@google/genai";
import dotenv from "dotenv";
import path from "path";
import { fileURLToPath } from "url";

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());

// Initialize Gemini Client
const ai = process.env.GEMINI_API_KEY
  ? new GoogleGenAI({
      apiKey: process.env.GEMINI_API_KEY,
      httpOptions: {
        headers: {
          "User-Agent": "aistudio-build",
        },
      },
    })
  : null;

// Helper function to call Gemini with automatic fallback on 503 high demand
async function generateWithFallback(params: {
  contents: any;
  config?: any;
}) {
  if (!ai) return null;
  // gemini-3.1-flash-lite offers sub-second response times and zero 503 spike downtime
  const models = ["gemini-3.1-flash-lite", "gemini-3.8-flash"];
  for (const model of models) {
    try {
      const response = await ai.models.generateContent({
        model,
        contents: params.contents,
        config: params.config,
      });
      return response;
    } catch (err: any) {
      console.warn(`Model ${model} request failed:`, err?.message || err);
      // If 503 or unavailable, continue to next model
    }
  }
  return null;
}

// Health check for Vercel deployment testing
app.get(["/api/health", "/health", "/api"], (_req, res) => {
  res.json({
    status: "ok",
    app: "Drishti",
    geminiConfigured: !!ai,
    timestamp: new Date().toISOString(),
  });
});

// ==========================================
// 1. DRISHTI VOICE COMPANION — MENTAL WELLNESS
// ==========================================
app.post(["/api/companion/chat", "/companion/chat"], async (req, res) => {
  try {
    const { message, language = "en", history = [] } = req.body;

    if (!message || typeof message !== "string") {
      return res.status(400).json({ error: "Message is required." });
    }

    const langName =
      language === "hi" ? "Hindi (हिन्दी)" : language === "mr" ? "Marathi (मराठी)" : "English";

    // Immediate Crisis Check (Mental Health Safety)
    const crisisPatterns = [
      /suicide/i,
      /kill myself/i,
      /want to die/i,
      /end my life/i,
      /hurt myself/i,
      /आत्महत्या/i,
      /मरना चाहता/i,
      /स्वतःला संपवा/i,
      /मरण्याची इच्छा/i,
    ];
    const isCrisis = crisisPatterns.some((pattern) => pattern.test(message));

    if (isCrisis) {
      let crisisResponse = "";
      if (language === "hi") {
        crisisResponse =
          "मुझे आपकी बहुत चिंता है और आपकी सुरक्षा सबसे महत्वपूर्ण है। कृपया अकेले न रहें। आप तुरंत राष्ट्रीय मानसिक स्वास्थ्य हेल्पलाइन 'टेली-मानस' पर 14416 या 1800-891-4416 पर निःशुल्क कॉल करें, अथवा किरण हेल्पलाइन 1800-599-0019 या आपातकालीन नंबर 112 पर संपर्क करें। हम आपके साथ हैं।";
      } else if (language === "mr") {
        crisisResponse =
          "मला तुमची काळजी वाटते आणि तुमची सुरक्षितता सर्वात महत्त्वाची आहे. कृपया एकटे राहू नका. तुम्ही लगेच राष्ट्रीय मानसिक आरोग्य हेल्पलाइन 'टेली-मानस' 14416 किंवा 1800-891-4416 वर विनामूल्य कॉल करा, किंवा किरण हेल्पलाइन 1800-599-0019 किंवा 112 वर संपर्क साधा. आम्ही तुमच्या सोबत आहोत.";
      } else {
        crisisResponse =
          "I hear how much pain you are in right now, and your safety is deeply important. Please reach out to someone who can help you right now. You can call India's Tele-MANAS toll-free mental health helpline at 14416 or 1800-891-4416, the KIRAN helpline at 1800-599-0019, or Emergency 112 immediately. Please stay safe, you matter.";
      }
      return res.json({
        reply: crisisResponse,
        isCrisis: true,
        action: "crisis_alert",
      });
    }

    if (!ai) {
      // Fallback response when Gemini API key is not active
      const fallbackReplies: Record<string, string[]> = {
        en: [
          "I'm right here with you. Take a soft, gentle breath. Tell me what has been on your mind today.",
          "I hear you, and it is completely okay to feel this way. Would you like to do a calm 4-second breathing exercise together?",
          "You are not alone. Drishti is listening. How can I support you right this moment?",
        ],
        hi: [
          "मैं यहीं आपके साथ हूँ। एक गहरी और शांत साँस लें। आज आपके मन में क्या चल रहा है, मुझे बताएँ।",
          "मैं समझ सकता हूँ। इस तरह महसूस होना स्वाभाविक है। क्या आप मेरे साथ एक शांत श्वास व्यायाम करना चाहेंगे?",
          "आप अकेले नहीं हैं। दृष्टि आपके साथ है। इस पल मैं आपकी किस प्रकार मदद कर सकता हूँ?",
        ],
        mr: [
          "मी तुमच्या सोबतच आहे. एक दीर्घ आणि शांत श्वास घ्या. आज तुमच्या मनात काय विचार सुरू आहेत, मला सांगा.",
          "मी समजू शकतो. असे वाटणे अगदी स्वाभाविक आहे. आपण मिळून एक शांत श्वासोच्छ्वास व्यायाम करायचा का?",
          "तुम्ही एकटे नाही आहात. दृष्टी तुमच्या सोबत आहे. या क्षणी मी तुम्हाला कशी मदत करू शकतो?",
        ],
      };
      const list = fallbackReplies[language] || fallbackReplies.en;
      return res.json({
        reply: list[Math.floor(Math.random() * list.length)],
        isCrisis: false,
      });
    }

    const systemInstruction = `You are "Drishti Voice Companion", a warm, empathetic, respectful, and supportive conversational companion integrated into the Drishti accessibility app for visually impaired people in India.

Tone and Character:
- Natural, gentle, reassuring, and caring. Speak like a trusted companion speaking aloud to someone who cannot see the screen.
- Keep responses concise (1 to 3 short spoken sentences, max 50 words) so they are easy to listen to via text-to-speech without fatigue.
- Ask at most one single open question at a time and give the user room to reply.
- Never judge, dismiss, or patronize. Never claim to have physical human feelings or pretend to be a licensed doctor or clinical therapist.
- If the user says they are lonely, sad, tired, or anxious: validate their feelings with warmth and offer gentle encouragement.
- If the user requests a breathing exercise or relaxation: guide them into an easy 4-second breathing rhythm (breathe in for 4, hold for 4, breathe out for 4).
- If the user says something positive: celebrate with them genuinely.
- Always output your response in ${langName}. Use natural, everyday phrasing that sounds soothing when read aloud.`;

    const chatContents = history
      .slice(-6)
      .map((item: { role: string; text: string }) => ({
        role: item.role === "user" ? "user" : "model",
        parts: [{ text: item.text }],
      }));

    chatContents.push({
      role: "user",
      parts: [{ text: message }],
    });

    const response = await generateWithFallback({
      contents: chatContents,
      config: {
        systemInstruction,
        temperature: 0.7,
      },
    });

    const reply =
      response?.text?.trim() ||
      (language === "hi"
        ? "मैं यहीं आपके साथ हूँ। आज आपका दिन कैसा रहा?"
        : language === "mr"
        ? "मी तुमच्या सोबतच आहे. आजचा दिवस कसा गेला?"
        : "I'm right here with you. How are you feeling right now?");
    return res.json({ reply, isCrisis: false });
  } catch (err: any) {
    console.error("Companion chat error:", err);
    return res.status(500).json({
      error: "Unable to process conversation.",
      reply: "I am right here with you. Let's take a calm breath together. Could you please say that again?",
    });
  }
});

// ==========================================
// 2. "HEY DRISHTI SCHEDULE" — INTENT & DETAILS PARSER
// ==========================================
app.post(["/api/schedule/parse", "/schedule/parse"], async (req, res) => {
  try {
    const { transcript, currentTimeIso = new Date().toISOString(), language = "en" } = req.body;

    if (!transcript || typeof transcript !== "string") {
      return res.status(400).json({ error: "Transcript is required." });
    }

    if (!ai) {
      // Heuristic fallback parser
      const lower = transcript.toLowerCase();
      let action: "create" | "query" | "cancel" | "reschedule" | "complete" = "create";
      let category: "medication" | "doctor_appointment" | "medical_test" | "health_routine" | "personal" = "personal";

      if (lower.includes("what") || lower.includes("tell") || lower.includes("read") || lower.includes("check") || lower.includes("today") || lower.includes("schedule today") || lower.includes("remind me what") || lower.includes("कया है") || lower.includes("काय आहे")) {
        action = "query";
      } else if (lower.includes("cancel") || lower.includes("delete") || lower.includes("रद्द")) {
        action = "cancel";
      } else if (lower.includes("reschedule") || lower.includes("change time") || lower.includes("बदला")) {
        action = "reschedule";
      }

      if (lower.includes("medicine") || lower.includes("tablet") || lower.includes("pill") || lower.includes("दवा") || lower.includes("औषध")) {
        category = "medication";
      } else if (lower.includes("doctor") || lower.includes("appointment") || lower.includes("डॉक्टर") || lower.includes("भेट")) {
        category = "doctor_appointment";
      } else if (lower.includes("test") || lower.includes("checkup") || lower.includes("check-up") || lower.includes("तपासणी") || lower.includes("जांच")) {
        category = "medical_test";
      } else if (lower.includes("walk") || lower.includes("water") || lower.includes("exercise") || lower.includes("व्यायाम")) {
        category = "health_routine";
      }

      return res.json({
        action,
        title: transcript.replace(/hey drishti schedule/i, "").trim() || "Reminder",
        category,
        date: new Date().toISOString().split("T")[0],
        time: "08:00",
        repeat: lower.includes("every day") || lower.includes("daily") || lower.includes("every morning") ? "daily" : "none",
        confirmationRequired: action === "create",
        spokenPrompt: action === "create"
          ? `You want to set a reminder: "${transcript}". Should I save this for you?`
          : "Checking your schedule now.",
      });
    }

    const systemInstruction = `You are the Schedule & Medical Reminder Assistant for Drishti, an accessibility application for visually impaired people.
Current reference time (ISO): ${currentTimeIso}.
Language: ${language}.

Your job is to parse the user's spoken voice command following the activation "Hey Drishti Schedule" (or any scheduling request).
Interpret the requested action:
- "create": user wants to add a reminder or schedule an event (e.g. "remind me to take my medicine at 8 PM", "doctor appointment tomorrow at 11 AM").
- "query": user is asking what is on their schedule (e.g. "what is on my schedule today?", "read my medical reminders", "what appointments do I have tomorrow?").
- "cancel": user wants to cancel a reminder (e.g. "cancel my 8 PM medicine reminder").
- "reschedule": user wants to reschedule (e.g. "reschedule my appointment to Friday at 4 PM").
- "complete": user wants to mark an existing reminder as completed.

Categories:
- "medication": pills, eye drops, syrups, insulin, tablets.
- "doctor_appointment": visiting clinic, doctor consultation.
- "medical_test": blood test, eye checkup, scan, lab test.
- "health_routine": drinking water, eye exercise, walk, blood pressure check.
- "personal": other reminders.

Dates and Times:
- Compute actual future ISO date "YYYY-MM-DD" based on current reference time (${currentTimeIso}). "Tomorrow", "Next Monday", "Tonight", etc.
- Compute "HH:mm" in 24-hour format (e.g., 8 PM is "20:00", 8 AM is "08:00", 11 AM is "11:00"). If ambiguous or missing, default sensibly and mark confirmationRequired = true.
- Identify repeat: "none", "daily", "weekly", "weekdays".
- Produce a clear, spoken confirmation prompt that can be read aloud to a blind user to verify the interpreted details before saving.`;

    const response = await generateWithFallback({
      contents: `User voice input: "${transcript}"`,
      config: {
        systemInstruction,
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            action: {
              type: Type.STRING,
              description: "create | query | cancel | reschedule | complete",
            },
            title: {
              type: Type.STRING,
              description: "Clean title for the reminder/event without wake words",
            },
            category: {
              type: Type.STRING,
              description: "medication | doctor_appointment | medical_test | health_routine | personal",
            },
            date: {
              type: Type.STRING,
              description: "Target date in YYYY-MM-DD format",
            },
            time: {
              type: Type.STRING,
              description: "Target time in HH:mm 24-hour format",
            },
            repeat: {
              type: Type.STRING,
              description: "none | daily | weekly | weekdays",
            },
            notes: {
              type: Type.STRING,
              description: "Optional notes like dosage or directions",
            },
            confirmationRequired: {
              type: Type.BOOLEAN,
              description: "True if action modifies data and needs spoken confirmation",
            },
            spokenPrompt: {
              type: Type.STRING,
              description: "Spoken question or confirmation for the user (in natural conversational style)",
            },
          },
          required: ["action", "title", "category", "date", "time", "repeat", "confirmationRequired", "spokenPrompt"],
        },
      },
    });

    if (!response || !response.text) {
      // Heuristic fallback if model unavailable
      return res.json({
        action: "create",
        title: transcript.replace(/hey drishti schedule/i, "").trim() || "Reminder",
        category: "medication",
        date: new Date().toISOString().split("T")[0],
        time: "08:00",
        repeat: "daily",
        confirmationRequired: true,
        spokenPrompt: `You want to set a reminder: "${transcript}". Should I save it?`,
      });
    }

    const parsed = JSON.parse(response.text.trim());
    return res.json(parsed);
  } catch (err: any) {
    console.error("Schedule parser error:", err);
    return res.status(500).json({
      error: "Failed to parse schedule intent.",
      action: "query",
      spokenPrompt: "I had trouble understanding that schedule request. Could you please repeat the time and reminder?",
    });
  }
});

// ==========================================
// 3. GOVERNMENT SCHEMES — VOICE QUERY & MATCHING
// ==========================================
app.post(["/api/schemes/query", "/schemes/query"], async (req, res) => {
  try {
    const { query, language = "en" } = req.body;

    if (!query || typeof query !== "string") {
      return res.status(400).json({ error: "Query is required." });
    }

    if (!ai) {
      return res.json({
        answer: "There are key welfare schemes for visually impaired individuals such as the ADIP Scheme for free assistive devices, Swavalamban health insurance, National Scholarships on the National Scholarship Portal, and Sanjay Gandhi Niradhar Anudan in Maharashtra. Please browse the verified scheme cards below.",
        matchedCategories: ["central", "maharashtra", "scholarship", "assistive_devices"],
      });
    }

    const systemInstruction = `You are the Government Scheme Voice Advisor in the Drishti accessibility application.
You help visually impaired individuals in India understand genuine welfare schemes, scholarships, assistive aid programs, and pensions.

Key Verified Schemes Knowledge:
1. ADIP Scheme (Assistance to Disabled Persons for Purchase/Fitting of Aids and Appliances):
   - By Central Ministry of Social Justice & Empowerment (DEPwD / ALIMCO).
   - Free aids (smart canes, daisy players, screen reading laptops, braille kits) for individuals with 40%+ visual impairment and family income up to ₹30,000/month (100% subsidy) or ₹30,001-₹45,000 (50% subsidy).
2. Divyangjan Swavalamban Yojana / NHFDC:
   - Concessional loan schemes for self-employment & vocational education.
3. Pre-Matric & Post-Matric Scholarships for Students with Disabilities:
   - Administered via National Scholarship Portal (NSP, scholarships.gov.in) by DEPwD.
   - For students with 40%+ disability, income ceiling ₹2.5 Lakh/year.
4. National Fellowship for Persons with Disabilities (NFPwD):
   - M.Phil and Ph.D fellowship grants.
5. Unique Disability ID (UDID) Card (swavlambancard.gov.in):
   - Single national verified proof of disability for all benefits, bus/rail concessions.
6. Maharashtra State Government:
   - Sanjay Gandhi Niradhar Anudan Yojana (Divyang Pension): Monthly financial assistance for persons with blindness/disabilities.
   - Maharashtra State Disability Scholarship (MahaDBT): Educational allowance for visually impaired school & college students.
   - MSRTC Free Bus Concession: 100% fare concession for 100% blind persons, plus 50% for one companion.
   - Savitribai Phule Divyang Kanya Vivah: Marriage incentive grant.

Rules:
- NEVER fabricate schemes, deadlines, or false promises.
- Explain in simple, direct language suitable for text-to-speech.
- Keep the spoken response under 60 words.
- Answer in ${language === "hi" ? "Hindi" : language === "mr" ? "Marathi" : "English"}.`;

    const response = await generateWithFallback({
      contents: `User voice query: "${query}"`,
      config: {
        systemInstruction,
        temperature: 0.3,
      },
    });

    const answer =
      response?.text?.trim() ||
      "You can explore key verified schemes including the ADIP Scheme for free assistive devices, Swavalamban UDID card benefits, and Sanjay Gandhi Niradhar Anudan in Maharashtra.";
    return res.json({ answer });
  } catch (err: any) {
    console.error("Schemes query error:", err);
    return res.status(500).json({
      error: "Unable to query schemes.",
      answer: "You can explore the verified Central and Maharashtra government schemes listed in the cards below.",
    });
  }
});

// In dev mode (outside Vercel), mount Vite middleware; in prod, serve static dist
if (!process.env.VERCEL) {
  if (process.env.NODE_ENV !== "production") {
    const { createServer: createViteServer } = await import("vite");
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.resolve(__dirname, "dist");
    app.use(express.static(distPath));
    app.get("*", (_req, res) => {
      res.sendFile(path.resolve(distPath, "index.html"));
    });
  }

  app.listen(port, () => {
    console.log('\n========================================================');
    console.log('  🚀 Drishti is now RUNNING and READY!');
    console.log(`  👉 Open in your browser: http://localhost:${port}`);
    console.log('  (Keep this terminal open while using the app)');
    console.log('  (Press Ctrl + C whenever you want to stop the server)');
    console.log('========================================================\n');
  });
}

export default app;

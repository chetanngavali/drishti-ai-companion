import React from 'react';
import { Shield, Lock, Eye, Mic, MapPin, ArrowLeft } from 'lucide-react';

interface InfoScreenProps {
  type: 'safety' | 'privacy' | 'permissions';
  onBack: () => void;
}

export const InfoScreens: React.FC<InfoScreenProps> = ({ type, onBack }) => {
  return (
    <div className="w-full min-h-screen bg-slate-50/70 pb-28 pt-4 px-4 sm:px-6">
      <div className="max-w-2xl mx-auto space-y-6">
        <button
          onClick={onBack}
          className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1.5 cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Application</span>
        </button>

        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/90 shadow-xs space-y-5">
          {type === 'safety' && (
            <>
              <div className="flex items-center gap-2">
                <Shield className="w-6 h-6 text-blue-600" />
                <h1 className="text-2xl font-black text-slate-900">Safety &amp; Assistive-Use Notice</h1>
              </div>
              <div className="space-y-4 text-xs sm:text-sm text-slate-700 leading-relaxed">
                <p className="font-semibold text-slate-900">
                  DrishtiAI provides assistive environmental perception, navigation, and voice assistance. Detection and navigation may be affected by lighting, weather, camera limitations, sensor errors, connectivity, GPS accuracy, or environmental conditions.
                </p>
                <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 space-y-2">
                  <h4 className="font-bold">Key Environmental Limitations:</h4>
                  <ul className="list-disc pl-5 space-y-1">
                    <li><strong>Lighting &amp; Glare:</strong> Low light, heavy rain, or direct sunlight can degrade camera object detection.</li>
                    <li><strong>GPS &amp; Urban Canyons:</strong> GPS may be inaccurate or unavailable indoors and between high-rise buildings. Clock directions are advisory.</li>
                    <li><strong>AI Vision:</strong> Computer vision can occasionally misclassify or miss fast-moving obstacles. Always use your traditional cane and independent judgment for street crossing.</li>
                    <li><strong>Voice in Noise:</strong> Speech recognition may misunderstand phrases in crowded or noisy areas.</li>
                    <li><strong>Emergencies:</strong> Do not rely solely on mobile applications during critical emergencies; dial 112 directly.</li>
                  </ul>
                </div>
              </div>
            </>
          )}

          {type === 'privacy' && (
            <>
              <div className="flex items-center gap-2">
                <Lock className="w-6 h-6 text-blue-600" />
                <h1 className="text-2xl font-black text-slate-900">Privacy &amp; Data Policy</h1>
              </div>
              <div className="space-y-4 text-xs sm:text-sm text-slate-700 leading-relaxed">
                <p>
                  DrishtiAI is designed with strict data minimization principles and respects applicable Indian data privacy standards.
                </p>
                <div className="p-4 rounded-2xl bg-blue-50 border border-blue-200 text-blue-900 space-y-2">
                  <h4 className="font-bold">Local-First Sensor Processing:</h4>
                  <p>
                    Camera video frames for obstacle perception are processed exclusively in volatile device memory and are <strong>never stored, uploaded, or transmitted</strong> to any remote server.
                  </p>
                </div>
                <div className="space-y-2">
                  <h4 className="font-bold text-slate-900">User Rights &amp; Voice Privacy:</h4>
                  <p>
                    Conversations with the Drishti Voice Companion are stored locally on your device only with your explicit consent. You can clear your conversation history at any time with a single tap in the Voice Companion screen.
                  </p>
                  <p>
                    Medical reminders and calendar events are stored privately in your browser storage and are never sold or shared with third-party advertisers.
                  </p>
                </div>
              </div>
            </>
          )}

          {type === 'permissions' && (
            <>
              <div className="flex items-center gap-2">
                <Eye className="w-6 h-6 text-blue-600" />
                <h1 className="text-2xl font-black text-slate-900">Permission Center</h1>
              </div>
              <div className="space-y-4 text-xs sm:text-sm text-slate-700 leading-relaxed">
                <p>
                  DrishtiAI uses device hardware solely to deliver assistive features. Sensors are activated only during active user sessions:
                </p>
                <div className="space-y-3">
                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200">
                    <div className="flex items-center gap-2 font-bold text-slate-900 mb-1">
                      <Mic className="w-4 h-4 text-blue-600" /> Microphone
                    </div>
                    <p className="text-xs text-slate-600">
                      Used for voice companion conversations, "Hey Drishti Schedule" voice input, and wake-phrase listening. Audio is not recorded or sold.
                    </p>
                  </div>

                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200">
                    <div className="flex items-center gap-2 font-bold text-slate-900 mb-1">
                      <Eye className="w-4 h-4 text-blue-600" /> Camera
                    </div>
                    <p className="text-xs text-slate-600">
                      Used for live obstacle detection and scene description. Frames are processed in real-time in memory and never uploaded.
                    </p>
                  </div>

                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200">
                    <div className="flex items-center gap-2 font-bold text-slate-900 mb-1">
                      <MapPin className="w-4 h-4 text-blue-600" /> Location / GPS
                    </div>
                    <p className="text-xs text-slate-600">
                      Used for pedestrian turn directions, nearby transit discovery, and sharing emergency coordinates with guardians during SOS.
                    </p>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

package com.example.drishti

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.drishti.navigation.AppNavigation
import com.example.drishti.ui.theme.DrishtiTheme
import com.example.drishti.util.LocaleHelper
import com.example.drishti.voice.DrishtiWakeWordService

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val langCode = LocaleHelper.getSelectedLanguageCode(newBase)
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, langCode))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val langCode = LocaleHelper.getSelectedLanguageCode(this)
        LocaleHelper.applyLocale(this, langCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val langCode = LocaleHelper.getSelectedLanguageCode(this)
        LocaleHelper.applyLocale(this, langCode)
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        LocaleHelper.init(this)
        enableEdgeToEdge()
        handleIntent(intent)



        setContent {
            val context = LocalContext.current
            val langCode by LocaleHelper.appLanguageCode.collectAsState()
            val localizedContext = remember(langCode) {
                LocaleHelper.applyLocale(context, langCode)
            }
            val currentConfig = LocalConfiguration.current
            val localizedConfig = remember(langCode, currentConfig) {
                Configuration(currentConfig).apply {
                    val locale = LocaleHelper.toLocale(langCode)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        setLocales(LocaleList(locale))
                    } else {
                        @Suppress("DEPRECATION")
                        this.locale = locale
                    }
                    setLayoutDirection(locale)
                }
            }

            key(langCode) {
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides localizedConfig,
                    LocalActivityResultRegistryOwner provides this
                ) {
                    DrishtiTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        if (action != null && action in listOf(
                "START_MANUAL_COMMAND",
                "DISABLE_WAKEWORD",
                "ENABLE_WAKEWORD",
                "PAUSE_WAKEWORD",
                "RESUME_WAKEWORD",
                "SET_HANDS_FREE",
                "SET_PUSH_TO_TALK"
            )
        ) {
            val serviceIntent = Intent(this, DrishtiWakeWordService::class.java).apply {
                this.action = action
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    private var isHardwareScanning = false
    private var liveObstacleJob: kotlinx.coroutines.Job? = null

    /**
     * Starts background hardware and communication services ONLY after safety consent
     * has been recorded and required runtime permissions are verified.
     */
    fun maybeStartAuthorizedServices() {
        val prefs = getSharedPreferences("drishti_settings", Context.MODE_PRIVATE)
        val isSafetyNoticeAcknowledged = prefs.getBoolean("safety_notice_acknowledged_v1", false)
        val isPermissionOnboardingCompleted = prefs.getBoolean("permission_onboarding_completed_v1", false)

        // Strict guard: Do not start hardware access before consent or onboarding
        if (!isSafetyNoticeAcknowledged || !isPermissionOnboardingCompleted) {
            return
        }

        val appContext = applicationContext

        // Call Manager: only if READ_PHONE_STATE permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                com.example.drishti.call.DrishtiCallManager.getInstance(appContext)
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "CallManager init deferred: ${e.message}")
            }
        }

        // Capability detection query
        try {
            com.example.drishti.voice.AssistantCapabilityManager.getCapabilities(this)
        } catch (_: Exception) {}

        // BLE Scanning: verify Bluetooth permissions and avoid battery impact
        val hasBlePermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        if (hasBlePermissions && !isHardwareScanning) {
            try {
                com.example.drishti.hardware.HardwareDistanceBridge.getInstance(appContext).startScan()
                isHardwareScanning = true
                startObstacleTelemetryCollector()
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "BLE startScan deferred: ${e.message}")
            }
        }

        // Wake word service: only if Tier 2, enabled, and RECORD_AUDIO is granted
        val tierStr = prefs.getString(com.example.drishti.voice.DrishtiActivationTier.PREF_KEY, com.example.drishti.voice.DrishtiActivationTier.DEFAULT.name)
        val currentTier = com.example.drishti.voice.DrishtiActivationTier.fromString(tierStr)
        val isTier2 = (currentTier == com.example.drishti.voice.DrishtiActivationTier.TIER_2_HANDS_FREE)
        val isWakeEnabled = isTier2 && prefs.getBoolean("wake_service_enabled", false)

        if (isWakeEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                val serviceIntent = Intent(this, DrishtiWakeWordService::class.java).apply {
                    action = "ENABLE_WAKEWORD"
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "WakeWordService start deferred: ${e.message}")
            }
        }

        // Live Status & Telemetry Sharing: auto-restart background service if user enabled it
        try {
            com.example.drishti.location.DeviceLiveStatusManager.startServiceIfEnabled(appContext)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "DeviceLiveStatusService start deferred: ${e.message}")
        }
    }

    private fun startObstacleTelemetryCollector() {
        if (liveObstacleJob?.isActive == true) return
        liveObstacleJob = lifecycleScope.launch {
            var lastWasValid = false
            var lastReportedCm = -1
            com.example.drishti.hardware.HardwareDistanceBridge.getInstance(applicationContext).latestReading.collect { reading ->
                if (reading != null && reading.isValid) {
                    val cm = (reading.distanceMeters * 100).toInt()
                    if (!lastWasValid || kotlin.math.abs(cm - lastReportedCm) >= 5) {
                        lastWasValid = true
                        lastReportedCm = cm
                        android.util.Log.i("MainActivity", "=== PHASE_4A_LIVE_OBSTACLE_DETECTED ===")
                        android.util.Log.i("MainActivity", "PHASE_4A_LIVE_READING: distMm=${reading.distanceMm} cm=$cm valid=${reading.isValid} ts=${reading.timestamp}")
                        val clearRes = com.example.drishti.voice.ActionRegistry.executeActionWithResult(applicationContext, "IS_PATH_CLEAR", emptyMap())
                        android.util.Log.i("MainActivity", "PHASE_4A_LIVE_CMD_PATH_CLEAR: spoken=\"${clearRes.responseText}\"")
                        val aheadRes = com.example.drishti.voice.ActionRegistry.executeActionWithResult(applicationContext, "WHAT_IS_AHEAD", emptyMap())
                        android.util.Log.i("MainActivity", "PHASE_4A_LIVE_CMD_WHAT_AHEAD: spoken=\"${aheadRes.responseText}\"")
                    }
                } else if (lastWasValid && reading != null && !reading.isValid) {
                    lastWasValid = false
                    lastReportedCm = -1
                    android.util.Log.i("MainActivity", "=== PHASE_4A_LIVE_BASELINE_RECOVERED ===")
                    android.util.Log.i("MainActivity", "PHASE_4A_LIVE_BASELINE: distMm=${reading.distanceMm} status=${reading.errorCode} ts=${reading.timestamp}")
                    val clearRes = com.example.drishti.voice.ActionRegistry.executeActionWithResult(applicationContext, "IS_PATH_CLEAR", emptyMap())
                    android.util.Log.i("MainActivity", "PHASE_4A_LIVE_CMD_PATH_CLEAR: spoken=\"${clearRes.responseText}\"")
                }
            }
        }
    }

    private fun stopHardwareScanning() {
        if (isHardwareScanning) {
            try {
                com.example.drishti.hardware.HardwareDistanceBridge.getInstance(applicationContext).stopScan()
            } catch (_: Exception) {}
            isHardwareScanning = false
        }
        liveObstacleJob?.cancel()
        liveObstacleJob = null
    }

    override fun onResume() {
        super.onResume()
        maybeStartAuthorizedServices()
    }

    override fun onStop() {
        super.onStop()
        stopHardwareScanning()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHardwareScanning()
    }
}

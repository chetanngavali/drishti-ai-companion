package com.example.drishti.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.drishti.R
import com.example.drishti.auth.AuthValidator
import com.example.drishti.auth.ParentChildRelationshipModel
import com.example.drishti.auth.ProfileModel
import com.example.drishti.hardware.Esp32ConnectionState
import com.example.drishti.hardware.HardwareDistanceBridge
import com.example.drishti.supabase.EmergencyContactModel
import com.example.drishti.supabase.DataResult
import com.example.drishti.supabase.SupabaseAuth
import com.example.drishti.supabase.SupabaseRepository
import com.example.drishti.audio.DrishtiMicManager
import com.example.drishti.ui.screens.info.*
import com.example.drishti.ui.theme.IntelligentBlue
import com.example.drishti.util.EmergencyContactResolver
import com.example.drishti.util.HapticHelper
import com.example.drishti.util.LocaleHelper
import com.example.drishti.util.findActivity
import com.example.drishti.voice.AssistantCapabilityManager
import com.example.drishti.voice.DrishtiActivationTier
import com.example.drishti.voice.DrishtiWakeWordService
import android.provider.Settings
import kotlinx.coroutines.launch
import java.util.Locale

enum class InfoDialogType {
    NONE,
    SAFETY_NOTICE,
    PRIVACY_POLICY,
    PERMISSION_CENTER,
    TERMS_OF_USE,
    ACCESSIBILITY_NOTICE,
    EMERGENCY_DISCLAIMER,
    SECURITY_CENTER,
    AI_TRANSPARENCY,
    INCIDENT_REPORT,
    SECURITY_REPORT,
    GUARDIAN_CONSENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE) }
    val currentProfile by SupabaseAuth.currentProfileFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val supabaseRepo = remember { SupabaseRepository(context) }

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var activeInfoDialog by remember { mutableStateOf(InfoDialogType.NONE) }

    // Languages State
    val reactiveAppLangCode by LocaleHelper.appLanguageCode.collectAsState()
    var selectedAppLangCode by remember(reactiveAppLangCode) { mutableStateOf(reactiveAppLangCode) }
    var selectedVoiceLangCode by remember { mutableStateOf(LocaleHelper.getVoiceLanguageCode(context)) }

    // Voice & Speech State
    var voiceSpeed by remember { mutableStateOf(sharedPrefs.getFloat("voice_speed", 1.0f)) }
    var voiceVolume by remember { mutableStateOf(sharedPrefs.getFloat("voice_volume", 1.0f)) }
    var wakeServiceEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("wake_service_enabled", false)) }
    var wakeSensitivity by remember { mutableStateOf(sharedPrefs.getString("wake_sensitivity", "ADAPTIVE") ?: "ADAPTIVE") }
    var currentTier by remember {
        val tierStr = sharedPrefs.getString(DrishtiActivationTier.PREF_KEY, DrishtiActivationTier.DEFAULT.name)
        mutableStateOf(DrishtiActivationTier.fromString(tierStr))
    }
    var showHandsFreeWarningDialog by remember { mutableStateOf(false) }
    var assistantCapabilities by remember { mutableStateOf(AssistantCapabilityManager.getCapabilities(context)) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                assistantCapabilities = AssistantCapabilityManager.getCapabilities(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Accessibility State
    var largeText by remember { mutableStateOf(sharedPrefs.getBoolean("large_text", false)) }
    var highContrast by remember { mutableStateOf(sharedPrefs.getBoolean("high_contrast", false)) }
    var talkBackSupport by remember { mutableStateOf(sharedPrefs.getBoolean("talkback_support", true)) }
    var hapticTouch by remember { mutableStateOf(sharedPrefs.getBoolean("haptic_touch", true)) }
    var hapticNavigation by remember { mutableStateOf(sharedPrefs.getBoolean("haptic_navigation", true)) }
    var hapticSafety by remember { mutableStateOf(sharedPrefs.getBoolean("haptic_safety", true)) }

    // Emergency Contact
    val activeUserId = remember(currentProfile) { currentProfile?.id ?: supabaseRepo.getAuthenticatedUserId() }
    val initialContact = remember(activeUserId) {
        EmergencyContactResolver.resolve(sharedPrefs, activeUserId)
    }
    var emergencyContactName by remember(activeUserId) { mutableStateOf(initialContact.name) }
    var emergencyContactPhone by remember(activeUserId) { mutableStateOf(initialContact.phone) }
    var showEditContactDialog by remember { mutableStateOf(false) }

    // Guardian Pairing State
    var generatedPairingCode by remember { mutableStateOf<String?>(null) }
    var pairingCodeExpiresAt by remember { mutableStateOf<String?>(null) }
    var isGeneratingCode by remember { mutableStateOf(false) }
    var pairingBannerMessage by remember { mutableStateOf<String?>(null) }
    var guardianRelationships by remember { mutableStateOf<List<ParentChildRelationshipModel>>(emptyList()) }
    var guardianProfiles by remember { mutableStateOf<Map<String, ProfileModel?>>(emptyMap()) }
    var isRespondingToPairing by remember { mutableStateOf(false) }

    // Smart Cane & Optional Hardware Bridge State
    val hardwareBridge = remember { HardwareDistanceBridge.getInstance(context) }
    val esp32State by hardwareBridge.connectionState.collectAsState()
    val tofReading by hardwareBridge.latestReading.collectAsState()
    var showEsp32SettingsDialog by remember { mutableStateOf(false) }

    fun refreshGuardianRelationships() {
        coroutineScope.launch {
            val list = supabaseRepo.getParentChildRelationships()
            guardianRelationships = list
            val profileMap = mutableMapOf<String, ProfileModel?>()
            list.forEach { rel ->
                val p = supabaseRepo.getProfile(rel.parent_id)
                profileMap[rel.parent_id] = p
            }
            guardianProfiles = profileMap
        }
    }

    LaunchedEffect(activeUserId) {
        val resolved = EmergencyContactResolver.resolve(sharedPrefs, activeUserId)
        emergencyContactName = resolved.name
        emergencyContactPhone = resolved.phone

        refreshGuardianRelationships()
        try {
            val contacts = supabaseRepo.getEmergencyContacts()
            if (contacts.isNotEmpty()) {
                val primary = contacts.first()
                if (primary.phone.isNotBlank()) {
                    emergencyContactName = primary.name
                    emergencyContactPhone = primary.phone
                    if (!activeUserId.isNullOrBlank()) {
                        EmergencyContactResolver.save(sharedPrefs, activeUserId, primary.name, primary.phone)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    val wakePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            currentTier = DrishtiActivationTier.TIER_2_HANDS_FREE
            DrishtiMicManager.setActivationTier(DrishtiActivationTier.TIER_2_HANDS_FREE)
            val serviceIntent = Intent(context, DrishtiWakeWordService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
            wakeServiceEnabled = true
            sharedPrefs.edit()
                .putString(DrishtiActivationTier.PREF_KEY, DrishtiActivationTier.TIER_2_HANDS_FREE.name)
                .putBoolean("wake_service_enabled", true)
                .apply()
            if (hapticTouch) HapticHelper.vibrateSuccess(context)
            Toast.makeText(context, "Hands-Free \"Hey Drishti\" Active", Toast.LENGTH_SHORT).show()
        } else {
            currentTier = DrishtiActivationTier.TIER_1_ON_DEMAND
            DrishtiMicManager.setActivationTier(DrishtiActivationTier.TIER_1_ON_DEMAND)
            wakeServiceEnabled = false
            sharedPrefs.edit()
                .putString(DrishtiActivationTier.PREF_KEY, DrishtiActivationTier.TIER_1_ON_DEMAND.name)
                .putBoolean("wake_service_enabled", false)
                .apply()
            Toast.makeText(context, "Microphone permission required for wake word", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Screen Header
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ──────────────────────────────────────────────
            // 👤 1. Account Section
            // ──────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    icon = Icons.Rounded.AccountCircle,
                    title = stringResource(R.string.section_account)
                ) {
                    // Profile Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentProfile?.full_name ?: "Embedded User",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentProfile?.email ?: "Authenticated Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.account_role_embedded),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Sign Out Button
                    Button(
                        onClick = { showSignOutDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics { contentDescription = "Sign out button" }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_sign_out),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // ──────────────────────────────────────────────
            // 🎙️ 2. Voice & Language Section
            // ──────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.section_voice_language)
                ) {
                    // App Language Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.app_language_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.app_language_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("en", "English", stringResource(R.string.lang_english)),
                                Triple("hi", "हिन्दी", stringResource(R.string.lang_hindi)),
                                Triple("mr", "मराठी", stringResource(R.string.lang_marathi))
                            ).forEach { (code, name, label) ->
                                val isSelected = selectedAppLangCode == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (!isSelected) {
                                            selectedAppLangCode = code
                                            LocaleHelper.setAppLanguage(context, code, name)
                                            if (hapticTouch) HapticHelper.vibrateSuccess(context)
                                            context.findActivity()?.recreate()
                                        }
                                    },
                                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.height(44.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Voice Language Selector (Separate from UI Language)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.voice_language_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.voice_language_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("auto", "Auto Detect", stringResource(R.string.lang_auto_detect)),
                                Triple("en", "English", stringResource(R.string.lang_english)),
                                Triple("hi", "हिन्दी", stringResource(R.string.lang_hindi)),
                                Triple("mr", "मराठी", stringResource(R.string.lang_marathi))
                            ).forEach { (code, name, label) ->
                                val isSelected = selectedVoiceLangCode == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedVoiceLangCode = code
                                        LocaleHelper.setVoiceLanguage(context, code, name)
                                        if (hapticTouch) HapticHelper.vibrateSuccess(context)
                                        Toast.makeText(context, "Voice Language: $label", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.height(44.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Speech Settings (Voice speed & Volume)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.speech_settings_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.voice_speed_title), style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${String.format(Locale.US, "%.1f", voiceSpeed)}x", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = voiceSpeed,
                            onValueChange = {
                                voiceSpeed = it
                                sharedPrefs.edit().putFloat("voice_speed", it).apply()
                            },
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.voice_volume_title), style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${(voiceVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = voiceVolume,
                            onValueChange = {
                                voiceVolume = it
                                sharedPrefs.edit().putFloat("voice_volume", it).apply()
                            },
                            valueRange = 0.1f..1.0f,
                            steps = 8,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Voice Activation Tiers
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Voice Activation Mode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose how you want to activate Drishti AI. Privacy-preserving On-Demand mode keeps your microphone completely off when idle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // TIER 1: Privacy Mode (ON-DEMAND) - Default
                            Surface(
                                onClick = {
                                    if (currentTier != DrishtiActivationTier.TIER_1_ON_DEMAND) {
                                        currentTier = DrishtiActivationTier.TIER_1_ON_DEMAND
                                        DrishtiMicManager.setActivationTier(DrishtiActivationTier.TIER_1_ON_DEMAND)
                                        context.stopService(Intent(context, DrishtiWakeWordService::class.java))
                                        wakeServiceEnabled = false
                                        sharedPrefs.edit()
                                            .putString(DrishtiActivationTier.PREF_KEY, DrishtiActivationTier.TIER_1_ON_DEMAND.name)
                                            .putBoolean("wake_service_enabled", false)
                                            .apply()
                                        if (hapticTouch) HapticHelper.vibrateSuccess(context)
                                        Toast.makeText(context, "On-Demand Mode Active (Microphone OFF)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentTier == DrishtiActivationTier.TIER_1_ON_DEMAND)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (currentTier == DrishtiActivationTier.TIER_1_ON_DEMAND)
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = (currentTier == DrishtiActivationTier.TIER_1_ON_DEMAND),
                                        onClick = null
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "On-Demand (Privacy Default)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Microphone stays completely OFF until you activate Drishti (via BLE cane button, volume shortcut, or screen trigger). Android microphone privacy indicator will NOT appear while idle.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // TIER 2: Hands-Free ("Hey Drishti")
                            Surface(
                                onClick = {
                                    if (currentTier != DrishtiActivationTier.TIER_2_HANDS_FREE) {
                                        showHandsFreeWarningDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentTier == DrishtiActivationTier.TIER_2_HANDS_FREE)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (currentTier == DrishtiActivationTier.TIER_2_HANDS_FREE)
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = (currentTier == DrishtiActivationTier.TIER_2_HANDS_FREE),
                                        onClick = null
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Hands-Free \"Hey Drishti\"",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Drishti continuously listens for the wake phrase \"Hey Drishti\". Android may display the microphone privacy indicator.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (currentTier == DrishtiActivationTier.TIER_2_HANDS_FREE) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = "Hands-Free Wake Word: ON",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                    Text(
                                                        text = "\"Hey Drishti\" detection is active. Your microphone is being used continuously.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Acoustic Sensitivity",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(
                                                    "ADAPTIVE" to "Adaptive",
                                                    "STANDARD" to "Standard",
                                                    "HIGH_NOISE" to "High Noise"
                                                ).forEach { (modeKey, modeLabel) ->
                                                    val isSelected = wakeSensitivity.equals(modeKey, ignoreCase = true)
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            wakeSensitivity = modeKey
                                                            sharedPrefs.edit().putString("wake_sensitivity", modeKey).apply()
                                                        },
                                                        label = { Text(modeLabel, style = MaterialTheme.typography.bodySmall) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // TIER 3: Android Digital Assistant
                            Surface(
                                onClick = {
                                    if (currentTier != DrishtiActivationTier.TIER_3_ASSISTANT) {
                                        currentTier = DrishtiActivationTier.TIER_3_ASSISTANT
                                        DrishtiMicManager.setActivationTier(DrishtiActivationTier.TIER_3_ASSISTANT)
                                        context.stopService(Intent(context, DrishtiWakeWordService::class.java))
                                        wakeServiceEnabled = false
                                        sharedPrefs.edit()
                                            .putString(DrishtiActivationTier.PREF_KEY, DrishtiActivationTier.TIER_3_ASSISTANT.name)
                                            .putBoolean("wake_service_enabled", false)
                                            .apply()
                                        if (hapticTouch) HapticHelper.vibrateSuccess(context)
                                        Toast.makeText(context, "System Assistant Mode Selected", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentTier == DrishtiActivationTier.TIER_3_ASSISTANT)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (currentTier == DrishtiActivationTier.TIER_3_ASSISTANT)
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = (currentTier == DrishtiActivationTier.TIER_3_ASSISTANT),
                                        onClick = null
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Android Digital Assistant",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Use Drishti through Android-supported assistant integration. Availability depends on your device and Android version.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (currentTier == DrishtiActivationTier.TIER_3_ASSISTANT) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.asst_refresh_status),
                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                assistantCapabilities = AssistantCapabilityManager.getCapabilities(context)
                                                                Toast.makeText(context, "Capability status refreshed", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Refresh,
                                                                contentDescription = stringResource(R.string.asst_refresh_status),
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = stringResource(R.string.asst_capability_sub),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 2.dp)
                                                    )

                                                    // 1. Default Assistant Active
                                                    CapabilityMatrixRow(
                                                        title = stringResource(R.string.asst_default_active),
                                                        statusText = if (assistantCapabilities.defaultAssistantActive) stringResource(R.string.asst_status_active) else stringResource(R.string.asst_status_inactive),
                                                        isPositive = assistantCapabilities.defaultAssistantActive,
                                                        isNeutral = false,
                                                        explanation = if (assistantCapabilities.defaultAssistantActive)
                                                            stringResource(R.string.asst_default_active_exp)
                                                        else
                                                            stringResource(R.string.asst_default_inactive_exp)
                                                    )

                                                    // 2. Voice Interaction Service
                                                    CapabilityMatrixRow(
                                                        title = stringResource(R.string.asst_voice_interaction),
                                                        statusText = if (assistantCapabilities.voiceInteractionService) stringResource(R.string.asst_status_available) else stringResource(R.string.asst_status_unavailable),
                                                        isPositive = assistantCapabilities.voiceInteractionService,
                                                        isNeutral = false,
                                                        explanation = stringResource(R.string.asst_voice_interaction_exp)
                                                    )

                                                    // 3. System Assistant
                                                    CapabilityMatrixRow(
                                                        title = stringResource(R.string.asst_system_assistant),
                                                        statusText = if (assistantCapabilities.systemAssistantSupported) stringResource(R.string.asst_status_available) else stringResource(R.string.asst_status_unavailable),
                                                        isPositive = assistantCapabilities.systemAssistantSupported,
                                                        isNeutral = false,
                                                        explanation = stringResource(R.string.asst_system_assistant_exp)
                                                    )

                                                    // 4. Hardware DSP Hotword
                                                    CapabilityMatrixRow(
                                                        title = stringResource(R.string.asst_hardware_dsp),
                                                        statusText = if (assistantCapabilities.hardwareDspHotword) stringResource(R.string.asst_status_available) else stringResource(R.string.asst_status_unsupported),
                                                        isPositive = assistantCapabilities.hardwareDspHotword,
                                                        isNeutral = !assistantCapabilities.hardwareDspHotword,
                                                        explanation = if (assistantCapabilities.hardwareDspHotword)
                                                            stringResource(R.string.asst_hardware_dsp_avail_exp)
                                                        else
                                                            stringResource(R.string.asst_hardware_dsp_unavail_exp)
                                                    )

                                                    // 5. Application Wake Word
                                                    CapabilityMatrixRow(
                                                        title = stringResource(R.string.asst_app_wakeword),
                                                        statusText = if (assistantCapabilities.applicationWakeWord) stringResource(R.string.asst_status_available) else stringResource(R.string.asst_status_unavailable),
                                                        isPositive = assistantCapabilities.applicationWakeWord,
                                                        isNeutral = false,
                                                        explanation = stringResource(R.string.asst_app_wakeword_exp)
                                                    )

                                                    // 6. Active Recognition Engine
                                                    RecognitionEngineRow(
                                                        engineName = assistantCapabilities.activeRecognitionEngine,
                                                        isAvailable = assistantCapabilities.activeRecognitionEngine != "Unavailable"
                                                    )

                                                    // 7. On-Device Recognition
                                                    CapabilityMatrixRow(
                                                        title = stringResource(R.string.asst_on_device_rec),
                                                        statusText = if (assistantCapabilities.onDeviceRecognition) stringResource(R.string.asst_status_available) else stringResource(R.string.asst_status_unavailable),
                                                        isPositive = assistantCapabilities.onDeviceRecognition,
                                                        isNeutral = false,
                                                        explanation = if (assistantCapabilities.onDeviceRecognition)
                                                            stringResource(R.string.asst_on_device_avail_exp)
                                                        else
                                                            stringResource(R.string.asst_on_device_unavail_exp)
                                                    )

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    if (!assistantCapabilities.defaultAssistantActive) {
                                                        Button(
                                                            onClick = {
                                                                try {
                                                                    val intent = AssistantCapabilityManager.createAssistantSettingsIntent(context)
                                                                    context.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "Could not open assistant settings", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(stringResource(R.string.asst_btn_set_assistant))
                                                        }
                                                    } else {
                                                        OutlinedButton(
                                                            onClick = {
                                                                try {
                                                                    val intent = AssistantCapabilityManager.createAssistantSettingsIntent(context)
                                                                    context.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "Could not open assistant settings", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(stringResource(R.string.asst_btn_manage_settings))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showHandsFreeWarningDialog) {
                            AlertDialog(
                                onDismissRequest = { showHandsFreeWarningDialog = false },
                                title = {
                                    Text(
                                        text = stringResource(R.string.hands_free_dialog_title),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = stringResource(R.string.hands_free_dialog_msg1),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.hands_free_dialog_msg2),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showHandsFreeWarningDialog = false
                                            wakePermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                        }
                                    ) {
                                        Text(stringResource(R.string.btn_enable_hands_free))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showHandsFreeWarningDialog = false }) {
                                        Text(stringResource(R.string.btn_cancel))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ──────────────────────────────────────────────
            // ♿ 3. Accessibility Section
            // ──────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    icon = Icons.AutoMirrored.Rounded.AccessibleForward,
                    title = stringResource(R.string.section_accessibility)
                ) {
                    AccessibilityToggleRow(
                        title = stringResource(R.string.large_text_title),
                        description = stringResource(R.string.large_text_desc),
                        checked = largeText,
                        onCheckedChange = {
                            largeText = it
                            sharedPrefs.edit().putBoolean("large_text", it).apply()
                            if (hapticTouch) HapticHelper.vibrateSuccess(context)
                        }
                    )

                    AccessibilityToggleRow(
                        title = stringResource(R.string.high_contrast_title),
                        description = stringResource(R.string.high_contrast_desc),
                        checked = highContrast,
                        onCheckedChange = {
                            highContrast = it
                            sharedPrefs.edit().putBoolean("high_contrast", it).apply()
                            if (hapticTouch) HapticHelper.vibrateSuccess(context)
                        }
                    )

                    AccessibilityToggleRow(
                        title = stringResource(R.string.talkback_support_title),
                        description = stringResource(R.string.talkback_support_desc),
                        checked = talkBackSupport,
                        onCheckedChange = {
                            talkBackSupport = it
                            sharedPrefs.edit().putBoolean("talkback_support", it).apply()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    AccessibilityToggleRow(
                        title = stringResource(R.string.haptic_touch_title),
                        description = "Vibrate on button clicks and mode selections",
                        checked = hapticTouch,
                        onCheckedChange = {
                            hapticTouch = it
                            sharedPrefs.edit().putBoolean("haptic_touch", it).apply()
                        }
                    )

                    AccessibilityToggleRow(
                        title = stringResource(R.string.haptic_nav_title),
                        description = "Directional pulses for turn-by-turn guidance",
                        checked = hapticNavigation,
                        onCheckedChange = {
                            hapticNavigation = it
                            sharedPrefs.edit().putBoolean("haptic_navigation", it).apply()
                        }
                    )

                    AccessibilityToggleRow(
                        title = stringResource(R.string.haptic_safety_title),
                        description = "Urgent tactile pulses for proximity warnings",
                        checked = hapticSafety,
                        onCheckedChange = {
                            hapticSafety = it
                            sharedPrefs.edit().putBoolean("haptic_safety", it).apply()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    LegalHubLinkRow(
                        icon = Icons.AutoMirrored.Rounded.AccessibleForward,
                        title = stringResource(R.string.item_accessibility_notice_title),
                        subtitle = stringResource(R.string.item_accessibility_notice_sub),
                        onClick = { activeInfoDialog = InfoDialogType.ACCESSIBILITY_NOTICE }
                    )
                }
            }

            // ──────────────────────────────────────────────
            // 👨‍👩‍👧 4. Guardian & Family Section
            // ──────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    icon = Icons.Rounded.People,
                    title = stringResource(R.string.section_guardian_family)
                ) {
                    // Mandatory Consent Statement
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.guardian_consent_notice),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Open Full Guardian & Consent Page Button
                    OutlinedButton(
                        onClick = { activeInfoDialog = InfoDialogType.GUARDIAN_CONSENT },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.title_manage_guardian_pairing), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    // Quick Active Code Display if generated
                    if (generatedPairingCode != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.guardian_code_active),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = AuthValidator.formatPairingCodeForDisplay(generatedPairingCode ?: ""),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isGeneratingCode = true
                            pairingBannerMessage = null
                            coroutineScope.launch {
                                val res = supabaseRepo.generatePairingCode()
                                isGeneratingCode = false
                                when (res) {
                                    is DataResult.Success -> {
                                        generatedPairingCode = res.data.code
                                        pairingCodeExpiresAt = res.data.expires_at
                                        pairingBannerMessage = "Code generated! Share this 6-digit code with your Guardian."
                                        if (hapticTouch) HapticHelper.vibrateSuccess(context)
                                        refreshGuardianRelationships()
                                    }
                                    is DataResult.Failure -> {
                                        pairingBannerMessage = res.message
                                    }
                                    else -> {
                                        pairingBannerMessage = "Network error. Unable to generate code."
                                    }
                                }
                            }
                        },
                        enabled = !isGeneratingCode,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (generatedPairingCode != null) stringResource(R.string.guardian_btn_regenerate) else stringResource(R.string.guardian_btn_generate),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Emergency Contact Card
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sos_call_emergency_contact),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (emergencyContactName.isNotBlank()) "$emergencyContactName ($emergencyContactPhone)" else "No contact set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showEditContactDialog = true }) {
                            Text(stringResource(R.string.btn_edit))
                        }
                    }
                }
            }

            // ──────────────────────────────────────────────
            // 🛡️ 5. Safety, Privacy & Information Section
            // ──────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    icon = Icons.Rounded.Policy,
                    title = stringResource(R.string.section_safety_privacy_info)
                ) {
                    Text(
                        text = "Detailed assistive disclaimers, privacy policy notice, hardware permissions, and security architecture.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LegalHubLinkRow(
                        icon = Icons.Rounded.Warning,
                        title = stringResource(R.string.item_safety_notice_title),
                        subtitle = stringResource(R.string.item_safety_notice_sub),
                        onClick = { activeInfoDialog = InfoDialogType.SAFETY_NOTICE }
                    )

                    LegalHubLinkRow(
                        icon = Icons.Rounded.PrivacyTip,
                        title = stringResource(R.string.item_privacy_policy_title),
                        subtitle = stringResource(R.string.item_privacy_policy_sub),
                        onClick = { activeInfoDialog = InfoDialogType.PRIVACY_POLICY }
                    )

                    LegalHubLinkRow(
                        icon = Icons.Rounded.SecurityUpdateGood,
                        title = stringResource(R.string.item_permission_center_title),
                        subtitle = stringResource(R.string.item_permission_center_sub),
                        onClick = { activeInfoDialog = InfoDialogType.PERMISSION_CENTER }
                    )

                    LegalHubLinkRow(
                        icon = Icons.Rounded.Sos,
                        title = stringResource(R.string.item_emergency_sos_title),
                        subtitle = stringResource(R.string.item_emergency_sos_sub),
                        onClick = { activeInfoDialog = InfoDialogType.EMERGENCY_DISCLAIMER }
                    )

                    LegalHubLinkRow(
                        icon = Icons.Rounded.SmartToy,
                        title = stringResource(R.string.item_ai_transparency_title),
                        subtitle = stringResource(R.string.item_ai_transparency_sub),
                        onClick = { activeInfoDialog = InfoDialogType.AI_TRANSPARENCY }
                    )

                    LegalHubLinkRow(
                        icon = Icons.Rounded.Shield,
                        title = stringResource(R.string.item_security_center_title),
                        subtitle = stringResource(R.string.item_security_center_sub),
                        onClick = { activeInfoDialog = InfoDialogType.SECURITY_CENTER }
                    )

                    LegalHubLinkRow(
                        icon = Icons.Rounded.Gavel,
                        title = stringResource(R.string.item_terms_of_use_title),
                        subtitle = stringResource(R.string.item_terms_of_use_sub),
                        onClick = { activeInfoDialog = InfoDialogType.TERMS_OF_USE }
                    )
                }
            }

            // ──────────────────────────────────────────────
            // ⚠️ 6. Support & Reporting Section
            // ──────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    icon = Icons.Rounded.SupportAgent,
                    title = stringResource(R.string.section_support_reporting)
                ) {
                    LegalHubLinkRow(
                        icon = Icons.Rounded.ReportProblem,
                        title = stringResource(R.string.item_report_incident_title),
                        subtitle = stringResource(R.string.item_report_incident_sub),
                        onClick = { activeInfoDialog = InfoDialogType.INCIDENT_REPORT }
                    )

                    LegalHubLinkRow(
                        icon = Icons.Rounded.BugReport,
                        title = stringResource(R.string.item_security_report_title),
                        subtitle = stringResource(R.string.item_security_report_sub),
                        onClick = { activeInfoDialog = InfoDialogType.SECURITY_REPORT }
                    )
                }
            }

            // ──────────────────────────────────────────────
            // ℹ️ 7. About Section
            // ──────────────────────────────────────────────
            item {
                SettingsSectionCard(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.section_about)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.about_drishti_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.about_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(R.string.about_version_label)}: ${stringResource(R.string.about_version_value)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.about_opensource_title),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.about_opensource_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ──────────────────────────────────────────────
            // 🦯 8. Optional Hardware Integration (Experimental/Future)
            // ──────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(R.string.hardware_optional_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = stringResource(R.string.hardware_optional_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Status: ${when (esp32State) {
                                Esp32ConnectionState.STREAMING -> "Connected (Streaming)"
                                Esp32ConnectionState.CONNECTED -> "Connected"
                                Esp32ConnectionState.SCANNING -> "Scanning..."
                                else -> "Not Connected (Standalone App Mode)"
                            }}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (hapticTouch) HapticHelper.vibrateInfo(context)
                                showEsp32SettingsDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue)
                        ) {
                            Icon(Icons.Rounded.SettingsInputComponent, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open ESP32 Smart Cane Settings",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reset_settings_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.reset_settings_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                if (hapticTouch) HapticHelper.vibrateWarning(context)
                                showResetDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_reset_settings),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Reset Preferences Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.reset_settings_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.reset_settings_dialog_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        // Reset local preferences
                        voiceSpeed = 1.0f
                        voiceVolume = 1.0f
                        largeText = false
                        highContrast = false
                        talkBackSupport = true
                        hapticTouch = true
                        hapticNavigation = true
                        hapticSafety = true
                        currentTier = DrishtiActivationTier.TIER_1_ON_DEMAND
                        wakeServiceEnabled = false
                        wakeSensitivity = "ADAPTIVE"

                        sharedPrefs.edit()
                            .putFloat("voice_speed", 1.0f)
                            .putFloat("voice_volume", 1.0f)
                            .putBoolean("large_text", false)
                            .putBoolean("high_contrast", false)
                            .putBoolean("talkback_support", true)
                            .putBoolean("haptic_touch", true)
                            .putBoolean("haptic_navigation", true)
                            .putBoolean("haptic_safety", true)
                            .putString(DrishtiActivationTier.PREF_KEY, DrishtiActivationTier.TIER_1_ON_DEMAND.name)
                            .putBoolean("wake_service_enabled", false)
                            .putString("wake_sensitivity", "ADAPTIVE")
                            .putFloat("tof_warning_threshold_cm", 150f)
                            .apply()

                        DrishtiMicManager.setActivationTier(DrishtiActivationTier.TIER_1_ON_DEMAND)
                        context.stopService(Intent(context, DrishtiWakeWordService::class.java))

                        HapticHelper.vibrateSuccess(context)
                        Toast.makeText(context, context.getString(R.string.reset_settings_success), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_reset_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.dialog_sign_out_title)) },
            text = { Text(stringResource(R.string.dialog_sign_out_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        SupabaseAuth(context).signOut()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Full-Screen ESP32 Smart Cane Hardware Settings Dialog
    if (showEsp32SettingsDialog) {
        Dialog(
            onDismissRequest = { showEsp32SettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Esp32SettingsScreen(
                onNavigateBack = { showEsp32SettingsDialog = false }
            )
        }
    }

    // Edit Emergency Contact Dialog
    if (showEditContactDialog) {
        val dialogContext = remember(selectedAppLangCode) { LocaleHelper.applyLocale(context, selectedAppLangCode) }
        CompositionLocalProvider(LocalContext provides dialogContext) {
            var nameInput by remember { mutableStateOf(emergencyContactName) }
            var phoneInput by remember { mutableStateOf(emergencyContactPhone) }
            var nameError by remember { mutableStateOf<String?>(null) }
            var phoneError by remember { mutableStateOf<String?>(null) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var isSaving by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { if (!isSaving) showEditContactDialog = false },
                title = { Text(stringResource(R.string.edit_emergency_contact_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            nameError = null
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.contact_name_label)) },
                        isError = nameError != null,
                        supportingText = {
                            if (nameError != null) {
                                Text(nameError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = {
                            phoneInput = it
                            phoneError = null
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.phone_number_label)) },
                        isError = phoneError != null,
                        supportingText = {
                            if (phoneError != null) {
                                Text(phoneError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving,
                    onClick = {
                        val trimmedName = nameInput.trim()
                        val trimmedPhone = phoneInput.trim()
                        var hasValidationErrors = false

                        if (trimmedName.isEmpty()) {
                            nameError = "Contact name cannot be empty."
                            hasValidationErrors = true
                        } else if (trimmedName.length < 2) {
                            nameError = "Contact name must be at least 2 characters."
                            hasValidationErrors = true
                        }

                        if (trimmedPhone.isEmpty()) {
                            phoneError = "Phone number cannot be empty."
                            hasValidationErrors = true
                        } else if (trimmedPhone.length < 5 || !trimmedPhone.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }) {
                            phoneError = "Please enter a valid phone number."
                            hasValidationErrors = true
                        }

                        if (hasValidationErrors) return@Button

                        val currentUserId = activeUserId ?: supabaseRepo.getAuthenticatedUserId()
                        if (currentUserId.isNullOrBlank()) {
                            errorMessage = "No active session found. Please sign in again."
                            return@Button
                        }

                        isSaving = true
                        errorMessage = null

                        coroutineScope.launch {
                            val saveRes = supabaseRepo.saveEmergencyContact(
                                EmergencyContactModel(
                                    user_id = currentUserId,
                                    name = trimmedName,
                                    phone = trimmedPhone,
                                    relation = "Emergency Contact"
                                )
                            )

                            isSaving = false
                            when (saveRes) {
                                is DataResult.Success -> {
                                    emergencyContactName = trimmedName
                                    emergencyContactPhone = trimmedPhone

                                    EmergencyContactResolver.save(sharedPrefs, currentUserId, trimmedName, trimmedPhone)

                                    showEditContactDialog = false
                                    if (hapticTouch) HapticHelper.vibrateSuccess(context)
                                    Toast.makeText(context, "Emergency contact updated successfully", Toast.LENGTH_SHORT).show()
                                }
                                is DataResult.Unauthorized -> {
                                    errorMessage = "Session authorization error. Please sign in again."
                                }
                                is DataResult.Offline -> {
                                    errorMessage = "Device is offline. Check internet connection to update contact."
                                }
                                is DataResult.Failure -> {
                                    errorMessage = "Failed to update contact: ${saveRes.message}"
                                }
                                else -> {
                                    errorMessage = "Could not update emergency contact. Please try again."
                                }
                            }
                        }
                    }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_saving))
                    } else {
                        Text(stringResource(R.string.btn_save))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { showEditContactDialog = false }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
        }
    }

    // Modal Overlays for all detailed information pages
    if (activeInfoDialog != InfoDialogType.NONE) {
        Dialog(
            onDismissRequest = { activeInfoDialog = InfoDialogType.NONE },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            when (activeInfoDialog) {
                InfoDialogType.SAFETY_NOTICE -> SafetyNoticeScreen(
                    onAcknowledge = { activeInfoDialog = InfoDialogType.NONE },
                    showBackButton = true,
                    onBack = { activeInfoDialog = InfoDialogType.NONE }
                )
                InfoDialogType.PRIVACY_POLICY -> PrivacyAndDataPolicyScreen(onBack = { activeInfoDialog = InfoDialogType.NONE })
                InfoDialogType.PERMISSION_CENTER -> PermissionCenterScreen(onBack = { activeInfoDialog = InfoDialogType.NONE })
                InfoDialogType.TERMS_OF_USE -> TermsOfUseScreen(onBack = { activeInfoDialog = InfoDialogType.NONE })
                InfoDialogType.ACCESSIBILITY_NOTICE -> AccessibilityNoticeScreen(onBack = { activeInfoDialog = InfoDialogType.NONE })
                InfoDialogType.EMERGENCY_DISCLAIMER -> EmergencySosDisclaimerScreen(
                    onBack = { activeInfoDialog = InfoDialogType.NONE },
                    onConfigureContacts = {
                        activeInfoDialog = InfoDialogType.NONE
                        showEditContactDialog = true
                    }
                )
                InfoDialogType.SECURITY_CENTER -> SecurityCenterScreen(
                    onBack = { activeInfoDialog = InfoDialogType.NONE },
                    onReportVulnerability = { activeInfoDialog = InfoDialogType.SECURITY_REPORT }
                )
                InfoDialogType.AI_TRANSPARENCY -> AiTransparencyScreen(onBack = { activeInfoDialog = InfoDialogType.NONE })
                InfoDialogType.INCIDENT_REPORT -> IncidentReportScreen(onBack = { activeInfoDialog = InfoDialogType.NONE })
                InfoDialogType.SECURITY_REPORT -> IncidentReportScreen(
                    onBack = { activeInfoDialog = InfoDialogType.NONE },
                    initialCategory = "Privacy or Security issue"
                )
                InfoDialogType.GUARDIAN_CONSENT -> GuardianConsentScreen(onBack = { activeInfoDialog = InfoDialogType.NONE })
                InfoDialogType.NONE -> {}
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun AccessibilityToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun LegalHubLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
private fun CapabilityMatrixRow(
    title: String,
    statusText: String,
    isPositive: Boolean,
    isNeutral: Boolean = false,
    explanation: String? = null
) {
    val statusColor = when {
        isPositive -> Color(0xFF16A34A) // Positive Green
        isNeutral -> Color(0xFF64748B)  // Muted Slate (for unsupported DSP hardware)
        else -> Color(0xFFD97706)       // Amber Warning (e.g. Inactive default assistant)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title: $statusText${if (explanation != null) ", $explanation" else ""}"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
        }
        if (!explanation.isNullOrBlank()) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun RecognitionEngineRow(
    engineName: String,
    isAvailable: Boolean
) {
    val statusColor = if (isAvailable) Color(0xFF16A34A) else Color(0xFF64748B)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Active Recognition Engine: $engineName"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Recognition Engine",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
        }
        Text(
            text = engineName,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

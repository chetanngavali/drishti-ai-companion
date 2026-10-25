package com.example.drishti.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.compose.ui.res.stringResource
import com.example.drishti.R
import com.example.drishti.sos.EmergencyTriggerType
import com.example.drishti.ui.theme.DangerRed
import com.example.drishti.ui.theme.DrishtiTheme
import com.example.drishti.util.HapticHelper
import com.example.drishti.util.LocaleHelper
import com.example.drishti.viewmodel.EmergencyViewModel

class EmergencyActivity : ComponentActivity() {
    private val viewModel: EmergencyViewModel by viewModels()

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
        enableEdgeToEdge()

        val triggerStr = intent.getStringExtra("trigger_type")
        val triggerType = when (triggerStr) {
            "VOICE_COMMAND" -> EmergencyTriggerType.VOICE_COMMAND
            "COLLISION_HAZARD" -> EmergencyTriggerType.COLLISION_HAZARD
            "FALL_DETECTED" -> EmergencyTriggerType.FALL_DETECTED
            else -> EmergencyTriggerType.MANUAL_BUTTON
        }

        // Auto-trigger confirmation countdown if not already active or counting down
        if (!viewModel.sosState.value.isActive && !viewModel.sosState.value.isCountingDown) {
            viewModel.triggerSos(triggerType = triggerType, skipCountdown = false)
        }

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
                    LocalConfiguration provides localizedConfig
                ) {
                    DrishtiTheme {
                        EmergencyContent(
                            viewModel = viewModel,
                            onCallEmergency = {
                                try {
                                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:112")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    if (ActivityCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.CALL_PHONE
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        startActivity(intent)
                                    } else {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        startActivity(dialIntent)
                                    }
                                } catch (_: Exception) {}
                            },
                            onCallContact = { phone ->
                                try {
                                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    if (ActivityCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.CALL_PHONE
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        startActivity(intent)
                                    } else {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        startActivity(dialIntent)
                                    }
                                } catch (_: Exception) {}
                            },
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyContent(
    viewModel: EmergencyViewModel,
    onCallEmergency: () -> Unit,
    onCallContact: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sosState by viewModel.sosState.collectAsState()
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section: Header & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            color = if (sosState.isActive) DangerRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = stringResource(R.string.sos_alert_title),
                        tint = DangerRed,
                        modifier = Modifier.size(72.dp)
                    )
                }

                if (sosState.isCountingDown) {
                    Text(
                        text = "SOS CONFIRMATION",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DangerRed,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "${sosState.countdownSeconds}s",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                        color = DangerRed
                    )
                    Text(
                        text = "Broadcasting to approved guardians in ${sosState.countdownSeconds} seconds. Tap cancel if triggered by mistake.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else if (sosState.isActive) {
                    Text(
                        text = "EMERGENCY SOS ACTIVE",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = DangerRed,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = sosState.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guardians Alerted: ${sosState.guardiansNotified}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    Text(
                        text = "SOS STANDBY",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = sosState.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Middle Section: Telemetry Cards (GPS, ToF Cane, Battery)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Telemetry Payload",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (sosState.isActive) {
                            Text(
                                text = "STREAMING (6s)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DangerRed
                            )
                        }
                    }

                    HorizontalDivider()

                    // 1. Live Coordinates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sos_telemetry_gps), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = if (sosState.lastLatitude != null && sosState.lastLongitude != null)
                                "%.4f, %.4f".format(sosState.lastLatitude, sosState.lastLongitude)
                            else viewModel.locationText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 2. ToF Cane Sensor Proximity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sos_telemetry_tof), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = if (sosState.lastTofDistanceMm != null)
                                "${sosState.lastTofDistanceMm} mm (${sosState.lastTofDistanceMm!! / 10} cm)"
                            else "Standby / 450 mm",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 3. Device & Cane Battery
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.BatteryFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sos_telemetry_battery), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${sosState.lastBatteryPercent ?: 95}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 4. Trigger Origin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sos_telemetry_trigger), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = sosState.triggerType?.displayName ?: "Manual SOS",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Section: Action Controls
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                if (sosState.isCountingDown) {
                    Button(
                        onClick = {
                            viewModel.triggerSos(sosState.triggerType ?: EmergencyTriggerType.MANUAL_BUTTON, skipCountdown = true)
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BROADCAST NOW",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.cancelCountdown()
                            onBack()
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel / False Alarm", style = MaterialTheme.typography.titleMedium)
                    }
                } else if (sosState.isActive) {
                    val emergencyPrefs = remember { context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE) }
                    val localName = remember { emergencyPrefs.getString("emergency_name", "") ?: "" }
                    val localPhone = remember { emergencyPrefs.getString("emergency_phone", "") ?: "" }

                    val resolvedContact = remember(localPhone, viewModel.emergencyContacts) {
                        if (localPhone.isNotBlank()) {
                            Pair(localName.ifBlank { "Guardian" }, localPhone)
                        } else if (viewModel.emergencyContacts.isNotEmpty()) {
                            val c = viewModel.emergencyContacts.first()
                            Pair(c.name.ifBlank { "Guardian" }, c.phone)
                        } else {
                            null
                        }
                    }

                    if (resolvedContact != null && resolvedContact.second.isNotBlank()) {
                        Button(
                            onClick = { onCallContact(resolvedContact.second) },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .semantics { contentDescription = "Call Emergency Contact ${resolvedContact.first}" }
                        ) {
                            Icon(Icons.Rounded.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CALL ${resolvedContact.first.uppercase()}: ${resolvedContact.second}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Button(
                        onClick = onCallEmergency,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .semantics { contentDescription = "Call Emergency 112 button" }
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CALL 112 (NATIONAL EMERGENCY)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.resolveSos()
                            onBack()
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sos_resolve_safe), style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.cancelSos()
                            onBack()
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Text(stringResource(R.string.sos_cancel_alarm), style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.triggerSos(EmergencyTriggerType.MANUAL_BUTTON, skipCountdown = false)
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Text(stringResource(R.string.sos_start_emergency), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(stringResource(R.string.sos_return_nav), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

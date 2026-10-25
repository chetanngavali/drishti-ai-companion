package com.example.drishti.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.drishti.R
import com.example.drishti.sos.EmergencySosManager
import com.example.drishti.sos.EmergencyTriggerType
import com.example.drishti.ui.theme.DangerRed
import com.example.drishti.util.HapticHelper

@Composable
fun EmergencyScreen() {
    val context = LocalContext.current
    val sosManager = remember { EmergencySosManager.getInstance(context) }
    val sosState by sosManager.state.collectAsState()

    val sharedPrefs = remember { context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE) }
    var emergencyContactName by remember { mutableStateOf(sharedPrefs.getString("emergency_name", "") ?: "") }
    var emergencyContactPhone by remember { mutableStateOf(sharedPrefs.getString("emergency_phone", "") ?: "") }

    LaunchedEffect(Unit) {
        val currentName = sharedPrefs.getString("emergency_name", "") ?: ""
        val currentPhone = sharedPrefs.getString("emergency_phone", "") ?: ""
        if (currentPhone.isNotBlank()) {
            emergencyContactName = currentName
            emergencyContactPhone = currentPhone
        }
        try {
            val repo = com.example.drishti.supabase.SupabaseRepository(context)
            val contacts = repo.getEmergencyContacts()
            if (contacts.isNotEmpty()) {
                val primary = contacts.first()
                if (primary.phone.isNotBlank()) {
                    emergencyContactName = primary.name
                    emergencyContactPhone = primary.phone
                    sharedPrefs.edit()
                        .putString("emergency_name", primary.name)
                        .putString("emergency_phone", primary.phone)
                        .apply()
                }
            }
        } catch (_: Exception) {}
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = emergencyContactPhone.ifBlank { "112" }
        if (granted && ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$target")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$target")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Emergency Warning Icon",
                    tint = DangerRed,
                    modifier = Modifier.size(96.dp)
                )

                Text(
                    text = when {
                        sosState.isCountingDown -> stringResource(R.string.sos_title_countdown_format, sosState.countdownSeconds)
                        sosState.isActive -> stringResource(R.string.sos_title_active)
                        else -> stringResource(R.string.sos_title_standby)
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (sosState.isCountingDown || sosState.isActive) DangerRed else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics {
                        contentDescription = when {
                            sosState.isCountingDown -> "SOS Countdown ${sosState.countdownSeconds} seconds remaining"
                            sosState.isActive -> "Emergency SOS Active"
                            else -> "Emergency Assistance Screen"
                        }
                    }
                )

                Text(
                    text = when {
                        sosState.isCountingDown -> stringResource(R.string.sos_desc_countdown)
                        sosState.isActive -> stringResource(R.string.sos_desc_active)
                        else -> stringResource(R.string.sos_desc_standby)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Live SOS Status Card" }
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.sos_telemetry_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (sosState.isActive) {
                            stringResource(R.string.sos_status_active_fmt, sosState.guardiansNotified) + "\n" +
                            "Lat: ${sosState.lastLatitude ?: "Locating..."}, Lng: ${sosState.lastLongitude ?: "Locating..."}\n" +
                            "Cane ToF: ${sosState.lastTofDistanceMm?.let { "$it mm" } ?: "Standby"} • Bat: ${sosState.lastBatteryPercent ?: 95}%"
                        } else if (sosState.isCountingDown) {
                            stringResource(R.string.sos_countdown_starting_fmt, sosState.countdownSeconds)
                        } else {
                            stringResource(R.string.sos_status_standby)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (sosState.isCountingDown) {
                    Button(
                        onClick = {
                            sosManager.triggerEmergency(EmergencyTriggerType.MANUAL_BUTTON, skipCountdown = true)
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Text(stringResource(R.string.sos_btn_broadcast_now), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { sosManager.cancelCountdown() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Text(stringResource(R.string.sos_btn_cancel_countdown), style = MaterialTheme.typography.titleMedium)
                    }
                } else if (sosState.isActive) {
                    if (emergencyContactPhone.isNotBlank()) {
                        Button(
                            onClick = {
                                HapticHelper.vibrateEmergency(context)
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$emergencyContactPhone")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics { contentDescription = "Call Emergency Contact $emergencyContactName" }
                        ) {
                            Icon(Icons.Rounded.Phone, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            val contactLabel = emergencyContactName.ifBlank { stringResource(R.string.sos_call_emergency_contact) }.uppercase()
                            Text(
                                text = stringResource(R.string.sos_btn_call_contact_fmt, contactLabel, emergencyContactPhone),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = {
                            sosManager.resolveEmergency()
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Text(stringResource(R.string.sos_btn_resolve), style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(context, EmergencyActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("EmergencyScreen", "Failed to launch EmergencyActivity", e)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .semantics { contentDescription = "Open Full Emergency Control Activity" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Text(stringResource(R.string.sos_btn_control_room), style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(context, EmergencyActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    putExtra("trigger_type", "MANUAL_BUTTON")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("EmergencyScreen", "Failed to launch EmergencyActivity", e)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .semantics { contentDescription = "Trigger Emergency SOS button" }
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sos_btn_trigger),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    if (emergencyContactPhone.isNotBlank()) {
                        Button(
                            onClick = {
                                HapticHelper.vibrateEmergency(context)
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$emergencyContactPhone")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .semantics { contentDescription = "Call Emergency Contact $emergencyContactName $emergencyContactPhone" }
                        ) {
                            Icon(Icons.Rounded.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            val contactLabel = emergencyContactName.ifBlank { stringResource(R.string.sos_call_emergency_contact) }.uppercase()
                            Text(
                                text = stringResource(R.string.sos_btn_call_contact_fmt, contactLabel, emergencyContactPhone),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                HapticHelper.vibrateEmergency(context)
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .semantics { contentDescription = "Call National Emergency 112" }
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, tint = DangerRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sos_btn_call_112),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                HapticHelper.vibrateEmergency(context)
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:112")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics { contentDescription = "Call Emergency 112 button" }
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, tint = DangerRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sos_btn_call_112),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Tip: No saved emergency contact found. Add your parent's number in Settings or Sign Up for 1-tap guardian calls & SMS alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

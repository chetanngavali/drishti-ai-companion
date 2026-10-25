package com.example.drishti.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.drishti.auth.ParentChildRelationshipModel
import com.example.drishti.auth.ProfileModel
import com.example.drishti.auth.UserRole
import com.example.drishti.sos.EmergencySessionModel
import com.example.drishti.sos.EmergencyTelemetryModel
import com.example.drishti.supabase.*
import com.example.drishti.ui.theme.DangerRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun ParentDashboardScreen(
    onSignOut: () -> Unit
) {
    com.example.drishti.parent.ParentDashboardMainContainer(
        onSignOut = onSignOut
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyParentDashboardContent(
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabaseAuth = remember { SupabaseAuth(context) }
    val supabaseRepo = remember { SupabaseRepository(context) }


    val currentProfile by SupabaseAuth.currentProfileFlow.collectAsState()

    var relationships by remember { mutableStateOf<List<ParentChildRelationshipModel>>(emptyList()) }
    var linkedChildSettings by remember { mutableStateOf<Map<String, UserSettingsModel?>>(emptyMap()) }
    var linkedChildContacts by remember { mutableStateOf<Map<String, List<EmergencyContactModel>>>(emptyMap()) }
    var linkedChildActiveSessions by remember { mutableStateOf<Map<String, EmergencySessionModel?>>(emptyMap()) }
    var linkedChildTelemetryTrails by remember { mutableStateOf<Map<String, List<EmergencyTelemetryModel>>>(emptyMap()) }
    var childProfiles by remember { mutableStateOf<Map<String, ProfileModel?>>(emptyMap()) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var showLinkDialog by remember { mutableStateOf(false) }
    var pairingCodeInput by remember { mutableStateOf("") }
    var isLinking by remember { mutableStateOf(false) }
    var linkDialogError by remember { mutableStateOf<String?>(null) }

    var showSignOutDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        Log.i("ParentDashboard", "CHANGES_TAB_ENTER: timestamp=${System.currentTimeMillis()}")
        Log.i("ParentDashboard", "CHANGES_SCREEN_COMPOSED: timestamp=${System.currentTimeMillis()}")
        onDispose {
            Log.i("ParentDashboard", "CHANGES_SCREEN_DISPOSED: timestamp=${System.currentTimeMillis()}")
        }
    }

    fun refreshDashboardData(silent: Boolean = false) {
        if (!silent) isLoading = true
        errorMessage = null
        Log.i("ParentDashboard", "CHANGES_LOAD_START: timestamp=${System.currentTimeMillis()}")
        coroutineScope.launch {
            try {
                withTimeoutOrNull(15000L) {
                    val list = supabaseRepo.getParentChildRelationships()
                    relationships = list

                    val settingsMap = mutableMapOf<String, UserSettingsModel?>()
                    val contactsMap = mutableMapOf<String, List<EmergencyContactModel>>()
                    val profileMap = mutableMapOf<String, ProfileModel?>()
                    val activeSessionsMap = mutableMapOf<String, EmergencySessionModel?>()
                    val telemetryMap = mutableMapOf<String, List<EmergencyTelemetryModel>>()

                    list.forEach { rel ->
                        val childId = rel.embedded_user_id
                        val profile = supabaseRepo.getProfile(childId)
                        profileMap[childId] = profile

                        if (rel.status == "approved") {
                            val settings = supabaseRepo.getLinkedChildSettings(childId)
                            settingsMap[childId] = settings

                            val contacts = supabaseRepo.getLinkedChildEmergencyContacts(childId)
                            contactsMap[childId] = contacts

                            val activeSession = supabaseRepo.getLinkedChildActiveEmergencySession(childId)
                            activeSessionsMap[childId] = activeSession

                            if (activeSession?.id != null) {
                                val trail = supabaseRepo.getEmergencyTelemetryTrail(activeSession.id, limit = 10)
                                telemetryMap[childId] = trail
                            }
                        }
                    }

                    childProfiles = profileMap
                    linkedChildSettings = settingsMap
                    linkedChildContacts = contactsMap
                    linkedChildActiveSessions = activeSessionsMap
                    linkedChildTelemetryTrails = telemetryMap
                } ?: run {
                    Log.w("ParentDashboard", "CHANGES_LOAD_ERROR: Timeout loading dashboard data at timestamp=${System.currentTimeMillis()}")
                    errorMessage = "Network request timed out."
                }
                isLoading = false
                Log.i("ParentDashboard", "CHANGES_LOAD_SUCCESS: timestamp=${System.currentTimeMillis()}")
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.localizedMessage ?: "Failed to load dashboard data."
                Log.e("ParentDashboard", "CHANGES_LOAD_ERROR: timestamp=${System.currentTimeMillis()} - ${e.message}", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDashboardData()
    }

    // Auto-refresh when an active emergency session is present (safeguarded against rapid toggle loops)
    val hasActiveSos = remember(linkedChildActiveSessions) {
        linkedChildActiveSessions.values.any { it?.isActive == true }
    }

    LaunchedEffect(hasActiveSos) {
        if (hasActiveSos) {
            while (true) {
                delay(10000L)
                refreshDashboardData(silent = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DrishtiAI",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Parent & Guardian Portal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshDashboardData() },
                        modifier = Modifier.semantics { contentDescription = "Refresh dashboard" }
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                    }
                    IconButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.semantics { contentDescription = "Sign out from parent portal" }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Parent Identity Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.FamilyRestroom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProfile?.full_name ?: "Parent Account",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentProfile?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = (currentProfile?.relationship ?: "Guardian").uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                AuthErrorBanner(errorMessage = errorMessage)
                AnimatedVisibility(visible = !successMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = successMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Linked Embedded Users",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FilledTonalButton(
                        onClick = {
                            pairingCodeInput = ""
                            linkDialogError = null
                            showLinkDialog = true
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pair Device", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (relationships.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Linked Accounts",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Pair Device' above and enter the 6-digit pairing code generated on the Embedded User's device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(relationships) { rel ->
                    val childId = rel.embedded_user_id
                    val childProfile = childProfiles[childId]
                    val settings = linkedChildSettings[childId]
                    val contacts = linkedChildContacts[childId] ?: emptyList()

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = childProfile?.full_name ?: "Embedded User",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = childProfile?.email ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (rel.status) {
                                        "approved" -> MaterialTheme.colorScheme.primaryContainer
                                        "pending" -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.errorContainer
                                    }
                                ) {
                                    Text(
                                        text = rel.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = when (rel.status) {
                                            "approved" -> MaterialTheme.colorScheme.onPrimaryContainer
                                            "pending" -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (rel.status == "approved") {
                                val activeSession = linkedChildActiveSessions[childId]
                                val telemetryTrail = linkedChildTelemetryTrails[childId] ?: emptyList()

                                if (activeSession != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = DangerRed.copy(alpha = 0.12f),
                                        border = BorderStroke(1.5.dp, DangerRed),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Warning,
                                                        contentDescription = "Active Emergency Alert",
                                                        tint = DangerRed,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "EMERGENCY SOS ACTIVE",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                        color = DangerRed
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = DangerRed
                                                ) {
                                                    Text(
                                                        text = "LIVE",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = androidx.compose.ui.graphics.Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Trigger: ${activeSession.trigger_type} • Started: ${activeSession.started_at?.take(19)?.replace("T", " ") ?: "Just now"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (activeSession.last_latitude != null && activeSession.last_longitude != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "GPS: %.5f, %.5f (±%.1fm)".format(
                                                        activeSession.last_latitude,
                                                        activeSession.last_longitude,
                                                        activeSession.last_accuracy_m ?: 5f
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))

                                                FilledTonalButton(
                                                    onClick = {
                                                        try {
                                                            val geoUri = Uri.parse("geo:${activeSession.last_latitude},${activeSession.last_longitude}?q=${activeSession.last_latitude},${activeSession.last_longitude}(Drishti+Child+SOS)")
                                                            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(mapIntent)
                                                        } catch (_: Exception) {}
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Open Live Location in Maps", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }

                                            if (activeSession.last_tof_distance_mm != null || activeSession.device_battery_percent != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    if (activeSession.last_tof_distance_mm != null) {
                                                        Text(
                                                            text = "Smart Cane: ${activeSession.last_tof_distance_mm} mm (${activeSession.last_tof_distance_mm / 10} cm)",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (activeSession.device_battery_percent != null) {
                                                        Text(
                                                            text = "Battery: ${activeSession.device_battery_percent}%",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            if (telemetryTrail.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Telemetry Trail (${telemetryTrail.size} points received):",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                telemetryTrail.take(3).forEach { pt ->
                                                    Text(
                                                        text = "• %.5f, %.5f | ToF: ${pt.tof_distance_mm ?: "-"}mm | ${pt.created_at?.takeLast(8) ?: ""}".trim(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "SOS Status: All Clear (No active emergencies)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Text(
                                    text = "Device Safety Settings",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Hazard Detection:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (settings?.hazard_detection == true) "Active (${settings.hazard_sensitivity})" else "Disabled",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Haptic Alerts:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (settings?.haptic_safety == true) "Enabled" else "Disabled",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (contacts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Emergency Contacts (${contacts.size})",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    contacts.forEach { contact ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${contact.name} (${contact.relation})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = contact.phone,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Pair Embedded User Dialog (6-Digit Handshake)
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isLinking) showLinkDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pair with Embedded User",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter the 6-digit numeric pairing code generated on the Embedded User's device (Settings → Pair with Guardian).\n\nPairing codes expire after 15 minutes, are single-use, and rate-limited against brute force.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!linkDialogError.isNullOrBlank()) {
                        Text(
                            text = linkDialogError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = pairingCodeInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            if (filtered.length <= 6) {
                                pairingCodeInput = filtered
                                linkDialogError = null
                            }
                        },
                        label = { Text("6-Digit Pairing Code") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validation = com.example.drishti.auth.AuthValidator.validatePairingCode(pairingCodeInput)
                        if (validation is com.example.drishti.auth.AuthValidator.ValidationResult.Invalid) {
                            linkDialogError = validation.errorMessage
                            return@Button
                        }
                        isLinking = true
                        coroutineScope.launch {
                            val res = supabaseRepo.submitPairingCode(pairingCodeInput)
                            isLinking = false
                            when (res) {
                                is DataResult.Success -> {
                                    showLinkDialog = false
                                    successMessage = res.data.message ?: "Pairing request submitted. Waiting for approval on the user's device."
                                    refreshDashboardData()
                                }
                                is DataResult.Failure -> {
                                    linkDialogError = res.message
                                }
                                is DataResult.ValidationError -> {
                                    linkDialogError = res.message
                                }
                                else -> {
                                    linkDialogError = "Network connection failed. Try again."
                                }
                            }
                        }
                    },
                    enabled = !isLinking && pairingCodeInput.length == 6
                ) {
                    if (isLinking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Verify & Pair")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLinkDialog = false },
                    enabled = !isLinking
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out from the Parent & Guardian portal?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        supabaseAuth.signOut()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

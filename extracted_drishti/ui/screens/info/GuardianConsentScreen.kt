package com.example.drishti.ui.screens.info

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.drishti.R
import com.example.drishti.auth.AuthValidator
import com.example.drishti.auth.ParentChildRelationshipModel
import com.example.drishti.auth.ProfileModel
import com.example.drishti.supabase.DataResult
import com.example.drishti.supabase.SupabaseRepository
import com.example.drishti.util.HapticHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianConsentScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabaseRepo = remember { SupabaseRepository(context) }
    val scrollState = rememberScrollState()

    var generatedPairingCode by remember { mutableStateOf<String?>(null) }
    var pairingCodeExpiresAt by remember { mutableStateOf<String?>(null) }
    var isGeneratingCode by remember { mutableStateOf(false) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }
    var guardianRelationships by remember { mutableStateOf<List<ParentChildRelationshipModel>>(emptyList()) }
    var guardianProfiles by remember { mutableStateOf<Map<String, ProfileModel?>>(emptyMap()) }
    var isRespondingToPairing by remember { mutableStateOf(false) }

    fun refreshRelationships() {
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

    LaunchedEffect(Unit) {
        refreshRelationships()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.section_guardian_family),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Mandatory Consent & Safety Notice Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Explicit Consent & Pairing Policy",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.guardian_consent_notice),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.guardian_consent_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!bannerMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bannerMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Section 1: Pairing Code
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.guardian_pairing_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (generatedPairingCode != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.guardian_code_active),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = AuthValidator.formatPairingCodeForDisplay(generatedPairingCode ?: ""),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.guardian_code_expiry),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isGeneratingCode = true
                            bannerMessage = null
                            coroutineScope.launch {
                                val res = supabaseRepo.generatePairingCode()
                                isGeneratingCode = false
                                when (res) {
                                    is DataResult.Success -> {
                                        generatedPairingCode = res.data.code
                                        pairingCodeExpiresAt = res.data.expires_at
                                        bannerMessage = "Code generated! Share this 6-digit code with your Guardian."
                                        HapticHelper.vibrateSuccess(context)
                                        refreshRelationships()
                                    }
                                    is DataResult.Failure -> {
                                        bannerMessage = res.message
                                    }
                                    else -> {
                                        bannerMessage = "Network error. Unable to generate code."
                                    }
                                }
                            }
                        },
                        enabled = !isGeneratingCode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics { contentDescription = "Generate pairing code button" },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isGeneratingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (generatedPairingCode != null) stringResource(R.string.guardian_btn_regenerate) else stringResource(R.string.guardian_btn_generate),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Section 2: Pending Requests
            val pendingRequests = guardianRelationships.filter { it.status == "pending" }
            if (pendingRequests.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.guardian_pending_requests)} (${pendingRequests.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )

                pendingRequests.forEach { rel ->
                    val guardian = guardianProfiles[rel.parent_id]
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Request from: ${guardian?.full_name ?: "Guardian / Parent"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = guardian?.email ?: "Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val relId = rel.id
                                        if (relId != null) {
                                            isRespondingToPairing = true
                                            coroutineScope.launch {
                                                val res = supabaseRepo.respondToPairingRequest(relId, approve = true)
                                                isRespondingToPairing = false
                                                if (res is DataResult.Success) {
                                                    bannerMessage = "Guardian approved successfully."
                                                    generatedPairingCode = null
                                                    HapticHelper.vibrateSuccess(context)
                                                    refreshRelationships()
                                                } else if (res is DataResult.Failure) {
                                                    bannerMessage = res.message
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isRespondingToPairing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.guardian_btn_approve))
                                }

                                OutlinedButton(
                                    onClick = {
                                        val relId = rel.id
                                        if (relId != null) {
                                            isRespondingToPairing = true
                                            coroutineScope.launch {
                                                val res = supabaseRepo.respondToPairingRequest(relId, approve = false)
                                                isRespondingToPairing = false
                                                if (res is DataResult.Success) {
                                                    bannerMessage = "Guardian request declined."
                                                    refreshRelationships()
                                                } else if (res is DataResult.Failure) {
                                                    bannerMessage = res.message
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isRespondingToPairing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(stringResource(R.string.guardian_btn_decline))
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Approved Guardians
            val approvedGuardians = guardianRelationships.filter { it.status == "approved" }
            Text(
                text = "${stringResource(R.string.guardian_approved_guardians)} (${approvedGuardians.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (approvedGuardians.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.guardian_no_linked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                approvedGuardians.forEach { rel ->
                    val guardian = guardianProfiles[rel.parent_id]
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = guardian?.full_name ?: "Linked Guardian",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = guardian?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Status: ${stringResource(R.string.guardian_status_active)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            TextButton(
                                onClick = {
                                    val relId = rel.id
                                    if (relId != null) {
                                        coroutineScope.launch {
                                            val res = supabaseRepo.respondToPairingRequest(relId, approve = false)
                                            if (res is DataResult.Success) {
                                                bannerMessage = "Guardian link revoked."
                                                refreshRelationships()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.guardian_btn_revoke))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

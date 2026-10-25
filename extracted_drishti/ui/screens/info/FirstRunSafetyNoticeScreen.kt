package com.example.drishti.ui.screens.info

import android.app.Activity
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.drishti.R
import com.example.drishti.ui.theme.DangerRed
import com.example.drishti.util.HapticHelper
import com.example.drishti.util.LocaleHelper
import com.example.drishti.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstRunSafetyNoticeScreen(
    onAcknowledge: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showFullSafetyDialog by remember { mutableStateOf(false) }

    val currentLangCode = remember { mutableStateOf(LocaleHelper.getSelectedLanguageCode(context)) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Quick language selector chips on first run
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("en", "EN", "English"),
                            Triple("hi", "हिन्दी", "हिन्दी"),
                            Triple("mr", "मराठी", "मराठी")
                        ).forEach { (code, label, displayName) ->
                            val isSelected = currentLangCode.value == code
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        currentLangCode.value = code
                                        LocaleHelper.setAppLanguage(context, code, displayName)
                                        HapticHelper.vibrateSuccess(context)
                                        context.findActivity()?.recreate()
                                    }
                                },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DangerRed.copy(alpha = 0.10f),
                    border = BorderStroke(1.5.dp, DangerRed.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DangerRed.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.first_run_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = DangerRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.first_run_subtitle),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Subtitle Guidance
                Text(
                    text = stringResource(R.string.splash_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 7 Concise Safety Points Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ConciseSafetyItem(Icons.Rounded.Assistant, stringResource(R.string.first_run_point_1))
                        ConciseSafetyItem(Icons.Rounded.CameraAlt, stringResource(R.string.first_run_point_2))
                        ConciseSafetyItem(Icons.Rounded.GpsFixed, stringResource(R.string.first_run_point_3))
                        ConciseSafetyItem(Icons.Rounded.Mic, stringResource(R.string.first_run_point_4))
                        ConciseSafetyItem(Icons.Rounded.CloudOff, stringResource(R.string.first_run_point_5))
                        ConciseSafetyItem(Icons.Rounded.Shield, stringResource(R.string.first_run_point_6))
                        ConciseSafetyItem(Icons.Rounded.LocalHospital, stringResource(R.string.first_run_point_7))
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val btnContinueText = stringResource(R.string.btn_understand_continue)
                val btnReadInfoText = stringResource(R.string.btn_read_safety_info)

                Button(
                    onClick = onAcknowledge,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = btnContinueText }
                ) {
                    Text(
                        text = btnContinueText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = { showFullSafetyDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .semantics { contentDescription = btnReadInfoText }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = btnReadInfoText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }

    if (showFullSafetyDialog) {
        Dialog(
            onDismissRequest = { showFullSafetyDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            SafetyNoticeScreen(
                onAcknowledge = {
                    showFullSafetyDialog = false
                    onAcknowledge()
                },
                showBackButton = true,
                onBack = { showFullSafetyDialog = false }
            )
        }
    }
}

@Composable
private fun ConciseSafetyItem(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

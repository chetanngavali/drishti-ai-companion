package com.example.drishti.homemapping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.drishti.homemapping.model.MapChangeLevel
import com.example.drishti.ui.theme.IntelligentBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapUpdateScreen(
    viewModel: HomeMappingViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeMap = uiState.activeMap
    val pendingChanges by viewModel.changeDetector.pendingChanges.collectAsState()

    val lightBg = Color(0xFFFAFCFF)
    val navyText = Color(0xFF0F172A)
    val grayText = Color(0xFF64748B)

    var appliedSuccess by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = lightBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.semantics { contentDescription = "Back to Home Mapping" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = navyText
                    )
                }
                Column {
                    Text(
                        text = "Furniture & Layout Changes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = navyText
                    )
                    Text(
                        text = "Automatic multi-observation change verification",
                        style = MaterialTheme.typography.bodySmall,
                        color = grayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (appliedSuccess) {
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF166534))
                        Text(
                            text = "Map version updated and changes synchronized securely!",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            if (pendingChanges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No pending map changes detected",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = navyText
                        )
                        Text(
                            text = "Your saved home map matches recent camera sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pendingChanges) { change ->
                        val (badgeBg, badgeFg) = when (change.changeLevel) {
                            MapChangeLevel.LEVEL_1_TEMPORARY -> Color(0xFFFEF3C7) to Color(0xFFB45309)
                            MapChangeLevel.LEVEL_2_PERSISTENT_OBJECT -> Color(0xFFE0F2FE) to IntelligentBlue
                            MapChangeLevel.LEVEL_3_ROOM_GEOMETRY -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0)))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = change.changeDescription,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = navyText
                                    )
                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = change.changeLevel.name.replace("LEVEL_", "L"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = badgeFg,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Observations: ${change.observationCount} • Discovered: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(change.timestampMs))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = grayText
                                )
                            }
                        }
                    }
                }

                if (activeMap != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            viewModel.applyConfirmedChanges(activeMap)
                            appliedSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Confirmed Changes to Map")
                    }
                }
            }
        }
    }
}

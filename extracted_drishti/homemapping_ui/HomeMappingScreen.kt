package com.example.drishti.homemapping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import com.example.drishti.homemapping.model.AdminReviewStatus
import com.example.drishti.homemapping.model.HomeMap
import com.example.drishti.ui.theme.IntelligentBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMappingScreen(
    viewModel: HomeMappingViewModel,
    onStartNewScan: () -> Unit = {},
    onContinueIncompleteScan: () -> Unit = {},
    onViewMap: (HomeMap) -> Unit = {},
    onReviewObjects: (HomeMap) -> Unit = {},
    onViewUpdates: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Light Theme Colors
    val lightBg = Color(0xFFFAFCFF)
    val lightSurface = Color(0xFFF0F8FF)
    val navyText = Color(0xFF0F172A)
    val grayText = Color(0xFF64748B)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = lightBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ─────────────────────────────────────────────────────────────
            // Header Bar
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.semantics { contentDescription = "Back to Main Navigation" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = navyText
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Home Mapping & 3D Intelligence",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = navyText
                    )
                    Text(
                        text = "Spatial floor plans, room geometry & obstacle intelligence",
                        style = MaterialTheme.typography.bodySmall,
                        color = grayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─────────────────────────────────────────────────────────────
            // Incomplete Scan Banner (Resume Capability)
            // ─────────────────────────────────────────────────────────────
            if (uiState.hasIncompleteScan) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onContinueIncompleteScan() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF59E0B)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PendingActions,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Incomplete Scan Found",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Tap to resume your previous room scan without losing data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                        }
                        Button(
                            onClick = { onContinueIncompleteScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Resume", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // Primary Action: Start New Home Scan
            // ─────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onStartNewScan() },
                colors = CardDefaults.cardColors(containerColor = IntelligentBlue)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddLocationAlt,
                            contentDescription = "Start Scan",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start New Home Scan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Guided room scan with camera & object recognition",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─────────────────────────────────────────────────────────────
            // Feature Quick Action Tiles (Review, Updates, Admin Review)
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Detected Objects Review Tile
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val active = uiState.activeMap ?: uiState.savedMaps.firstOrNull()
                            if (active != null) onReviewObjects(active)
                        },
                    colors = CardDefaults.cardColors(containerColor = lightSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = null,
                            tint = IntelligentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Review Objects",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = navyText
                        )
                        Text(
                            text = "Inspect detected furniture",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = grayText
                        )
                    }
                }

                // Map Updates Tile
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onViewUpdates() },
                    colors = CardDefaults.cardColors(containerColor = lightSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Update,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Map Updates",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = navyText
                        )
                        Text(
                            text = "Furniture changes",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = grayText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─────────────────────────────────────────────────────────────
            // Saved Homes Section
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "My Saved Homes (${uiState.savedMaps.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = navyText
                )
                if (uiState.savedMaps.isNotEmpty()) {
                    Text(
                        text = "Encrypted On-Device",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF16A34A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.savedMaps.isEmpty()) {
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
                            imageVector = Icons.Rounded.Map,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No saved home maps yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = navyText
                        )
                        Text(
                            text = "Tap 'Start New Home Scan' to create your 2D & 3D indoor map.",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.savedMaps) { map ->
                        SavedHomeCard(
                            map = map,
                            onViewClick = { onViewMap(map) },
                            onReviewClick = { onReviewObjects(map) },
                            onDeleteClick = { viewModel.deleteHomeMap(map.mapId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedHomeCard(
    map: HomeMap,
    onViewClick: () -> Unit,
    onReviewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val navyText = Color(0xFF0F172A)
    val grayText = Color(0xFF64748B)

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(map.updatedAtMs) { dateFormat.format(Date(map.updatedAtMs)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onViewClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = map.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = navyText
                    )
                    Text(
                        text = "Updated $formattedDate • Version ${map.currentVersion}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = grayText
                    )
                }

                // Review Status Chip
                val (statusBg, statusFg) = when (map.reviewStatus) {
                    AdminReviewStatus.APPROVED -> Color(0xFFDCFCE7) to Color(0xFF166534)
                    AdminReviewStatus.REVIEW_PENDING -> Color(0xFFFEF3C7) to Color(0xFFB45309)
                    AdminReviewStatus.RESCAN_REQUIRED -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
                    AdminReviewStatus.PROCESSING -> Color(0xFFE0F2FE) to IntelligentBlue
                    else -> Color(0xFFF1F5F9) to Color(0xFF475569)
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = map.reviewStatus.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Area: %.1f m²".format(map.totalFloorAreaSqMeters),
                    style = MaterialTheme.typography.bodySmall,
                    color = grayText
                )
                Text(
                    text = "Rooms: ${map.floors.sumOf { it.rooms.size }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = grayText
                )
                Text(
                    text = "Objects: ${map.totalObjectCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = grayText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.semantics { contentDescription = "Delete Map" }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onReviewClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Objects", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onViewClick,
                        colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("3D Map", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

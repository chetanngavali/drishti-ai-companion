package com.example.drishti.homemapping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drishti.homemapping.model.HouseholdObject
import com.example.drishti.homemapping.model.RoomBoundary
import com.example.drishti.homemapping.navigation.HomeMapIndoorNavigator
import com.example.drishti.ui.theme.IntelligentBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapViewerScreen(
    viewModel: HomeMappingViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val activeMap = uiState.activeMap

    val navigator = remember { HomeMapIndoorNavigator(context) }
    var selectedObject by remember { mutableStateOf<HouseholdObject?>(null) }
    var selectedRoom by remember { mutableStateOf<RoomBoundary?>(null) }
    var navigationStatusMessage by remember { mutableStateOf<String?>(null) }

    // Light Theme Architectural Canvas Colors
    val lightBg = Color(0xFFFAFCFF)
    val canvasBg = Color(0xFFF1F5F9)
    val navyText = Color(0xFF0F172A)
    val grayText = Color(0xFF64748B)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = lightBg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─────────────────────────────────────────────────────────────
            // Header Bar
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Back to Home Mapping Dashboard" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = navyText
                        )
                    }
                    Column {
                        Text(
                            text = activeMap?.displayName ?: "Home 3D Map",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = navyText
                        )
                        Text(
                            text = "Area: %.1f m² • Objects: ${activeMap?.totalObjectCount ?: 0}".format(activeMap?.totalFloorAreaSqMeters ?: 0f),
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }

                // 2D / 3D Mode Toggle Button
                Button(
                    onClick = { viewModel.toggle3DViewMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.is3DViewMode) Icons.Rounded.ViewInAr else Icons.Rounded.Map,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.is3DViewMode) "3D View" else "2D Plan", fontSize = 13.sp)
                }
            }

            // ─────────────────────────────────────────────────────────────
            // Viewer Filter / Toggle Bar
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.showObjectLabels,
                    onClick = { viewModel.toggleObjectLabels() },
                    label = { Text("Labels", fontSize = 12.sp) },
                    leadingIcon = {
                        if (uiState.showObjectLabels) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                )

                FilterChip(
                    selected = uiState.showRoomBoundaries,
                    onClick = { viewModel.toggleRoomBoundaries() },
                    label = { Text("Boundaries", fontSize = 12.sp) },
                    leadingIcon = {
                        if (uiState.showRoomBoundaries) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                )

                // Room Selector Chip if rooms exist
                val rooms = activeMap?.floors?.flatMap { it.rooms } ?: emptyList()
                if (rooms.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "${rooms.size} Rooms",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = navyText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // Interactive 2D / 3D Canvas Viewport
            // ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(canvasBg)
            ) {
                if (activeMap != null) {
                    if (uiState.is3DViewMode) {
                        HomeMap3DView(
                            map = activeMap,
                            selectedRoomId = selectedRoom?.roomId,
                            selectedObjectId = selectedObject?.objectId,
                            onRoomSelected = { selectedRoom = it },
                            onObjectSelected = { selectedObject = it }
                        )
                    } else {
                        HomeMapCanvas2D(
                            map = activeMap,
                            selectedRoomId = selectedRoom?.roomId,
                            selectedObjectId = selectedObject?.objectId,
                            onRoomSelected = { selectedRoom = it },
                            onObjectSelected = { selectedObject = it }
                        )
                    }

                    // Floating Navigation HUD / Controls Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = if (uiState.is3DViewMode) "Drag: Rotate | Pinch: Zoom" else "Pan & Pinch to zoom",
                                style = MaterialTheme.typography.labelSmall,
                                color = grayText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No map loaded", color = grayText)
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // Object / Room Inspector Bottom Sheet
            // ─────────────────────────────────────────────────────────────
            val inspectedObject = selectedObject ?: activeMap?.floors?.flatMap { it.rooms }?.flatMap { it.objects }?.firstOrNull()
            if (inspectedObject != null && activeMap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Category,
                                    contentDescription = null,
                                    tint = IntelligentBlue
                                )
                                Text(
                                    text = inspectedObject.category.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = navyText
                                )
                            }
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "%.0f%% Conf".format(inspectedObject.imageConfidence * 100),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Dimensions & Position info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Size: %.1fm × %.1fm × %.1fm".format(
                                    inspectedObject.dimensions.width,
                                    inspectedObject.dimensions.depth,
                                    inspectedObject.dimensions.height
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = grayText
                            )
                            Text(
                                text = "Pos: (%.1f, %.1f)".format(inspectedObject.position.x, inspectedObject.position.z),
                                style = MaterialTheme.typography.bodySmall,
                                color = grayText
                            )
                        }

                        if (navigationStatusMessage != null) {
                            Text(
                                text = navigationStatusMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = IntelligentBlue
                            )
                        }

                        // Navigation Action Button
                        Button(
                            onClick = {
                                val rooms = activeMap.floors.flatMap { it.rooms }
                                val targetRoom = rooms.firstOrNull { it.roomId == inspectedObject.roomId } ?: rooms.firstOrNull()
                                val startRoom = rooms.lastOrNull() ?: targetRoom

                                if (startRoom != null && targetRoom != null) {
                                    val route = navigator.planRoute(activeMap, startRoom.roomId, targetRoom.roomId)
                                    if (route != null) {
                                        val firstStep = route.steps.firstOrNull()
                                        val stepText = if (firstStep != null) navigator.getMultilingualStepInstruction(firstStep, "en") else "Proceed toward ${inspectedObject.category.displayName}"
                                        navigationStatusMessage = "Route: ${route.fromLocationName} -> ${route.toLocationName} (%.1fm). Step 1: $stepText".format(route.totalDistanceMeters)
                                    } else {
                                        navigationStatusMessage = "Navigation ready at destination: ${inspectedObject.category.displayName}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Rounded.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigate to ${inspectedObject.category.displayName}")
                        }
                    }
                }
            }
        }
    }
}

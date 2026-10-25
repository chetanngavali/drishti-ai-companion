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
import androidx.compose.ui.unit.sp
import com.example.drishti.ui.theme.IntelligentBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapReviewScreen(
    viewModel: HomeMappingViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeMap = uiState.activeMap

    val lightBg = Color(0xFFFAFCFF)
    val navyText = Color(0xFF0F172A)
    val grayText = Color(0xFF64748B)

    var submittedForReview by remember { mutableStateOf(false) }

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
                        text = "Review Detected Objects",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = navyText
                    )
                    Text(
                        text = "Verify furniture, positions & spatial confidence",
                        style = MaterialTheme.typography.bodySmall,
                        color = grayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeMap == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active map to review", color = grayText)
                }
            } else {
                val allObjects = activeMap.floors.flatMap { f -> f.rooms.flatMap { r -> r.objects } }

                if (submittedForReview) {
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
                                text = "Map submitted for admin review successfully!",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }

                if (allObjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No objects detected in this map yet.", color = grayText)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allObjects) { obj ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0)))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = obj.category.displayName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = navyText
                                        )
                                        Text(
                                            text = "Position: (%.1f, %.1f) • Size: %.1fm × %.1fm".format(
                                                obj.position.x,
                                                obj.position.z,
                                                obj.dimensions.width,
                                                obj.dimensions.depth
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = grayText
                                        )
                                        Text(
                                            text = "Confidence: %.0f%% • Uncertainty: ±%.2fm".format(
                                                obj.imageConfidence * 100,
                                                obj.measurementUncertaintyMeters
                                            ),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = IntelligentBlue
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = "Verified Object",
                                            tint = Color(0xFF166534),
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        activeMap.mapId.let { viewModel.submitMapForAdminReview(it) }
                        submittedForReview = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Map for Quality Verification")
                }
            }
        }
    }
}

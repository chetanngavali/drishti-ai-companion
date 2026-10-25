package com.example.drishti.homemapping.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.drishti.homemapping.model.MappingMode
import com.example.drishti.homemapping.model.RoomBoundary
import com.example.drishti.ui.theme.IntelligentBlue
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScanScreen(
    viewModel: HomeMappingViewModel,
    onScanFinished: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var roomNameInput by remember { mutableStateOf("Living Room") }
    var showGuidedDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showAddCornerDialog by remember { mutableStateOf(false) }
    var inputCornerX by remember { mutableStateOf("") }
    var inputCornerZ by remember { mutableStateOf("") }

    val currentPoints by viewModel.scanner.currentRoomPoints.collectAsState()
    val activeMode by viewModel.scanner.activeMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Real-time calculated polygon area (Shoelace)
    val calculatedArea = remember(currentPoints) {
        RoomBoundary(roomName = "Scan Area", polygonPoints = currentPoints).calculatedAreaSqMeters
    }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ─────────────────────────────────────────────────────────────
            // Top Bar & Capability Mode Badge
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = "Guided Home Scan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = navyText
                        )
                        Text(
                            text = "Scan room boundaries & corners",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }

                // Capability Mode Indicator Chip
                Surface(
                    color = when (activeMode) {
                        MappingMode.MODE_A_ARCORE_DEPTH -> Color(0xFFDCFCE7)
                        MappingMode.MODE_B_CAMERA_ONLY_CALIBRATED -> Color(0xFFE0F2FE)
                        MappingMode.MODE_C_GUIDED_FALLBACK -> Color(0xFFFEF3C7)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (activeMode) {
                            MappingMode.MODE_A_ARCORE_DEPTH -> "Mode A: AR/Depth"
                            MappingMode.MODE_B_CAMERA_ONLY_CALIBRATED -> "Mode B: Camera"
                            MappingMode.MODE_C_GUIDED_FALLBACK -> "Mode C: Manual"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (activeMode) {
                            MappingMode.MODE_A_ARCORE_DEPTH -> Color(0xFF166534)
                            MappingMode.MODE_B_CAMERA_ONLY_CALIBRATED -> IntelligentBlue
                            MappingMode.MODE_C_GUIDED_FALLBACK -> Color(0xFFB45309)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─────────────────────────────────────────────────────────────
            // Viewfinder / CameraX Stream or Permission Prompt
            // ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, IntelligentBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val executor = Executors.newSingleThreadExecutor()

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    // Camera frame is available for tracking & visual feature extraction
                                    imageProxy.close()
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (exc: Exception) {
                                    // Handle camera binding failure
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay HUD on camera feed
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top HUD Info Pill
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Vertices: ${currentPoints.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Live Area: %.1f m²".format(calculatedArea),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        // Crosshair Target Guide in center
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CenterFocusStrong,
                                contentDescription = "Aim at corner",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        // Guidance Text
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "Aim at room corner floor point and tap 'Add Corner'",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Camera access is needed to scan room corners and household objects.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue)
                        ) {
                            Text("Grant Camera Access")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─────────────────────────────────────────────────────────────
            // Controls & Room Actions
            // ─────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = lightSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = roomNameInput,
                            onValueChange = { roomNameInput = it },
                            label = { Text("Room Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Mode Selector / Fallback Button
                        OutlinedButton(
                            onClick = { showGuidedDialog = true },
                            modifier = Modifier.semantics { contentDescription = "Switch to Mode C Manual Guided Room" }
                        ) {
                            Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manual", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showCalibrationDialog = true },
                            modifier = Modifier.semantics { contentDescription = "Calibrate Metric Scale" }
                        ) {
                            Icon(Icons.Rounded.Straighten, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scale", fontSize = 12.sp)
                        }
                    }

                    // Vertex Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (currentPoints.isEmpty()) {
                                    // First corner is recorded at room origin (0, 0, 0)
                                    viewModel.addScanVertex(0f, 0f, 0f)
                                } else {
                                    // Prompt for user-measured or visual offset of next corner
                                    val lastPoint = currentPoints.last()
                                    inputCornerX = "%.1f".format(lastPoint.x)
                                    inputCornerZ = "%.1f".format(lastPoint.z + 3.0f)
                                    showAddCornerDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .semantics { contentDescription = "Add room corner vertex" }
                        ) {
                            Icon(imageVector = Icons.Rounded.AddLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Corner")
                        }

                        OutlinedButton(
                            onClick = { viewModel.undoLastVertex() },
                            enabled = currentPoints.isNotEmpty(),
                            modifier = Modifier
                                .weight(0.8f)
                                .height(48.dp)
                                .semantics { contentDescription = "Undo last corner" }
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.Undo, contentDescription = null)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Undo")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.pauseAndSaveIncompleteScan(homeName = "My Home", currentRoomName = roomNameInput)
                            },
                            modifier = Modifier
                                .weight(0.8f)
                                .height(48.dp)
                                .semantics { contentDescription = "Pause and save scan for later" }
                        ) {
                            Icon(imageVector = Icons.Rounded.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Pause")
                        }
                    }

                    // Finish Scan Action
                    Button(
                        onClick = {
                            viewModel.finishRoomScanAndSave(roomName = roomNameInput)
                            onScanFinished()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .semantics { contentDescription = "Finish scan and save encrypted map" }
                    ) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Finish & Save Map (${currentPoints.size} corners, %.1f m²)".format(calculatedArea),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Mode C: Manual Guided Room Fallback Dialog
    // ─────────────────────────────────────────────────────────────
    if (showGuidedDialog) {
        var guidedWidth by remember { mutableStateOf("4.2") }
        var guidedLength by remember { mutableStateOf("5.0") }

        AlertDialog(
            onDismissRequest = { showGuidedDialog = false },
            title = { Text("Mode C: Guided Room Fallback") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "For phones without depth sensors or difficult lighting, enter measured room dimensions directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = grayText
                    )
                    OutlinedTextField(
                        value = guidedWidth,
                        onValueChange = { guidedWidth = it },
                        label = { Text("Width (meters)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = guidedLength,
                        onValueChange = { guidedLength = it },
                        label = { Text("Length (meters)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = guidedWidth.toFloatOrNull() ?: 4.0f
                        val l = guidedLength.toFloatOrNull() ?: 5.0f
                        viewModel.addGuidedRoom(roomNameInput, l, w)
                        showGuidedDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue)
                ) {
                    Text("Add Room")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuidedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Mode B: Scale Calibration Dialog (Door reference 2.0m)
    // ─────────────────────────────────────────────────────────────
    if (showCalibrationDialog) {
        var refMeters by remember { mutableStateOf("2.0") }

        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = { Text("Scale Calibration (Mode B)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Calibrate monocular metric scale using a standard known dimension such as standard doorway height (typically 2.0 meters).",
                        style = MaterialTheme.typography.bodySmall,
                        color = grayText
                    )
                    OutlinedTextField(
                        value = refMeters,
                        onValueChange = { refMeters = it },
                        label = { Text("Reference Dimension (meters)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ref = refMeters.toFloatOrNull() ?: 2.0f
                        viewModel.calibrateScale(ref, 600f)
                        showCalibrationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue)
                ) {
                    Text("Apply Calibration")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalibrationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Manual/Visual Corner Input Dialog
    // ─────────────────────────────────────────────────────────────
    if (showAddCornerDialog) {
        AlertDialog(
            onDismissRequest = { showAddCornerDialog = false },
            title = { Text("Add Corner #${currentPoints.size + 1}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter the spatial coordinates (in meters) for this corner relative to the room origin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = grayText
                    )
                    OutlinedTextField(
                        value = inputCornerX,
                        onValueChange = { inputCornerX = it },
                        label = { Text("X position (meters)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputCornerZ,
                        onValueChange = { inputCornerZ = it },
                        label = { Text("Z position (meters)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val x = inputCornerX.toFloatOrNull() ?: 0f
                        val z = inputCornerZ.toFloatOrNull() ?: 0f
                        viewModel.addScanVertex(x, 0f, z)
                        showAddCornerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IntelligentBlue)
                ) {
                    Text("Add Vertex")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCornerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

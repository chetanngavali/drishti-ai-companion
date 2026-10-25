package com.example.drishti.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.drishti.R
import com.example.drishti.hardware.Esp32ConnectionState
import com.example.drishti.ui.theme.*
import com.example.drishti.util.HapticHelper
import com.example.drishti.vision.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    onNavigateBack: () -> Unit = {},
    onTriggerSos: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val visionManager = remember { VisionManager(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    DisposableEffect(context) {
        onDispose {
            visionManager.shutdown()
            analysisExecutor.shutdown()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            visionManager.announce("Camera permission granted. Real-time perception active.", PriorityLevel.IMPORTANT_SIGN, context)
        } else {
            visionManager.announce("Camera permission is required for visual mobility assistance.", PriorityLevel.SAFETY_WARNING, context)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var activeMode by remember { mutableStateOf("ASSIST") }
    var isFlashOn by remember { mutableStateOf(false) }
    var isExpandedView by remember { mutableStateOf(false) }

    // Smart Cane & Camera Stream State
    val hardwareBridge = remember { visionManager.hardwareDistanceBridge }
    val esp32State by hardwareBridge.connectionState.collectAsState()

    val blePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            hardwareBridge.startScan()
        }
    }

    val sharedPrefs = remember { context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE) }
    var cameraInputMode by remember {
        mutableStateOf(sharedPrefs.getString("camera_input_mode", "AUTO") ?: "AUTO")
    }

    val esp32CamManager = remember { visionManager.esp32CamManager }
    val esp32CamState by esp32CamManager.connectionState.collectAsState()
    val esp32CamFrame by esp32CamManager.latestFrame.collectAsState()
    val esp32CamFps by esp32CamManager.streamFps.collectAsState()
    val esp32CamError by esp32CamManager.lastError.collectAsState()

    val isUsingSmartCaneCamera = when (cameraInputMode) {
        "PHONE" -> false
        "ESP32_CAM", "SMART_CANE" -> true
        else -> (esp32CamState == Esp32CamConnectionState.STREAMING || esp32CamFrame != null)
    }

    // AI Perception Pipeline analysis throttled to 3 Hz (1 frame every 300ms) in background thread
    var lastAnalysisTimeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(esp32CamFrame) {
        val frame = esp32CamFrame
        val now = System.currentTimeMillis()
        if (isUsingSmartCaneCamera && frame != null && now - lastAnalysisTimeMs >= 300L) {
            lastAnalysisTimeMs = now
            withContext(Dispatchers.Default) {
                visionManager.perceptionPipeline.analyzeBitmap(frame, rotationDegrees = 0)
            }
        }
    }

    // Colors matching the exact design specification
    val lightBg = Color(0xFFF8FAFC)
    val vividBlue = Color(0xFF1A73E8)
    val lightBlueSurface = Color(0xFFF0F6FE)
    val lightBluePill = Color(0xFFE2EDFE)
    val navyText = Color(0xFF0F172A)
    val graySubtext = Color(0xFF64748B)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = lightBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─────────────────────────────────────────────────────────────
            // 2A. HEADER (Back, Blue Icon, Title/Subtitle, SOS)
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Back Arrow
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = "Navigate Back",
                            tint = navyText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Rounded Square Blue Camera Icon
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = vividBlue,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Title & Subtitle
                    Column {
                        Text(
                            text = "Scan",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = navyText
                        )
                        Text(
                            text = "Live Camera",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp
                            ),
                            color = graySubtext
                        )
                    }
                }

                // Red Rounded SOS Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.clickable {
                        HapticHelper.vibrateWarning(context)
                        onTriggerSos()
                        try {
                            val intent = Intent(context, EmergencyActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SOS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─────────────────────────────────────────────────────────────
            // 2B. CAMERA CONNECTION STATUS STRIP & PAIR BUTTON
            // ─────────────────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = lightBlueSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val isConnected = esp32CamState == Esp32CamConnectionState.STREAMING || isUsingSmartCaneCamera
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) SuccessGreen else WarningYellow)
                        )
                        Text(
                            text = when {
                                esp32CamState == Esp32CamConnectionState.STREAMING -> "ESP32-CAM Connected"
                                esp32CamState == Esp32CamConnectionState.CONNECTING || esp32CamState == Esp32CamConnectionState.AUTHENTICATING -> "Connecting ESP32..."
                                esp32State == Esp32ConnectionState.SCANNING -> "Searching Cane..."
                                isUsingSmartCaneCamera -> "ESP32-CAM Active"
                                else -> "Phone Camera Active"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = navyText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Right Side Actions: Pair/Stream Button + FPS Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "Connect ESP32 / Pair" Action Button
                        if (esp32CamState == Esp32CamConnectionState.STREAMING) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.clickable {
                                    visionManager.disconnectEsp32Cam()
                                    cameraInputMode = "PHONE"
                                    sharedPrefs.edit().putString("camera_input_mode", "PHONE").apply()
                                    HapticHelper.vibrateInfo(context)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = null, tint = DangerRed, modifier = Modifier.size(14.dp))
                                    Text(stringResource(R.string.btn_disconnect), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = DangerRed)
                                }
                            }
                        } else if (esp32State == Esp32ConnectionState.SCANNING || esp32CamState == Esp32CamConnectionState.CONNECTING) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = lightBluePill
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = vividBlue)
                                    Text(stringResource(R.string.status_connecting), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = vividBlue)
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = vividBlue,
                                modifier = Modifier.clickable {
                                    HapticHelper.vibrateInfo(context)
                                    cameraInputMode = "SMART_CANE"
                                    sharedPrefs.edit().putString("camera_input_mode", "SMART_CANE").apply()

                                    // Trigger BLE scan and stream connection
                                    if (hardwareBridge.bleManager.hasBlePermissions()) {
                                        hardwareBridge.startScan()
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        blePermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
                                    }

                                    val streamUrl = sharedPrefs.getString("esp32_cam_url", "http://192.168.4.1/stream") ?: "http://192.168.4.1/stream"
                                    visionManager.startRealCaneStreaming(streamUrl)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.BluetoothSearching, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text(stringResource(R.string.scan_btn_pair_stream), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color.White)
                                }
                            }
                        }

                        // FPS Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = lightBluePill
                        ) {
                            Text(
                                text = if (esp32CamFps > 0f) "%.1f FPS".format(esp32CamFps) else "Live FPS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = vividBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─────────────────────────────────────────────────────────────
            // 2C. MAIN CAMERA PREVIEW VIEWPORT
            // ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, vividBlue, RoundedCornerShape(28.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Live Stream Video Feed / CameraX Fallback
                if (isUsingSmartCaneCamera && esp32CamFrame != null) {
                    Image(
                        bitmap = esp32CamFrame!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.scan_live_feed_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (hasCameraPermission) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    imageCapture = ImageCapture.Builder().build()
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                        .build().also {
                                            it.setAnalyzer(analysisExecutor, visionManager.perceptionPipeline)
                                        }
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture,
                                        imageAnalysis
                                    )
                                } catch (_: Exception) {}
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Camera permission required for Vision Workspace",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = vividBlue)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }

                // If ESP32 stream is connecting or unavailable
                if (isUsingSmartCaneCamera && esp32CamFrame == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (esp32CamState == Esp32CamConnectionState.CONNECTING || esp32CamState == Esp32CamConnectionState.AUTHENTICATING) {
                                CircularProgressIndicator(color = vividBlue, modifier = Modifier.size(36.dp))
                                Text(stringResource(R.string.scan_connecting_stream), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Icon(Icons.Rounded.VideocamOff, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(42.dp))
                                Text(
                                    text = esp32CamError ?: stringResource(R.string.scan_stream_disconnected),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        val streamUrl = sharedPrefs.getString("esp32_cam_url", "http://192.168.4.1/stream") ?: "http://192.168.4.1/stream"
                                        visionManager.startRealCaneStreaming(streamUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = vividBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.scan_start_stream))
                                }
                            }
                        }
                    }
                }

                // ── Top-Left "● Live" Badge ──
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Text(
                            text = "Live",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // ── Top-Right Fullscreen / Expand Icon ──
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clickable {
                            isExpandedView = !isExpandedView
                            HapticHelper.vibrateInfo(context)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpandedView) Icons.Rounded.FullscreenExit else Icons.Rounded.CropFree,
                            contentDescription = "Expand Camera View",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // ── Central Focus / Scan Reticle ──
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.Center)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 3.dp.toPx()
                        val len = 22.dp.toPx()
                        val color = Color.White.copy(alpha = 0.85f)

                        // Top-Left ┌
                        drawLine(color, start = Offset(0f, 0f), end = Offset(len, 0f), strokeWidth = stroke)
                        drawLine(color, start = Offset(0f, 0f), end = Offset(0f, len), strokeWidth = stroke)

                        // Top-Right ┐
                        drawLine(color, start = Offset(size.width, 0f), end = Offset(size.width - len, 0f), strokeWidth = stroke)
                        drawLine(color, start = Offset(size.width, 0f), end = Offset(size.width, len), strokeWidth = stroke)

                        // Bottom-Left └
                        drawLine(color, start = Offset(0f, size.height), end = Offset(len, size.height), strokeWidth = stroke)
                        drawLine(color, start = Offset(0f, size.height), end = Offset(0f, size.height - len), strokeWidth = stroke)

                        // Bottom-Right ┘
                        drawLine(color, start = Offset(size.width, size.height), end = Offset(size.width - len, size.height), strokeWidth = stroke)
                        drawLine(color, start = Offset(size.width, size.height), end = Offset(size.width, size.height - len), strokeWidth = stroke)

                        // Center Crosshair +
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val cross = 10.dp.toPx()
                        drawLine(color, start = Offset(cx - cross, cy), end = Offset(cx + cross, cy), strokeWidth = stroke)
                        drawLine(color, start = Offset(cx, cy - cross), end = Offset(cx, cy + cross), strokeWidth = stroke)
                    }
                }

                // ── Bottom Viewport Controls Row (Flashlight, Shutter, Camera Switch) ──
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Control: Flashlight / Torch Button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.50f),
                        modifier = Modifier
                            .size(52.dp)
                            .clickable {
                                isFlashOn = !isFlashOn
                                esp32CamManager.toggleFlash()
                                HapticHelper.vibrateInfo(context)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                                contentDescription = stringResource(R.string.scan_toggle_flash),
                                tint = if (isFlashOn) WarningYellow else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Center Control: Large Double-Ring Capture / Scan Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = vividBlue,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    HapticHelper.vibrateSuccess(context)
                                    val modeName = when (activeMode) {
                                        "PATH" -> "Checking walking path"
                                        "DESCRIBE" -> "Describing scene"
                                        else -> "Assisting mobility path"
                                    }
                                    visionManager.announce(modeName, PriorityLevel.IMPORTANT_SIGN, context)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = stringResource(R.string.scan_capture),
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Right Control: Camera Switch (Cane Cam ↔ Phone Cam)
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.50f),
                        modifier = Modifier
                            .size(52.dp)
                            .clickable {
                                val nextMode = if (isUsingSmartCaneCamera) "PHONE" else "SMART_CANE"
                                cameraInputMode = nextMode
                                sharedPrefs.edit().putString("camera_input_mode", nextMode).apply()
                                HapticHelper.vibrateInfo(context)
                                val label = if (nextMode == "SMART_CANE") "Switched to Smart Cane camera" else "Switched to phone camera"
                                visionManager.announce(label, PriorityLevel.IMPORTANT_SIGN, context)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Cameraswitch,
                                contentDescription = stringResource(R.string.scan_switch_camera),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─────────────────────────────────────────────────────────────
            // 2D. BOTTOM MODE SELECTOR (Path, Assist, Describe)
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Path
                val isPathSelected = (activeMode == "PATH")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isPathSelected) vividBlue else lightBlueSurface,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            activeMode = "PATH"
                            HapticHelper.vibrateInfo(context)
                            visionManager.announce("Path mode active.", PriorityLevel.IMPORTANT_SIGN, context)
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                            contentDescription = null,
                            tint = if (isPathSelected) Color.White else vividBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.scan_mode_path),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = if (isPathSelected) Color.White else navyText
                        )
                    }
                }

                // Button 2: Assist (Selected by default)
                val isAssistSelected = (activeMode == "ASSIST")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAssistSelected) vividBlue else lightBlueSurface,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            activeMode = "ASSIST"
                            HapticHelper.vibrateInfo(context)
                            visionManager.announce("Assist mode active.", PriorityLevel.IMPORTANT_SIGN, context)
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (isAssistSelected) Color.White else vividBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.scan_mode_assist),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = if (isAssistSelected) Color.White else navyText
                        )
                    }
                }

                // Button 3: Describe
                val isDescribeSelected = (activeMode == "DESCRIBE")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDescribeSelected) vividBlue else lightBlueSurface,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            activeMode = "DESCRIBE"
                            HapticHelper.vibrateInfo(context)
                            visionManager.announce("Describe mode active.", PriorityLevel.IMPORTANT_SIGN, context)
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = if (isDescribeSelected) Color.White else vividBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.scan_mode_describe),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = if (isDescribeSelected) Color.White else navyText
                        )
                    }
                }
            }
        }
    }
}

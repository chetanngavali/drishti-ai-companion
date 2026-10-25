package com.example.drishti.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.drishti.R
import com.example.drishti.hardware.HardwareDistanceBridge
import com.example.drishti.hardware.Esp32ConnectionState
import com.example.drishti.vision.Esp32CamConnectionState
import com.example.drishti.ui.theme.IntelligentBlue
import com.example.drishti.ui.theme.SuccessGreen
import com.example.drishti.util.HapticHelper
import com.example.drishti.vision.VisionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescribeScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val visionManager = remember { VisionManager(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val hardwareBridge = remember { HardwareDistanceBridge.getInstance(context) }
    val hardwareBridgeState by hardwareBridge.connectionState.collectAsState()
    val esp32CamManager = remember { visionManager.esp32CamManager }
    val esp32CamState by esp32CamManager.connectionState.collectAsState()
    val esp32CamFrame by esp32CamManager.latestFrame.collectAsState()

    val isEsp32Connected = hardwareBridgeState == Esp32ConnectionState.CONNECTED ||
        hardwareBridgeState == Esp32ConnectionState.STREAMING ||
        esp32CamState == Esp32CamConnectionState.STREAMING ||
        esp32CamState == Esp32CamConnectionState.CONNECTING

    // When Smart Cane / ESP32 is connected, release phone camera hardware immediately
    LaunchedEffect(isEsp32Connected) {
        if (isEsp32Connected) {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        cameraProviderFuture.get().unbindAll()
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(context))
            } catch (_: Exception) {}
        }
    }

    val speechEngine = remember { com.example.drishti.audio.SpeechEngineManager(context) }
    var sceneDescription by remember { mutableStateOf("Initializing camera and AI vision...") }
    var isAnalyzing by remember { mutableStateOf(true) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    DisposableEffect(context) {
        onDispose {
            speechEngine.shutdown()
            visionManager.shutdown()
        }
    }

    val runDescribe = {
        val currentBitmap = if (isEsp32Connected && esp32CamFrame != null) {
            esp32CamFrame
        } else null

        if (currentBitmap != null) {
            isAnalyzing = true
            sceneDescription = "Analyzing surroundings with Gemini AI..."
            coroutineScope.launch(Dispatchers.IO) {
                val desc = visionManager.describeSurroundings(currentBitmap)
                withContext(Dispatchers.Main) {
                    isAnalyzing = false
                    sceneDescription = desc
                    speechEngine.speakAsync(desc, priority = com.example.drishti.audio.SpeechPriority.HIGH)
                    HapticHelper.vibrateSuccess(context)
                }
            }
        } else {
            val cap = imageCapture
            if (cap != null) {
                isAnalyzing = true
                sceneDescription = "Analyzing surroundings with Gemini AI..."
                val executor = Executors.newSingleThreadExecutor()
                cap.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = image.toBitmap()
                        image.close()
                        coroutineScope.launch(Dispatchers.IO) {
                            val desc = visionManager.describeSurroundings(bitmap)
                            withContext(Dispatchers.Main) {
                                isAnalyzing = false
                                sceneDescription = desc
                                speechEngine.speakAsync(desc, priority = com.example.drishti.audio.SpeechPriority.HIGH)
                                HapticHelper.vibrateSuccess(context)
                            }
                        }
                    }
                    override fun onError(exception: ImageCaptureException) {
                        coroutineScope.launch(Dispatchers.Main) {
                            isAnalyzing = false
                            sceneDescription = "Failed to capture image: ${exception.localizedMessage}"
                        }
                    }
                })
            } else {
                isAnalyzing = false
                sceneDescription = "Camera capture not initialized."
            }
        }
    }

    LaunchedEffect(imageCapture) {
        if (!isEsp32Connected && imageCapture != null) {
            runDescribe()
        }
    }

    LaunchedEffect(esp32CamFrame) {
        if (isEsp32Connected && esp32CamFrame != null && sceneDescription.contains("Initializing")) {
            runDescribe()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isEsp32Connected) {
            if (esp32CamFrame != null) {
                Image(
                    bitmap = esp32CamFrame!!.asImageBitmap(),
                    contentDescription = "Smart Cane Camera View",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF151922)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = SuccessGreen,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Smart Cane Connected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = when (esp32CamState) {
                                Esp32CamConnectionState.AUTHENTICATING -> "Authenticating with Smart Cane..."
                                Esp32CamConnectionState.CONNECTING -> "Connecting to Smart Cane camera..."
                                Esp32CamConnectionState.RECONNECTING -> "Reconnecting to Smart Cane camera..."
                                Esp32CamConnectionState.AUTH_FAILED -> "Authentication failed (401)"
                                Esp32CamConnectionState.UNAVAILABLE -> "Camera unavailable (503)"
                                Esp32CamConnectionState.TIMED_OUT -> "Camera connection timed out"
                                Esp32CamConnectionState.ERROR -> "Camera offline"
                                Esp32CamConnectionState.DISCONNECTED -> "Camera Wi-Fi disconnected"
                                else -> "Streaming Smart Cane external camera..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder().build()
                        imageCapture = capture
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
        }

        // Overlay Telemetry & Minimal AI Scene Understanding View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top telemetry bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.semantics { contentDescription = "Telemetry: AI Vision Active" }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(SuccessGreen)
                        )
                        Text(
                            text = stringResource(R.string.describe_ai_active),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateBack,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.camera_hud_close))
                }
            }

            // Bottom AI Scene Understanding Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .semantics { contentDescription = "AI Scene Description: $sceneDescription" }
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = IntelligentBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.describe_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }

                    Text(
                        text = sceneDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                speechEngine.speakAsync(sceneDescription, priority = com.example.drishti.audio.SpeechPriority.HIGH)
                                HapticHelper.vibrateSuccess(context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.describe_repeat_audio))
                        }

                        OutlinedButton(
                            onClick = {
                                runDescribe()
                                HapticHelper.vibrateInfo(context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.describe_rescan))
                        }
                    }
                }
            }
        }
    }
}

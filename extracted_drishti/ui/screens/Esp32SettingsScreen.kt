package com.example.drishti.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drishti.hardware.Esp32ConnectionState
import com.example.drishti.hardware.HardwareDistanceBridge
import com.example.drishti.ui.theme.*
import com.example.drishti.util.HapticHelper
import com.example.drishti.vision.Esp32CamConnectionState
import com.example.drishti.vision.Esp32CamManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Esp32SettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE) }

    val hardwareBridge = remember { HardwareDistanceBridge.getInstance(context) }
    val esp32BleState by hardwareBridge.connectionState.collectAsState()
    val tofReading by hardwareBridge.latestReading.collectAsState()
    val caneLastError by hardwareBridge.lastErrorMessage.collectAsState()

    val esp32CamManager = remember { Esp32CamManager.getInstance() }
    val camState by esp32CamManager.connectionState.collectAsState()
    val camFrame by esp32CamManager.latestFrame.collectAsState()
    val camFps by esp32CamManager.streamFps.collectAsState()
    val camError by esp32CamManager.lastError.collectAsState()

    var streamUrl by remember {
        mutableStateOf(sharedPrefs.getString("esp32_cam_url", "http://192.168.4.1/stream") ?: "http://192.168.4.1/stream")
    }

    var cameraInputMode by remember {
        mutableStateOf(sharedPrefs.getString("camera_input_mode", "AUTO") ?: "AUTO")
    }

    var warningThresholdCm by remember {
        mutableStateOf(sharedPrefs.getFloat("tof_warning_threshold_cm", 150f))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        text = "ESP32 Smart Cane Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Hardware Cane Prototype & Camera Controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Camera Input Selection Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.Videocam, contentDescription = null, tint = IntelligentBlue)
                                Text(
                                    text = "Camera Source Selection",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Text(
                                text = "Choose which camera is used for visual scanning and object detection.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            listOf(
                                "AUTO" to "Automatic (Smart Cane camera when connected, phone camera fallback)",
                                "SMART_CANE" to "Smart Cane ESP32-CAM Only",
                                "PHONE" to "Phone Built-in Camera Only"
                            ).forEach { (mode, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            cameraInputMode = mode
                                            sharedPrefs.edit().putString("camera_input_mode", mode).apply()
                                            HapticHelper.vibrateInfo(context)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = cameraInputMode == mode,
                                        onClick = {
                                            cameraInputMode = mode
                                            sharedPrefs.edit().putString("camera_input_mode", mode).apply()
                                            HapticHelper.vibrateInfo(context)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (cameraInputMode == mode) FontWeight.Bold else FontWeight.Normal),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. ESP32-CAM Video Stream Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.Camera, contentDescription = null, tint = IntelligentBlue)
                                Text(
                                    text = "ESP32-CAM Video Stream",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Stream URL Input
                            OutlinedTextField(
                                value = streamUrl,
                                onValueChange = {
                                    streamUrl = it
                                    sharedPrefs.edit().putString("esp32_cam_url", it).apply()
                                },
                                label = { Text("MJPEG Stream Endpoint URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        HapticHelper.vibrateInfo(context)
                                        esp32CamManager.startStreaming(streamUrl, context)
                                    },
                                    enabled = camState != Esp32CamConnectionState.STREAMING,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start Stream")
                                }

                                OutlinedButton(
                                    onClick = {
                                        HapticHelper.vibrateInfo(context)
                                        esp32CamManager.stopStreaming()
                                    },
                                    enabled = camState == Esp32CamConnectionState.STREAMING,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop")
                                }
                            }

                            // Live Stream Viewfinder Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                val currentFrame = camFrame
                                if (camState == Esp32CamConnectionState.STREAMING && currentFrame != null) {
                                    Image(
                                        bitmap = currentFrame.asImageBitmap(),
                                        contentDescription = "ESP32 CAM Stream",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // FPS badge
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "LIVE • %.1f FPS".format(camFps),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SuccessGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Rounded.VideocamOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = when (camState) {
                                                Esp32CamConnectionState.CONNECTING -> "Connecting to ESP32-CAM..."
                                                Esp32CamConnectionState.ERROR -> camError ?: "Stream error"
                                                else -> "Stream Disconnected"
                                            },
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            // Onboard Torch / Flash Lamp Button
                            OutlinedButton(
                                onClick = {
                                    HapticHelper.vibrateInfo(context)
                                    esp32CamManager.toggleFlash { success ->
                                        val msg = if (success) "Flash toggled" else "Flash failed"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Toggle ESP32 Onboard Flash Light")
                            }
                        }
                    }
                }

                // 3. Smart Cane ToF / Ultrasonic Sensor Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.Sensors, contentDescription = null, tint = IntelligentBlue)
                                Text(
                                    text = "Smart Cane Distance Sensor (BLE / ToF)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("BLE Status:", style = MaterialTheme.typography.bodyMedium)
                                Surface(
                                    color = if (esp32BleState == Esp32ConnectionState.STREAMING) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = esp32BleState.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (esp32BleState == Esp32ConnectionState.STREAMING) SuccessGreen else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            val readingCm = tofReading?.let { if (it.isValid) (it.distanceMeters * 100).toInt() else null }
                            Text(
                                text = "Live Distance Reading: ${readingCm?.let { "$it cm" } ?: "No active distance reading"}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (readingCm != null && readingCm < warningThresholdCm) DangerRed else MaterialTheme.colorScheme.onSurface
                            )

                            // Threshold Slider
                            Column {
                                Text(
                                    text = "Obstacle Warning Distance: ${warningThresholdCm.toInt()} cm",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Slider(
                                    value = warningThresholdCm,
                                    onValueChange = {
                                        warningThresholdCm = it
                                        sharedPrefs.edit().putFloat("tof_warning_threshold_cm", it).apply()
                                    },
                                    valueRange = 50f..300f,
                                    steps = 25
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isPairing = (esp32BleState == Esp32ConnectionState.SCANNING || esp32BleState == Esp32ConnectionState.CONNECTING)
                                Button(
                                    onClick = {
                                        HapticHelper.vibrateInfo(context)
                                        Toast.makeText(context, "Scanning for Drishti Smart Cane...", Toast.LENGTH_SHORT).show()
                                        hardwareBridge.startScan()
                                    },
                                    enabled = !isPairing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isPairing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (esp32BleState == Esp32ConnectionState.CONNECTING) "Connecting..." else "Searching...")
                                    } else if (esp32BleState == Esp32ConnectionState.CONNECTED || esp32BleState == Esp32ConnectionState.STREAMING) {
                                        Text("Paired ✓")
                                    } else {
                                        Text("Pair Smart Cane")
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        HapticHelper.vibrateWarning(context)
                                        Toast.makeText(context, "Testing cane vibration motor", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Test Haptics")
                                }
                            }

                            if (!caneLastError.isNullOrBlank() && (esp32BleState == Esp32ConnectionState.ERROR || esp32BleState == Esp32ConnectionState.DISCONNECTED)) {
                                Text(
                                    text = caneLastError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DangerRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

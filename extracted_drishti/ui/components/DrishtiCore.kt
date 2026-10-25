package com.example.drishti.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drishti.ui.theme.IntelligentBlue
import com.example.drishti.ui.theme.SkyBlueAccent
import com.example.drishti.ui.theme.SuccessGreen
import kotlin.math.cos
import kotlin.math.sin

enum class DrishtiCoreState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ATTENTION,
    LOW_BATTERY,
    ERROR,
    EMERGENCY,
    DANGER,   // Backward compatibility alias for EMERGENCY
    SCANNING  // Backward compatibility alias for PROCESSING
}

@Composable
fun DrishtiCore(
    state: DrishtiCoreState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val infiniteTransition = rememberInfiniteTransition(label = "drishti_core_transition")

    // 1. Organic 3D Floating Motion (Y & X sway + subtle 3D tilt)
    val floatY by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_float_y"
    )

    val floatX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_float_x"
    )

    // 2. State-aware breathing pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (state) {
            DrishtiCoreState.IDLE -> 1.06f
            DrishtiCoreState.LISTENING -> 1.15f
            DrishtiCoreState.PROCESSING, DrishtiCoreState.SCANNING -> 1.09f
            DrishtiCoreState.SPEAKING -> 1.13f
            DrishtiCoreState.ATTENTION -> 1.08f
            DrishtiCoreState.LOW_BATTERY -> 1.10f
            DrishtiCoreState.ERROR -> 1.12f
            DrishtiCoreState.EMERGENCY, DrishtiCoreState.DANGER -> 1.18f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    DrishtiCoreState.IDLE -> 2800
                    DrishtiCoreState.LISTENING -> 900
                    DrishtiCoreState.PROCESSING, DrishtiCoreState.SCANNING -> 700
                    DrishtiCoreState.SPEAKING -> 800
                    DrishtiCoreState.ATTENTION -> 1400
                    DrishtiCoreState.LOW_BATTERY -> 1200
                    DrishtiCoreState.ERROR -> 800
                    DrishtiCoreState.EMERGENCY, DrishtiCoreState.DANGER -> 400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // 3. Rotation angle for processing vortex, chromatic overlay, and inner tech ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == DrishtiCoreState.PROCESSING || state == DrishtiCoreState.SCANNING) 3000 else 16000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_rotation"
    )

    // 4. Glare shimmer movement across sphere
    val glarePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glare_phase"
    )

    // 5. Expanding energy wave pulse
    val energyWaveRadius by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == DrishtiCoreState.EMERGENCY || state == DrishtiCoreState.DANGER) 500 else if (state == DrishtiCoreState.LISTENING) 1000 else 2000,
                easing = FastOutLinearInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_wave"
    )

    val energyWaveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == DrishtiCoreState.EMERGENCY || state == DrishtiCoreState.DANGER) 500 else if (state == DrishtiCoreState.LISTENING) 1000 else 2000,
                easing = FastOutLinearInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_wave_alpha"
    )

    // 6. Audio Wave Animation Phase for Listening and Speaking States
    val audioWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "audio_wave_phase"
    )

    // Theme Color Mapping for each state
    val stateThemeColors = when (state) {
        DrishtiCoreState.IDLE -> listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF2563EB), Color(0xFF0F172A))
        DrishtiCoreState.LISTENING -> listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1))
        DrishtiCoreState.PROCESSING, DrishtiCoreState.SCANNING -> listOf(Color(0xFFF3E8FF), Color(0xFFA855F7), Color(0xFF7C3AED), Color(0xFF312E81))
        DrishtiCoreState.SPEAKING -> listOf(Color(0xFFE0F2FE), Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1E3A8A))
        DrishtiCoreState.ATTENTION -> listOf(Color(0xFFFEF9C3), Color(0xFFFACC15), Color(0xFFD97706), Color(0xFF78350F))
        DrishtiCoreState.LOW_BATTERY -> listOf(Color(0xFFFFEDD5), Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFF7C2D12))
        DrishtiCoreState.ERROR -> listOf(Color(0xFFFFE4E6), Color(0xFFF43F5E), Color(0xFFE11D48), Color(0xFF881337))
        DrishtiCoreState.EMERGENCY, DrishtiCoreState.DANGER -> listOf(Color(0xFFFEE2E2), Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFF7F1D1D))
    }

    val primaryGlowColor = stateThemeColors[1]

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "core_press_scale"
    )

    Box(
        modifier = modifier
            .size(290.dp)
            .graphicsLayer {
                translationY = floatY
                translationX = floatX
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = "Drishti Core AI Voice Orb. State: $state. Tap to activate."
            },
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Atmospheric Outer Glow
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlowColor.copy(alpha = 0.28f),
                            stateThemeColors[0].copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Layer 2: Expanding Translucent Energy Field Ring
        Canvas(modifier = Modifier.size(268.dp)) {
            drawCircle(
                color = primaryGlowColor.copy(alpha = energyWaveAlpha),
                radius = (size.minDimension / 2f) * 0.72f * energyWaveRadius,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        // Layer 3: Concentric Floating Outer Rings
        Canvas(modifier = Modifier.size(252.dp)) {
            val centerPx = size.minDimension / 2f

            // Outer Ring 1: Translucent Sweep
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryGlowColor.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.65f),
                        stateThemeColors[2].copy(alpha = 0.25f),
                        primaryGlowColor.copy(alpha = 0.45f)
                    )
                ),
                radius = centerPx - 1.dp.toPx(),
                style = Stroke(width = 1.4.dp.toPx())
            )

            // Outer Ring 2: Thin Accent Ring
            drawCircle(
                color = primaryGlowColor.copy(alpha = 0.50f),
                radius = centerPx - 12.dp.toPx(),
                style = Stroke(width = 1.0.dp.toPx())
            )

            // Outer Ring 3: Glass Rim Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.38f),
                radius = centerPx - 22.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Layer 4: Processing Cosmic Vortex / Scanning Swirl Arcs
        if (state == DrishtiCoreState.PROCESSING || state == DrishtiCoreState.SCANNING) {
            Canvas(
                modifier = Modifier
                    .size(232.dp)
                    .graphicsLayer { rotationZ = rotationAngle * 2.5f }
            ) {
                val center = size.width / 2f
                val radius = (size.minDimension / 2f) - 4.dp.toPx()

                // Cosmic Swirl Ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFA855F7).copy(alpha = 0.88f),
                            Color(0xFF38BDF8),
                            Color.Transparent
                        )
                    ),
                    radius = radius,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Orbiting Particle Stars
                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90.0) + (rotationAngle.toDouble() * 0.5))
                    val px = center + (radius * cos(angle)).toFloat()
                    val py = center + (radius * sin(angle)).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = (2.5 + (i * 0.5)).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }

        // Layer 5 & 6: Modern Glass Sphere (190dp diameter) with 3D gradient & state-specific visual effects
        Box(
            modifier = Modifier
                .size(190.dp)
                .scale(if (state == DrishtiCoreState.SPEAKING) pulseScale else 1f),
            contentAlignment = Alignment.Center
        ) {
            // Drop Shadow
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(stateThemeColors[2].copy(alpha = 0.35f), Color.Transparent),
                        radius = size.minDimension * 0.52f
                    )
                )
            }

            // Glass Sphere Interior
            Box(
                modifier = Modifier
                    .size(178.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = stateThemeColors,
                            center = Offset(115f, 85f),
                            radius = 240f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Iridescent chromatic swirl overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotationAngle }
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.32f),
                                    primaryGlowColor.copy(alpha = 0.40f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.26f),
                                    stateThemeColors[1].copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.32f)
                                )
                            )
                        )
                )

                // Technical Radial Ticks / AI Processing Ring (48 ticks)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotationAngle * 0.5f }
                ) {
                    val center = size.width / 2f
                    val radius = (size.width / 2f) * 0.80f
                    val tickLength = 6.dp.toPx()
                    val totalTicks = 48

                    for (i in 0 until totalTicks) {
                        val angleRad = Math.toRadians((i * (360.0 / totalTicks)))
                        val startX = center + (radius * cos(angleRad)).toFloat()
                        val startY = center + (radius * sin(angleRad)).toFloat()
                        val endX = center + ((radius - tickLength) * cos(angleRad)).toFloat()
                        val endY = center + ((radius - tickLength) * sin(angleRad)).toFloat()

                        val isHighlighted = (i % 6 == 0)
                        val tickColor = if (isHighlighted) {
                            Color.White.copy(alpha = 0.82f)
                        } else {
                            primaryGlowColor.copy(alpha = 0.38f)
                        }

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isHighlighted) 1.6.dp.toPx() else 1.0.dp.toPx()
                        )
                    }
                }

                // Audio Wave Visualizer Bars for Listening and Speaking States
                if (state == DrishtiCoreState.LISTENING || state == DrishtiCoreState.SPEAKING) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = size.width / 2f
                        val centerY = size.height / 2f
                        val barWidth = 3.dp.toPx()
                        val barSpacing = 3.5.dp.toPx()
                        val barCount = 4

                        val baseHeight = if (state == DrishtiCoreState.LISTENING) 15.dp.toPx() else 22.dp.toPx()

                        // Left Audio Wave Bars
                        for (i in 0 until barCount) {
                            val phaseOffset = i * 0.8f
                            val heightFactor = 0.35f + 0.65f * sin(audioWavePhase + phaseOffset)
                            val barHeight = baseHeight * heightFactor
                            val x = center - 38.dp.toPx() - (i * (barWidth + barSpacing))

                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.90f),
                                topLeft = Offset(x, centerY - (barHeight / 2f)),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                            )
                        }

                        // Right Audio Wave Bars
                        for (i in 0 until barCount) {
                            val phaseOffset = i * 0.8f
                            val heightFactor = 0.35f + 0.65f * sin(audioWavePhase + phaseOffset + 0.5f)
                            val barHeight = baseHeight * heightFactor
                            val x = center + 38.dp.toPx() + (i * (barWidth + barSpacing)) - barWidth

                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.90f),
                                topLeft = Offset(x, centerY - (barHeight / 2f)),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                            )
                        }
                    }
                }

                // Dynamic Glass Specular Reflections (Crescent Top Highlight + Shimmering Glass Rim Line)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Shimmering crescent top highlight offset
                    val highlightOffsetY = (sin(glarePhase * 6.28318f) * 4.dp.toPx())

                    // Crescent Top-Left Highlight
                    drawOval(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.88f),
                                Color.White.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            startY = h * 0.05f + highlightOffsetY,
                            endY = h * 0.42f + highlightOffsetY
                        ),
                        topLeft = Offset(w * 0.20f, h * 0.07f + highlightOffsetY),
                        size = Size(w * 0.60f, h * 0.38f)
                    )

                    // Refraction Glass Rim Line
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.72f),
                                Color.Transparent,
                                primaryGlowColor.copy(alpha = 0.45f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        ),
                        radius = (size.minDimension / 2f) - 1.2.dp.toPx(),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Central Badge Container
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(1.2.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        DrishtiCoreState.EMERGENCY, DrishtiCoreState.DANGER -> {
                            Text(
                                text = "SOS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                        }
                        DrishtiCoreState.ATTENTION -> {
                            Text(
                                text = "!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp
                                ),
                                color = Color.White
                            )
                        }
                        DrishtiCoreState.LOW_BATTERY -> {
                            Icon(
                                imageVector = Icons.Rounded.BatteryAlert,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        DrishtiCoreState.ERROR -> {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = when (state) {
                                    DrishtiCoreState.IDLE -> Icons.Rounded.Mic
                                    DrishtiCoreState.LISTENING -> Icons.Rounded.Mic
                                    DrishtiCoreState.PROCESSING, DrishtiCoreState.SCANNING -> Icons.Rounded.AutoAwesome
                                    DrishtiCoreState.SPEAKING -> Icons.Rounded.Mic
                                    else -> Icons.Rounded.Mic
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

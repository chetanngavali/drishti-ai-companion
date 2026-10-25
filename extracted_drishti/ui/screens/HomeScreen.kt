package com.example.drishti.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drishti.R
import com.example.drishti.feature.home.HomeNavigationAction
import com.example.drishti.feature.home.HomeViewModel
import com.example.drishti.ui.components.DrishtiCore
import com.example.drishti.ui.components.DrishtiCoreState
import com.example.drishti.ui.theme.DangerRed
import com.example.drishti.ui.theme.IntelligentBlue
import com.example.drishti.ui.theme.SkyBlueAccent
import com.example.drishti.util.HapticHelper
import com.example.drishti.util.LocaleHelper
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToNearby: () -> Unit,
    onNavigateToDescribe: () -> Unit,
    onNavigateToEmergency: () -> Unit = {},
    onOpenNavigation: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.provideFactory(LocalContext.current.applicationContext as Application)),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val appLanguageCode by LocaleHelper.appLanguageCode.collectAsState()

    // Load home data via ViewModel utilizing languageCode
    LaunchedEffect(appLanguageCode) {
        viewModel.loadHomeData(appLanguageCode)
    }

    LaunchedEffect(uiState.dynamicSuggestions) {
        if (uiState.dynamicSuggestions.isNotEmpty()) {
            while (true) {
                delay(4000L)
                val nextIndex = (uiState.suggestionIndex + 1) % uiState.dynamicSuggestions.size
                viewModel.updateSuggestionIndex(nextIndex)
            }
        }
    }

    // Handle navigation actions emitted by ViewModel from voice routing
    LaunchedEffect(uiState.navigationAction) {
        when (uiState.navigationAction) {
            is HomeNavigationAction.NavigateToScan -> {
                onNavigateToScan()
                viewModel.onNavigationActionConsumed()
            }
            is HomeNavigationAction.NavigateToMap -> {
                onNavigateToMap()
                viewModel.onNavigationActionConsumed()
            }
            is HomeNavigationAction.NavigateToNearby -> {
                onNavigateToNearby()
                viewModel.onNavigationActionConsumed()
            }
            is HomeNavigationAction.NavigateToDescribe -> {
                onNavigateToDescribe()
                viewModel.onNavigationActionConsumed()
            }
            null -> {}
        }
    }

    // --- Atmospheric Background Animation Transitions ---
    val infiniteTransition = rememberInfiniteTransition(label = "home_bg_transition")

    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_wave_1"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_wave_2"
    )

    val wavePhase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_wave_3"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_glow_pulse"
    )

    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_particle"
    )

    val borderShimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_shimmer"
    )

    // Dynamic state-aware glow color & intensity
    val glowColor = when (uiState.coreState) {
        DrishtiCoreState.EMERGENCY, DrishtiCoreState.DANGER, DrishtiCoreState.ERROR -> DangerRed
        DrishtiCoreState.ATTENTION -> Color(0xFFF59E0B)
        DrishtiCoreState.LOW_BATTERY -> Color(0xFFEA580C)
        DrishtiCoreState.PROCESSING, DrishtiCoreState.SCANNING -> Color(0xFFA855F7)
        DrishtiCoreState.LISTENING -> SkyBlueAccent
        DrishtiCoreState.SPEAKING -> SkyBlueAccent
        else -> SkyBlueAccent
    }

    val glowAlphaMultiplier = when (uiState.coreState) {
        DrishtiCoreState.LISTENING -> 1.6f
        DrishtiCoreState.PROCESSING, DrishtiCoreState.SCANNING -> 1.4f
        DrishtiCoreState.SPEAKING -> 1.3f
        DrishtiCoreState.EMERGENCY, DrishtiCoreState.DANGER, DrishtiCoreState.ERROR -> 1.8f
        DrishtiCoreState.ATTENTION, DrishtiCoreState.LOW_BATTERY -> 1.5f
        else -> 1.0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFCFF))
    ) {
        // Continuous, state-aware background canvas with multi-layered light waves, streaks, and drifting particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Central atmospheric halo converging toward voice core
            val baseRadius = w * 0.72f * glowPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = (0.18f * glowAlphaMultiplier).coerceAtMost(0.38f)),
                        Color(0xFF38BDF8).copy(alpha = (0.09f * glowAlphaMultiplier).coerceAtMost(0.22f)),
                        Color(0xFFEEF2FF).copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.52f),
                    radius = baseRadius
                )
            )

            // 2. Flowing Light Ribbon Layer A
            val waveAOffset = (wavePhase1 * w) - (w * 0.5f)
            val pathA = Path().apply {
                moveTo(-w * 0.2f + waveAOffset, h * 0.26f)
                cubicTo(
                    w * 0.3f + waveAOffset, h * 0.18f + (glowPulse * 15f),
                    w * 0.7f + waveAOffset, h * 0.34f - (glowPulse * 15f),
                    w * 1.2f + waveAOffset, h * 0.28f
                )
                lineTo(w * 1.2f + waveAOffset, h * 0.48f)
                cubicTo(
                    w * 0.7f + waveAOffset, h * 0.54f,
                    w * 0.3f + waveAOffset, h * 0.38f,
                    -w * 0.2f + waveAOffset, h * 0.44f
                )
                close()
            }
            drawPath(
                path = pathA,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE0F2FE).copy(alpha = 0.16f * glowAlphaMultiplier),
                        Color(0xFF38BDF8).copy(alpha = 0.10f * glowAlphaMultiplier),
                        IntelligentBlue.copy(alpha = 0.06f * glowAlphaMultiplier),
                        Color.Transparent
                    ),
                    start = Offset(-w * 0.2f + waveAOffset, h * 0.26f),
                    end = Offset(w * 1.2f + waveAOffset, h * 0.48f)
                )
            )

            // 3. Flowing Light Ribbon Layer B
            val waveBOffset = (wavePhase2 * w) - (w * 0.5f)
            val pathB = Path().apply {
                moveTo(-w * 0.2f - waveBOffset, h * 0.64f)
                cubicTo(
                    w * 0.35f - waveBOffset, h * 0.56f - (glowPulse * 12f),
                    w * 0.68f - waveBOffset, h * 0.70f + (glowPulse * 12f),
                    w * 1.2f - waveBOffset, h * 0.64f
                )
                lineTo(w * 1.2f - waveBOffset, h * 0.82f)
                cubicTo(
                    w * 0.68f - waveBOffset, h * 0.88f,
                    w * 0.35f - waveBOffset, h * 0.74f,
                    -w * 0.2f - waveBOffset, h * 0.80f
                )
                close()
            }
            drawPath(
                path = pathB,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF38BDF8).copy(alpha = 0.10f * glowAlphaMultiplier),
                        Color(0xFFEEF2FF).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(-w * 0.2f - waveBOffset, h * 0.64f),
                    end = Offset(w * 1.2f - waveBOffset, h * 0.82f)
                )
            )

            // 4. Flowing Light Ribbon Layer C (Secondary undulating wave)
            val waveCOffset = (wavePhase3 * w * 0.8f) - (w * 0.4f)
            val pathC = Path().apply {
                moveTo(-w * 0.1f + waveCOffset, h * 0.42f)
                cubicTo(
                    w * 0.4f + waveCOffset, h * 0.36f,
                    w * 0.6f + waveCOffset, h * 0.48f,
                    w * 1.1f + waveCOffset, h * 0.42f
                )
                lineTo(w * 1.1f + waveCOffset, h * 0.58f)
                cubicTo(
                    w * 0.6f + waveCOffset, h * 0.64f,
                    w * 0.4f + waveCOffset, h * 0.52f,
                    -w * 0.1f + waveCOffset, h * 0.58f
                )
                close()
            }
            drawPath(
                path = pathC,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF38BDF8).copy(alpha = 0.07f * glowAlphaMultiplier),
                        Color.Transparent
                    )
                )
            )

            // 5. Luminous Light Streaks flowing subtly through background
            val streakX1 = (wavePhase1 * w * 1.4f) - (w * 0.2f)
            val pathStreak1 = Path().apply {
                moveTo(streakX1, h * 0.22f)
                cubicTo(streakX1 + w * 0.25f, h * 0.30f, streakX1 + w * 0.4f, h * 0.18f, streakX1 + w * 0.6f, h * 0.26f)
            }
            drawPath(
                path = pathStreak1,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.60f),
                        Color(0xFF38BDF8).copy(alpha = 0.38f),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // 6. Floating ambient light particles drifting smoothly
            val pY1 = ((particlePhase + 0.0f) % 1.0f) * h
            val pY2 = ((particlePhase + 0.33f) % 1.0f) * h
            val pY3 = ((particlePhase + 0.66f) % 1.0f) * h
            val pY4 = ((particlePhase + 0.85f) % 1.0f) * h

            drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.26f), radius = 2.8.dp.toPx(), center = Offset(w * 0.15f, pY1))
            drawCircle(color = IntelligentBlue.copy(alpha = 0.20f), radius = 2.2.dp.toPx(), center = Offset(w * 0.85f, pY2))
            drawCircle(color = Color.White.copy(alpha = 0.60f), radius = 2.0.dp.toPx(), center = Offset(w * 0.48f, pY3))
            drawCircle(color = Color(0xFF60A5FA).copy(alpha = 0.22f), radius = 2.4.dp.toPx(), center = Offset(w * 0.28f, pY4))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Top Navigation Controls Row (Menu, Language Selector, SOS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: Circular white glassmorphism Menu button
                val menuInteractionSource = remember { MutableInteractionSource() }
                val isMenuPressed by menuInteractionSource.collectIsPressedAsState()
                val menuScale by animateFloatAsState(
                    targetValue = if (isMenuPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "menu_scale"
                )

                Surface(
                    onClick = onOpenNavigation,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.94f),
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = borderShimmerAlpha)),
                    modifier = Modifier
                        .size(44.dp)
                        .scale(menuScale)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Open Navigation Menu"
                        },
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = null,
                            tint = IntelligentBlue,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // Center: Wide rounded pill Language Selector
                var showLanguageDialog by remember { mutableStateOf(false) }
                Surface(
                    onClick = { showLanguageDialog = true },
                    color = Color.White.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = borderShimmerAlpha)),
                    modifier = Modifier.height(44.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = IntelligentBlue,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = when (appLanguageCode) {
                                "hi" -> "Auto • हिन्दी"
                                "mr" -> "Auto • मराठी"
                                else -> "Auto • English"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            ),
                            color = Color(0xFF0F172A),
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = IntelligentBlue,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (showLanguageDialog) {
                    AlertDialog(
                        onDismissRequest = { showLanguageDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.language_select_title),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    Triple("en", "English", "English"),
                                    Triple("hi", "हिन्दी", "Hindi"),
                                    Triple("mr", "मराठी", "Marathi")
                                ).forEach { (code, native, eng) ->
                                    val isCurrent = appLanguageCode == code
                                    Surface(
                                        onClick = {
                                            LocaleHelper.setDefaultLanguageEverywhere(context, code, native)
                                            HapticHelper.vibrateSuccess(context)
                                            showLanguageDialog = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isCurrent) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isCurrent) IntelligentBlue else Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$native ($eng)",
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) IntelligentBlue else Color(0xFF0F172A)
                                            )
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = IntelligentBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLanguageDialog = false }) {
                                Text(stringResource(R.string.btn_cancel))
                            }
                        }
                    )
                }

                // Right: Emergency SOS Quick Action Button
                val sosInteractionSource = remember { MutableInteractionSource() }
                val isSosPressed by sosInteractionSource.collectIsPressedAsState()
                val sosScale by animateFloatAsState(
                    targetValue = if (isSosPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "sos_scale"
                )

                Surface(
                    onClick = {
                        HapticHelper.vibrateWarning(context)
                        onNavigateToEmergency()
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFFEF2F2),
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier
                        .height(44.dp)
                        .scale(sosScale)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Trigger Emergency SOS"
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.nav_sos_btn),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = Color(0xFFDC2626),
                        )
                    }
                }
            }

            // 2. Center-Aligned Greeting Typography with Subtle Motion Scale
            val titlePulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.012f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "title_pulse"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(titlePulseScale)
                    .padding(top = 18.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Greeting Line 1
                Text(
                    text = stringResource(R.string.home_greeting_hello),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                    ),
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                )

                // Greeting Line 2
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = IntelligentBlue)) {
                            append(stringResource(R.string.home_greeting_ask_me))
                        }
                        append(stringResource(R.string.home_greeting_anything_you_need))
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                    ),
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                )
            }

            // 3. Centered Modernized Drishti Voice Core Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DrishtiCore(
                    state = uiState.coreState,
                    onClick = {
                        HapticHelper.vibrateInfo(context)
                        viewModel.onVoiceButtonClicked()
                    },
                )
            }

            // 4. Bottom Glassmorphism Suggestion Bar
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.94f),
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = borderShimmerAlpha)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val iconPulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "icon_pulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(iconPulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (uiState.coreState != DrishtiCoreState.IDLE) Icons.Rounded.GraphicEq else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = IntelligentBlue,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    val safeIndex = if (uiState.dynamicSuggestions.isNotEmpty()) uiState.suggestionIndex % uiState.dynamicSuggestions.size else 0
                    val currentText = if (uiState.coreState != DrishtiCoreState.IDLE) {
                        uiState.coreSpokenText
                    } else if (uiState.dynamicSuggestions.isNotEmpty()) {
                        uiState.dynamicSuggestions[safeIndex]
                    } else {
                        "read document"
                    }

                    AnimatedContent(
                        targetState = currentText,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "suggestion_animation",
                        modifier = Modifier.weight(1f),
                    ) { targetText ->
                        Text(
                            text = buildAnnotatedString {
                                if (uiState.coreState == DrishtiCoreState.IDLE) {
                                    withStyle(
                                        style = SpanStyle(
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium,
                                        )
                                    ) {
                                        append("${stringResource(R.string.home_try_saying_prefix).trim()} ")
                                    }
                                    withStyle(
                                        style = SpanStyle(
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    ) {
                                        append("'$targetText'")
                                    }
                                } else {
                                    withStyle(
                                        style = SpanStyle(
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    ) {
                                        append(targetText)
                                    }
                                }
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

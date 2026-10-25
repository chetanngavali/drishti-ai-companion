package com.example.drishti.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drishti.R
import com.example.drishti.ui.theme.IntelligentBlue
import com.example.drishti.util.HapticHelper
import com.example.drishti.util.LocaleHelper

data class LanguageOption(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val subtitle: String,
    val greeting: String
)

private val SUPPORTED_LANGUAGES = listOf(
    LanguageOption(
        code = "en",
        nativeName = "English",
        englishName = "English",
        subtitle = "App interface and voice guidance in English",
        greeting = "Hello • Welcome to DrishtiAI"
    ),
    LanguageOption(
        code = "hi",
        nativeName = "हिन्दी",
        englishName = "Hindi",
        subtitle = "ऐप इंटरफ़ेस और आवाज़ मार्गदर्शन हिन्दी में",
        greeting = "नमस्ते • दृष्टि AI में आपका स्वागत है"
    ),
    LanguageOption(
        code = "mr",
        nativeName = "मराठी",
        englishName = "Marathi",
        subtitle = "ॲप इंटरफेस आणि बोलून मार्गदर्शन मराठीत",
        greeting = "नमस्कार • दृष्टी AI मध्ये आपले स्वागत आहे"
    )
)

/**
 * First-run Language Selection Screen presented right after splash screen.
 * Automatically saves the chosen language as default everywhere (UI + Voice TTS + Speech Recognition).
 */
@Composable
fun LanguageSelectionScreen(
    onLanguageConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCode by remember {
        mutableStateOf(LocaleHelper.getSelectedLanguageCode(context))
    }

    // Smooth entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "lang_alpha"
    )
    val contentTranslationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "lang_transY"
    )

    // Slow ambient wave ribbons matching Drishti aesthetic
    val infiniteTransition = rememberInfiniteTransition(label = "lang_bg_transition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lang_wave_phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFCFF))
            .systemBarsPadding()
            .imePadding()
    ) {
        // Continuous soft animated wave background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val waveOffset = (wavePhase * w) - (w * 0.5f)
            val pathA = Path().apply {
                moveTo(-w * 0.2f + waveOffset, h * 0.20f)
                cubicTo(
                    w * 0.3f + waveOffset, h * 0.14f,
                    w * 0.7f + waveOffset, h * 0.26f,
                    w * 1.2f + waveOffset, h * 0.22f
                )
                lineTo(w * 1.2f + waveOffset, h * 0.38f)
                cubicTo(
                    w * 0.7f + waveOffset, h * 0.42f,
                    w * 0.3f + waveOffset, h * 0.30f,
                    -w * 0.2f + waveOffset, h * 0.34f
                )
                close()
            }
            drawPath(
                path = pathA,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE0F2FE).copy(alpha = 0.35f),
                        Color(0xFF38BDF8).copy(alpha = 0.18f),
                        IntelligentBlue.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    start = Offset(-w * 0.2f + waveOffset, h * 0.20f),
                    end = Offset(w * 1.2f + waveOffset, h * 0.38f)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
                .graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentTranslationY.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Hero Icon Emblem
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.5.dp, Color(0xFFE0F2FE)),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = IntelligentBlue,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Role / Category Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.language_select_badge),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = IntelligentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = stringResource(R.string.language_select_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    ),
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = stringResource(R.string.language_select_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp
                    ),
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Language Option Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SUPPORTED_LANGUAGES.forEach { lang ->
                        val isSelected = selectedCode == lang.code

                        LanguageSelectionCard(
                            language = lang,
                            isSelected = isSelected,
                            onSelect = {
                                selectedCode = lang.code
                                HapticHelper.vibrateSuccess(context)
                                LocaleHelper.setDefaultLanguageEverywhere(context, lang.code, lang.nativeName)
                            }
                        )
                    }
                }
            }

            // Bottom Confirm Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val selectedOption = SUPPORTED_LANGUAGES.firstOrNull { it.code == selectedCode }
                    ?: SUPPORTED_LANGUAGES[0]

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val btnScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.97f else 1.0f,
                    animationSpec = tween(durationMillis = 150),
                    label = "lang_btn_scale"
                )

                Surface(
                    onClick = {
                        HapticHelper.vibrateSuccess(context)
                        LocaleHelper.setDefaultLanguageEverywhere(
                            context,
                            selectedOption.code,
                            selectedOption.nativeName
                        )
                        onLanguageConfirmed()
                    },
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent,
                    shadowElevation = 8.dp,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(btnScale)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Confirm Language Selection and Continue"
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        IntelligentBlue,
                                        Color(0xFF0284C7)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.language_select_continue, selectedOption.nativeName),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSelectionCard(
    language: LanguageOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) IntelligentBlue else Color(0xFFE2E8F0),
        animationSpec = tween(durationMillis = 200),
        label = "card_border"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFF0F9FF) else Color.White,
        animationSpec = tween(durationMillis = 200),
        label = "card_bg"
    )
    val cardElevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 1.dp,
        animationSpec = tween(durationMillis = 200),
        label = "card_elevation"
    )

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shadowElevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .semantics {
                role = Role.RadioButton
                contentDescription = "${language.nativeName} (${language.englishName})"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Radio indicator or check emblem
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) IntelligentBlue else Color(0xFFF1F5F9),
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) IntelligentBlue else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = language.nativeName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = if (isSelected) IntelligentBlue else Color(0xFF0F172A)
                        )
                        if (language.englishName != language.nativeName) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${language.englishName})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = language.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

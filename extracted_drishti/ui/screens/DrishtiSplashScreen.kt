package com.example.drishti.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drishti.R

/**
 * DrishtiSplashScreen — Native Android Landing & Splash Screen.
 *
 * Implements the required 6-layer architecture with precise alignment:
 * 1. Native Background:
 *    - Very light blue-white gradient base (#FFFFFF -> #F7FAFD -> #EDF5FD -> #E4F0FA)
 *    - Soft atmospheric radial glow behind the subject's upper body / head
 *    - Animated organic translucent wave ribbons (16s, 21s, 27s, 32s)
 *    - Ambient floating micro-particles
 * 2. Brand Area:
 *    - Official app branding using tight cropped ic_drishti_branding (from ic_launcher_foreground.png)
 *    - Sized nicely without empty transparent padding
 * 3. Tagline Layer:
 *    - "Your Intelligent Companion"
 *    - "for a More Independent Life"
 *    - Subtle horizontal animated blue -> cyan accent line
 * 4. Character Layer:
 *    - Person PNG (drishti_person.png) as foreground character asset ONLY
 *    - Sits cleanly on the bottom behind loading layer, uncropped, aspect ratio preserved
 *    - Soft radial halo behind head/shoulders
 * 5. Capability / Ambient Tags Layer (Balanced on BOTH sides):
 *    - Left side: Voice Assistant, Read Text, Navigation
 *    - Right side: Live Location, Emergency SOS
 *    - Frosted glass appearance, vibrant blue/cyan/coral badges, dynamic floating motion
 * 6. Bottom Loading Layer:
 *    - Truthful stage title and subtitle driven by initialization stages
 *    - Accessible progress bar with ProgressBarRangeInfo semantics and smooth shimmer highlight
 */
@Composable
fun DrishtiSplashScreen(
    progress: Float,
    modifier: Modifier = Modifier,
    statusTitle: String = "Initializing Drishti...",
    statusSubtitle: String = "Setting up your intelligent assistant",
    isIndeterminate: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite_anim")

    // Wave 1: 16s slowly right
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_16s"
    )

    // Wave 2: 21s slowly left
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(21000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_21s"
    )

    // Wave 3: 27s slowly upward/right
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(27000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_27s"
    )

    // Wave 4: 32s slowly downward/left
    val wave4 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_32s"
    )

    // Atmospheric breathing glow pulse behind character
    val atmosphericPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "atmospheric_glow_pulse"
    )

    // Tag float offsets for organic, floating motion
    val tag1Float by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tag1_float"
    )

    val tag2Float by infiniteTransition.animateFloat(
        initialValue = 7f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tag2_float"
    )

    val tag3Float by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tag3_float"
    )

    val tag4Float by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tag4_float"
    )

    val tag5Float by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tag5_float"
    )

    // Tag glow pulse
    val tagGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tag_glow"
    )

    // Shimmer highlight moving across the progress bar
    val progressShimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress_shimmer"
    )

    // Tagline small loading accent shimmer
    val accentOffset by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accent_shimmer"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                paneTitle = "Drishti Splash Screen"
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF7FAFD),
                        Color(0xFFEDF5FD),
                        Color(0xFFE4F0FA)
                    )
                )
            )
    ) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth

        // =========================================================================
        // LAYER 1: ATMOSPHERIC LIGHTING, FLOWING WAVES & PARTICLES
        // =========================================================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Soft atmospheric radial glow behind the person's upper body / head
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE).copy(alpha = 0.55f * atmosphericPulse),
                        Color(0xFFBAE6FD).copy(alpha = 0.30f * atmosphericPulse),
                        Color(0xFF7DD3FC).copy(alpha = 0.12f * atmosphericPulse),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.44f),
                    radius = w * 0.70f
                ),
                radius = w * 0.70f,
                center = Offset(w * 0.5f, h * 0.44f)
            )

            // Secondary softer cyan glow around chest/cane level
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.14f * atmosphericPulse),
                        Color(0xFF60A5FA).copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.62f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.5f, h * 0.62f)
            )

            // 2. Animated Flowing Wave 1 (16s -> slowly right)
            val o1 = (wave1 * w) - (w * 0.5f)
            val p1 = Path().apply {
                moveTo(-w * 0.3f + o1, h * 0.30f)
                cubicTo(
                    w * 0.2f + o1, h * 0.24f,
                    w * 0.65f + o1, h * 0.36f,
                    w * 1.3f + o1, h * 0.28f
                )
                lineTo(w * 1.3f + o1, h * 0.35f)
                cubicTo(
                    w * 0.65f + o1, h * 0.42f,
                    w * 0.2f + o1, h * 0.30f,
                    -w * 0.3f + o1, h * 0.36f
                )
                close()
            }
            drawPath(
                path = p1,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF38BDF8).copy(alpha = 0.09f * atmosphericPulse),
                        Color(0xFF93C5FD).copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )

            // 3. Animated Flowing Wave 2 (21s <- slowly left)
            val o2 = (wave2 * w) - (w * 0.5f)
            val p2 = Path().apply {
                moveTo(-w * 0.3f + o2, h * 0.64f)
                cubicTo(
                    w * 0.25f + o2, h * 0.58f,
                    w * 0.70f + o2, h * 0.70f,
                    w * 1.3f + o2, h * 0.62f
                )
                lineTo(w * 1.3f + o2, h * 0.71f)
                cubicTo(
                    w * 0.70f + o2, h * 0.78f,
                    w * 0.25f + o2, h * 0.65f,
                    -w * 0.3f + o2, h * 0.72f
                )
                close()
            }
            drawPath(
                path = p2,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF0284C7).copy(alpha = 0.08f),
                        Color(0xFF38BDF8).copy(alpha = 0.12f * atmosphericPulse),
                        Color.Transparent
                    )
                )
            )

            // 4. Animated Flowing Wave 3 (27s -> slowly upward/right)
            val o3 = (wave3 * w) - (w * 0.5f)
            val v3 = (wave3 - 0.5f) * 30f
            val p3 = Path().apply {
                moveTo(-w * 0.3f + o3, h * 0.78f + v3)
                cubicTo(
                    w * 0.30f + o3, h * 0.72f + v3,
                    w * 0.75f + o3, h * 0.82f + v3,
                    w * 1.3f + o3, h * 0.75f + v3
                )
                lineTo(w * 1.3f + o3, h * 0.82f + v3)
                cubicTo(
                    w * 0.75f + o3, h * 0.88f + v3,
                    w * 0.30f + o3, h * 0.78f + v3,
                    -w * 0.3f + o3, h * 0.84f + v3
                )
                close()
            }
            drawPath(
                path = p3,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFBAE6FD).copy(alpha = 0.12f),
                        Color(0xFF38BDF8).copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            )

            // 5. Animated Flowing Wave 4 (32s <- slowly downward/left)
            val o4 = (wave4 * w) - (w * 0.5f)
            val v4 = (wave4 - 0.5f) * -25f
            val p4 = Path().apply {
                moveTo(-w * 0.2f + o4, h * 0.52f + v4)
                cubicTo(
                    w * 0.35f + o4, h * 0.46f + v4,
                    w * 0.68f + o4, h * 0.56f + v4,
                    w * 1.25f + o4, h * 0.50f + v4
                )
                lineTo(w * 1.25f + o4, h * 0.56f + v4)
                cubicTo(
                    w * 0.68f + o4, h * 0.62f + v4,
                    w * 0.35f + o4, h * 0.51f + v4,
                    -w * 0.2f + o4, h * 0.57f + v4
                )
                close()
            }
            drawPath(
                path = p4,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE0F2FE).copy(alpha = 0.14f),
                        Color(0xFF7DD3FC).copy(alpha = 0.07f),
                        Color.Transparent
                    )
                )
            )

            // 6. Ambient Micro-particles (soft glowing cyan/blue specks)
            val particleOffsets = listOf(
                Offset(w * 0.16f, h * 0.34f + tag1Float * 0.8f) to 2.8f,
                Offset(w * 0.84f, h * 0.38f - tag2Float * 0.8f) to 3.2f,
                Offset(w * 0.22f, h * 0.58f - tag3Float * 0.6f) to 2.2f,
                Offset(w * 0.80f, h * 0.66f + tag4Float * 0.8f) to 2.5f,
                Offset(w * 0.12f, h * 0.75f + tag1Float * 0.5f) to 3.0f,
                Offset(w * 0.88f, h * 0.80f - tag2Float * 0.5f) to 2.0f
            )
            particleOffsets.forEach { (pos, radius) ->
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.35f * atmosphericPulse),
                    radius = radius,
                    center = pos
                )
            }
        }

        // =========================================================================
        // LAYER 2: BRAND AREA (Top-Center)
        // Uses tight cropped app logo without excessive padding
        // =========================================================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Official Drishti App Logo (tight cropped without empty canvas margin)
            Image(
                painter = painterResource(id = R.drawable.ic_drishti_branding),
                contentDescription = "Drishti App Logo",
                modifier = Modifier
                    .height(68.dp)
                    .wrapContentWidth(),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(10.dp))

            // =====================================================================
            // LAYER 3: TAGLINE LAYER
            // "Your Intelligent Companion for a More Independent Life"
            // + Small Animated Blue -> Cyan Loading Accent
            // =====================================================================
            Text(
                text = "Your Intelligent Companion",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp
            )
            Text(
                text = "for a More Independent Life",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Small subtle horizontal animated blue -> cyan accent line
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(26.dp)
                        .graphicsLayer { translationX = accentOffset }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0284C7),
                                    Color(0xFF38BDF8)
                                )
                            )
                        )
                )
            }
        }

        // =========================================================================
        // LAYER 4: CHARACTER LAYER (Person PNG as foreground character asset ONLY)
        // Occupies ~58% of content height, positioned in lower-middle, ContentScale.Fit
        // Aspect ratio strictly preserved, face uncropped, NO circle around person
        // =========================================================================
        val characterHeight = totalHeight * 0.58f

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 120.dp), // Leaves ample breathing room for bottom loading layer
            contentAlignment = Alignment.BottomCenter
        ) {
            // Soft atmospheric backlight halo directly behind the person's head & shoulders
            Box(
                modifier = Modifier
                    .size(characterHeight * 0.72f)
                    .align(Alignment.Center)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE0F2FE).copy(alpha = 0.65f * atmosphericPulse),
                                    Color(0xFFBAE6FD).copy(alpha = 0.35f * atmosphericPulse),
                                    Color(0xFF38BDF8).copy(alpha = 0.12f * atmosphericPulse),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.38f),
                                radius = size.width * 0.52f
                            ),
                            radius = size.width * 0.52f,
                            center = Offset(size.width * 0.5f, size.height * 0.38f)
                        )
                    }
            )

            // Person foreground character PNG
            Image(
                painter = painterResource(id = R.drawable.drishti_person),
                contentDescription = "Drishti Companion",
                modifier = Modifier
                    .height(characterHeight)
                    .wrapContentWidth(),
                contentScale = ContentScale.Fit
            )
        }

        // =========================================================================
        // LAYER 5: CAPABILITY / AMBIENT TAGS BALANCED ON BOTH SIDES WITH MOTION
        // Left Side: Voice Assistant, Read Text, Navigation Support
        // Right Side: Live Location, Emergency SOS
        // =========================================================================
        // 1. Voice Assistant (Upper-Left)
        AmbientPillBadge(
            icon = Icons.Rounded.Mic,
            iconTint = Color(0xFF0284C7),
            iconBg = Color(0xFFE0F2FE),
            title = "Voice",
            subtitle = "Assistant",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, bottom = 150.dp)
                .graphicsLayer {
                    translationY = tag1Float
                    translationX = tag1Float * 0.3f
                },
            glowAlpha = tagGlow
        )

        // 2. Read Text (Mid-Left)
        AmbientPillBadge(
            icon = Icons.Rounded.Description,
            iconTint = Color(0xFF0284C7),
            iconBg = Color(0xFFE0F2FE),
            title = "Read",
            subtitle = "Text",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, top = 20.dp)
                .graphicsLayer {
                    translationY = tag3Float
                    translationX = -tag3Float * 0.3f
                },
            glowAlpha = tagGlow
        )

        // 3. Navigation Support (Lower-Left)
        AmbientPillBadge(
            icon = Icons.Rounded.NearMe,
            iconTint = Color(0xFF0284C7),
            iconBg = Color(0xFFE0F2FE),
            title = "Navigation",
            subtitle = "Support",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, top = 175.dp)
                .graphicsLayer {
                    translationY = tag4Float
                    translationX = tag4Float * 0.35f
                },
            glowAlpha = tagGlow
        )

        // 4. Live Location (Upper-Right)
        AmbientPillBadge(
            icon = Icons.Rounded.Place,
            iconTint = Color(0xFF0284C7),
            iconBg = Color(0xFFE0F2FE),
            title = "Live",
            subtitle = "Location",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp, bottom = 120.dp)
                .graphicsLayer {
                    translationY = tag2Float
                    translationX = -tag2Float * 0.3f
                },
            glowAlpha = tagGlow
        )

        // 5. Emergency SOS (Mid-Right)
        AmbientPillBadge(
            icon = Icons.Rounded.Shield,
            iconTint = Color(0xFFEF4444), // Coral Red accent
            iconBg = Color(0xFFFEE2E2),
            title = "Emergency",
            subtitle = "SOS",
            subtitleColor = Color(0xFFEF4444),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp, top = 55.dp)
                .graphicsLayer {
                    translationY = tag5Float
                    translationX = tag5Float * 0.3f
                },
            glowAlpha = tagGlow
        )

        // =========================================================================
        // LAYER 6: BOTTOM LOADING LAYER
        // "Initializing Drishti...", subtitle & clean prominent progress bar (78% width)
        // =========================================================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F264A),
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = statusSubtitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B),
                letterSpacing = 0.15.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            val clampedProgress = progress.coerceIn(0.04f, 1.0f)

            // Rounded Progress Bar (78% width, 10dp height with soft glow & shimmer)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(10.dp)
                    .semantics {
                        if (!isIndeterminate) {
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = clampedProgress,
                                range = 0f..1f,
                                steps = 0
                            )
                            contentDescription = "$statusTitle, ${(clampedProgress * 100).toInt()} percent complete"
                        } else {
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                            contentDescription = "$statusTitle, loading in progress"
                        }
                    }
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x260284C7),
                        spotColor = Color(0x330EA5E9)
                    )
                    .clip(CircleShape)
                    .background(Color(0xFFE2EDF8)) // Soft light-blue track
            ) {
                // Fill Bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (isIndeterminate) 1.0f else clampedProgress)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0052CC), // Rich deep blue
                                    Color(0xFF0284C7), // Vibrant blue
                                    Color(0xFF0EA5E9), // Sky blue
                                    Color(0xFF38BDF8)  // Bright cyan
                                )
                            )
                        )
                ) {
                    // Moving shimmer highlight inside the progress bar fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.35f)
                            .graphicsLayer {
                                translationX = progressShimmer * 280f
                            }
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.50f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

/**
 * Enhanced frosted-glass ambient capability badge.
 * Features stacked title/subtitle for optimal proportion, clear iconography,
 * animated floating motion, and soft drop shadow.
 */
@Composable
private fun AmbientPillBadge(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    subtitleColor: Color = Color(0xFF334155),
    glowAlpha: Float = 1.0f
) {
    Row(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x200F264A),
                spotColor = Color(0x330284C7)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFFFFFFF).copy(alpha = 0.92f * glowAlpha))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFBAE6FD).copy(alpha = 0.7f),
                        Color(0xFF7DD3FC).copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F264A),
                lineHeight = 14.sp
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = subtitleColor,
                lineHeight = 13.sp
            )
        }
    }
}

package com.example.drishti.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.drishti.R
import com.example.drishti.ui.screens.info.PrivacyAndDataPolicyScreen
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * DRISHTI AI — ROLE SELECTION / AUTH LANDING SCREEN REDESIGN
 *
 * Implemented 100% as native Jetpack Compose components.
 * Adheres strictly to the Drishti visual identity:
 * - Ultra-light background (#F8FBFF) with flowing ambient wave layers and floating particles.
 * - Official Drishti logo with smooth entrance animation.
 * - "Choose Your Role" with primary blue highlighted accent.
 * - Premium interactive role cards for Embedded User & Parent/Guardian with native vector visual platforms,
 *   feature chips, and responsive press interactions.
 * - Side-by-side sign-up buttons with press animations.
 * - Supabase TLS security footer and clickable Safety Disclosures & Privacy Notice.
 */
@Composable
fun AuthLandingScreen(
    onSelectEmbeddedSignIn: () -> Unit,
    onSelectEmbeddedSignUp: () -> Unit,
    onSelectParentSignIn: () -> Unit,
    onSelectParentSignUp: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showLandingInfoDialog by remember { mutableStateOf(false) }

    // Screen load entrance trigger
    var isScreenEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(30)
        isScreenEntered = true
    }

    // Top Brand Logo entrance animation (0 -> 1 alpha, 0.94 -> 1.0 scale over 500ms)
    val logoAlpha by animateFloatAsState(
        targetValue = if (isScreenEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (isScreenEntered) 1f else 0.94f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    // Staggered Role Cards entrance animation
    var areCardsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        areCardsVisible = true
    }

    val card1Alpha by animateFloatAsState(
        targetValue = if (areCardsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 40, easing = FastOutSlowInEasing),
        label = "card1Alpha"
    )
    val card1OffsetY by animateFloatAsState(
        targetValue = if (areCardsVisible) 0f else 20f,
        animationSpec = tween(durationMillis = 500, delayMillis = 40, easing = FastOutSlowInEasing),
        label = "card1OffsetY"
    )

    val card2Alpha by animateFloatAsState(
        targetValue = if (areCardsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 160, easing = FastOutSlowInEasing),
        label = "card2Alpha"
    )
    val card2OffsetY by animateFloatAsState(
        targetValue = if (areCardsVisible) 0f else 20f,
        animationSpec = tween(durationMillis = 500, delayMillis = 160, easing = FastOutSlowInEasing),
        label = "card2OffsetY"
    )

    // Subtle ambient background wave motion (18-30s loop, LinearEasing)
    val ambientTransition = rememberInfiniteTransition(label = "ambientWaves")
    val wavePhase1 by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase1"
    )
    val wavePhase2 by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase2"
    )

    Scaffold(
        containerColor = Color(0xFFF8FBFF)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF8FBFF),
                            Color(0xFFF0F6FC),
                            Color(0xFFEBF4FD)
                        )
                    )
                )
        ) {
            // Subtle ambient wave field & 5 drifting particles (3%–8% opacity)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Wave Layer 1
                val path1 = Path()
                path1.moveTo(0f, h * 0.18f)
                val segments = 4
                for (i in 0..segments) {
                    val x = (w / segments) * i
                    val offset = (wavePhase1 * 2 * Math.PI) + (i * 1.5)
                    val y = (h * 0.18f) + (sin(offset).toFloat() * 18.dp.toPx())
                    if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
                }
                path1.lineTo(w, 0f)
                path1.lineTo(0f, 0f)
                path1.close()
                drawPath(
                    path = path1,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.05f),
                            Color(0xFF0284C7).copy(alpha = 0.04f),
                            Color(0xFF06B6D4).copy(alpha = 0.06f)
                        )
                    )
                )

                // Wave Layer 2 (Cross wave lower down)
                val path2 = Path()
                path2.moveTo(0f, h * 0.42f)
                for (i in 0..segments) {
                    val x = (w / segments) * i
                    val offset = (wavePhase2 * 2 * Math.PI) + (i * 1.8)
                    val y = (h * 0.42f) + (sin(offset).toFloat() * 22.dp.toPx())
                    if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
                }
                path2.lineTo(w, h * 0.55f)
                path2.lineTo(0f, h * 0.55f)
                path2.close()
                drawPath(
                    path = path2,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFBAE6FD).copy(alpha = 0.07f),
                            Color(0xFFE0F2FE).copy(alpha = 0.05f),
                            Color(0xFF38BDF8).copy(alpha = 0.06f)
                        )
                    )
                )

                // 5 subtle floating particles
                val drift1 = sin(wavePhase1 * 2 * Math.PI).toFloat() * 12.dp.toPx()
                val drift2 = sin(wavePhase2 * 2 * Math.PI).toFloat() * 14.dp.toPx()

                val particles = listOf(
                    Offset(w * 0.12f, h * 0.22f + drift1),
                    Offset(w * 0.88f, h * 0.16f - drift2),
                    Offset(w * 0.25f, h * 0.62f + drift2),
                    Offset(w * 0.90f, h * 0.70f - drift1),
                    Offset(w * 0.50f, h * 0.92f + drift1)
                )
                for (p in particles) {
                    drawCircle(
                        color = Color(0xFF0284C7).copy(alpha = 0.15f),
                        radius = 2.5.dp.toPx(),
                        center = p
                    )
                }
            }

            // Main Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. TOP BRAND AREA with entrance animation
                DrishtiBrandHeader(
                    alpha = logoAlpha,
                    scale = logoScale
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 2. ROLE TITLE SECTION
                RoleTitleSection()

                Spacer(modifier = Modifier.height(30.dp))

                // 3. EMBEDDED USER CARD
                RoleSelectionCard(
                    title = stringResource(R.string.auth_role_embedded_title),
                    description = stringResource(R.string.auth_role_embedded_desc),
                    visualType = RoleVisualType.EMBEDDED,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = card1Alpha
                            translationY = card1OffsetY.dp.toPx()
                        },
                    onClick = onSelectEmbeddedSignIn
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 4. PARENT / GUARDIAN CARD
                RoleSelectionCard(
                    title = stringResource(R.string.auth_role_parent_title),
                    description = stringResource(R.string.auth_role_parent_desc),
                    visualType = RoleVisualType.PARENT,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = card2Alpha
                            translationY = card2OffsetY.dp.toPx()
                        },
                    onClick = onSelectParentSignIn
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 5. "NEW TO DRISHTIAI?" DIVIDER
                NewToDrishtiDivider()

                Spacer(modifier = Modifier.height(20.dp))

                // 6. SIGN-UP BUTTONS (Side-by-side)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SignupActionButton(
                        text = stringResource(R.string.auth_btn_embedded_signup),
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onSelectEmbeddedSignUp
                    )

                    SignupActionButton(
                        text = stringResource(R.string.auth_btn_parent_signup),
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = onSelectParentSignUp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 7. SECURITY INFORMATION
                SecurityFooter()

                Spacer(modifier = Modifier.height(10.dp))

                // 8. PRIVACY NOTICE
                PrivacyNoticeLink(
                    onClick = { showLandingInfoDialog = true }
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (showLandingInfoDialog) {
        Dialog(
            onDismissRequest = { showLandingInfoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PrivacyAndDataPolicyScreen(
                onBack = { showLandingInfoDialog = false }
            )
        }
    }
}

/**
 * Top brand area featuring official Drishti logo and typography.
 */
@Composable
fun DrishtiBrandHeader(
    alpha: Float,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Official brand logo asset (Eye emblem)
        Image(
            painter = painterResource(id = R.drawable.drishti_logo_eye),
            contentDescription = "DrishtiAI Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(140.dp)
                .height(86.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // DRISHTI-AI Brand Name
        val brandPrefix = stringResource(R.string.auth_drishti_brand_prefix)
        val brandSuffix = stringResource(R.string.auth_drishti_brand_suffix)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF0F172A), fontWeight = FontWeight.Black)) {
                    append(brandPrefix)
                }
                withStyle(SpanStyle(color = Color(0xFF0284C7), fontWeight = FontWeight.Black)) {
                    append(brandSuffix)
                }
            },
            fontSize = 26.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = stringResource(R.string.auth_drishti_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Role title section: "Choose Your Role" with highlighted "Role".
 */
@Composable
fun RoleTitleSection(modifier: Modifier = Modifier) {
    val choosePrefix = stringResource(R.string.auth_choose_role_prefix)
    val chooseSuffix = stringResource(R.string.auth_choose_role_suffix)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF0F172A))) {
                    append(choosePrefix)
                }
                withStyle(SpanStyle(color = Color(0xFF0284C7))) {
                    append(chooseSuffix)
                }
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.auth_choose_role_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 340.dp)
        )
    }
}

enum class RoleVisualType {
    EMBEDDED,
    PARENT
}

/**
 * Premium native Role Selection Card.
 * Fully interactive with press scale (1.0 -> 0.985), border glow, elevated platform visual,
 * chips, and circular arrow button.
 */
@Composable
fun RoleSelectionCard(
    title: String,
    description: String,
    visualType: RoleVisualType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.982f else 1.0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
        animationSpec = tween(durationMillis = 160),
        label = "borderColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isPressed) 10.dp else 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $description. Tap to choose $title."
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFFAFCFF),
                            Color(0xFFF4F9FE)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Visual Area: Real character visual matching role
                RoleVisualIllustration(visualType = visualType)

                Spacer(modifier = Modifier.width(16.dp))

                // Card Text
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 19.sp,
                            fontSize = 13.5.sp
                        ),
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right circular action button
                RoleActionButton(isHovered = isPressed)
            }
        }
    }
}

/**
 * Left Visual Area: Clean, natural character illustration rendered directly with transparent background.
 * Strictly no square boxes, flat platforms, or geometric containers.
 */
@Composable
fun RoleVisualIllustration(
    visualType: RoleVisualType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(105.dp)
            .height(115.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ultra-subtle ambient soft circular light to blend image naturally onto the card
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE).copy(alpha = 0.55f),
                        Color(0xFFBAE6FD).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.55f),
                    radius = w * 0.48f
                ),
                topLeft = Offset(w * 0.05f, h * 0.10f),
                size = Size(w * 0.90f, h * 0.85f)
            )
        }

        // Direct character illustration without borders, platforms, or boxes
        when (visualType) {
            RoleVisualType.EMBEDDED -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_role_embedded_user),
                    contentDescription = "Embedded User Character",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            RoleVisualType.PARENT -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_role_parents),
                    contentDescription = "Parents Guardians Character",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                )
            }
        }
    }
}



/**
 * Right Circular Action Button with arrow.
 */
@Composable
fun RoleActionButton(
    isHovered: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = if (isHovered) Color(0xFFE0F2FE) else Color(0xFFF0F7FF),
        border = BorderStroke(1.dp, if (isHovered) Color(0xFF38BDF8) else Color(0xFFBAE6FD)),
        modifier = modifier
            .size(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Select role",
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * "New to DrishtiAI?" with subtle horizontal lines.
 */
@Composable
fun NewToDrishtiDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0xFFCBD5E1))
                    )
                )
        )

        Text(
            text = stringResource(R.string.auth_new_to_drishti),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = Color(0xFF64748B),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFCBD5E1), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * Sign-Up Action Button with press scale animation.
 */
@Composable
fun SignupActionButton(
    text: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "btnScale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = if (isPrimary) Color(0xFF0284C7) else Color(0xFFFFFFFF),
        border = if (isPrimary) null else BorderStroke(1.5.dp, Color(0xFF0284C7)),
        shadowElevation = if (isPrimary) 4.dp else 0.dp,
        modifier = modifier
            .height(56.dp)
            .scale(scale)
            .semantics {
                contentDescription = "$text button. Tap to register."
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = if (isPrimary) Color(0xFFFFFFFF) else Color(0xFF0284C7),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = if (isPrimary) Color(0xFFFFFFFF) else Color(0xFF0284C7),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Security footnote: native lock icon + supported claims.
 */
@Composable
fun SecurityFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.auth_security_note),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Clickable Safety Disclosures & Privacy Notice link.
 */
@Composable
fun PrivacyNoticeLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val linkText = stringResource(R.string.auth_safety_privacy_btn)
    TextButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = linkText
        }
    ) {
        Text(
            text = linkText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = Color(0xFF0284C7)
        )
    }
}

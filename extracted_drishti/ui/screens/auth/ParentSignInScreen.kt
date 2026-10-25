package com.example.drishti.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drishti.R
import com.example.drishti.auth.AuthResult
import com.example.drishti.auth.AuthValidator
import com.example.drishti.auth.UserRole
import com.example.drishti.supabase.SupabaseAuth
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * DrishtiAI — Parent / Guardian Login Screen
 *
 * Visual Focus:
 * - Clean Parent/Guardian hero illustration (father + mother couple) as primary focal point.
 * - NO Drishti logo or eye emblem on this screen.
 * - Smooth entrance animations with subtle staggered transitions.
 * - Fluid atmospheric wave motion in background (pale blue/cyan/white).
 * - Modern focused fields, primary gradient action button, and switch role button.
 * - Preserves 100% of existing Supabase authentication logic & error handling.
 */
@Composable
fun ParentSignInScreen(
    onSignInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onSwitchRole: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val supabaseAuth = remember { SupabaseAuth(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // ── STAGGERED PAGE ENTRANCE ANIMATION (Total under ~650ms) ───────────
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val heroAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 550, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "parentHeroAlpha"
    )
    val heroScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.94f,
        animationSpec = tween(durationMillis = 550, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "parentHeroScale"
    )
    val heroTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 18f,
        animationSpec = tween(durationMillis = 550, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "parentHeroTranslationY"
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "parentLabelAlpha"
    )
    val labelTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 14f,
        animationSpec = tween(durationMillis = 400, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "parentLabelTranslationY"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "parentTitleAlpha"
    )
    val titleTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 14f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "parentTitleTranslationY"
    )

    val formAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 450, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "parentFormAlpha"
    )
    val formTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 16f,
        animationSpec = tween(durationMillis = 450, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "parentFormTranslationY"
    )

    // ── SUBTLE AMBIENT WAVE MOTION (20-24s period, very low opacity) ────
    val infiniteTransition = rememberInfiniteTransition(label = "ambientWavesParent")
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase1"
    )
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase2"
    )

    fun handleSignIn() {
        if (isLoading) return
        focusManager.clearFocus()
        errorMessage = null
        emailError = null
        passwordError = null

        val emailVal = AuthValidator.validateEmail(email)
        if (emailVal is AuthValidator.ValidationResult.Invalid) {
            emailError = emailVal.errorMessage
            return
        }

        if (password.isEmpty()) {
            passwordError = "Password cannot be empty."
            return
        }

        isLoading = true
        coroutineScope.launch {
            val result = supabaseAuth.signIn(
                email = email.trim(),
                password = password,
                expectedRole = UserRole.PARENT
            )
            isLoading = false
            when (result) {
                is AuthResult.Success -> {
                    onSignInSuccess()
                }
                is AuthResult.RoleMismatch -> {
                    errorMessage = "This account is registered as an Embedded User account. Please use Embedded User login."
                }
                is AuthResult.Failure -> {
                    errorMessage = result.reason
                }
                is AuthResult.NetworkUnavailable -> {
                    errorMessage = "Unable to connect to DrishtiAI services. Check your internet connection."
                }
                is AuthResult.ConfirmationRequired -> {
                    errorMessage = "Please verify your email (${result.email}) before signing in."
                }
                is AuthResult.SessionExpired -> {
                    errorMessage = "Your session has expired. Please sign in again."
                }
            }
        }
    }

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
            // Subtle flowing ambient waves (pale blue/cyan/white)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Top wave near hero
                val path1 = Path()
                path1.moveTo(0f, h * 0.22f)
                val segments = 4
                for (i in 0..segments) {
                    val x = (w / segments) * i
                    val offset = (wavePhase1 * 2 * Math.PI) + (i * 1.5)
                    val y = (h * 0.22f) + (sin(offset).toFloat() * 16.dp.toPx())
                    if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
                }
                path1.lineTo(w, 0f)
                path1.lineTo(0f, 0f)
                path1.close()
                drawPath(
                    path = path1,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.045f),
                            Color(0xFF0284C7).copy(alpha = 0.035f),
                            Color(0xFF06B6D4).copy(alpha = 0.050f)
                        )
                    )
                )

                // Lower crossing wave
                val path2 = Path()
                path2.moveTo(0f, h * 0.48f)
                for (i in 0..segments) {
                    val x = (w / segments) * i
                    val offset = (wavePhase2 * 2 * Math.PI) + (i * 1.8)
                    val y = (h * 0.48f) + (sin(offset).toFloat() * 18.dp.toPx())
                    if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
                }
                path2.lineTo(w, h * 0.60f)
                path2.lineTo(0f, h * 0.60f)
                path2.close()
                drawPath(
                    path = path2,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFBAE6FD).copy(alpha = 0.05f),
                            Color(0xFFE0F2FE).copy(alpha = 0.04f),
                            Color(0xFF38BDF8).copy(alpha = 0.045f)
                        )
                    )
                )
            }

            // Main Content Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. HERO — PARENT / GUARDIAN (No logo, no platform, clean transparent asset)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .graphicsLayer {
                            alpha = heroAlpha
                            scaleX = heroScale
                            scaleY = heroScale
                            translationY = heroTranslationY.dp.toPx()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Soft natural ambient glow behind character
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE0F2FE).copy(alpha = 0.65f),
                                    Color(0xFFBAE6FD).copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.5f, h * 0.52f),
                                radius = w * 0.38f
                            ),
                            topLeft = Offset(w * 0.15f, h * 0.08f),
                            size = Size(w * 0.70f, h * 0.88f)
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.ic_role_parents),
                        contentDescription = "Parent and Guardian Caregivers",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 240.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. PARENT / GUARDIAN PILL LABEL
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier
                        .height(36.dp)
                        .graphicsLayer {
                            alpha = labelAlpha
                            translationY = labelTranslationY.dp.toPx()
                        }
                        .semantics {
                            contentDescription = "Active Role: Parent / Guardian"
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFF0284C7), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.auth_badge_parent_guardian),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF0369A1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. WELCOME TITLE & DESCRIPTION
                val welcomePrefix = stringResource(R.string.auth_welcome_prefix)
                val welcomeSuffix = stringResource(R.string.auth_welcome_suffix)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp)
                        .graphicsLayer {
                            alpha = titleAlpha
                            translationY = titleTranslationY.dp.toPx()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF0F172A))) {
                                append(welcomePrefix)
                            }
                            withStyle(SpanStyle(color = Color(0xFF0284C7))) {
                                append(welcomeSuffix)
                            }
                        },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.auth_parent_signin_desc),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 21.sp,
                            fontSize = 14.5.sp
                        ),
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 340.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. FORM FIELDS & ACTIONS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = formAlpha
                            translationY = formTranslationY.dp.toPx()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthErrorBanner(
                        errorMessage = errorMessage,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Email Field
                    ModernAuthTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) emailError = null
                        },
                        label = stringResource(R.string.auth_label_parent_email),
                        placeholder = stringResource(R.string.auth_placeholder_parent_email),
                        leadingIcon = Icons.Rounded.Email,
                        isError = emailError != null,
                        errorMessage = emailError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    ModernAuthTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError != null) passwordError = null
                        },
                        label = stringResource(R.string.auth_label_password),
                        placeholder = "••••••••",
                        leadingIcon = Icons.Rounded.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        isError = passwordError != null,
                        errorMessage = passwordError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleSignIn() }
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot Password Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val forgotBtnText = stringResource(R.string.auth_btn_forgot_password)
                        TextButton(
                            onClick = onNavigateToForgotPassword,
                            modifier = Modifier.semantics {
                                contentDescription = forgotBtnText
                            }
                        ) {
                            Text(
                                text = forgotBtnText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                ),
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Sign In Button
                    ModernPrimaryButton(
                        text = stringResource(R.string.auth_btn_signin_as_parent),
                        isLoading = isLoading,
                        onClick = { handleSignIn() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Account Creation Link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.auth_new_guardian),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val createAccText = stringResource(R.string.auth_btn_create_account)
                        TextButton(
                            onClick = onNavigateToSignUp,
                            modifier = Modifier.semantics {
                                contentDescription = createAccText
                            }
                        ) {
                            Text(
                                text = createAccText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Switch Role Outlined Button
                    ModernSwitchRoleButton(
                        text = stringResource(R.string.auth_btn_switch_role),
                        onClick = onSwitchRole
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

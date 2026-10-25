package com.example.drishti.ui.screens.auth

import android.content.Context
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
import androidx.compose.material.icons.rounded.*
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
 * DrishtiAI — Parent / Guardian Sign Up Screen
 *
 * Visual Focus:
 * - Clean Parent / Guardian couple illustration as primary hero visual.
 * - NO Drishti logo or eye emblem on this screen.
 * - Staggered smooth entrance animation.
 * - Subtle ambient flowing waves.
 * - 22dp rounded input fields with active focus glow.
 * - Primary gradient Create Parent Account button with press micro-interactions.
 * - 100% Supabase auth and profile data persistence (full_name, email, role, phone, relationship).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onSwitchRole: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val supabaseAuth = remember { SupabaseAuth(context) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Parent") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    val relationships = listOf("Parent", "Mother", "Father", "Guardian", "Sibling", "Caregiver")
    var relationshipExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // ── STAGGERED PAGE ENTRANCE ANIMATION ────────────────────────────
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
        targetValue = if (startAnimation) 0f else 14f,
        animationSpec = tween(durationMillis = 550, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "parentHeroTranslationY"
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "parentLabelAlpha"
    )
    val labelTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 10f,
        animationSpec = tween(durationMillis = 400, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "parentLabelTranslationY"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "parentTitleAlpha"
    )
    val titleTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 10f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "parentTitleTranslationY"
    )

    val formAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 450, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "parentFormAlpha"
    )
    val formTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 12f,
        animationSpec = tween(durationMillis = 450, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "parentFormTranslationY"
    )

    // ── SUBTLE AMBIENT WAVE MOTION ───────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "ambientWavesParentSignUp")
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase1"
    )
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase2"
    )

    fun handleSignUp() {
        if (isLoading) return
        focusManager.clearFocus()
        errorMessage = null
        nameError = null
        emailError = null
        phoneError = null
        passwordError = null
        confirmError = null

        val nameVal = AuthValidator.validateFullName(fullName)
        if (nameVal is AuthValidator.ValidationResult.Invalid) {
            nameError = nameVal.errorMessage
            return
        }

        val emailVal = AuthValidator.validateEmail(email)
        if (emailVal is AuthValidator.ValidationResult.Invalid) {
            emailError = emailVal.errorMessage
            return
        }

        if (phone.trim().isEmpty()) {
            phoneError = "Phone number is required for emergency contact."
            return
        }

        val passVal = AuthValidator.validatePassword(password)
        if (passVal is AuthValidator.ValidationResult.Invalid) {
            passwordError = passVal.errorMessage
            return
        }

        val confirmVal = AuthValidator.validatePasswordConfirmation(password, confirmPassword)
        if (confirmVal is AuthValidator.ValidationResult.Invalid) {
            confirmError = confirmVal.errorMessage
            return
        }

        isLoading = true
        coroutineScope.launch {
            val result = supabaseAuth.signUp(
                email = email.trim(),
                password = password,
                fullName = fullName.trim(),
                role = UserRole.PARENT,
                phone = phone.trim(),
                preferredLanguage = "English",
                relationship = relationship.trim()
            )
            isLoading = false
            when (result) {
                is AuthResult.Success -> {
                    context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE)
                        .edit()
                        .putString("emergency_user_id", result.userId)
                        .putString("emergency_phone", phone.trim())
                        .putString("emergency_name", fullName.trim())
                        .putString("emergency_phone_${result.userId}", phone.trim())
                        .putString("emergency_name_${result.userId}", fullName.trim())
                        .putString("profile_full_name", fullName.trim())
                        .putString("profile_phone", phone.trim())
                        .putString("profile_email", email.trim())
                        .apply()
                    onSignUpSuccess()
                }
                is AuthResult.ConfirmationRequired -> {
                    errorMessage = "Parent account registration initiated! Please check your email (${result.email}) to verify before signing in."
                }
                is AuthResult.Failure -> {
                    errorMessage = result.reason
                }
                is AuthResult.NetworkUnavailable -> {
                    errorMessage = "Unable to connect to DrishtiAI services. Check your internet connection."
                }
                else -> {
                    errorMessage = "Parent account registration could not be completed."
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
                .imePadding()
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
            // Subtle flowing ambient waves
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

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
                // 1. HERO — PARENT / GUARDIAN (No logo, clean transparent asset)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .graphicsLayer {
                            alpha = heroAlpha
                            scaleX = heroScale
                            scaleY = heroScale
                            translationY = heroTranslationY.dp.toPx()
                        },
                    contentAlignment = Alignment.Center
                ) {
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
                            .widthIn(max = 230.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. PARENT PILL LABEL
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
                            contentDescription = "Sign up role: Parent / Guardian"
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
                            text = stringResource(R.string.auth_badge_parent_signup),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 11.5.sp
                            ),
                            color = Color(0xFF0369A1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. TITLE & DESCRIPTION
                val createPrefix = stringResource(R.string.auth_create_account_prefix)
                val createSuffix = stringResource(R.string.auth_create_account_suffix)
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
                                append(createPrefix)
                            }
                            withStyle(SpanStyle(color = Color(0xFF0284C7))) {
                                append(createSuffix)
                            }
                        },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.auth_parent_signup_desc),
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

                // 4. FORM FIELDS
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

                    // Full Name
                    ModernAuthTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (nameError != null) nameError = null
                        },
                        label = stringResource(R.string.auth_label_full_name),
                        placeholder = "Jane Doe",
                        leadingIcon = Icons.Rounded.Person,
                        isError = nameError != null,
                        errorMessage = nameError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Email Address
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

                    // Phone Number (Required for Parents)
                    ModernAuthTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            if (phoneError != null) phoneError = null
                        },
                        label = stringResource(R.string.auth_label_emergency_phone),
                        placeholder = stringResource(R.string.auth_placeholder_phone),
                        leadingIcon = Icons.Rounded.Phone,
                        isError = phoneError != null,
                        errorMessage = phoneError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Relationship Dropdown
                    ExposedDropdownMenuBox(
                        expanded = relationshipExpanded,
                        onExpandedChange = { relationshipExpanded = !relationshipExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFFFFFFFF),
                            border = BorderStroke(1.dp, if (relationshipExpanded) Color(0xFF0284C7) else Color(0xFFE2E8F0)),
                            shadowElevation = if (relationshipExpanded) 3.dp else 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.People,
                                    contentDescription = null,
                                    tint = if (relationshipExpanded) Color(0xFF0284C7) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Relationship to User",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = relationship,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationshipExpanded)
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = relationshipExpanded,
                            onDismissRequest = { relationshipExpanded = false }
                        ) {
                            relationships.forEach { rel ->
                                DropdownMenuItem(
                                    text = { Text(rel) },
                                    onClick = {
                                        relationship = rel
                                        relationshipExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    ModernAuthTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError != null) passwordError = null
                        },
                        label = stringResource(R.string.auth_label_password_rules),
                        placeholder = "••••••••",
                        leadingIcon = Icons.Rounded.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        isError = passwordError != null,
                        errorMessage = passwordError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Confirm Password Field
                    ModernAuthTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmError != null) confirmError = null
                        },
                        label = stringResource(R.string.auth_label_confirm_password),
                        placeholder = "••••••••",
                        leadingIcon = Icons.Rounded.LockReset,
                        isPassword = true,
                        passwordVisible = confirmPasswordVisible,
                        onPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        isError = confirmError != null,
                        errorMessage = confirmError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleSignUp() }
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Create Parent Account Button
                    ModernPrimaryButton(
                        text = stringResource(R.string.auth_btn_create_account),
                        isLoading = isLoading,
                        onClick = { handleSignUp() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign In Link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.auth_already_have_account),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val signInText = stringResource(R.string.auth_btn_signin)
                        TextButton(
                            onClick = onNavigateToSignIn,
                            modifier = Modifier.semantics {
                                contentDescription = signInText
                            }
                        ) {
                            Text(
                                text = signInText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Switch Role Button
                    ModernSwitchRoleButton(
                        text = stringResource(R.string.auth_btn_switch_role),
                        onClick = onSwitchRole
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

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
import com.example.drishti.supabase.DataResult
import com.example.drishti.supabase.EmergencyContactModel
import com.example.drishti.util.LocaleHelper
import com.example.drishti.supabase.SupabaseAuth
import com.example.drishti.supabase.SupabaseRepository
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * DrishtiAI — Embedded User Sign Up Screen
 *
 * Visual Focus:
 * - Clean Embedded User hero illustration (visually impaired boy with cane) as primary hero element.
 * - NO Drishti logo or eye emblem on this screen.
 * - Staggered smooth entrance motion system.
 * - Ambient background waves with low opacity.
 * - 22dp rounded input fields with active focus glow.
 * - Primary gradient Create Account button with press micro-interactions.
 * - 100% Supabase auth and repository data persistence (full_name, email, role, phone, preferred_language, emergency contact).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddedSignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onSwitchRole: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val supabaseAuth = remember { SupabaseAuth(context) }
    val supabaseRepo = remember { SupabaseRepository(context) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val defaultLangCode = remember { LocaleHelper.getSelectedLanguageCode(context) }
    val initialLanguageDisplay = remember(defaultLangCode) {
        when (defaultLangCode) {
            "hi" -> "हिन्दी (Hindi)"
            "mr" -> "मराठी (Marathi)"
            else -> "English"
        }
    }
    var selectedLanguage by remember { mutableStateOf(initialLanguageDisplay) }
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var contactSaveError by remember { mutableStateOf<String?>(null) }
    var createdUserId by remember { mutableStateOf<String?>(null) }
    var isSavingContact by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    val languages = listOf("English", "हिन्दी (Hindi)", "मराठी (Marathi)")
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // ── STAGGERED PAGE ENTRANCE ANIMATION ────────────────────────────
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val heroAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 550, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "heroAlpha"
    )
    val heroScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.94f,
        animationSpec = tween(durationMillis = 550, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "heroScale"
    )
    val heroTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 14f,
        animationSpec = tween(durationMillis = 550, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "heroTranslationY"
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "labelAlpha"
    )
    val labelTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 10f,
        animationSpec = tween(durationMillis = 400, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "labelTranslationY"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )
    val titleTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 10f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "titleTranslationY"
    )

    val formAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 450, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "formAlpha"
    )
    val formTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 12f,
        animationSpec = tween(durationMillis = 450, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "formTranslationY"
    )

    // ── SUBTLE AMBIENT WAVE MOTION ───────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "ambientWavesSignUp")
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

    fun retrySaveContact() {
        val userId = createdUserId ?: return
        if (isSavingContact) return
        val contactName = emergencyContactName.trim().ifBlank { "Emergency Contact" }
        val contactPhone = emergencyContactPhone.trim()
        isSavingContact = true
        coroutineScope.launch {
            val saveRes = supabaseRepo.saveEmergencyContact(
                EmergencyContactModel(
                    user_id = userId,
                    name = contactName,
                    phone = contactPhone,
                    relation = "Emergency Contact"
                )
            )
            isSavingContact = false
            when (saveRes) {
                is DataResult.Success -> {
                    contactSaveError = null
                    onSignUpSuccess()
                }
                is DataResult.Unauthorized -> {
                    contactSaveError = "Session authorization error. Please sign in to verify and save your emergency contact."
                }
                is DataResult.Offline -> {
                    contactSaveError = "Device is offline. Check your network connection and retry."
                }
                is DataResult.Failure -> {
                    contactSaveError = "Retry failed: ${saveRes.message}"
                }
                else -> {
                    contactSaveError = "Could not save emergency contact. Please try again."
                }
            }
        }
    }

    fun handleSignUp() {
        if (isLoading) return
        focusManager.clearFocus()
        errorMessage = null
        contactSaveError = null
        nameError = null
        emailError = null
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
                role = UserRole.EMBEDDED_USER,
                phone = phone.ifBlank { null },
                preferredLanguage = selectedLanguage.split(" ").first()
            )
            when (result) {
                is AuthResult.Success -> {
                    // Update user profile info locally in drishti_settings
                    context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE).edit()
                        .putString("profile_full_name", fullName.trim())
                        .putString("profile_email", email.trim())
                        .apply()

                    if (emergencyContactPhone.isNotBlank()) {
                        val contactName = emergencyContactName.trim().ifBlank { "Emergency Contact" }
                        val contactPhone = emergencyContactPhone.trim()

                        // Explicitly persist to Supabase backend with authenticated user_id
                        val saveRes = supabaseRepo.saveEmergencyContact(
                            EmergencyContactModel(
                                user_id = result.userId,
                                name = contactName,
                                phone = contactPhone,
                                relation = "Emergency Contact"
                            )
                        )
                        isLoading = false
                        when (saveRes) {
                            is DataResult.Success -> {
                                onSignUpSuccess()
                            }
                            is DataResult.Unauthorized -> {
                                createdUserId = result.userId
                                contactSaveError = "Account created, but emergency contact could not be saved because the active session was not established. You can retry or proceed to Home and add it in Settings."
                            }
                            is DataResult.Offline -> {
                                createdUserId = result.userId
                                contactSaveError = "Account created, but your device is offline. The emergency contact could not be synchronized with Supabase. You can retry when connected or proceed to Home."
                            }
                            is DataResult.Failure -> {
                                createdUserId = result.userId
                                contactSaveError = "Account created, but the emergency contact could not be saved: ${saveRes.message}. You can retry or proceed to Home."
                            }
                            else -> {
                                createdUserId = result.userId
                                contactSaveError = "Account created, but saving your emergency contact encountered an error. You can retry or proceed to Home."
                            }
                        }
                    } else {
                        isLoading = false
                        onSignUpSuccess()
                    }
                }
                is AuthResult.ConfirmationRequired -> {
                    isLoading = false
                    errorMessage = "Registration initiated! Please check your email (${result.email}) to confirm your account before signing in."
                }
                is AuthResult.Failure -> {
                    isLoading = false
                    errorMessage = result.reason
                }
                is AuthResult.NetworkUnavailable -> {
                    isLoading = false
                    errorMessage = "Unable to connect to DrishtiAI services. Check your internet connection."
                }
                else -> {
                    isLoading = false
                    errorMessage = "Account registration could not be completed. Please try again."
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FBFF)
    ) { innerPadding ->
        if (contactSaveError != null) {
            AlertDialog(
                onDismissRequest = { /* Force explicit user resolution */ },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = "Emergency Contact Not Saved",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = contactSaveError ?: ""
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { retrySaveContact() },
                        enabled = !isSavingContact
                    ) {
                        if (isSavingContact) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = "Retry Saving Contact")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            contactSaveError = null
                            onSignUpSuccess()
                        },
                        enabled = !isSavingContact
                    ) {
                        Text(text = "Continue to Home")
                    }
                }
            )
        }

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
                // 1. HERO — EMBEDDED USER (No logo, clean transparent asset)
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
                        painter = painterResource(id = R.drawable.ic_role_embedded_user),
                        contentDescription = "Visually Impaired Embedded User with White Cane",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 230.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. EMBEDDED USER PILL LABEL
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
                            contentDescription = "Sign up role: Embedded User"
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
                            text = stringResource(R.string.auth_badge_embedded_signup),
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
                        text = stringResource(R.string.auth_embedded_signup_desc),
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
                        placeholder = stringResource(R.string.auth_placeholder_name),
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
                        label = stringResource(R.string.auth_label_email),
                        placeholder = stringResource(R.string.auth_placeholder_email),
                        leadingIcon = Icons.Rounded.Email,
                        isError = emailError != null,
                        errorMessage = emailError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Phone Number (Optional)
                    ModernAuthTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = stringResource(R.string.auth_label_phone_optional),
                        placeholder = stringResource(R.string.auth_placeholder_phone),
                        leadingIcon = Icons.Rounded.Phone,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preferred Language Dropdown
                    ExposedDropdownMenuBox(
                        expanded = languageMenuExpanded,
                        onExpandedChange = { languageMenuExpanded = !languageMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFFFFFFFF),
                            border = BorderStroke(1.dp, if (languageMenuExpanded) Color(0xFF0284C7) else Color(0xFFE2E8F0)),
                            shadowElevation = if (languageMenuExpanded) 3.dp else 1.dp,
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
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = if (languageMenuExpanded) Color(0xFF0284C7) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.auth_label_preferred_language),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = selectedLanguage,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuExpanded)
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = { languageMenuExpanded = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedLanguage = lang
                                        languageMenuExpanded = false
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
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Compact Emergency Contact Subsection
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_sec_emergency_contact),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.auth_sec_emergency_contact_sub),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ModernAuthTextField(
                            value = emergencyContactName,
                            onValueChange = { emergencyContactName = it },
                            label = stringResource(R.string.auth_label_emergency_name),
                            placeholder = stringResource(R.string.auth_placeholder_emergency_name),
                            leadingIcon = Icons.Rounded.Person
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ModernAuthTextField(
                            value = emergencyContactPhone,
                            onValueChange = { emergencyContactPhone = it },
                            label = stringResource(R.string.auth_label_emergency_phone),
                            placeholder = stringResource(R.string.auth_placeholder_phone),
                            leadingIcon = Icons.Rounded.Phone,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { handleSignUp() }
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Create Account Button
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
                        val signInBtnText = stringResource(R.string.auth_btn_signin)
                        TextButton(
                            onClick = onNavigateToSignIn,
                            modifier = Modifier.semantics {
                                contentDescription = signInBtnText
                            }
                        ) {
                            Text(
                                text = signInBtnText,
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

package com.example.drishti.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.drishti.auth.AuthResult
import com.example.drishti.auth.AuthValidator
import com.example.drishti.supabase.SupabaseAuth
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabaseAuth = remember { SupabaseAuth(context) }

    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var successSubmitted by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    fun handleSendResetLink() {
        if (isLoading) return
        errorMessage = null
        emailError = null

        val emailVal = AuthValidator.validateEmail(email)
        if (emailVal is AuthValidator.ValidationResult.Invalid) {
            emailError = emailVal.errorMessage
            return
        }

        isLoading = true
        coroutineScope.launch {
            val result = supabaseAuth.recoverPassword(email)
            isLoading = false
            when (result) {
                is AuthResult.Success -> {
                    successSubmitted = true
                }
                is AuthResult.Failure -> {
                    errorMessage = result.reason
                }
                is AuthResult.NetworkUnavailable -> {
                    errorMessage = "Unable to connect to DrishtiAI services. Check your internet connection."
                }
                else -> {
                    errorMessage = "Failed to send password recovery link."
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                AuthHeader(
                    title = "Forgot Password?",
                    subtitle = "Enter your registered email address to receive a secure recovery link."
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (successSubmitted) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Recovery Link Sent",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "If an account exists for $email, a secure password recovery email has been sent. Please check your inbox and follow the instructions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    AuthErrorBanner(
                        errorMessage = errorMessage,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    AuthTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) emailError = null
                        },
                        label = "Registered Email Address",
                        placeholder = "user@example.com",
                        leadingIcon = Icons.Rounded.Email,
                        isError = emailError != null,
                        errorMessage = emailError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AuthPrimaryButton(
                        text = "Send Recovery Link",
                        onClick = { handleSendResetLink() },
                        isLoading = isLoading,
                        enabled = email.isNotBlank()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .semantics {
                            contentDescription = "Return to Sign In"
                        }
                ) {
                    Text(
                        text = "Back to Sign In",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

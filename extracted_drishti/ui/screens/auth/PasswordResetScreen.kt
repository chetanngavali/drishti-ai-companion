package com.example.drishti.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
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
import androidx.compose.ui.unit.dp
import com.example.drishti.auth.AuthResult
import com.example.drishti.auth.AuthValidator
import com.example.drishti.supabase.SupabaseAuth
import kotlinx.coroutines.launch

@Composable
fun PasswordResetScreen(
    onPasswordResetSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabaseAuth = remember { SupabaseAuth(context) }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    fun handleUpdatePassword() {
        if (isLoading) return
        errorMessage = null
        passwordError = null
        confirmError = null

        val passVal = AuthValidator.validatePassword(newPassword)
        if (passVal is AuthValidator.ValidationResult.Invalid) {
            passwordError = passVal.errorMessage
            return
        }

        val confirmVal = AuthValidator.validatePasswordConfirmation(newPassword, confirmPassword)
        if (confirmVal is AuthValidator.ValidationResult.Invalid) {
            confirmError = confirmVal.errorMessage
            return
        }

        isLoading = true
        coroutineScope.launch {
            val result = supabaseAuth.updatePassword(newPassword)
            isLoading = false
            when (result) {
                is AuthResult.Success -> {
                    isSuccess = true
                }
                is AuthResult.Failure -> {
                    errorMessage = result.reason
                }
                is AuthResult.SessionExpired -> {
                    errorMessage = "Your password recovery session has expired. Please request a new recovery link."
                }
                is AuthResult.NetworkUnavailable -> {
                    errorMessage = "Unable to connect to DrishtiAI services. Check your internet connection."
                }
                else -> {
                    errorMessage = "Failed to update password. Please try again."
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
                    title = "Set New Password",
                    subtitle = "Enter and confirm your new secure password."
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (isSuccess) {
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
                                text = "Password Updated Successfully",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You can now sign in using your new password.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    AuthPrimaryButton(
                        text = "Continue to Sign In",
                        onClick = onPasswordResetSuccess
                    )
                } else {
                    AuthErrorBanner(
                        errorMessage = errorMessage,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    AuthTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            if (passwordError != null) passwordError = null
                        },
                        label = "New Password (min 8 chars, 1 letter, 1 number)",
                        leadingIcon = Icons.Rounded.Lock,
                        isPassword = true,
                        passwordVisible = newPasswordVisible,
                        onPasswordVisibilityToggle = { newPasswordVisible = !newPasswordVisible },
                        isError = passwordError != null,
                        errorMessage = passwordError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmError != null) confirmError = null
                        },
                        label = "Confirm New Password",
                        leadingIcon = Icons.Rounded.LockReset,
                        isPassword = true,
                        passwordVisible = confirmPasswordVisible,
                        onPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        isError = confirmError != null,
                        errorMessage = confirmError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AuthPrimaryButton(
                        text = "Update Password",
                        onClick = { handleUpdatePassword() },
                        isLoading = isLoading,
                        enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank()
                    )
                }
            }

            if (!isSuccess) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics { contentDescription = "Cancel and return to previous screen" }
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

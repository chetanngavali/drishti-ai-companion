package com.example.drishti.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = IntelligentBlue,
    onPrimary = PureWhiteSurface,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SkyBlueAccent,
    onSecondary = DeepSlateText,
    secondaryContainer = SurfaceVariantColor,
    onSecondaryContainer = DeepSlateText,
    background = SoftNeutralBackground,
    onBackground = DeepSlateText,
    surface = PureWhiteSurface,
    onSurface = DeepSlateText,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = SlateGrayMuted,
    error = DangerRed,
    onError = PureWhiteSurface,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorOnContainerLight
)

// WCAG AAA-compliant high contrast color scheme (black/white contrast with bold accents)
val HighContrastColorScheme = lightColorScheme(
    primary = Color(0xFF0038A8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF002266),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF0044CC),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5EEFF),
    onSecondaryContainer = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF000000),
    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD1D1),
    onErrorContainer = Color(0xFF660000)
)

val LargeTypography = Typography(
    displayLarge = Typography.displayLarge.copy(fontSize = 62.sp, lineHeight = 70.sp),
    headlineLarge = Typography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 44.sp),
    titleLarge = Typography.titleLarge.copy(fontSize = 25.sp, lineHeight = 32.sp),
    titleMedium = Typography.titleMedium.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleSmall = Typography.titleSmall.copy(fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = Typography.bodyLarge.copy(fontSize = 21.sp, lineHeight = 29.sp),
    bodyMedium = Typography.bodyMedium.copy(fontSize = 19.sp, lineHeight = 27.sp),
    bodySmall = Typography.bodySmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = Typography.labelLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
    labelMedium = Typography.labelMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = Typography.labelSmall.copy(fontSize = 13.sp, lineHeight = 18.sp)
)

@Composable
fun DrishtiTheme(
    dynamicColor: Boolean = false,
    highContrastOverride: Boolean? = null,
    largeTextOverride: Boolean? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember {
        try {
            context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    var isHighContrastState by remember(sharedPrefs, highContrastOverride) {
        mutableStateOf(highContrastOverride ?: (sharedPrefs?.getBoolean("high_contrast", false) ?: false))
    }
    var isLargeTextState by remember(sharedPrefs, largeTextOverride) {
        mutableStateOf(largeTextOverride ?: (sharedPrefs?.getBoolean("large_text", false) ?: false))
    }

    DisposableEffect(sharedPrefs, highContrastOverride, largeTextOverride) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "high_contrast" && highContrastOverride == null) {
                isHighContrastState = prefs.getBoolean("high_contrast", false)
            }
            if (key == "large_text" && largeTextOverride == null) {
                isLargeTextState = prefs.getBoolean("large_text", false)
            }
        }
        sharedPrefs?.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs?.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val isHighContrast = highContrastOverride ?: isHighContrastState
    val isLargeText = largeTextOverride ?: isLargeTextState

    val colorScheme = when {
        isHighContrast -> HighContrastColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                dynamicLightColorScheme(context)
            } catch (_: Exception) {
                LightColorScheme
            }
        }
        else -> LightColorScheme
    }

    val typography = if (isLargeText) LargeTypography else Typography

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}


# Drishti AI — UI Package Export

This archive contains ONLY the user interface components of Drishti AI:

## 1. `ui/` (Jetpack Compose UI)
- **`screens/`**:
  - `HomeScreen.kt` — Primary accessible dashboard with large tactile cards
  - `SettingsScreen.kt` — Comprehensive accessibility, voice, hardware & language settings
  - `NavigationScreen.kt` — Live outdoor turn-by-turn guidance and POI UI
  - `ScanScreen.kt` — Object and obstacle detection viewfinder HUD
  - `DescribeScreen.kt` — Scene description UI
  - `EmergencyScreen.kt` / `EmergencyActivity.kt` — Full-screen SOS triggers and countdowns
  - `LanguageSelectionScreen.kt` — Multilingual selector (मराठी, हिन्दी, English)
  - `NearbyScreen.kt` — Nearby transit & location explorer
  - `OfflineMapsScreen.kt` — Offline map download and management
  - `Esp32SettingsScreen.kt` — Smart cane / ESP32-CAM peripheral status UI
  - `DrishtiSplashScreen.kt` — Custom animated splash screen
- **`screens/auth/`**:
  - `AuthLandingScreen.kt`, `EmbeddedSignInScreen.kt`, `EmbeddedSignUpScreen.kt`
  - `ParentSignInScreen.kt`, `ParentSignUpScreen.kt`, `ParentDashboardScreen.kt`
  - `ForgotPasswordScreen.kt`, `PasswordResetScreen.kt`, `AuthComponents.kt`
- **`screens/info/`**:
  - `FirstRunSafetyNoticeScreen.kt` — Mandatory onboarding safety guidance
- **`components/`**:
  - `DrishtiCore.kt` — Core accessible buttons, high-contrast badges, cards, text styles
  - `DrishtiEyeLogo.kt` — Vector branding element
- **`theme/`**:
  - `Color.kt` — Curated high-contrast accessible color tokens
  - `Theme.kt` — Dynamic Material3 dark/light accessible theme provider
  - `Type.kt` — Accessible typography scales

## 2. `homemapping_ui/`
- Indoor 3D & 2D interactive canvas mapping viewports, review screens, and scanning HUD:
  - `HomeMappingScreen.kt`, `HomeScanScreen.kt`, `HomeMapViewerScreen.kt`
  - `HomeMapReviewScreen.kt`, `HomeMapUpdateScreen.kt`
  - `HomeMap3DRenderer.kt`, `HomeMapCanvasRenderer.kt`
  - `HomeMappingViewModel.kt`, `HomeMappingState.kt`

## 3. `navigation/`
- `AppNavigation.kt` — Root Compose navigation controller, animated transitions, accessible bottom navigation tabs

## 4. `MainActivity.kt`
- Edge-to-edge Compose host activity applying `DrishtiTheme` and locale contexts

## 5. `res/` (UI Assets & Resources)
- **`drawable/`** — Vector icons, logos, branding assets, splash backgrounds
- **`layout/`** — Voice assistant overlays and accessibility status widgets
- **`values/`** — Colors, base strings, accessibility dimensions, themes
- **`values-hi/`** — Localized Hindi UI strings
- **`values-mr/`** — Localized Marathi UI strings
- **`values-night/`** — Dark mode theme definitions

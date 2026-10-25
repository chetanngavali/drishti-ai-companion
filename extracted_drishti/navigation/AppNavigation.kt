package com.example.drishti.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import com.example.drishti.ui.theme.IntelligentBlue
import com.example.drishti.homemapping.ui.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.drishti.auth.AuthResult
import com.example.drishti.auth.ProfileModel
import com.example.drishti.auth.UserRole
import com.example.drishti.supabase.SupabaseAuth
import com.example.drishti.ui.screens.*
import com.example.drishti.ui.screens.auth.*
import com.example.drishti.ui.screens.info.FirstRunSafetyNoticeScreen
import com.example.drishti.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.drishti.ui.components.DrishtiCore
import com.example.drishti.ui.components.DrishtiCoreState
import com.example.drishti.ui.components.DrishtiEyeLogo
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

@Serializable
sealed interface TopLevelRoute {
    val title: String
    val icon: ImageVector
}

@Serializable
data object HomeRoute : TopLevelRoute {
    override val title = "Home"
    override val icon = Icons.Rounded.Home
}

@Serializable
data object ScanVisionRoute : TopLevelRoute {
    override val title = "Scan"
    override val icon = Icons.Rounded.Visibility
}

@Serializable
data object NavigationMapRoute : TopLevelRoute {
    override val title = "Navigate"
    override val icon = Icons.Rounded.Navigation
}

@Serializable
data object SettingsRoute : TopLevelRoute {
    override val title = "Settings"
    override val icon = Icons.Rounded.Settings
}

@Serializable
data object DescribeRoute : TopLevelRoute {
    override val title = "Describe"
    override val icon = Icons.Rounded.AutoAwesome
}

@Serializable
data object NearbyRoute : TopLevelRoute {
    override val title = "Nearby"
    override val icon = Icons.Rounded.Place
}

@Serializable
data object EmergencyRoute : TopLevelRoute {
    override val title = "Emergency"
    override val icon = Icons.Rounded.Warning
}

@Serializable
data object HomeMappingRoute : TopLevelRoute {
    override val title = "Home Mapping"
    override val icon = Icons.Rounded.Map
}

val TOP_LEVEL_ROUTES: List<TopLevelRoute> = listOf(
    HomeRoute,
    ScanVisionRoute,
    NavigationMapRoute,
    HomeMappingRoute,
    SettingsRoute
)

data class CanonicalPlaceResult(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val category: String,
    val requestId: String
) {
    fun toPlaceItem(): PlaceItem {
        return PlaceItem(
            id = placeId,
            name = name,
            category = category,
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distanceMeters,
            address = address,
            rating = null
        )
    }
}

data class CanonicalVoiceResults(
    val requestId: String,
    val category: String,
    val places: List<CanonicalPlaceResult>,
    val timestamp: Long = System.currentTimeMillis()
)

object NearbyNavigationState {
    val requestedCategory = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val canonicalVoiceResults = kotlinx.coroutines.flow.MutableStateFlow<CanonicalVoiceResults?>(null)
    val voiceNavState = kotlinx.coroutines.flow.MutableStateFlow(VoiceNavigationState.IDLE)
    val selectedDestination = kotlinx.coroutines.flow.MutableStateFlow<SelectedCanonicalPlace?>(null)
    val selectedTravelMode = kotlinx.coroutines.flow.MutableStateFlow<NavigationTravelMode?>(null)

    fun clearVoiceResults() {
        canonicalVoiceResults.value = null
        resetConversationalNav()
    }

    fun resetConversationalNav() {
        voiceNavState.value = VoiceNavigationState.IDLE
        selectedDestination.value = null
        selectedTravelMode.value = null
    }

    fun mapToScreenCategory(category: String): String {
        return when (category.lowercase(java.util.Locale.ROOT).trim()) {
            "hospital", "hospitals", "clinic", "doctor" -> "Hospitals"
            "pharmacy", "pharmacies", "chemist", "medical store", "medical" -> "Pharmacies"
            "atm", "atms", "bank", "banks" -> "ATMs"
            "railway station", "railway stations", "transit_station", "train station" -> "Railway stations"
            "bus stop", "bus stops", "bus station", "bus stand" -> "Bus stops"
            "restaurant", "restaurants", "food", "cafe" -> "Restaurants"
            else -> "Hospitals"
        }
    }
}

object AppNavigationController {
    val pendingRoute = kotlinx.coroutines.flow.MutableStateFlow<TopLevelRoute?>(null)
    fun navigateTo(route: TopLevelRoute): Boolean {
        return try {
            pendingRoute.value = route
            true
        } catch (e: Exception) {
            false
        }
    }
}

class TopLevelBackStack<T : Any>(startKey: T) {
    private var topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>> = linkedMapOf(
        startKey to mutableStateListOf(startKey)
    )

    var topLevelKey by mutableStateOf(startKey)
        private set

    val backStack = mutableStateListOf(startKey)

    private fun updateBackStack() =
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }

    fun addTopLevel(key: T) {
        if (topLevelStacks[key] == null) {
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            topLevelStacks.apply {
                remove(key)?.let {
                    put(key, it)
                }
            }
        }
        topLevelKey = key
        updateBackStack()
    }

    fun removeLast() {
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        topLevelStacks.remove(removedKey)
        if (topLevelStacks.isNotEmpty()) {
            topLevelKey = topLevelStacks.keys.last()
        }
        updateBackStack()
    }
}

sealed interface AppAuthState {
    object Initializing : AppAuthState
    object Unauthenticated : AppAuthState
    data class Authenticated(val role: UserRole, val profile: ProfileModel?) : AppAuthState
}

sealed interface AuthScreenRoute {
    object Landing : AuthScreenRoute
    object EmbeddedSignIn : AuthScreenRoute
    object EmbeddedSignUp : AuthScreenRoute
    object ParentSignIn : AuthScreenRoute
    object ParentSignUp : AuthScreenRoute
    object ForgotPassword : AuthScreenRoute
    object PasswordReset : AuthScreenRoute
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val supabaseAuth = remember { SupabaseAuth(context) }

    var isAppInitializing by remember { mutableStateOf(true) }
    var startupProgress by remember { mutableFloatStateOf(0.15f) }
    var startupStatus by remember { mutableStateOf("Initializing Drishti...") }
    var authState by remember { mutableStateOf<AppAuthState>(AppAuthState.Unauthenticated) }
    var currentAuthRoute by remember { mutableStateOf<AuthScreenRoute>(AuthScreenRoute.Landing) }

    val sharedPrefs = remember { context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE) }
    var isSafetyNoticeAcknowledged by remember {
        mutableStateOf(sharedPrefs.getBoolean("safety_notice_acknowledged_v1", false))
    }

    LaunchedEffect(Unit) {
        val minDelay = launch { delay(800L) } // Brief 800ms minimum delay to avoid jarring UI flicker
        try {
            startupProgress = 0.25f
            startupStatus = "Initializing Drishti..."
            delay(100L)

            startupProgress = 0.55f
            startupStatus = "Checking session..."

            // Bounded session restoration with 3500ms max timeout
            val result = withTimeoutOrNull(3500L) {
                supabaseAuth.restoreSession(timeoutMs = 3000L)
            }

            startupProgress = 0.90f
            startupStatus = "Preparing application..."

            if (result is AuthResult.Success && result.profile != null) {
                val role = UserRole.fromBackendKeyOrNull(result.profile.role)
                if (role != null) {
                    authState = AppAuthState.Authenticated(role, result.profile)
                } else {
                    authState = AppAuthState.Unauthenticated
                }
            } else {
                authState = AppAuthState.Unauthenticated
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            authState = AppAuthState.Unauthenticated
        } finally {
            minDelay.join()
            startupProgress = 1.0f
            startupStatus = "Ready"
            delay(80L)
            isAppInitializing = false
        }
    }

    val scope = rememberCoroutineScope()

    var isLanguageSelected by remember {
        mutableStateOf(sharedPrefs.getBoolean("language_selected_v1", false))
    }

    // 1. Initializing / Splash Screen (Truthful stage progress 0.15 -> 1.0)
    if (isAppInitializing) {
        val animatedProgress by animateFloatAsState(
            targetValue = startupProgress,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "splash_progress_anim"
        )

        DrishtiSplashScreen(
            progress = animatedProgress,
            statusTitle = "Initializing Drishti...",
            statusSubtitle = startupStatus
        )
        return
    }

    // 2. Language Selection Screen (Immediately after Splash, ensures default language everywhere)
    if (!isLanguageSelected) {
        LanguageSelectionScreen(
            onLanguageConfirmed = {
                isLanguageSelected = true
            }
        )
        return
    }

    // 3. First-Run Safety Notice Gate (Interrupts only on first launch until acknowledged)
    if (!isSafetyNoticeAcknowledged) {
        FirstRunSafetyNoticeScreen(
            onAcknowledge = {
                sharedPrefs.edit().putBoolean("safety_notice_acknowledged_v1", true).apply()
                isSafetyNoticeAcknowledged = true
                scope.launch {
                    try {
                        com.example.drishti.supabase.SupabaseRepository(context).recordUserConsent("SAFETY_NOTICE", "v1.0", true)
                    } catch (_: Exception) {}
                }
            }
        )
        return
    }

    // 3. Sequential Permission Onboarding Flow
    // Checks if any required permissions are missing; if so, displays one permission per page.
    var isPermissionOnboardingCompleted by remember {
        mutableStateOf(sharedPrefs.getBoolean("permission_onboarding_completed_v1", false))
    }

    val permissionManager = remember {
        com.example.drishti.permission.PermissionOnboardingManager(
            context = context,
            onAllPermissionsComplete = {
                sharedPrefs.edit().putBoolean("permission_onboarding_completed_v1", true).apply()
                isPermissionOnboardingCompleted = true
                (context as? com.example.drishti.MainActivity)?.maybeStartAuthorizedServices()
            }
        )
    }

    // Always re-verify real system state: if a required permission is revoked from Settings, show onboarding
    val hasAllRequiredPermissions = remember(isPermissionOnboardingCompleted, permissionManager.currentPageIndex) {
        com.example.drishti.permission.DrishtiPermissionCatalog.areAllRequiredGranted(context)
    }

    if (!isPermissionOnboardingCompleted || !hasAllRequiredPermissions) {
        com.example.drishti.permission.PermissionOnboardingScreen(
            manager = permissionManager
        )
        return
    }

    // 4. Main Application Content (Animated transitions between Auth and Authenticated)
    AnimatedContent(
        targetState = authState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppAuthStateTransition"
    ) { state ->
        when (state) {
            is AppAuthState.Initializing -> {
                // Handled above by isAppInitializing
            }

            is AppAuthState.Unauthenticated -> {
                BackHandler(enabled = currentAuthRoute != AuthScreenRoute.Landing) {
                    currentAuthRoute = AuthScreenRoute.Landing
                }

                when (currentAuthRoute) {
                    is AuthScreenRoute.Landing -> {
                        AuthLandingScreen(
                            onSelectEmbeddedSignIn = { currentAuthRoute = AuthScreenRoute.EmbeddedSignIn },
                            onSelectEmbeddedSignUp = { currentAuthRoute = AuthScreenRoute.EmbeddedSignUp },
                            onSelectParentSignIn = { currentAuthRoute = AuthScreenRoute.ParentSignIn },
                            onSelectParentSignUp = { currentAuthRoute = AuthScreenRoute.ParentSignUp }
                        )
                    }

                    is AuthScreenRoute.EmbeddedSignIn -> {
                        EmbeddedSignInScreen(
                            onSignInSuccess = {
                                val role = SupabaseAuth.currentRoleFlow.value
                                val profile = SupabaseAuth.currentProfileFlow.value
                                if (role == UserRole.EMBEDDED_USER && profile != null) {
                                    authState = AppAuthState.Authenticated(role, profile)
                                } else {
                                    authState = AppAuthState.Unauthenticated
                                    currentAuthRoute = AuthScreenRoute.Landing
                                }
                            },
                            onNavigateToSignUp = { currentAuthRoute = AuthScreenRoute.EmbeddedSignUp },
                            onNavigateToForgotPassword = { currentAuthRoute = AuthScreenRoute.ForgotPassword },
                            onSwitchRole = { currentAuthRoute = AuthScreenRoute.Landing }
                        )
                    }

                    is AuthScreenRoute.EmbeddedSignUp -> {
                        EmbeddedSignUpScreen(
                            onSignUpSuccess = {
                                val role = SupabaseAuth.currentRoleFlow.value
                                val profile = SupabaseAuth.currentProfileFlow.value
                                if (role == UserRole.EMBEDDED_USER && profile != null) {
                                    authState = AppAuthState.Authenticated(role, profile)
                                } else {
                                    currentAuthRoute = AuthScreenRoute.EmbeddedSignIn
                                }
                            },
                            onNavigateToSignIn = { currentAuthRoute = AuthScreenRoute.EmbeddedSignIn },
                            onSwitchRole = { currentAuthRoute = AuthScreenRoute.Landing }
                        )
                    }

                    is AuthScreenRoute.ParentSignIn -> {
                        ParentSignInScreen(
                            onSignInSuccess = {
                                val role = SupabaseAuth.currentRoleFlow.value
                                val profile = SupabaseAuth.currentProfileFlow.value
                                if (role == UserRole.PARENT && profile != null) {
                                    authState = AppAuthState.Authenticated(role, profile)
                                } else {
                                    authState = AppAuthState.Unauthenticated
                                    currentAuthRoute = AuthScreenRoute.Landing
                                }
                            },
                            onNavigateToSignUp = { currentAuthRoute = AuthScreenRoute.ParentSignUp },
                            onNavigateToForgotPassword = { currentAuthRoute = AuthScreenRoute.ForgotPassword },
                            onSwitchRole = { currentAuthRoute = AuthScreenRoute.Landing }
                        )
                    }

                    is AuthScreenRoute.ParentSignUp -> {
                        ParentSignUpScreen(
                            onSignUpSuccess = {
                                val role = SupabaseAuth.currentRoleFlow.value
                                val profile = SupabaseAuth.currentProfileFlow.value
                                if (role == UserRole.PARENT && profile != null) {
                                    authState = AppAuthState.Authenticated(role, profile)
                                } else {
                                    currentAuthRoute = AuthScreenRoute.ParentSignIn
                                }
                            },
                            onNavigateToSignIn = { currentAuthRoute = AuthScreenRoute.ParentSignIn },
                            onSwitchRole = { currentAuthRoute = AuthScreenRoute.Landing }
                        )
                    }

                    is AuthScreenRoute.ForgotPassword -> {
                        ForgotPasswordScreen(
                            onNavigateBack = { currentAuthRoute = AuthScreenRoute.Landing }
                        )
                    }

                    is AuthScreenRoute.PasswordReset -> {
                        PasswordResetScreen(
                            onPasswordResetSuccess = { currentAuthRoute = AuthScreenRoute.Landing },
                            onNavigateBack = { currentAuthRoute = AuthScreenRoute.Landing }
                        )
                    }
                }
            }

            is AppAuthState.Authenticated -> {
                when (state.role) {
                    UserRole.PARENT -> {
                        ParentDashboardScreen(
                            onSignOut = {
                                supabaseAuth.signOut()
                                authState = AppAuthState.Unauthenticated
                                currentAuthRoute = AuthScreenRoute.Landing
                            }
                        )
                    }

                    UserRole.EMBEDDED_USER -> {
                        EmbeddedUserApp(
                            onSignOut = {
                                supabaseAuth.signOut()
                                authState = AppAuthState.Unauthenticated
                                currentAuthRoute = AuthScreenRoute.Landing
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preserved original DrishtiAI assistive vision & navigation interface for Embedded Users.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddedUserApp(
    onSignOut: () -> Unit
) {
    val topLevelBackStack = remember { TopLevelBackStack<Any>(HomeRoute) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppNavigationController.pendingRoute.collect { route ->
            if (route != null) {
                topLevelBackStack.addTopLevel(route)
                AppNavigationController.pendingRoute.value = null
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Header Section
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                DrishtiEyeLogo(
                                    width = 46.dp,
                                    height = 36.dp,
                                    showText = false
                                )
                            }
                            Column {
                                Text(
                                    text = "Drishti AI",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Assistive Navigation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Navigation Items in Sidebar
                        TOP_LEVEL_ROUTES.forEach { route ->
                            val isSelected = route == topLevelBackStack.topLevelKey
                            val navLabel = when (route) {
                                is HomeRoute -> stringResource(R.string.nav_home)
                                is ScanVisionRoute -> stringResource(R.string.nav_scan)
                                is NavigationMapRoute -> stringResource(R.string.nav_navigate)
                                is SettingsRoute -> stringResource(R.string.nav_settings)
                                else -> route.title
                            }

                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = route.icon,
                                        contentDescription = navLabel,
                                        tint = if (isSelected) IntelligentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = navLabel,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) IntelligentBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    topLevelBackStack.addTopLevel(route)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .height(56.dp)
                            )
                        }
                    }

                    // Bottom Emergency SOS Button inside Sidebar
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = {
                                topLevelBackStack.addTopLevel(EmergencyRoute)
                                coroutineScope.launch { drawerState.close() }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Emergency SOS",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Sign Out Button in Drawer
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    onSignOut()
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .semantics {
                                    contentDescription = "Sign Out button. Tap to log out."
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Sign Out",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (topLevelBackStack.topLevelKey != HomeRoute && topLevelBackStack.topLevelKey != ScanVisionRoute) {
                    TopAppBar(
                        title = {
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = when (topLevelBackStack.topLevelKey) {
                                        is ScanVisionRoute -> stringResource(R.string.nav_scan)
                                        is NavigationMapRoute -> stringResource(R.string.nav_navigate)
                                        is SettingsRoute -> stringResource(R.string.nav_settings)
                                        is DescribeRoute -> stringResource(R.string.nav_describe)
                                        is NearbyRoute -> stringResource(R.string.nav_nearby)
                                        is EmergencyRoute -> stringResource(R.string.nav_emergency)
                                        else -> stringResource(R.string.app_name)
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { topLevelBackStack.addTopLevel(HomeRoute) }) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBackIosNew,
                                    contentDescription = "Back to Home",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            // Emergency SOS Quick Button
                            Surface(
                                onClick = { topLevelBackStack.addTopLevel(EmergencyRoute) },
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .height(38.dp)
                                    .semantics { contentDescription = "Emergency SOS button" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.nav_sos_btn),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        ) { innerPadding ->
            NavDisplay(
                backStack = topLevelBackStack.backStack,
                onBack = { topLevelBackStack.removeLast() },
                modifier = Modifier.padding(innerPadding),
                entryProvider = entryProvider {
                    entry<HomeRoute> {
                        HomeScreen(
                            onNavigateToScan = { topLevelBackStack.addTopLevel(ScanVisionRoute) },
                            onNavigateToMap = { topLevelBackStack.addTopLevel(NavigationMapRoute) },
                            onNavigateToNearby = { topLevelBackStack.addTopLevel(NearbyRoute) },
                            onNavigateToDescribe = { topLevelBackStack.addTopLevel(DescribeRoute) },
                            onNavigateToEmergency = { topLevelBackStack.addTopLevel(EmergencyRoute) },
                            onOpenNavigation = { coroutineScope.launch { drawerState.open() } }
                        )
                    }
                    entry<ScanVisionRoute> {
                        ScanScreen()
                    }
                    entry<DescribeRoute> {
                        DescribeScreen(onNavigateBack = { topLevelBackStack.removeLast() })
                    }
                    entry<NavigationMapRoute> {
                        NavigationScreen()
                    }
                    entry<NearbyRoute> {
                        NearbyScreen()
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(onSignOut = onSignOut)
                    }
                    entry<EmergencyRoute> {
                        EmergencyScreen()
                    }
                    entry<HomeMappingRoute> {
                        val homeMappingViewModel: com.example.drishti.homemapping.ui.HomeMappingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        var currentSubScreen by remember { mutableStateOf("DASHBOARD") }

                        when (currentSubScreen) {
                            "DASHBOARD" -> com.example.drishti.homemapping.ui.HomeMappingScreen(
                                viewModel = homeMappingViewModel,
                                onStartNewScan = {
                                    homeMappingViewModel.startNewScan()
                                    currentSubScreen = "SCAN"
                                },
                                onContinueIncompleteScan = {
                                    homeMappingViewModel.continueIncompleteScan()
                                    currentSubScreen = "SCAN"
                                },
                                onViewMap = { map ->
                                    homeMappingViewModel.selectMap(map)
                                    currentSubScreen = "VIEWER"
                                },
                                onReviewObjects = { map ->
                                    homeMappingViewModel.selectMap(map)
                                    currentSubScreen = "REVIEW"
                                },
                                onViewUpdates = {
                                    currentSubScreen = "UPDATES"
                                },
                                onNavigateBack = { topLevelBackStack.removeLast() }
                            )
                            "SCAN" -> com.example.drishti.homemapping.ui.HomeScanScreen(
                                viewModel = homeMappingViewModel,
                                onScanFinished = { currentSubScreen = "VIEWER" },
                                onNavigateBack = { currentSubScreen = "DASHBOARD" }
                            )
                            "VIEWER" -> com.example.drishti.homemapping.ui.HomeMapViewerScreen(
                                viewModel = homeMappingViewModel,
                                onNavigateBack = { currentSubScreen = "DASHBOARD" }
                            )
                            "REVIEW" -> com.example.drishti.homemapping.ui.HomeMapReviewScreen(
                                viewModel = homeMappingViewModel,
                                onNavigateBack = { currentSubScreen = "DASHBOARD" }
                            )
                            "UPDATES" -> com.example.drishti.homemapping.ui.HomeMapUpdateScreen(
                                viewModel = homeMappingViewModel,
                                onNavigateBack = { currentSubScreen = "DASHBOARD" }
                            )
                        }
                    }
                }
            )
        }
    }
}

package com.example.drishti.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.drishti.R
import com.example.drishti.navigation.*
import com.example.drishti.navigation.indoor.*
import com.example.drishti.vision.safety.*
import com.example.drishti.supabase.SavedPlaceModel
import com.example.drishti.supabase.SessionTokenProvider
import com.example.drishti.supabase.SupabaseRepository
import com.example.drishti.util.HapticHelper
import com.example.drishti.voice.ActionRegistry
import com.example.drishti.voice.MicrophoneCoordinator
import com.example.drishti.voice.VoiceRecognizerManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class NavWorkspaceState {
    SEARCHING, PREVIEW, NAVIGATING, ARRIVED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mapManager = remember { MapManager(context) }
    val indoorOutdoorEngine = remember { IndoorOutdoorEngine(context) }
    val supabaseRepo = remember { SupabaseRepository(context) }
    val currentLanguageCode by com.example.drishti.util.LocaleHelper.appLanguageCode.collectAsState()

    val indoorEnv by indoorOutdoorEngine.currentEnvironment.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            mapManager.speak("Location permission granted. Google GPS live navigation active.")
        } else {
            mapManager.speak("Location permission is required for real-time navigation.", true)
        }
    }

    LaunchedEffect(Unit) {
        indoorOutdoorEngine.start()
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            mapManager.speak("Google Maps Navigation workspace ready. Speak or enter destination.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapManager.shutdown()
            indoorOutdoorEngine.stop()
        }
    }

    // State Variables
    var currentLatLng by remember { mutableStateOf<LatLng?>(null) }
    var isLocationAvailable by remember { mutableStateOf(false) }
    var destinationQuery by remember { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<DetailedPlace?>(null) }
    var searchResults by remember { mutableStateOf<List<DetailedPlace>>(emptyList()) }
    var autocompletePredictions by remember { mutableStateOf<List<AutocompleteResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchStatusMessage by remember { mutableStateOf<String?>(null) }

    var workspaceState by remember { mutableStateOf(NavWorkspaceState.SEARCHING) }
    var selectedTravelMode by remember { mutableStateOf("WALK") }
    var currentRoute by remember { mutableStateOf<ParsedRoute?>(null) }
    var isRouteCalculating by remember { mutableStateOf(false) }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var isListeningForDestination by remember { mutableStateOf(false) }
    var isAlongRouteSearchOpen by remember { mutableStateOf(false) }
    var isHeadUpMode by remember { mutableStateOf(true) }
    var isVoiceMuted by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng ?: com.google.android.gms.maps.model.LatLng(20.5937, 78.9629), 15f)
    }

    // Turn-by-Turn engine state observation
    val navEngineState by mapManager.navigationEngine.navState.collectAsState()

    // Continuous Full-Time Real-Time GPS Tracking
    val liveGpsLocation by mapManager.locationFlow.collectAsState()

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            mapManager.startContinuousLocationTracking()
            val initialLoc = mapManager.getCurrentLocation()
            if (initialLoc != null) {
                currentLatLng = LatLng(initialLoc.latitude, initialLoc.longitude)
                isLocationAvailable = true
                cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLatLng!!, 16f)
                indoorOutdoorEngine.onLocationUpdated(initialLoc)
            }
        } else {
            isLocationAvailable = false
        }
    }

    LaunchedEffect(liveGpsLocation, workspaceState, isHeadUpMode, navEngineState) {
        val loc = liveGpsLocation
        if (loc != null) {
            val newLatLng = LatLng(loc.latitude, loc.longitude)
            currentLatLng = newLatLng
            isLocationAvailable = true
            indoorOutdoorEngine.onLocationUpdated(loc)

            if (workspaceState == NavWorkspaceState.NAVIGATING && navEngineState is NavState.Active) {
                val active = navEngineState as NavState.Active
                val bearing = if (isHeadUpMode) {
                    if (active.currentHeading > 0f) active.currentHeading else active.targetBearing
                } else {
                    0f
                }
                val tilt = if (isHeadUpMode) 45f else 0f
                val zoom = if (isHeadUpMode) 18f else 16.5f

                val targetCamPos = CameraPosition.Builder()
                    .target(newLatLng)
                    .zoom(zoom)
                    .tilt(tilt)
                    .bearing(bearing)
                    .build()

                cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(targetCamPos), 600)
            } else if (workspaceState == NavWorkspaceState.SEARCHING && !cameraPositionState.isMoving) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLng(newLatLng))
            }
        }
    }

    // Register ActionRegistry listeners so voice commands can control the navigation UI
    DisposableEffect(Unit) {
        ActionRegistry.registerNavigationListener("NAVIGATE") { params ->
            val dest = params["destination"]?.toString() ?: ""
            val lat = params["latitude"] as? Double
            val lng = params["longitude"] as? Double
            if (lat != null && lng != null) {
                val place = DetailedPlace(
                    id = "voice_target",
                    name = dest,
                    address = dest,
                    latitude = lat,
                    longitude = lng,
                    category = "Destination",
                    distanceMeters = if (currentLatLng != null) NavigationEngine.distanceMeters(currentLatLng!!, LatLng(lat, lng)).toInt() else 0
                )
                selectedPlace = place
                workspaceState = NavWorkspaceState.PREVIEW
            } else if (dest.isNotBlank()) {
                destinationQuery = dest
                performSearch(dest, mapManager, currentLatLng, coroutineScope, { isSearching = it }, { searchResults = it }, { searchStatusMessage = it }, { place ->
                    selectedPlace = place
                    workspaceState = NavWorkspaceState.PREVIEW
                })
            }
        }

        ActionRegistry.registerNavigationListener("SEARCH_NEARBY") { params ->
            val cat = params["category"]?.toString() ?: "hospital"
            performNearbySearch(cat, mapManager, currentLatLng, coroutineScope, { isSearching = it }, { searchResults = it }, { searchStatusMessage = it })
        }

        ActionRegistry.registerNavigationListener("RECENTER_MAP") {
            coroutineScope.launch {
                currentLatLng?.let { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 17f)) }
            }
        }

        ActionRegistry.registerNavigationListener("STOP_NAVIGATION") {
            mapManager.navigationEngine.stopNavigation()
            indoorOutdoorEngine.stop()
            workspaceState = NavWorkspaceState.SEARCHING
            currentRoute = null
            selectedPlace = null
        }

        // Indoor voice action bindings
        ActionRegistry.indoorStatusProvider = {
            indoorOutdoorEngine.statusMessage.value
        }
        ActionRegistry.stairCountProvider = {
            val steps = indoorOutdoorEngine.stepCounter.totalWalkingSteps.value
            val stairs = indoorOutdoorEngine.stairDetector.stairMetrics.value
            "Walking steps: $steps. Stairs: estimated ${stairs.estimatedSteps} steps ${stairs.direction.name.lowercase()}."
        }
        ActionRegistry.floorStatusProvider = {
            val floor = indoorOutdoorEngine.floorEstimator.floorState.value.estimatedFloor
            val conf = (indoorOutdoorEngine.floorEstimator.floorState.value.confidence * 100).toInt()
            "Estimated floor $floor with $conf% confidence."
        }

        ActionRegistry.registerNavigationListener("GO_OUTSIDE") {
            indoorOutdoorEngine.startIndoorExitGuidance()
            mapManager.speak("Indoor exit guidance active. Follow instructions to ground floor.", true)
        }
        ActionRegistry.registerNavigationListener("INDOOR_STEP_COUNT") {
            val steps = indoorOutdoorEngine.stepCounter.totalWalkingSteps.value
            val stairs = indoorOutdoorEngine.stairDetector.stairMetrics.value
            mapManager.speak("Walking steps: $steps. Stairs: estimated ${stairs.estimatedSteps} steps.", true)
        }
        ActionRegistry.registerNavigationListener("WHICH_FLOOR") {
            val floor = indoorOutdoorEngine.floorEstimator.floorState.value.estimatedFloor
            mapManager.speak("Current estimated floor is $floor.", true)
        }
        ActionRegistry.registerNavigationListener("CONFIRM_FLOOR") {
            val floor = indoorOutdoorEngine.floorEstimator.floorState.value.estimatedFloor
            indoorOutdoorEngine.floorEstimator.confirmFloor(floor)
            mapManager.speak("Floor $floor confirmed. Continue toward exit.", true)
        }

        // Outdoor handoff callback
        indoorOutdoorEngine.setOnOutdoorHandoffListener {
            mapManager.speak("Building exit reached. Outdoor Google GPS navigation is now active.", true)
            HapticHelper.vibrateSuccess(context)
            if (workspaceState == NavWorkspaceState.PREVIEW && currentRoute != null && selectedPlace != null) {
                workspaceState = NavWorkspaceState.NAVIGATING
                mapManager.navigationEngine.startNavigation(currentRoute!!, selectedPlace!!)
            }
        }

        onDispose {
            ActionRegistry.unregisterNavigationListener("NAVIGATE")
            ActionRegistry.unregisterNavigationListener("SEARCH_NEARBY")
            ActionRegistry.unregisterNavigationListener("RECENTER_MAP")
            ActionRegistry.unregisterNavigationListener("STOP_NAVIGATION")
            ActionRegistry.unregisterNavigationListener("GO_OUTSIDE")
            ActionRegistry.unregisterNavigationListener("INDOOR_STEP_COUNT")
            ActionRegistry.unregisterNavigationListener("WHICH_FLOOR")
            ActionRegistry.unregisterNavigationListener("CONFIRM_FLOOR")
            ActionRegistry.indoorStatusProvider = null
            ActionRegistry.stairCountProvider = null
            ActionRegistry.floorStatusProvider = null
        }
    }

    // Autocomplete debounce job
    var autocompleteJob by remember { mutableStateOf<Job?>(null) }
    fun onQueryChanged(newQuery: String) {
        destinationQuery = newQuery
        autocompleteJob?.cancel()
        if (newQuery.trim().length >= 2 && workspaceState == NavWorkspaceState.SEARCHING) {
            autocompleteJob = coroutineScope.launch {
                delay(300) // 300ms debounce
                val predictions = mapManager.placesService.getAutocompletePredictions(
                    query = newQuery,
                    center = if (isLocationAvailable) currentLatLng else null
                )
                autocompletePredictions = predictions
            }
        } else {
            autocompletePredictions = emptyList()
        }
    }

    // Function to calculate real Google Routes API route
    fun calculateLiveRoute(destination: DetailedPlace, mode: String) {
        if (!isLocationAvailable) {
            mapManager.speak("Your current location is unavailable. Cannot calculate route.", true)
            return
        }
        isRouteCalculating = true
        coroutineScope.launch {
            val result = mapManager.routesClient.calculateRoute(
                origin = currentLatLng!!,
                destination = LatLng(destination.latitude, destination.longitude),
                travelMode = mode
            )
            isRouteCalculating = false
            result.fold(
                onSuccess = { route ->
                    currentRoute = route
                    workspaceState = NavWorkspaceState.PREVIEW
                    // Announce preview
                    val kmText = if (route.distanceMeters >= 1000) {
                        String.format(Locale.US, "%.1f kilometers", route.distanceMeters / 1000f)
                    } else {
                        "${route.distanceMeters} meters"
                    }
                    mapManager.speak("Route to ${destination.name} is $kmText and approximately ${route.durationText}.", true)
                    HapticHelper.vibrateSuccess(context)
                    // Focus camera to fit both origin (start node) and destination (end node) in full view
                    try {
                        val boundsBuilder = LatLngBounds.builder()
                        currentLatLng?.let { boundsBuilder.include(it) }
                        boundsBuilder.include(LatLng(destination.latitude, destination.longitude))
                        route.polylinePoints.forEach { boundsBuilder.include(it) }
                        val bounds = boundsBuilder.build()
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                    } catch (_: Exception) {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(destination.latitude, destination.longitude), 16f))
                    }
                },
                onFailure = { err ->
                    searchStatusMessage = "Route calculation failed: ${err.message ?: "Google Routes service unavailable."}"
                    mapManager.speak("Unable to calculate route to ${destination.name}.", true)
                    HapticHelper.vibrateWarning(context)
                }
            )
        }
    }

    // Auto-consume pending destination or query from NavigationDestinationHolder
    val pendingDestHolder by NavigationDestinationHolder.pendingDestination.collectAsState()
    val pendingQueryHolder by NavigationDestinationHolder.pendingQuery.collectAsState()

    LaunchedEffect(pendingDestHolder, pendingQueryHolder, isLocationAvailable) {
        val dest = pendingDestHolder
        val query = pendingQueryHolder
        if (dest != null && isLocationAvailable) {
            selectedPlace = dest
            destinationQuery = dest.name
            calculateLiveRoute(dest, selectedTravelMode)
            NavigationDestinationHolder.clear()
        } else if (!query.isNullOrBlank() && isLocationAvailable) {
            destinationQuery = query
            performSearch(
                query = query,
                mapManager = mapManager,
                currentLatLng = currentLatLng,
                scope = coroutineScope,
                setSearching = { isSearching = it },
                setResults = { searchResults = it },
                setStatus = { searchStatusMessage = it },
                onFirstResult = { place ->
                    selectedPlace = place
                    calculateLiveRoute(place, selectedTravelMode)
                }
            )
            NavigationDestinationHolder.clearQuery()
        }
    }

    // Mic launcher for voice search
    val destinationMicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startDestinationVoiceRecognition(context, onResult = { spoken ->
                destinationQuery = spoken
                performSearch(spoken, mapManager, currentLatLng, coroutineScope, { isSearching = it }, { searchResults = it }, { searchStatusMessage = it }, { place ->
                    selectedPlace = place
                    calculateLiveRoute(place, selectedTravelMode)
                })
            }, onStateChanged = { listening ->
                isListeningForDestination = listening
            })
        } else {
            Toast.makeText(context, "Microphone permission required for voice destination search", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle arrival state
    LaunchedEffect(navEngineState) {
        if (navEngineState is NavState.Arrived) {
            workspaceState = NavWorkspaceState.ARRIVED
            NearbyNavigationState.voiceNavState.value = VoiceNavigationState.NAVIGATION_COMPLETED
        }
    }

    // Safety Fusion Manager: combines navigation route guidance with real-time camera safety
    val safetyFusionManager = remember {
        SafetyFusionManager(context) { text, isUrgent ->
            mapManager.speak(text, isUrgent)
        }
    }

    DisposableEffect(indoorOutdoorEngine) {
        indoorOutdoorEngine.cameraEnvironmentEngine.onObstacleEventListener = { event ->
            safetyFusionManager.onObstacleDetected(event)
        }
        onDispose {
            indoorOutdoorEngine.cameraEnvironmentEngine.onObstacleEventListener = null
            safetyFusionManager.clear()
        }
    }

    // Observe Conversational Navigation State Machine
    val conversationalDest by NearbyNavigationState.selectedDestination.collectAsState()
    val conversationalMode by NearbyNavigationState.selectedTravelMode.collectAsState()
    val conversationalVoiceNavState by NearbyNavigationState.voiceNavState.collectAsState()

    LaunchedEffect(conversationalDest, conversationalMode, conversationalVoiceNavState) {
        val dest = conversationalDest
        val mode = conversationalMode
        if (dest != null && mode != null && (conversationalVoiceNavState == VoiceNavigationState.STARTING_NAVIGATION || conversationalVoiceNavState == VoiceNavigationState.NAVIGATING)) {
            val place = DetailedPlace(
                id = dest.placeId,
                name = dest.name,
                address = dest.address,
                latitude = dest.latitude,
                longitude = dest.longitude,
                category = "Conversational",
                distanceMeters = dest.distanceMeters
            )
            selectedPlace = place
            selectedTravelMode = mode.name
            calculateLiveRoute(place, mode.name)
        }
    }

    LaunchedEffect(currentRoute, conversationalVoiceNavState) {
        val route = currentRoute
        val place = selectedPlace
        if (route != null && place != null && (conversationalVoiceNavState == VoiceNavigationState.STARTING_NAVIGATION || conversationalVoiceNavState == VoiceNavigationState.NAVIGATING)) {
            workspaceState = NavWorkspaceState.NAVIGATING
            NearbyNavigationState.voiceNavState.value = VoiceNavigationState.NAVIGATING
            android.util.Log.i("NavigationScreen", "NAVIGATING")
            android.util.Log.i("DrishtiWakeWordService", "NAVIGATING")
            mapManager.navigationEngine.startNavigation(route, place)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC) // Design token Background #F8FAFC
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Status & Accessibility Header (Visible in Search & Preview)
            if (workspaceState != NavWorkspaceState.NAVIGATING) {
                Surface(
                    color = Color.White,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Language Indicator
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when (currentLanguageCode) {
                                    "hi" -> "Auto • हिन्दी"
                                    "mr" -> "Auto • मराठी"
                                    else -> "Auto • English"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Live GPS Status Indicator
                        Surface(
                            color = if (isLocationAvailable) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isLocationAvailable) Color(0xFF16A34A) else Color(0xFFDC2626))
                                )
                                Text(
                                    text = if (isLocationAvailable) stringResource(R.string.nav_gps_active) else stringResource(R.string.nav_gps_unavailable),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isLocationAvailable) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Center Map Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission,
                        mapType = mapType
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = true
                    )
                ) {
                    // Real Current Location Marker (if MyLocation layer is not already showing)
                    if (isLocationAvailable) {
                        Marker(
                            state = rememberMarkerState(position = currentLatLng ?: com.google.android.gms.maps.model.LatLng(0.0,0.0)),
                            title = "Your Location",
                            snippet = "GPS Live Tracking",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }

                    // Real Destination Marker
                    selectedPlace?.let { place ->
                        val destLatLng = LatLng(place.latitude, place.longitude)
                        Marker(
                            state = rememberMarkerState(position = destLatLng),
                            title = place.name,
                            snippet = "${place.distanceMeters}m • ${place.address}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }

                    // Real Search Result Markers
                    if (workspaceState == NavWorkspaceState.SEARCHING) {
                        searchResults.forEach { place ->
                            Marker(
                                state = rememberMarkerState(position = LatLng(place.latitude, place.longitude)),
                                title = place.name,
                                snippet = "${place.distanceMeters}m away",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                                onClick = {
                                    selectedPlace = place
                                    calculateLiveRoute(place, selectedTravelMode)
                                    true
                                }
                            )
                        }
                    }

                    // Real Route Polyline computed from Google Routes API
                    currentRoute?.polylinePoints?.let { points ->
                        if (points.isNotEmpty()) {
                            Polyline(
                                points = points,
                                color = Color(0xFF2563EB), // Primary #2563EB
                                width = 14f
                            )
                        }
                    }
                }

                // Top Maneuver Overlay during ACTIVE navigation (Google Maps Style Green Banner)
                if (workspaceState == NavWorkspaceState.NAVIGATING && navEngineState is NavState.Active) {
                    val active = navEngineState as NavState.Active
                    val conciseInstruction = cleanAndSimplifyInstruction(active.currentInstruction)
                    val nextInstructionClean = active.nextInstruction?.let { cleanAndSimplifyInstruction(it) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .align(Alignment.TopCenter),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF046A55), // Google Maps Primary Dark Emerald Green
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Prominent Google Maps maneuver arrow
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = getManeuverIcon(active.upcomingManeuver),
                                            contentDescription = "Upcoming maneuver: ${active.upcomingManeuver}",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.nav_maneuver_distance_format, active.distanceToNextManeuver),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF6EE7B7) // High contrast mint green
                                        )
                                        Surface(
                                            color = if (active.isFacingRightDirection) Color(0xFF064E3B) else Color(0xFF78350F),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${active.relativeClockDirection} • ${active.headingInstruction}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (active.isFacingRightDirection) Color(0xFF86EFAC) else Color(0xFFFDE68A),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = conciseInstruction,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        // Google Maps Secondary "Then [next maneuver]" chip
                        if (!nextInstructionClean.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFF034D3E),
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 4.dp,
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.nav_then_format, ""),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = getManeuverIcon(active.nextManeuver ?: "STRAIGHT"),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = nextInstructionClean,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFFE2E8F0),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Search Bar & Voice Input (When in SEARCHING state)
                if (workspaceState == NavWorkspaceState.SEARCHING) {
                    val searchDesc = stringResource(R.string.nav_search_desc)
                    val voiceDesc = stringResource(R.string.nav_voice_input_desc)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = destinationQuery,
                                        onValueChange = { onQueryChanged(it) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { contentDescription = searchDesc },
                                        placeholder = { Text(stringResource(R.string.nav_search_placeholder)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = {
                                            autocompletePredictions = emptyList()
                                            performSearch(destinationQuery, mapManager, currentLatLng, coroutineScope, { isSearching = it }, { searchResults = it }, { searchStatusMessage = it }, { place ->
                                                selectedPlace = place
                                                calculateLiveRoute(place, selectedTravelMode)
                                            })
                                        }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )

                                    if (destinationQuery.isNotBlank()) {
                                        IconButton(onClick = {
                                            destinationQuery = ""
                                            autocompletePredictions = emptyList()
                                        }) {
                                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.nav_clear_search), tint = Color(0xFF475569))
                                        }
                                    }

                                    // Voice Input Button
                                    IconButton(
                                        onClick = {
                                            HapticHelper.vibrateInfo(context)
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                startDestinationVoiceRecognition(context, onResult = { spoken ->
                                                    destinationQuery = spoken
                                                    performSearch(spoken, mapManager, currentLatLng, coroutineScope, { isSearching = it }, { searchResults = it }, { searchStatusMessage = it }, { place ->
                                                        selectedPlace = place
                                                        calculateLiveRoute(place, selectedTravelMode)
                                                    })
                                                }, onStateChanged = { listening ->
                                                    isListeningForDestination = listening
                                                })
                                            } else {
                                                destinationMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDBEAFE))
                                            .semantics { contentDescription = voiceDesc }
                                    ) {
                                        Icon(
                                            imageVector = if (isListeningForDestination) Icons.Rounded.Hearing else Icons.Rounded.Mic,
                                            contentDescription = stringResource(R.string.nav_voice_input),
                                            tint = Color(0xFF2563EB)
                                        )
                                    }
                                }

                                // Autocomplete Prediction List
                                if (autocompletePredictions.isNotEmpty()) {
                                    HorizontalDivider()
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 240.dp)
                                    ) {
                                        items(autocompletePredictions) { prediction ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        autocompletePredictions = emptyList()
                                                        destinationQuery = prediction.primaryText
                                                        coroutineScope.launch {
                                                            isSearching = true
                                                            val place = mapManager.placesService.fetchPlaceDetails(prediction.placeId, currentLatLng)
                                                            isSearching = false
                                                            if (place != null) {
                                                                selectedPlace = place
                                                                calculateLiveRoute(place, selectedTravelMode)
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(Icons.Rounded.Place, contentDescription = null, tint = Color(0xFF2563EB))
                                                Column {
                                                    Text(
                                                        text = prediction.primaryText,
                                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    if (prediction.secondaryText.isNotBlank()) {
                                                        Text(
                                                            text = prediction.secondaryText,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF475569)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Category quick filter chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val categories = listOf(
                                "Hospitals" to R.string.cat_hospitals,
                                "Pharmacies" to R.string.cat_pharmacies,
                                "ATMs" to R.string.cat_atms,
                                "Restaurants" to R.string.cat_restaurants,
                                "Petrol pumps" to R.string.cat_petrol_pumps,
                                "Police station" to R.string.cat_police_station
                            )
                            items(categories) { (catQuery, catResId) ->
                                SuggestionChip(
                                    onClick = {
                                        performNearbySearch(catQuery, mapManager, currentLatLng, coroutineScope, { isSearching = it }, { searchResults = it }, { searchStatusMessage = it })
                                    },
                                    label = { Text(stringResource(catResId)) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = Color.White.copy(alpha = 0.95f),
                                        labelColor = Color(0xFF0F172A)
                                    )
                                )
                            }
                        }

                        // Search status banner if any
                        searchStatusMessage?.let { status ->
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFFDC2626))
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { searchStatusMessage = null }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.camera_hud_close), tint = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Action Controls: Compass, Voice Mute, Search on Way, Map Type, Recenter
                val mapTypeDesc = stringResource(R.string.nav_map_type_desc)
                val mapTypeLabel = stringResource(R.string.nav_map_type)
                val recenterDesc = stringResource(R.string.nav_recenter_desc)
                val myLocationLabel = stringResource(R.string.nav_my_location)
                val compassDesc = if (isHeadUpMode) stringResource(R.string.nav_compass_head_up) else stringResource(R.string.nav_compass_north_up)
                val voiceGuidanceLabel = stringResource(R.string.nav_voice_guidance)
                val searchOnWayLabel = stringResource(R.string.nav_search_on_way)

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Maps Style Compass Button (Active during Navigation)
                    if (workspaceState == NavWorkspaceState.NAVIGATING) {
                        SmallFloatingActionButton(
                            onClick = {
                                isHeadUpMode = !isHeadUpMode
                                HapticHelper.vibrateInfo(context)
                                coroutineScope.launch {
                                    val loc = currentLatLng
                                    if (loc != null) {
                                        val bearing = if (isHeadUpMode) {
                                            (navEngineState as? NavState.Active)?.currentHeading ?: 0f
                                        } else 0f
                                        val tilt = if (isHeadUpMode) 45f else 0f
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.Builder()
                                                    .target(loc)
                                                    .zoom(18f)
                                                    .tilt(tilt)
                                                    .bearing(bearing)
                                                    .build()
                                            ),
                                            500
                                        )
                                    }
                                }
                            },
                            containerColor = Color.White,
                            contentColor = if (isHeadUpMode) Color(0xFFDC2626) else Color(0xFF475569),
                            modifier = Modifier.semantics { contentDescription = compassDesc }
                        ) {
                            Icon(
                                Icons.Rounded.Explore,
                                contentDescription = compassDesc,
                                tint = if (isHeadUpMode) Color(0xFFDC2626) else Color(0xFF475569)
                            )
                        }

                        // Search along route icon (Google Maps magnifying glass)
                        SmallFloatingActionButton(
                            onClick = {
                                isAlongRouteSearchOpen = !isAlongRouteSearchOpen
                                if (isAlongRouteSearchOpen) {
                                    coroutineScope.launch {
                                        currentRoute?.let { r ->
                                            val alongPlaces = mapManager.placesService.searchAlongRoute(r.polylinePoints, "pharmacy")
                                            searchResults = alongPlaces
                                        }
                                    }
                                }
                                HapticHelper.vibrateInfo(context)
                            },
                            containerColor = Color.White,
                            contentColor = Color(0xFF0F172A),
                            modifier = Modifier.semantics { contentDescription = searchOnWayLabel }
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = searchOnWayLabel)
                        }

                        // Voice Guidance Mute / Unmute Button
                        SmallFloatingActionButton(
                            onClick = {
                                isVoiceMuted = !isVoiceMuted
                                HapticHelper.vibrateInfo(context)
                                if (isVoiceMuted) {
                                    mapManager.speak("Voice guidance muted.", true)
                                } else {
                                    mapManager.speak("Voice guidance unmuted.", true)
                                }
                            },
                            containerColor = Color.White,
                            contentColor = if (isVoiceMuted) Color(0xFFDC2626) else Color(0xFF0F172A),
                            modifier = Modifier.semantics { contentDescription = voiceGuidanceLabel }
                        ) {
                            Icon(
                                imageVector = if (isVoiceMuted) Icons.Rounded.NotificationsOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = voiceGuidanceLabel,
                                tint = if (isVoiceMuted) Color(0xFFDC2626) else Color(0xFF0F172A)
                            )
                        }
                    }

                    // Map Type Toggle
                    SmallFloatingActionButton(
                        onClick = {
                            mapType = if (mapType == MapType.NORMAL) MapType.SATELLITE else MapType.NORMAL
                            HapticHelper.vibrateInfo(context)
                        },
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A),
                        modifier = Modifier.semantics { contentDescription = mapTypeDesc }
                    ) {
                        Icon(Icons.Rounded.Layers, contentDescription = mapTypeLabel)
                    }

                    // Recenter on GPS location
                    FloatingActionButton(
                        onClick = {
                            HapticHelper.vibrateInfo(context)
                            coroutineScope.launch {
                                val loc = mapManager.getCurrentLocation()
                                if (loc != null) {
                                    currentLatLng = LatLng(loc.latitude, loc.longitude)
                                    isLocationAvailable = true
                                    if (workspaceState == NavWorkspaceState.NAVIGATING) {
                                        val bearing = if (isHeadUpMode) {
                                            (navEngineState as? NavState.Active)?.currentHeading ?: 0f
                                        } else 0f
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.Builder()
                                                    .target(currentLatLng!!)
                                                    .zoom(18f)
                                                    .tilt(if (isHeadUpMode) 45f else 0f)
                                                    .bearing(bearing)
                                                    .build()
                                            ),
                                            600
                                        )
                                    } else {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 17f))
                                    }
                                    mapManager.speak("Centered on current GPS location.", false)
                                } else {
                                    mapManager.speak("Your current location is unavailable.", true)
                                }
                            }
                        },
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                        modifier = Modifier.semantics { contentDescription = recenterDesc }
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = myLocationLabel)
                    }
                }
            }

            // 3. Accessible Bottom Sheet & Navigation Panel
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Indoor-to-Outdoor Accessible State Card
                    if (indoorEnv != NavigationEnvironment.OUTDOOR) {
                        IndoorOutdoorCard(
                            engine = indoorOutdoorEngine,
                            onOutdoorHandoffClicked = {
                                if (workspaceState == NavWorkspaceState.PREVIEW && currentRoute != null && selectedPlace != null) {
                                    workspaceState = NavWorkspaceState.NAVIGATING
                                    mapManager.navigationEngine.startNavigation(currentRoute!!, selectedPlace!!)
                                }
                            },
                            onSpeakRequested = { msg ->
                                mapManager.speak(msg, true)
                            }
                        )
                    }

                    when (workspaceState) {
                        NavWorkspaceState.SEARCHING -> {
                            if (searchResults.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.nav_nearby_places_format, searchResults.size),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF0F172A)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(searchResults) { place ->
                                        val placeAwayText = stringResource(R.string.nav_places_away_format, place.distanceMeters)
                                        Surface(
                                            color = Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(16.dp),
                                            tonalElevation = 2.dp,
                                            modifier = Modifier
                                                .clickable {
                                                    selectedPlace = place
                                                    calculateLiveRoute(place, selectedTravelMode)
                                                }
                                                .semantics { contentDescription = "${place.name}, $placeAwayText" }
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = place.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "${place.distanceMeters} m • ${place.category}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF475569)
                                                )
                                                if (place.rating != null) {
                                                    Text(
                                                        text = "★ ${place.rating}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFFF59E0B)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.nav_accessible_title),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF2563EB)
                                )
                                Text(
                                    text = stringResource(R.string.nav_accessible_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        NavWorkspaceState.PREVIEW -> {
                            selectedPlace?.let { place ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.name,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = place.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    IconButton(onClick = {
                                        workspaceState = NavWorkspaceState.SEARCHING
                                        currentRoute = null
                                        selectedPlace = null
                                    }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Close preview")
                                    }
                                }

                                // Travel Mode Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val modes = listOf(
                                        Triple("WALK", R.string.mode_walk, Icons.AutoMirrored.Rounded.DirectionsWalk),
                                        Triple("DRIVE", R.string.mode_drive, Icons.Rounded.DirectionsCar),
                                        Triple("TWO_WHEELER", R.string.mode_two_wheeler, Icons.Rounded.TwoWheeler)
                                    )
                                    modes.forEach { (mode, labelRes, icon) ->
                                        FilterChip(
                                            selected = selectedTravelMode == mode,
                                            onClick = {
                                                selectedTravelMode = mode
                                                calculateLiveRoute(place, mode)
                                            },
                                            label = { Text(stringResource(labelRes)) },
                                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF2563EB),
                                                selectedLabelColor = Color.White,
                                                selectedLeadingIconColor = Color.White
                                            )
                                        )
                                    }
                                }

                                currentRoute?.let { route ->
                                    Surface(
                                        color = Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = route.durationText,
                                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF2563EB)
                                                )
                                                Text(
                                                    text = stringResource(R.string.nav_route_metrics_format, route.distanceMeters, route.steps.size),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF475569)
                                                )
                                            }
                                            // Save Place Button
                                            IconButton(
                                                onClick = {
                                                    if (!SessionTokenProvider.hasSession()) {
                                                        Toast.makeText(context, "Sign in to save favorite places", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        coroutineScope.launch {
                                                            val saveRes = supabaseRepo.savePlace(
                                                                SavedPlaceModel(
                                                                    name = place.name,
                                                                    latitude = place.latitude,
                                                                    longitude = place.longitude,
                                                                    address = place.address
                                                                )
                                                            )
                                                            if (saveRes is com.example.drishti.supabase.DataResult.Success) {
                                                                Toast.makeText(context, "Place saved to your Drishti account", Toast.LENGTH_SHORT).show()
                                                                mapManager.speak("Place saved to your account.", false)
                                                            }
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Rounded.BookmarkBorder, contentDescription = stringResource(R.string.nav_save_place), tint = Color(0xFF2563EB))
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            workspaceState = NavWorkspaceState.NAVIGATING
                                            mapManager.navigationEngine.startNavigation(route, place)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .semantics { contentDescription = "Start navigation to ${place.name}" },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                    ) {
                                        Icon(Icons.Rounded.Navigation, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.nav_start_navigation),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        NavWorkspaceState.NAVIGATING -> {
                            if (navEngineState is NavState.Active) {
                                val active = navEngineState as NavState.Active
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Google Maps Style Bottom Navigation Bar (Close Button, ETA, Distance, Arrival Time, Voice Button)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Quick Exit / Stop button (Google Maps Circle "X")
                                        val stopDesc = stringResource(R.string.nav_stop)
                                        IconButton(
                                            onClick = {
                                                HapticHelper.vibrateWarning(context)
                                                mapManager.navigationEngine.stopNavigation()
                                                workspaceState = NavWorkspaceState.SEARCHING
                                                currentRoute = null
                                                selectedPlace = null
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF1F5F9))
                                                .semantics { contentDescription = stopDesc }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stopDesc,
                                                tint = Color(0xFF0F172A),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // Central Trip Metrics: Big ETA, travel mode icon, distance and arrival time
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${active.etaMinutesRemaining} min",
                                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF047857) // Deep green ETA
                                                )
                                                Icon(
                                                    imageVector = when (selectedTravelMode) {
                                                        "DRIVE" -> Icons.Rounded.DirectionsCar
                                                        "TWO_WHEELER" -> Icons.Rounded.TwoWheeler
                                                        else -> Icons.AutoMirrored.Rounded.DirectionsWalk
                                                    },
                                                    contentDescription = null,
                                                    tint = Color(0xFF047857),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Text(
                                                text = "${active.totalDistanceRemaining} m • to ${active.destination.name}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = Color(0xFF475569),
                                                maxLines = 1
                                            )
                                        }

                                        // Repeat Voice guidance button (Accessible audio guidance trigger)
                                        val repeatHeadingDesc = stringResource(R.string.nav_repeat_heading_desc)
                                        val repeatInstructionDesc = stringResource(R.string.repeat_instruction)
                                        IconButton(
                                            onClick = {
                                                val guidance = "${active.currentInstruction}. Your next waypoint is at your ${active.relativeClockDirection}."
                                                mapManager.speak(guidance, true)
                                                if (active.isFacingRightDirection) {
                                                    HapticHelper.vibrateStraight(context)
                                                } else {
                                                    HapticHelper.vibrateOffHeading(context)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEFF6FF))
                                                .semantics { contentDescription = repeatHeadingDesc }
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                                contentDescription = repeatInstructionDesc,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Pedestrian Compass & Heading Alignment Card
                                    Surface(
                                        color = if (active.isFacingRightDirection) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (active.isFacingRightDirection) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Explore,
                                                    contentDescription = "Compass heading",
                                                    tint = if (active.isFacingRightDirection) Color(0xFF16A34A) else Color(0xFFD97706),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = stringResource(R.string.nav_heading_format, active.currentHeading.toInt(), active.relativeClockDirection),
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (active.isFacingRightDirection) Color(0xFF16A34A) else Color(0xFFD97706)
                                                    )
                                                    Text(
                                                        text = if (active.isFacingRightDirection) stringResource(R.string.nav_aligned_proceed) else stringResource(R.string.nav_turn_towards_format, active.relativeClockDirection, active.headingInstruction),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF475569)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        NavWorkspaceState.ARRIVED -> {
                            val destinationName = selectedPlace?.name ?: stringResource(R.string.destination_reached)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.nav_arrived_title),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.nav_arrived_desc_format, destinationName),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF475569)
                                )

                                Button(
                                    onClick = {
                                        workspaceState = NavWorkspaceState.SEARCHING
                                        selectedPlace = null
                                        currentRoute = null
                                        destinationQuery = ""
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Text(stringResource(R.string.nav_new_search), color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helper Search Functions
// ---------------------------------------------------------------------------

private fun performSearch(
    query: String,
    mapManager: MapManager,
    currentLatLng: LatLng?,
    scope: kotlinx.coroutines.CoroutineScope,
    setSearching: (Boolean) -> Unit,
    setResults: (List<DetailedPlace>) -> Unit,
    setStatus: (String?) -> Unit,
    onFirstResult: (DetailedPlace) -> Unit
) {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return
    setSearching(true)
    setStatus("Searching Google Places for \"$trimmed\"...")

    scope.launch {
        val results = mapManager.placesService.searchByText(trimmed, center = currentLatLng, radiusMeters = 5000.0)
        setSearching(false)
        if (results.isNotEmpty()) {
            setResults(results)
            setStatus(null)
            onFirstResult(results.first())
        } else {
            setResults(emptyList())
            setStatus("No places found for \"$trimmed\".")
            mapManager.speak("No matching places were found.", true)
        }
    }
}

private fun performNearbySearch(
    category: String,
    mapManager: MapManager,
    currentLatLng: LatLng?,
    scope: kotlinx.coroutines.CoroutineScope,
    setSearching: (Boolean) -> Unit,
    setResults: (List<DetailedPlace>) -> Unit,
    setStatus: (String?) -> Unit
) {
    if (currentLatLng == null) {
        android.util.Log.w("NavigationScreen", "[NearbySearch] Rejected: null location. Waiting for GPS fix.")
        setStatus("Waiting for current location...")
        return
    }
    android.util.Log.i("NavigationScreen", "[NearbySearch]\nlatitude=${currentLatLng.latitude}\nlongitude=${currentLatLng.longitude}\naccuracy=unknown\nlocationAgeMs=unknown")
    
    setSearching(true)
    setStatus("Searching for nearby $category...")

    scope.launch {
        val results = mapManager.placesService.searchNearby(center = currentLatLng, category = category, radiusMeters = 2500.0)
        setSearching(false)
        if (results.isNotEmpty()) {
            setResults(results)
            setStatus(null)
            mapManager.speak("Found ${results.size} $category nearby.", false)
        } else {
            setResults(emptyList())
            setStatus("No matching $category found nearby.")
            mapManager.speak("No matching places were found nearby.", true)
        }
    }
}

private fun getManeuverIcon(maneuver: String): ImageVector {
    return when (maneuver.uppercase()) {
        "TURN_LEFT", "TURN_SHARP_LEFT", "TURN_SLIGHT_LEFT" -> Icons.Rounded.TurnLeft
        "TURN_RIGHT", "TURN_SHARP_RIGHT", "TURN_SLIGHT_RIGHT" -> Icons.Rounded.TurnRight
        "UTURN_LEFT", "UTURN_RIGHT" -> Icons.Rounded.TurnLeft
        "ROUNDABOUT_ENTER", "ROUNDABOUT_EXIT" -> Icons.Rounded.Refresh
        else -> Icons.Rounded.Straight
    }
}

private fun cleanAndSimplifyInstruction(raw: String): String {
    var text = raw.replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .trim()

    // Strip trailing verbose notes like "Pass by the pharmacy..."
    val passByIdx = text.indexOf("Pass by", ignoreCase = true)
    if (passByIdx > 0) {
        text = text.substring(0, passByIdx).trim()
    }

    // Clean slash repetitions in long combined street names e.g. "Ambad Police Station Rd/KVN Naik Rd/Morwadi Rd..."
    // Keep at most 2 main road designations so it fits neatly like Google Maps
    if (text.contains("/")) {
        val parts = text.split("/").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size > 2) {
            text = "${parts[0]} / ${parts[1]}"
        }
    }

    // Strip trailing period if any
    if (text.endsWith(".")) {
        text = text.substring(0, text.length - 1).trim()
    }

    return text.ifBlank { raw }
}

private fun startDestinationVoiceRecognition(
    context: Context,
    onResult: (String) -> Unit,
    onStateChanged: (Boolean) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
        return
    }

    CoroutineScope(Dispatchers.Main).launch {
        val acquired = MicrophoneCoordinator.acquireCommandForScreen(context)
        if (!acquired) {
            Toast.makeText(context, "Microphone is busy. Please try again.", Toast.LENGTH_SHORT).show()
            return@launch
        }

        val voiceRecognizer = VoiceRecognizerManager(context)
        onStateChanged(true)

        voiceRecognizer.startListening(
            language = Locale.getDefault().toString(),
            onReady = { onStateChanged(true) },
            onResult = { result ->
                onStateChanged(false)
                MicrophoneCoordinator.releaseCommandForScreen(context)
                if (result.isNotBlank()) {
                    onResult(result)
                }
            },
            onError = { error ->
                onStateChanged(false)
                MicrophoneCoordinator.releaseCommandForScreen(context)
                Toast.makeText(context, "Voice search: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

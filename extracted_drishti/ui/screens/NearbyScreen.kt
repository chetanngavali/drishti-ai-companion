package com.example.drishti.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.drishti.R
import com.example.drishti.navigation.MapManager
import com.example.drishti.navigation.PlaceItem
import com.example.drishti.util.HapticHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen() {
    val context = LocalContext.current
    val mapManager = remember { MapManager(context) }

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
            mapManager.speak("Location permission granted. Discovering nearby essential places.")
        } else {
            mapManager.speak("Location permission needed to find nearby places.", true)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            mapManager.speak("Nearby Explorer active. Select category to discover hospitals, ATMs, transit, and more.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapManager.shutdown()
        }
    }

    var selectedCategory by remember {
        mutableStateOf(
            com.example.drishti.navigation.NearbyNavigationState.canonicalVoiceResults.value?.let {
                com.example.drishti.navigation.NearbyNavigationState.mapToScreenCategory(it.category)
            } ?: com.example.drishti.navigation.NearbyNavigationState.requestedCategory.value?.let {
                com.example.drishti.navigation.NearbyNavigationState.mapToScreenCategory(it)
            } ?: "Hospitals"
        )
    }

    val requestedCat by com.example.drishti.navigation.NearbyNavigationState.requestedCategory.collectAsState()
    val canonicalVoiceData by com.example.drishti.navigation.NearbyNavigationState.canonicalVoiceResults.collectAsState()

    var places by remember { mutableStateOf<List<PlaceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(requestedCat, canonicalVoiceData) {
        val voiceData = canonicalVoiceData
        val cat = requestedCat
        if (voiceData != null) {
            val targetCategory = com.example.drishti.navigation.NearbyNavigationState.mapToScreenCategory(voiceData.category)
            selectedCategory = targetCategory

            // VOICE_RESULT_PRESENT: render supplied canonical results directly without a second Places search
            val uiPlaceItems = voiceData.places.map { it.toPlaceItem() }
            places = uiPlaceItems
            isLoading = false

            val placeIds = uiPlaceItems.joinToString(",") { it.id }
            val names = uiPlaceItems.joinToString(", ") { it.name }
            val distances = uiPlaceItems.joinToString(", ") { "${it.distanceMeters}m" }
            Log.i("NearbyScreen", "VOICE_RESULT_PRESENT: requestId=${voiceData.requestId} count=${uiPlaceItems.size}")
            Log.i("NearbyScreen", "NEARBY_UI_RESULT_DATA requestId=${voiceData.requestId} placeIds=[$placeIds] names=[$names] distances=[$distances]")

            val voiceIds = voiceData.places.map { it.placeId }
            val uiIds = uiPlaceItems.map { it.id }
            val samePlaceIds = voiceIds == uiIds
            val sameOrder = voiceIds == uiIds
            val sameDistances = voiceData.places.map { it.distanceMeters } == uiPlaceItems.map { it.distanceMeters }
            val sameResultSet = samePlaceIds && sameOrder && sameDistances
            Log.i("NearbyScreen", "RESULT_CONSISTENCY_CHECK requestId=${voiceData.requestId} sameResultSet=$sameResultSet samePlaceIds=$samePlaceIds sameOrder=$sameOrder sameDistances=$sameDistances")

            com.example.drishti.navigation.NearbyNavigationState.requestedCategory.value = null
        } else if (cat != null) {
            selectedCategory = com.example.drishti.navigation.NearbyNavigationState.mapToScreenCategory(cat)
            com.example.drishti.navigation.NearbyNavigationState.requestedCategory.value = null
        }
    }

    val categories = listOf(
        "Hospitals" to R.string.cat_hospitals,
        "Pharmacies" to R.string.cat_pharmacies,
        "ATMs" to R.string.cat_atms,
        "Railway stations" to R.string.cat_railway_stations,
        "Bus stops" to R.string.cat_bus_stops,
        "Restaurants" to R.string.cat_restaurants
    )

    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLng by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val loc = mapManager.getCurrentLocation()
            if (loc != null) {
                currentLat = loc.latitude
                currentLng = loc.longitude
            }
        }
    }

    LaunchedEffect(selectedCategory, currentLat, currentLng) {
        val voiceData = com.example.drishti.navigation.NearbyNavigationState.canonicalVoiceResults.value
        if (voiceData != null) {
            Log.i("NearbyScreen", "NORMAL_UI_SEARCH_SKIPPED: canonical voice results already present for ${voiceData.category} requestId=${voiceData.requestId}")
            return@LaunchedEffect
        }
        
        val lat = currentLat
        val lng = currentLng
        if (lat == null || lng == null) {
            return@LaunchedEffect
        }

        Log.i("NearbyScreen", "NORMAL_UI_SEARCH category=$selectedCategory")
        isLoading = true
        val results = mapManager.searchPlace(selectedCategory, lat, lng)
        places = results
        isLoading = false
        if (results.isNotEmpty()) {
            mapManager.announceCategory(selectedCategory, results.size)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.nearby_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.nearby_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (categoryKey, categoryRes) ->
                    FilterChip(
                        selected = selectedCategory == categoryKey,
                        onClick = {
                            com.example.drishti.navigation.NearbyNavigationState.clearVoiceResults()
                            selectedCategory = categoryKey
                            HapticHelper.vibrateInfo(context)
                        },
                        label = { Text(stringResource(categoryRes)) },
                        leadingIcon = {
                            if (selectedCategory == categoryKey) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                            }
                        }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (places.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.nearby_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Use the Navigation screen to search for specific addresses or landmarks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Places List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(places) { place ->
                        PlaceCard(place = place, onSelect = {
                            HapticHelper.vibrateSuccess(context)
                            val announcement = "${place.name}, is ${place.distanceMeters} meters away at ${place.address}."
                            mapManager.speak(announcement, true)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceCard(place: PlaceItem, onSelect: () -> Unit) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = place.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = stringResource(R.string.nav_places_away_format, place.distanceMeters),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onSelect,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VolumeUp,
                    contentDescription = "Announce details for ${place.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

package com.example.drishti.homemapping.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.drishti.homemapping.data.HomeMapRepository
import com.example.drishti.homemapping.model.*
import com.example.drishti.homemapping.scanner.HomeSpatialScanner
import com.example.drishti.homemapping.scanner.MapChangeDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "HomeMappingViewModel"

class HomeMappingViewModel @JvmOverloads constructor(
    application: Application,
    customRepository: HomeMapRepository? = null
) : AndroidViewModel(application) {

    val repository = customRepository ?: HomeMapRepository(application)
    val scanner = HomeSpatialScanner(application)
    val changeDetector = MapChangeDetector()

    private val _uiState = MutableStateFlow(HomeMappingUiState())
    val uiState: StateFlow<HomeMappingUiState> = _uiState.asStateFlow()

    init {
        loadSavedMaps()
        checkIncompleteScan()
    }

    fun loadSavedMaps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val maps = repository.listLocalHomeMaps()
            val active = maps.firstOrNull() ?: _uiState.value.activeMap
            _uiState.value = _uiState.value.copy(
                savedMaps = maps,
                activeMap = active,
                isLoading = false
            )
            Log.i(TAG, "SAVED_MAPS_LOADED count=${maps.size}")
        }
    }

    fun checkIncompleteScan() {
        val incomplete = repository.loadIncompleteScan()
        _uiState.value = _uiState.value.copy(hasIncompleteScan = incomplete != null)
    }

    fun navigateToSubScreen(route: HomeMappingScreenRoute) {
        _uiState.value = _uiState.value.copy(currentSubScreen = route)
    }

    fun selectMap(map: HomeMap) {
        _uiState.value = _uiState.value.copy(
            activeMap = map,
            selectedRoomId = map.floors.firstOrNull()?.rooms?.firstOrNull()?.roomId,
            selectedObjectId = null
        )
    }

    fun toggle3DViewMode() {
        _uiState.value = _uiState.value.copy(is3DViewMode = !_uiState.value.is3DViewMode)
    }

    fun toggleObjectLabels() {
        _uiState.value = _uiState.value.copy(showObjectLabels = !_uiState.value.showObjectLabels)
    }

    fun toggleRoomBoundaries() {
        _uiState.value = _uiState.value.copy(showRoomBoundaries = !_uiState.value.showRoomBoundaries)
    }

    fun selectRoom(roomId: String?) {
        _uiState.value = _uiState.value.copy(selectedRoomId = roomId)
    }

    fun selectObject(objectId: String?) {
        _uiState.value = _uiState.value.copy(selectedObjectId = objectId)
    }

    fun startNewScan(roomName: String = "Living Room") {
        scanner.resetScanner()
        scanner.startRoomScan(roomName)
        _uiState.value = _uiState.value.copy(currentSubScreen = HomeMappingScreenRoute.SCAN)
    }

    fun continueIncompleteScan() {
        val incomplete = repository.loadIncompleteScan()
        if (incomplete != null) {
            scanner.resumeIncompleteScan(incomplete)
            _uiState.value = _uiState.value.copy(
                currentSubScreen = HomeMappingScreenRoute.SCAN,
                hasIncompleteScan = false
            )
            Log.i(TAG, "CONTINUE_INCOMPLETE_SCAN room=${incomplete.roomName}")
        }
    }

    fun pauseAndSaveIncompleteScan(homeName: String, currentRoomName: String) {
        val vertices = scanner.currentRoomPoints.value
        val scannedRooms = scanner.scannedRooms.value
        val state = IncompleteScanState(
            homeName = homeName,
            roomName = currentRoomName,
            vertices = vertices,
            scannedRooms = scannedRooms
        )
        repository.saveIncompleteScan(state)
        scanner.pauseScan()
        _uiState.value = _uiState.value.copy(hasIncompleteScan = true)
    }

    fun processScanCameraFrame(bitmap: Bitmap, roomId: String) {
        scanner.processCameraFrame(bitmap, roomId)
    }

    fun addScanVertex(x: Float, y: Float, z: Float) {
        scanner.addVertexPoint(Point3D(x, y, z))
    }

    fun undoLastVertex() {
        scanner.removeLastVertex()
    }

    fun calibrateScale(refMeters: Float, pixelHeight: Float) {
        scanner.setScaleCalibration(refMeters, pixelHeight)
    }

    fun setMappingMode(mode: MappingMode) {
        scanner.setMappingMode(mode)
    }

    fun addGuidedRoom(roomName: String, length: Float, width: Float) {
        scanner.addGuidedRectangularRoom(roomName, length, width)
    }

    fun finishRoomScanAndSave(roomName: String, homeName: String = "My Home", passphrase: CharArray? = null) {
        viewModelScope.launch {
            try {
                if (scanner.currentRoomPoints.value.size >= 3) {
                    scanner.finishRoomScan(roomName)
                }
                val generatedMap = scanner.generateCompletedHomeMap(_uiState.value.currentUserId, homeName)
                _uiState.value = _uiState.value.copy(activeMap = generatedMap, isLoading = true)

                val saveResult = repository.saveHomeMap(generatedMap, passphrase)
                saveResult.onSuccess { savedMap ->
                    repository.clearIncompleteScan()
                    _uiState.value = _uiState.value.copy(
                        activeMap = savedMap,
                        hasIncompleteScan = false,
                        isLoading = false,
                        successMessage = "Home map saved and encrypted securely!"
                    )
                    loadSavedMaps()
                    Log.i(TAG, "SCAN_FINISHED_AND_MAP_SAVED mapId=${savedMap.mapId}")
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to save map: ${err.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Scan validation error: ${e.message}"
                )
            }
        }
    }

    fun submitMapForAdminReview(mapId: String) {
        viewModelScope.launch {
            val map = _uiState.value.savedMaps.firstOrNull { it.mapId == mapId } ?: _uiState.value.activeMap
            if (map != null) {
                val updatedMap = map.copy(reviewStatus = AdminReviewStatus.REVIEW_PENDING)
                repository.saveHomeMap(updatedMap)
                loadSavedMaps()
                Log.i(TAG, "MAP_SUBMITTED_FOR_ADMIN_REVIEW mapId=$mapId")
            }
        }
    }

    fun adminApproveMap(mapId: String, reason: String? = null) {
        viewModelScope.launch {
            val map = _uiState.value.savedMaps.firstOrNull { it.mapId == mapId } ?: _uiState.value.activeMap
            if (map != null) {
                val updatedMap = map.copy(reviewStatus = AdminReviewStatus.APPROVED)
                repository.saveHomeMap(updatedMap)
                val event = AdminReviewEvent(
                    mapId = mapId,
                    adminUserId = _uiState.value.currentUserId,
                    action = "APPROVED",
                    notes = reason ?: "3D geometry, polygon floor area, and object spatial certainty verified."
                )
                repository.recordAdminReviewEvent(event)
                loadSavedMaps()
                Log.i(TAG, "ADMIN_APPROVED_MAP mapId=$mapId")
            }
        }
    }

    fun adminRejectMap(mapId: String, reason: String) {
        viewModelScope.launch {
            val map = _uiState.value.savedMaps.firstOrNull { it.mapId == mapId } ?: _uiState.value.activeMap
            if (map != null) {
                val updatedMap = map.copy(reviewStatus = AdminReviewStatus.REJECTED)
                repository.saveHomeMap(updatedMap)
                val event = AdminReviewEvent(
                    mapId = mapId,
                    adminUserId = _uiState.value.currentUserId,
                    action = "REJECTED",
                    notes = reason
                )
                repository.recordAdminReviewEvent(event)
                loadSavedMaps()
                Log.i(TAG, "ADMIN_REJECTED_MAP mapId=$mapId reason=$reason")
            }
        }
    }

    fun adminRequestRescan(mapId: String, reason: String) {
        viewModelScope.launch {
            val map = _uiState.value.savedMaps.firstOrNull { it.mapId == mapId } ?: _uiState.value.activeMap
            if (map != null) {
                val updatedMap = map.copy(reviewStatus = AdminReviewStatus.RESCAN_REQUIRED)
                repository.saveHomeMap(updatedMap)
                val event = AdminReviewEvent(
                    mapId = mapId,
                    adminUserId = _uiState.value.currentUserId,
                    action = "RESCAN_REQUESTED",
                    notes = reason
                )
                repository.recordAdminReviewEvent(event)
                loadSavedMaps()
                Log.i(TAG, "ADMIN_RESCAN_REQUESTED mapId=$mapId reason=$reason")
            }
        }
    }

    fun applyConfirmedChanges(map: HomeMap) {
        viewModelScope.launch {
            val pending = changeDetector.pendingChanges.value
            if (pending.isEmpty()) return@launch

            val newVersionNumber = map.currentVersion + 1
            val updatedMap = map.copy(
                currentVersion = newVersionNumber,
                updatedAtMs = System.currentTimeMillis()
            )
            repository.saveHomeMap(updatedMap)
            changeDetector.clearPendingChanges()
            loadSavedMaps()
            Log.i(TAG, "CONFIRMED_CHANGES_APPLIED newVersion=$newVersionNumber")
        }
    }

    fun deleteHomeMap(mapId: String) {
        val isTargetActiveMap = _uiState.value.activeMap?.mapId == mapId
        if (isTargetActiveMap) {
            _uiState.value = _uiState.value.copy(activeMap = null)
        }
        viewModelScope.launch {
            val syncResult = repository.deleteHomeMapWithSync(mapId)
            val newActiveMap = if (isTargetActiveMap) null else repository.activeMap.value
            if (syncResult.localDeleted) {
                loadSavedMaps()
                val cloudMsg = when (val res = syncResult.cloudResult) {
                    is com.example.drishti.supabase.DataResult.Success -> if (res.data) "Map deleted locally and in cloud." else "Map deleted locally. (No matching cloud record found)"
                    else -> "Map deleted locally. (Cloud sync pending or offline)"
                }
                _uiState.value = _uiState.value.copy(successMessage = cloudMsg, activeMap = newActiveMap)
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete local map files.",
                    activeMap = newActiveMap
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

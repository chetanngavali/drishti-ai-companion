package com.example.drishti.homemapping.ui

import com.example.drishti.homemapping.model.HomeMap

enum class HomeMappingScreenRoute {
    DASHBOARD,
    SCAN,
    VIEWER,
    REVIEW,
    UPDATES,
    SETTINGS
}

data class HomeMappingUiState(
    val savedMaps: List<HomeMap> = emptyList(),
    val activeMap: HomeMap? = null,
    val selectedRoomId: String? = null,
    val selectedObjectId: String? = null,
    val is3DViewMode: Boolean = true,
    val showObjectLabels: Boolean = true,
    val showRoomBoundaries: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUserId: String = "usr_embedded_drishti",
    val hasIncompleteScan: Boolean = false,
    val isAdminUser: Boolean = false,
    val currentSubScreen: HomeMappingScreenRoute = HomeMappingScreenRoute.DASHBOARD
)

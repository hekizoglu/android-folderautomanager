package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.runtime.Immutable

@Immutable
data class EditingCenterState(
    val pendingClassificationCount: Int = 0,
    val folderMergeCandidates: Int = 0,
    val appCorrectionsCount: Int = 0,
    val missingPermissionsCount: Int = 0,
    val staleAppsCount: Int = 0,
    val isLoading: Boolean = false,
) {
    val totalAlerts: Int
        get() = pendingClassificationCount + folderMergeCandidates +
            appCorrectionsCount + missingPermissionsCount + staleAppsCount

    val hasAnyAlert: Boolean
        get() = totalAlerts > 0
}

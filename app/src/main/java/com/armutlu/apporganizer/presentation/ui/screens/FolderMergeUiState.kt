package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.runtime.Immutable
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.usecase.folder.FolderMergePlan
import com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestion

@Immutable
data class FolderMergeUiState(
    val suggestions: List<FolderSuggestion> = emptyList(),
    val selectedSuggestionId: String? = null,
    val sourceFolderApps: List<AppInfo> = emptyList(),
    val selectableApps: List<AppInfo> = emptyList(),
    val selectedAppsToMove: Set<String> = emptySet(),
    val targetFolderId: String? = null,
    val mergePlan: FolderMergePlan? = null,
    val isProcessing: Boolean = false,
    val error: String? = null,
) {
    val isReadyToApprove: Boolean
        get() = mergePlan != null && selectedAppsToMove.isNotEmpty() && targetFolderId != null

    val currentIndex: Int
        get() = suggestions.indexOfFirst { it.id == selectedSuggestionId }

    val hasMore: Boolean
        get() = currentIndex >= 0 && currentIndex < suggestions.lastIndex
}

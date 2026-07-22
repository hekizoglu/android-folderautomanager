package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.runtime.Immutable
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category

@Immutable
data class ClassificationReviewUiState(
    val pendingApps: List<AppInfo> = emptyList(),
    val activePackage: String? = null,
    val activeApp: AppInfo? = null,
    val selectedCategory: Category? = null,
    val categoryChoices: Map<String, Category> = emptyMap(),
    val searchQuery: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0
) {
    val progress: Int get() = if (pendingApps.isNotEmpty()) {
        ((approvedCount + rejectedCount) * 100) / pendingApps.size
    } else 0

    val currentIndex: Int get() = pendingApps.indexOfFirst { it.packageName == activePackage }
    val hasMore: Boolean get() = currentIndex >= 0 && currentIndex < pendingApps.lastIndex
}

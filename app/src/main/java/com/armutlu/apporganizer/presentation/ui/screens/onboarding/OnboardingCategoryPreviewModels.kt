package com.armutlu.apporganizer.presentation.ui.screens.onboarding

import androidx.compose.runtime.Immutable
import com.armutlu.apporganizer.domain.models.AppInfo

/**
 * Onboarding Kategori Önizleme ekranı için kararlı ve test edilebilir UI durum modelleri.
 */
@Immutable
data class OnboardingCategoryCardUiModel(
    val categoryId: String,
    val title: String,
    val description: String,
    val colorHex: String,
    val iconEmoji: String,
    val appCount: Int,
    val previewApps: List<AppInfo>,
    val pendingCount: Int = 0,
)

enum class OnboardingPreviewState {
    LOADING,
    SUCCESS,
    EMPTY,
    ERROR,
}

@Immutable
data class OnboardingCategoryPreviewUiModel(
    val totalAppCount: Int,
    val totalFolderCount: Int,
    val categorizedAppCount: Int,
    val pendingCount: Int,
    val categories: List<OnboardingCategoryCardUiModel>,
    val state: OnboardingPreviewState,
)

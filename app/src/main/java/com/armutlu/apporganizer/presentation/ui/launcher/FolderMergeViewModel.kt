package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.folder.FolderMergeCandidateScorer
import com.armutlu.apporganizer.domain.usecase.folder.FolderMergePlan
import com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestion
import com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestionType
import com.armutlu.apporganizer.presentation.ui.screens.FolderMergeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FolderMergeViewModel @Inject constructor(
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderMergeUiState())
    val uiState: StateFlow<FolderMergeUiState> = _uiState.asStateFlow()

    fun loadSuggestions(apps: List<AppInfo>, categories: List<Category>) {
        viewModelScope.launch {
            try {
                val plans = FolderMergeCandidateScorer.score(apps)
                val suggestions = plans.map { plan ->
                    FolderSuggestion(
                        id = UUID.randomUUID().toString(),
                        type = FolderSuggestionType.MERGE_SMALL_FOLDER,
                        title = "${plan.sourceCategoryId} → ${plan.targetCategoryId}",
                        description = "Taşınabilir: ${plan.movablePackageNames.size}",
                        packageNames = plan.movablePackageNames,
                        targetCategoryId = plan.targetCategoryId,
                        confidence = plan.confidence,
                        sourceCategoryId = plan.sourceCategoryId,
                        reason = plan.reason,
                        lockedPackageNames = plan.lockedPackageNames,
                        sourceAppCount = plan.sourceAppCount,
                        targetAppCount = plan.targetAppCount,
                    )
                }
                _uiState.update { state ->
                    state.copy(
                        suggestions = suggestions,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun selectSuggestion(suggestionId: String) {
        val suggestion = _uiState.value.suggestions.find { it.id == suggestionId }
        if (suggestion != null) {
            val sourceFolderApps = _uiState.value.sourceFolderApps.filter {
                it.categoryId == suggestion.sourceCategoryId
            }
            val selectableApps = sourceFolderApps.filter { !it.isCategoryLocked }

            _uiState.update { state ->
                state.copy(
                    selectedSuggestionId = suggestionId,
                    sourceFolderApps = sourceFolderApps,
                    selectableApps = selectableApps,
                    selectedAppsToMove = emptySet(),
                    targetFolderId = suggestion.targetCategoryId,
                    error = null
                )
            }
        }
    }

    fun toggleAppSelection(packageName: String) {
        _uiState.update { state ->
            val newSelection = state.selectedAppsToMove.toMutableSet()
            if (newSelection.contains(packageName)) {
                newSelection.remove(packageName)
            } else {
                newSelection.add(packageName)
            }
            state.copy(selectedAppsToMove = newSelection)
        }
    }

    fun selectTargetFolder(categoryId: String) {
        _uiState.update { it.copy(targetFolderId = categoryId) }
    }

    fun approveMerge() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isReadyToApprove) return@launch

            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val suggestion = state.suggestions.find { it.id == state.selectedSuggestionId }
                if (suggestion != null && state.targetFolderId != null) {
                    val plan = FolderMergePlan(
                        sourceCategoryId = suggestion.sourceCategoryId,
                        targetCategoryId = state.targetFolderId,
                        movablePackageNames = state.selectedAppsToMove.toList(),
                        lockedPackageNames = state.sourceFolderApps
                            .filter { it.isCategoryLocked }
                            .map { it.packageName },
                        reason = suggestion.reason,
                        confidence = suggestion.confidence,
                        sourceAppCount = state.sourceFolderApps.size,
                        targetAppCount = 0
                    )
                    _uiState.update { it.copy(mergePlan = plan) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun rejectMerge() {
        _uiState.update { state ->
            state.copy(
                selectedSuggestionId = null,
                sourceFolderApps = emptyList(),
                selectableApps = emptyList(),
                selectedAppsToMove = emptySet(),
                targetFolderId = null,
                mergePlan = null,
                error = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun FolderMergePlan.toSuggestion() = com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestion(
        id = "$sourceCategoryId:$targetCategoryId:${movablePackageNames.hashCode()}",
        type = com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestionType.MERGE_SMALL_FOLDER,
        title = "Klasör birleştirme",
        description = "${movablePackageNames.size} uygulama taşınacak",
        packageNames = movablePackageNames,
        targetCategoryId = targetCategoryId,
        confidence = confidence,
        sourceCategoryId = sourceCategoryId,
        reason = reason,
        lockedPackageNames = lockedPackageNames,
        sourceAppCount = sourceAppCount,
        targetAppCount = targetAppCount
    )
}

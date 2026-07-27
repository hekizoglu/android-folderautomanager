package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.local.UndoMergeDao
import com.armutlu.apporganizer.data.local.UndoMergeEntity
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
import java.util.Stack
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FolderMergeViewModel @Inject constructor(
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val undoMergeDao: UndoMergeDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderMergeUiState())
    val uiState: StateFlow<FolderMergeUiState> = _uiState.asStateFlow()

    // Undo stack: most recent merge at top
    private val _undoStack = MutableStateFlow<Stack<UndoMergeEntity>>(Stack())
    val undoStack: StateFlow<Stack<UndoMergeEntity>> = _undoStack.asStateFlow()

    // Undo availability state
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private var allApps: List<AppInfo> = emptyList()

    init {
        viewModelScope.launch {
            val apps = appDao.getAllApps()
            val categories = categoryDao.getAllCategories()
            _categories.value = categories
            loadSuggestions(apps, categories)
        }
    }

    fun loadSuggestions(apps: List<AppInfo>, categories: List<Category>) {
        allApps = apps
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
            val sourceFolderApps = allApps.filter {
                it.categoryId == suggestion.sourceCategoryId
            }
            val selectableApps = sourceFolderApps.filter {
                !it.isCategoryLocked && it.packageName in suggestion.packageNames
            }

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

                    // Execute merge atomically
                    executeMerge(plan)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Atomic merge execution: update DB + record undo history
     */
    private suspend fun executeMerge(plan: FolderMergePlan) {
        try {
            // 1. Perform atomic category update
            appDao.batchUpdateCategoryForMerge(
                packageNames = plan.movablePackageNames,
                sourceCategoryId = plan.sourceCategoryId,
                targetCategoryId = plan.targetCategoryId
            )

            // 2. Record undo history
            val undoRecord = UndoMergeEntity.create(
                sourceCategoryId = plan.sourceCategoryId,
                targetCategoryId = plan.targetCategoryId,
                affectedPackages = plan.movablePackageNames
            )
            undoMergeDao.insertUndoMerge(undoRecord)

            // 3. Update undo stack UI state
            _undoStack.update { stack ->
                val newStack = Stack<UndoMergeEntity>()
                newStack.addAll(stack)
                newStack.push(undoRecord)
                newStack
            }
            _canUndo.update { true }

        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Birleştirme başarısız: ${e.message}") }
        }
    }

    /**
     * Undo the most recent merge operation
     */
    fun undoLastMerge() {
        viewModelScope.launch {
            try {
                val undoRecord = _undoStack.value.lastOrNull() ?: return@launch

                _uiState.update { it.copy(isProcessing = true, error = null) }

                // Restore packages to source category
                appDao.batchUpdateCategoryForMerge(
                    packageNames = undoRecord.getAffectedPackagesList(),
                    sourceCategoryId = undoRecord.targetCategoryId,
                    targetCategoryId = undoRecord.sourceCategoryId
                )

                // Pop from undo stack
                _undoStack.update { stack ->
                    val newStack = Stack<UndoMergeEntity>()
                    newStack.addAll(stack.dropLast(1))
                    newStack
                }
                _canUndo.update { _undoStack.value.isNotEmpty() }

                _uiState.update { it.copy(error = "Geri alındı") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Geri alma başarısız: ${e.message}") }
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
}

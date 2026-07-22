package com.armutlu.apporganizer.presentation.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.TurkishCategorySorter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ClassificationReviewViewModel @Inject constructor(
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassificationReviewUiState())
    val uiState: StateFlow<ClassificationReviewUiState> = _uiState.asStateFlow()

    private val _lastActionMs = MutableStateFlow(0L)
    private val doubleTapThresholdMs = 500L

    fun loadPendingApps(apps: List<AppInfo>, categories: List<Category>) {
        viewModelScope.launch {
            val sorted = TurkishCategorySorter.sortBy(apps, { it.appName }, Locale.getDefault())
            val categoryMap = categories.associateBy { it.categoryId }
            _uiState.update { state ->
                state.copy(
                    pendingApps = sorted,
                    categoryChoices = categoryMap,
                    activePackage = sorted.firstOrNull()?.packageName
                )
            }
            selectApp(sorted.firstOrNull()?.packageName ?: "")
        }
    }

    fun selectApp(packageName: String) {
        val app = _uiState.value.pendingApps.find { it.packageName == packageName }
        _uiState.update { state ->
            state.copy(
                activePackage = packageName,
                activeApp = app,
                selectedCategory = null,
                searchQuery = ""
            )
        }
    }

    fun selectCategory(appPkg: String, category: Category) {
        val now = System.currentTimeMillis()
        if (now - _lastActionMs.value < doubleTapThresholdMs) {
            return // İdempotency: çift tıklama önle
        }
        _lastActionMs.value = now

        _uiState.update { state ->
            state.copy(
                selectedCategory = if (state.selectedCategory?.categoryId == category.categoryId) null else category
            )
        }
    }

    fun approveCurrent() {
        val now = System.currentTimeMillis()
        if (now - _lastActionMs.value < doubleTapThresholdMs) return
        _lastActionMs.value = now

        val state = _uiState.value
        if (state.activePackage != null && state.selectedCategory != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isProcessing = true, error = null) }
                try {
                    // Repository call yapılacak (henüz stub)
                    advanceToNext()
                    _uiState.update { it.copy(approvedCount = it.approvedCount + 1) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                } finally {
                    _uiState.update { it.copy(isProcessing = false) }
                }
            }
        }
    }

    fun rejectCurrent() {
        val now = System.currentTimeMillis()
        if (now - _lastActionMs.value < doubleTapThresholdMs) return
        _lastActionMs.value = now

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                // Repository call yapılacak (henüz stub)
                advanceToNext()
                _uiState.update { it.copy(rejectedCount = it.rejectedCount + 1) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun skipCurrent() {
        advanceToNext()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun advanceToNext() {
        val state = _uiState.value
        val currentIndex = state.currentIndex
        if (currentIndex >= 0 && currentIndex < state.pendingApps.lastIndex) {
            selectApp(state.pendingApps[currentIndex + 1].packageName)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

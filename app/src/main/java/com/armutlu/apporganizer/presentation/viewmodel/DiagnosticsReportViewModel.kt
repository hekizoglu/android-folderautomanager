package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.utils.DiagnosticsReportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class DiagnosticsReportUiState(
    val isGenerating: Boolean = false,
    val shareIntent: Intent? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class DiagnosticsReportViewModel @Inject constructor(
    private val diagnosticsReportManager: DiagnosticsReportManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsReportUiState())
    val uiState: StateFlow<DiagnosticsReportUiState> = _uiState.asStateFlow()

    fun generateReport() {
        generate { diagnosticsReportManager.createShareIntent() }
    }

    fun generateFeedbackReport() {
        generate { diagnosticsReportManager.createFeedbackShareIntent() }
    }

    private fun generate(createIntent: suspend () -> Intent) {
        if (_uiState.value.isGenerating) return
        _uiState.value = _uiState.value.copy(isGenerating = true, errorMessage = null, shareIntent = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { createIntent() }
            }
            _uiState.value = result.fold(
                onSuccess = { shareIntent ->
                    DiagnosticsReportUiState(isGenerating = false, shareIntent = shareIntent)
                },
                onFailure = { error ->
                    Timber.e(error, "Saglik raporu olusturulamadi")
                    DiagnosticsReportUiState(
                        isGenerating = false,
                        errorMessage = "Saglik raporu su anda olusturulamadi.",
                    )
                },
            )
        }
    }

    fun consumeShareIntent() {
        _uiState.value = _uiState.value.copy(shareIntent = null)
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

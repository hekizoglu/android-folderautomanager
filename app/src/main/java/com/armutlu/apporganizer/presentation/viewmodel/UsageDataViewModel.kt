package com.armutlu.apporganizer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionTestStatus { IDLE, TESTING, SUCCESS, PARTIAL_SUCCESS, FAILED }

data class UsageDataUiState(
    val sharingEnabled: Boolean = false,
    val connectionStatus: ConnectionTestStatus = ConnectionTestStatus.IDLE,
)

class UsageDataViewModel(
    initialSharingEnabled: Boolean,
    private val persistSharingEnabled: (Boolean) -> Unit,
    private val testConnection: suspend () -> ConnectionTestStatus,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UsageDataUiState(sharingEnabled = initialSharingEnabled))
    val uiState: StateFlow<UsageDataUiState> = _uiState.asStateFlow()

    fun setSharingEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(sharingEnabled = enabled)
        persistSharingEnabled(enabled)
    }

    fun runConnectionTest() {
        if (_uiState.value.connectionStatus == ConnectionTestStatus.TESTING) return
        _uiState.value = _uiState.value.copy(connectionStatus = ConnectionTestStatus.TESTING)
        viewModelScope.launch {
            val result = runCatching { testConnection() }.getOrDefault(ConnectionTestStatus.FAILED)
            _uiState.value = _uiState.value.copy(connectionStatus = result)
        }
    }
}

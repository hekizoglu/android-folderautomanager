package com.armutlu.apporganizer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SearchSettingsViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _sourceOpInFlight = MutableStateFlow(false)
    val sourceOpInFlight: StateFlow<Boolean> = _sourceOpInFlight.asStateFlow()

    fun enableContactsSource() = runSourceOp {
        searchRepository.enableContactsSource()
    }

    fun disableContactsSource() = runSourceOp {
        searchRepository.disableContactsSource()
    }

    fun enableFilesSource() = runSourceOp {
        searchRepository.enableFilesSource()
    }

    fun disableFilesSource() = runSourceOp {
        searchRepository.disableFilesSource()
    }

    private fun runSourceOp(block: suspend () -> Unit) {
        viewModelScope.launch {
            _sourceOpInFlight.value = true
            try {
                block()
            } catch (e: Exception) {
                Timber.e(e, "Search source operation failed")
            } finally {
                _sourceOpInFlight.value = false
            }
        }
    }
}

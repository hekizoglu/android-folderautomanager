package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.usecase.privacy.PrivacyAnalyzer
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Gizlilik Raporu ekranının net UI durumları. */
sealed interface PrivacyReportUiState {
    data object Loading : PrivacyReportUiState

    /** Kullanıcı "Gizlilik Analizi" anahtarını kapatmış. */
    data object AnalyticsDisabled : PrivacyReportUiState

    data class Ready(val report: PrivacyAnalyzer.PrivacyReport) : PrivacyReportUiState
}

/**
 * Gizlilik Analizi ekranı için veri hazırlar — AppRepository'den görünür (gizli olmayan)
 * uygulama listesini alır, PrivacyAnalyzer ile PackageManager'dan hassas izin bilgisini
 * cıkarır. Tüm işlem cihazda yapılır, hiçbir veri sunucuya gönderilmez.
 */
@HiltViewModel
class PrivacyReportViewModel @Inject constructor(
    private val appRepository: AppRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrivacyReportUiState>(PrivacyReportUiState.Loading)
    val uiState: StateFlow<PrivacyReportUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val analyticsEnabled = AppPrefs.isPrivacyReportEnabled(context)
        if (!analyticsEnabled) {
            _uiState.value = PrivacyReportUiState.AnalyticsDisabled
            return
        }
        _uiState.value = PrivacyReportUiState.Loading
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) {
                runCatching {
                    val visibleApps = appRepository.getAllApps().filter { !it.isHidden }
                    PrivacyAnalyzer.analyze(context, visibleApps)
                }.onFailure { e ->
                    Timber.e(e, "Gizlilik raporu üretilemedi")
                }.getOrDefault(PrivacyAnalyzer.PrivacyReport(groups = emptyList()))
            }
            _uiState.value = PrivacyReportUiState.Ready(report)
        }
    }

    /** "Analiz kapalı" durumundan tek dokunuşla çıkış. */
    fun enableAnalytics() {
        AppPrefs.setPrivacyReportEnabled(context, true)
        refresh()
    }
}

package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.NotificationAnalyzer
import com.armutlu.apporganizer.utils.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Bildirim Raporu ekranının net UI durumları (Döngü 224 — UX ayrımı).
 *
 * Eski model `Report?` idi: izin kapalı / analiz kapalı / veri yok üçü de aynı
 * "boş rapor" görünümüne düşüyordu ve kullanıcı sebebini ayırt edemiyordu.
 * Bu sealed model her boş-durumu kendi açıklaması + eylem önerisiyle ayırır.
 */
sealed interface NotificationReportUiState {
    /** İlk yükleme — spinner. */
    data object Loading : NotificationReportUiState

    /** Bildirim erişim izni verilmemiş ve gösterilecek geçmiş veri de yok. */
    data object PermissionMissing : NotificationReportUiState

    /** Kullanıcı "Bildirim Analizi" anahtarını kapatmış ve gösterilecek veri yok. */
    data object AnalyticsDisabled : NotificationReportUiState

    /** İzin ve analiz açık ama henüz bildirim verisi birikmedi. */
    data object CollectingData : NotificationReportUiState

    /** Normal rapor. Bayraklar açıkken üstte uyarı bandı gösterilir. */
    data class Ready(
        val report: NotificationAnalyzer.Report,
        val permissionMissing: Boolean,
        val analyticsDisabled: Boolean,
    ) : NotificationReportUiState

    companion object {
        /**
         * Saf durum eşlemesi — unit test edilebilir (NotificationReportUiStateTest).
         * Öncelik: veri varsa her zaman rapor göster (bayraklarla uyar);
         * veri yoksa sebep sırası izin > analiz anahtarı > veri toplanıyor.
         */
        fun from(
            report: NotificationAnalyzer.Report?,
            permissionGranted: Boolean,
            analyticsEnabled: Boolean,
        ): NotificationReportUiState = when {
            report == null -> Loading
            report.totalNotifications > 0 -> Ready(
                report = report,
                permissionMissing = !permissionGranted,
                analyticsDisabled = !analyticsEnabled,
            )
            !permissionGranted -> PermissionMissing
            !analyticsEnabled -> AnalyticsDisabled
            else -> CollectingData
        }
    }
}

/**
 * Son 7 günün bildirim davranışını analiz eder — NotificationAnalyzer'ı DB verisiyle besler.
 */
@HiltViewModel
class NotificationReportViewModel @Inject constructor(
    private val notificationEventDao: NotificationEventDao,
    private val appDao: AppDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<NotificationReportUiState>(NotificationReportUiState.Loading)
    val uiState: StateFlow<NotificationReportUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val permissionGranted = checkListenerPermission()
        val analyticsEnabled = AppPrefs.isNotifAnalyticsEnabled(context)
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) {
                runCatching {
                    val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                    val events = notificationEventDao.eventsSince(since)
                    val appNames = appDao.getAllApps().associate { it.packageName to it.appName }
                    val usageMs = if (UsageStatsHelper.hasPermission(context)) {
                        UsageStatsHelper.getUsageCounts(context, days = 7)
                    } else {
                        emptyMap()
                    }
                    NotificationAnalyzer.analyze(events, appNames, usageMs)
                }.onFailure { e ->
                    Timber.e(e, "Bildirim raporu üretilemedi")
                }.getOrDefault(
                    NotificationAnalyzer.Report(0, emptyList(), emptyList(), emptyList(), emptyList())
                )
            }
            _uiState.value = NotificationReportUiState.from(report, permissionGranted, analyticsEnabled)
        }
    }

    /** "Analiz kapalı" durumundan tek dokunuşla çıkış — ayara gitmeye gerek yok. */
    fun enableAnalytics() {
        AppPrefs.setNotifAnalyticsEnabled(context, true)
        refresh()
    }

    private fun checkListenerPermission(): Boolean {
        return try {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            enabled?.contains(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }
}

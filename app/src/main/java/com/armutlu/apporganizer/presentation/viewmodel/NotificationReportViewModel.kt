package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.local.NotificationHistoryDao
import com.armutlu.apporganizer.domain.models.NotificationHistoryEntity
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.NotificationAccessUtils
import com.armutlu.apporganizer.utils.NotificationAnalyzer
import com.armutlu.apporganizer.utils.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class NotificationHistoryFilterOption(
    val packageName: String,
    val appName: String,
    val count: Int,
)

enum class NotificationReportRange {
    LAST_24_HOURS,
    LAST_7_DAYS,
    CUSTOM,
}

data class NotificationHistoryUiState(
    val entries: List<NotificationHistoryEntity> = emptyList(),
    val filters: List<NotificationHistoryFilterOption> = emptyList(),
    val selectedPackageName: String? = null,
    val appNames: Map<String, String> = emptyMap(),
    val totalCount: Int = 0,
) {
    companion object {
        fun build(
            entries: List<NotificationHistoryEntity>,
            appNames: Map<String, String>,
            requestedPackageName: String?,
        ): NotificationHistoryUiState {
            val packageCounts = entries.groupingBy { it.packageName }.eachCount()
            val effectiveSelection = requestedPackageName?.takeIf(packageCounts::containsKey)
            val filters = packageCounts.map { (packageName, count) ->
                NotificationHistoryFilterOption(
                    packageName = packageName,
                    appName = resolveAppName(packageName, appNames),
                    count = count,
                )
            }.sortedBy { it.appName.lowercase(Locale("tr", "TR")) }

            val sortedEntries = entries.sortedByDescending { it.postedAt }
            return NotificationHistoryUiState(
                entries = if (effectiveSelection == null) {
                    sortedEntries
                } else {
                    sortedEntries.filter { it.packageName == effectiveSelection }
                },
                filters = filters,
                selectedPackageName = effectiveSelection,
                appNames = appNames,
                totalCount = entries.size,
            )
        }

        fun resolveAppName(packageName: String, appNames: Map<String, String>): String =
            appNames[packageName]
                ?.takeIf { it.isNotBlank() }
                ?: packageName.substringAfterLast('.').takeIf { it.isNotBlank() }
                ?: packageName
    }
}

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

    data class Error(val message: String) : NotificationReportUiState

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
    private val notificationHistoryDao: NotificationHistoryDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<NotificationReportUiState>(NotificationReportUiState.Loading)
    val uiState: StateFlow<NotificationReportUiState> = _uiState.asStateFlow()

    private val _range = MutableStateFlow(NotificationReportRange.LAST_7_DAYS)
    val range: StateFlow<NotificationReportRange> = _range.asStateFlow()
    private var customStartMillis: Long? = null
    private var customEndMillis: Long? = null

    /** D242c — Bildirim Geçmişi sekmesi: gerçek başlık/metin, yalnızca ayar açıkken dolu. */
    val historyEnabled: Boolean get() = AppPrefs.isNotificationTextEnabled(context)

    private val rawHistory = notificationHistoryDao.observeRecent()
    private val _historyAppNames = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _selectedHistoryPackage = MutableStateFlow<String?>(null)

    val historyUiState: StateFlow<NotificationHistoryUiState> = combine(
        rawHistory,
        _historyAppNames,
        _selectedHistoryPackage,
    ) { entries, appNames, selectedPackage ->
        NotificationHistoryUiState.build(entries, appNames, selectedPackage)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        NotificationHistoryUiState(),
    )

    fun selectHistoryPackage(packageName: String?) {
        _selectedHistoryPackage.value = packageName
    }

    fun markHistoryRead(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { notificationHistoryDao.markRead(id) }
        }
    }

    fun markAllHistoryRead() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { notificationHistoryDao.markAllRead() }
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { notificationHistoryDao.deleteById(id) }
                .onFailure { Timber.e(it, "Bildirim geçmişi kaydı silinemedi") }
        }
    }

    init {
        refreshHistoryAppNames()
        refresh()
    }

    fun refresh() {
        refreshHistoryAppNames()
        val permissionGranted = checkListenerPermission()
        val analyticsEnabled = AppPrefs.isNotifAnalyticsEnabled(context)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val now = System.currentTimeMillis()
                    val (since, until) = resolveRange(now)
                    val events = notificationEventDao.eventsBetween(since, until)
                    val appNames = appDao.getAllApps().associate { it.packageName to it.appName }
                    val usageMs = if (UsageStatsHelper.hasPermission(context)) {
                        UsageStatsHelper.getUsageCounts(context, days = 7)
                    } else {
                        emptyMap()
                    }
                    NotificationAnalyzer.analyze(events, appNames, usageMs)
                }
            }
            _uiState.value = result.fold(
                onSuccess = { report ->
                    NotificationReportUiState.from(report, permissionGranted, analyticsEnabled)
                },
                onFailure = { error ->
                    Timber.e(error, "Bildirim raporu üretilemedi")
                    NotificationReportUiState.Error("Bildirim raporu su anda yuklenemedi.")
                }
            )
        }
    }

    fun setRange(range: NotificationReportRange) {
        if (range == NotificationReportRange.CUSTOM) return
        _range.value = range
        refresh()
    }

    fun setCustomRange(startMillis: Long, endMillis: Long) {
        customStartMillis = minOf(startMillis, endMillis)
        customEndMillis = maxOf(startMillis, endMillis) + TimeUnit.DAYS.toMillis(1)
        _range.value = NotificationReportRange.CUSTOM
        refresh()
    }

    private fun resolveRange(now: Long): Pair<Long, Long> = when (_range.value) {
        NotificationReportRange.LAST_24_HOURS -> now - TimeUnit.HOURS.toMillis(24) to now
        NotificationReportRange.LAST_7_DAYS -> now - TimeUnit.DAYS.toMillis(7) to now
        NotificationReportRange.CUSTOM -> {
            val start = customStartMillis ?: (now - TimeUnit.DAYS.toMillis(7))
            val end = customEndMillis ?: now
            start to end
        }
    }

    /** "Analiz kapalı" durumundan tek dokunuşla çıkış — ayara gitmeye gerek yok. */
    fun enableAnalytics() {
        AppPrefs.setNotifAnalyticsEnabled(context, true)
        refresh()
    }

    private fun refreshHistoryAppNames() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                appDao.getAllApps().associate { it.packageName to it.appName }
            }.onSuccess { appNames ->
                _historyAppNames.value = appNames
            }.onFailure { error ->
                Timber.w(error, "Bildirim geçmişi uygulama adları yüklenemedi")
            }
        }
    }

    private fun checkListenerPermission(): Boolean {
        return NotificationAccessUtils.isNotificationListenerEnabled(context)
    }

    /**
     * Tüm ham bildirim verilerini özet geçmeden ham CSV formatında (Yapay zeka analizi için)
     * dışarı aktarır.
     */
    fun exportAllNotificationsCsv(context: Context, onComplete: (android.net.Uri?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = runCatching {
                val appNames = appDao.getAllApps().associate { it.packageName to it.appName }
                val (since, until) = resolveRange(System.currentTimeMillis())
                val events = notificationEventDao.eventsBetween(since, until)
                val historyMap = historyUiState.value.entries.associateBy { Pair(it.packageName, it.postedAt) }
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

                val csv = buildString {
                    appendLine("id,package_name,app_name,title,text,posted_at,date_time,category,importance_score,was_suppressed")
                    events.forEach { event ->
                        val appName = appNames[event.packageName] ?: event.packageName
                        val history = historyMap[Pair(event.packageName, event.postedAt)]
                        val title = history?.title?.replace("\"", "\"\"")?.replace("\n", " ") ?: ""
                        val text = history?.text?.replace("\"", "\"\"")?.replace("\n", " ") ?: ""
                        val dateStr = sdf.format(java.util.Date(event.postedAt))
                        appendLine("${event.id},\"${event.packageName}\",\"${appName.replace("\"", "\"\"")}\",\"$title\",\"$text\",${event.postedAt},\"$dateStr\",\"${event.category}\",${event.importanceScore},${event.wasSuppressed}")
                    }
                }

                val fileName = "apporganizer_notifications_ai_export_${System.currentTimeMillis()}.csv"
                val file = java.io.File(context.cacheDir, fileName)
                file.writeText(csv, Charsets.UTF_8)

                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            }.onFailure { e ->
                Timber.e(e, "CSV export hatası")
            }.getOrNull()

            withContext(Dispatchers.Main) {
                onComplete(uri)
            }
        }
    }
}

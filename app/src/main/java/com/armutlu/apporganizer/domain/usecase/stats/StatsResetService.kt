package com.armutlu.apporganizer.domain.usecase.stats

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.utils.MissionPrefs
import com.armutlu.apporganizer.utils.TaskScoreManager
import com.armutlu.apporganizer.utils.WrappedSnapshotPrefs
import timber.log.Timber

/**
 * P0.4: İstatistik sıfırlama sihirbazı — kapsam seçimli.
 *
 * Eskiden [com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel.resetAllPrivacyData]
 * tek bir "hepsini sil" işlemiydi (kullanım sayaçları + son kullanım + bildirim metni/geçmişi +
 * favoriler — kullanıcı hiçbirini tek tek seçemiyordu). Bu servis her kapsamı bağımsız,
 * ayrı bir suspend fonksiyonla sıfırlar ve sonucu [ScopeResult] olarak döner — sessiz
 * başarısızlık yok, çağıran taraf (UI) her kapsamın başarılı/başarısız olduğunu raporlayabilir.
 *
 * Kapsamlar birbirinden bağımsızdır: biri başarısız olursa diğerleri yine de denenir
 * (kullanıcı 3 kapsam seçtiyse 1 tanesi hata verse bile diğer 2'si sıfırlanmalı).
 */
object StatsResetService {

    enum class Scope {
        USAGE_COUNTERS,        // usageCount / launchCount
        LAST_USED_TIMESTAMPS,  // lastUsedTimestamp
        NOTIFICATION_HISTORY,  // notification_events tablosu + apps.notificationText/notificationCount
        WRAPPED_SNAPSHOTS,     // WrappedSnapshotPrefs (haftalık/günlük karşılaştırma verisi)
        MISSION_PROGRESS       // MissionPrefs (görev puanı ve geçmişi)
    }

    data class ScopeResult(
        val scope: Scope,
        val success: Boolean,
        val error: Throwable? = null
    )

    /**
     * Seçilen kapsamları sırayla sıfırlar. Her kapsam kendi try/catch'i içinde çalışır —
     * biri hata verirse diğer kapsamlar yine de işlenir.
     */
    suspend fun reset(
        context: Context,
        repository: AppRepository,
        scopes: Set<Scope>
    ): List<ScopeResult> {
        if (scopes.isEmpty()) return emptyList()
        return scopes.map { scope -> resetScope(context, repository, scope) }
    }

    private suspend fun resetScope(
        context: Context,
        repository: AppRepository,
        scope: Scope
    ): ScopeResult = runCatching {
        when (scope) {
            Scope.USAGE_COUNTERS -> resetUsageCounters(repository)
            Scope.LAST_USED_TIMESTAMPS -> resetLastUsedTimestamps(repository)
            Scope.NOTIFICATION_HISTORY -> resetNotificationHistory(repository)
            Scope.WRAPPED_SNAPSHOTS -> resetWrappedSnapshots(context)
            Scope.MISSION_PROGRESS -> resetMissionProgress(context)  // P0.6: suspend oldu, runBlocking yok
        }
        ScopeResult(scope, success = true)
    }.getOrElse { e ->
        Timber.e(e, "StatsResetService: $scope sıfırlama hatası")
        ScopeResult(scope, success = false, error = e)
    }

    private suspend fun resetUsageCounters(repository: AppRepository) {
        repository.resetAllUsageCounters()
    }

    private suspend fun resetLastUsedTimestamps(repository: AppRepository) {
        repository.resetAllLastUsedTimestamps()
    }

    private suspend fun resetNotificationHistory(repository: AppRepository) {
        // P0.6: getAllApps() exception'ı throw edebilir, ancak diğer scopes'lar yine de çalışmalı
        try {
            repository.clearAllNotificationEvents()
            repository.clearAllNotificationTexts()
            val counts = repository.getAllApps().associate { it.packageName to 0 }
            repository.updateNotificationCounts(counts)
        } catch (e: Exception) {
            Timber.e(e, "StatsResetService: resetNotificationHistory hatası")
            throw e  // P0.6: Caller'a (resetScope) error döndür, sessiz başarısızlık yapma
        }
    }

    private fun resetWrappedSnapshots(context: Context) {
        // WrappedSnapshotPrefs ve PulseHistoryPrefs AYNI SharedPreferences dosyasını
        // ("wrapped_prefs") paylaşır — clear() ikisini de temizler (Döngü D01).
        WrappedSnapshotPrefs.clearAll(context)
    }

    private suspend fun resetMissionProgress(context: Context) {
        MissionPrefs.clearAll(context)
        TaskScoreManager.clearLegacyPrefs(context)
        // P0.6: runBlocking kaldır (IO thread'ini bloke eder). Suspend context'te async kullan.
        runCatching {
            val db = AppDatabase.getInstance(context)
            db.missionHistoryDao().clearAll()
            db.taskScoreEventDao().clearAll()
        }.onFailure {
            Timber.w(it, "StatsResetService: mission Room tabloları temizlenemedi, legacy prefs yine de sıfırlandı")
        }
    }
}

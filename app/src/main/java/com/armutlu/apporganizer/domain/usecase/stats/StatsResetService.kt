package com.armutlu.apporganizer.domain.usecase.stats

import android.content.Context
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.utils.MissionPrefs
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
            Scope.MISSION_PROGRESS -> resetMissionProgress(context)
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
        repository.clearAllNotificationEvents()
        repository.clearAllNotificationTexts()
        repository.updateNotificationCounts(
            repository.getAllApps().associate { it.packageName to 0 }
        )
    }

    private fun resetWrappedSnapshots(context: Context) {
        WrappedSnapshotPrefs.clearAll(context)
    }

    private fun resetMissionProgress(context: Context) {
        MissionPrefs.clearAll(context)
    }
}

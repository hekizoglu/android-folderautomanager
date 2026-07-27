package com.armutlu.apporganizer.domain.usecase.goals

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.domain.common.DataFreshnessResolver
import com.armutlu.apporganizer.domain.time.PeriodBoundary
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.missions.MissionUsageStatsSource
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P1 — Kategori bazlı kullanım için TEK kaynak. [com.armutlu.apporganizer.presentation.ui.screens.AppOrganizerDashboardScreen]'in
 * eski "son 7 gün, ISO haftadan bağımsız" penceresini (roadmap S9/S10 bulgusu) VE Dashboard'un
 * Compose içinde senkron `UsageStatsHelper` çağrısını (S7/S9) değiştirir — bu sınıf her zaman
 * çağıran taraftan `Dispatchers.IO` altında çağrılmalıdır (kendisi thread'i zorlamaz, suspend
 * fonksiyon olarak arayüz sağlar).
 *
 * [MissionUsageStatsSource] zaten [MissionMetricSnapshotProvider][com.armutlu.apporganizer.domain.usecase.missions.MissionMetricSnapshotProvider]
 * tarafından kullanılan aynı izin/test soyutlaması — paralel bir UsageStats erişim yolu
 * AÇILMIYOR, mevcut kaynağa paket-günlük foreground verisi için delege ediliyor.
 *
 * ISO hafta sınırları [PeriodBoundaryResolver] ile (Pazartesi başlangıç, DST-güvenli) hesaplanır.
 * Gizli/sistem uygulamaları hariç tutulur (mevcut `AppDao.getPackageNamesByCategory` deseniyle
 * aynı filtre — `isHidden=0 AND isSystemApp=0`, bkz. [AppDao.getAllApps]).
 */
@Singleton
class CategoryUsageSnapshotProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val periodBoundaryResolver: PeriodBoundaryResolver,
    private val dataFreshnessResolver: DataFreshnessResolver,
    private val clock: Clock,
    private val zoneId: ZoneId,
    private val usageStatsSource: MissionUsageStatsSource,
    private val appDao: AppDao,
) {

    suspend fun capture(usageWindowDays: Int = 14): CategoryUsageSnapshot {
        val nowMillis = clock.millis()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val epochDay = today.toEpochDay()

        val currentWeekBoundary = periodBoundaryResolver.currentIsoWeek()
        val previousWeekBoundary = periodBoundaryResolver.previousIsoWeek()

        // Görünür (gizli değil, sistem değil) uygulamaların paket->kategori eşlemesi TEK sorguda.
        val packageToCategory = appDao.getAllApps()
            .filter { !it.isHidden && !it.isSystemApp }
            .associate { it.packageName to it.categoryId }

        val sessions = usageStatsSource.getDailySessionUsage(context, days = usageWindowDays, nowMillis = nowMillis)

        if (sessions == null) {
            // İzin yok veya event verisi yok — sahte sıfır ÜRETİLMEZ (roadmap §4.1).
            return CategoryUsageSnapshot(
                capturedAt = nowMillis,
                previousWeekMinutesByCategory = emptyMap(),
                currentWeekMinutesByCategory = emptyMap(),
                validDataDayCount = 0,
                freshness = dataFreshnessResolver.resolve(null),
            )
        }

        val previousWeekMinutes = minutesByCategory(sessions, previousWeekBoundary, packageToCategory)
        val currentWeekMinutes = minutesByCategory(sessions, currentWeekBoundary, packageToCategory)

        // Geçerli gün sayısı — önceki tamamlanmış hafta içinde en az bir foreground kaydı olan
        // farklı epochDay sayısı (P2'nin MIN_DAYS_REQUIRED kontrolü için).
        val previousWeekStartEpochDay = Instant.ofEpochMilli(previousWeekBoundary.startInclusive)
            .atZone(zoneId).toLocalDate().toEpochDay()
        val previousWeekEndEpochDay = Instant.ofEpochMilli(previousWeekBoundary.endExclusive - 1)
            .atZone(zoneId).toLocalDate().toEpochDay()
        val validDataDayCount = sessions
            .filter { it.epochDay in previousWeekStartEpochDay..previousWeekEndEpochDay }
            .map { it.epochDay }
            .distinct()
            .size

        return CategoryUsageSnapshot(
            capturedAt = nowMillis,
            previousWeekMinutesByCategory = previousWeekMinutes,
            currentWeekMinutesByCategory = currentWeekMinutes,
            validDataDayCount = validDataDayCount,
            freshness = dataFreshnessResolver.resolve(nowMillis),
        )
    }

    private fun minutesByCategory(
        sessions: List<DailyPackageUsage>,
        boundary: PeriodBoundary,
        packageToCategory: Map<String, String>,
    ): Map<String, Long> {
        val startEpochDay = Instant.ofEpochMilli(boundary.startInclusive).atZone(zoneId).toLocalDate().toEpochDay()
        val endEpochDay = Instant.ofEpochMilli(boundary.endExclusive - 1).atZone(zoneId).toLocalDate().toEpochDay()

        return sessions
            .asSequence()
            .filter { it.epochDay in startEpochDay..endEpochDay }
            .mapNotNull { usage ->
                val categoryId = packageToCategory[usage.packageName] ?: return@mapNotNull null
                categoryId to usage.foregroundDurationMs
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, durations) -> durations.sum() / TimeUnit.MINUTES.toMillis(1) }
    }
}

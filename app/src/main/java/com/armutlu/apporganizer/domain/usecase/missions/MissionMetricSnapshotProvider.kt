package com.armutlu.apporganizer.domain.usecase.missions

import android.content.Context
import com.armutlu.apporganizer.data.local.TaskScoreEventDao
import com.armutlu.apporganizer.domain.common.DataFreshnessResolver
import com.armutlu.apporganizer.domain.time.PeriodBoundary
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.utils.TaskScoreManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dongu M02 — MissionMetricSnapshotProvider (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satir 790-848). Butun gorev metriklerini tek `now` ve tek UsageStats okumasi ile [MissionMetricSnapshot]
 * icinde toplar; `MissionsViewModel` artik veri hesaplamaz, bu snapshot'i tuketir.
 *
 * Adimlar (roadmap):
 * 1. Tek `now` degeri alinir ([clock]).
 * 2. [PeriodBoundaryResolver] ile gun/hafta sinirlari cozulur.
 * 3. UsageStats verisi BIR KEZ okunur ([usageStatsSource]).
 * 4. Gunluk ekran suresi global foreground degerinden hesaplanir.
 * 5. Kilit acma sayisi okunur.
 * 6. 23:00 sonrasi ilk kullanim zamani cikarilir.
 * 7. TaskScore event sayaclari DAO uzerinden okunur.
 * 8. Snapshot dondurulur.
 *
 * Izin yoksa kullanim metrikleri (ekran suresi/kilit acma/gece kullanimi) null olur; eylem
 * sayaclari (TaskScore tabanli) korunur — bunlar UsageStats izninden bagimsizdir.
 */
@Singleton
class MissionMetricSnapshotProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val periodBoundaryResolver: PeriodBoundaryResolver,
    private val dataFreshnessResolver: DataFreshnessResolver,
    private val taskScoreEventDao: TaskScoreEventDao,
    private val clock: Clock,
    private val zoneId: ZoneId,
    private val usageStatsSource: MissionUsageStatsSource,
) {

    suspend fun capture(usageWindowDays: Int = 14): MissionMetricSnapshot {
        // 1. Tek now degeri.
        val nowMillis = clock.millis()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val epochDay = today.toEpochDay()

        // 2. Gun/hafta sinirlari (H01) — ISO hafta (Pazartesi baslangicli), epochDay/7 DEGIL
        // (epoch gunu 0 = 1970-01-01 Persembe oldugundan bu heuristik Pazartesi'ye hizali degildir).
        val dayBoundary = periodBoundaryResolver.currentDay()
        val weekBoundary = periodBoundaryResolver.currentIsoWeek()
        val previousWeekBoundary = periodBoundaryResolver.previousIsoWeek()

        // 3. UsageStats verisi bir kez okunur.
        val sessions = usageStatsSource.getDailySessionUsage(context, days = usageWindowDays)

        // 4. Gunluk ekran suresi: gun icindeki tum paket girdileri ayni global foreground
        // degerini tasir (paylasilan "ekran acik" penceresi) — max alinir, toplanmaz.
        val minutesByDay: Map<Long, Long>? = sessions
            ?.groupBy { it.epochDay }
            ?.mapValues { (_, list) ->
                (list.maxOfOrNull { it.globalForegroundMs } ?: 0L) / TimeUnit.MINUTES.toMillis(1)
            }
        val screenTimeMinutesToday = if (minutesByDay != null) minutesByDay[epochDay] ?: 0L else null

        fun weekMinutes(boundary: PeriodBoundary): Long? {
            if (minutesByDay == null) return null
            val startEpochDay = Instant.ofEpochMilli(boundary.startInclusive).atZone(zoneId).toLocalDate().toEpochDay()
            // endExclusive gece yarisi oldugundan bir onceki gun dahildir.
            val endEpochDay = Instant.ofEpochMilli(boundary.endExclusive - 1).atZone(zoneId).toLocalDate().toEpochDay()
            return minutesByDay.filterKeys { it in startEpochDay..endEpochDay }.values.sum()
        }

        val screenTimeMinutesThisWeek = weekMinutes(weekBoundary)
        val screenTimeMinutesPreviousWeek = weekMinutes(previousWeekBoundary)

        // 5. Kilit acma sayisi.
        val unlockCountToday = usageStatsSource.getUnlockCount(context, days = 1)

        // 6. 23:00 sonrasi ilk kullanim zamani (varsa) — bugunun saatlik foreground kovalarindan
        // cikarilir. hourlyForegroundMs[23] > 0 ise gece kullanimi var demektir; kesin an
        // saatlik veriden bilinmez, bu yuzden saat diliminin baslangici (23:00 yerel) raporlanir.
        val todayEntries = sessions?.filter { it.epochDay == epochDay }
        val usedAfter23Today = todayEntries?.any { entry ->
            entry.hourlyForegroundMs.getOrNull(23)?.let { it > 0L } == true
        }
        val firstUseAfter23At = if (usedAfter23Today == true) {
            today.atTime(23, 0).atZone(zoneId).toInstant().toEpochMilli()
        } else {
            null
        }

        // 7. TaskScore event sayaclari (DAO) — UsageStats izninden bagimsiz, her zaman dolu.
        val dayStart = dayBoundary.startInclusive
        val dayEnd = dayBoundary.endExclusive - 1
        val weekStart = weekBoundary.startInclusive
        val weekEnd = weekBoundary.endExclusive - 1

        val classificationActionsToday = taskScoreEventDao.countEventsBetweenByKeys(
            dayStart,
            dayEnd,
            listOf(
                TaskScoreManager.EventType.ClassificationApproved.eventKey,
                TaskScoreManager.EventType.ClassificationCorrected.eventKey,
            ),
        )
        val notificationReportViewedToday = taskScoreEventDao.countEventsBetweenByKeys(
            dayStart,
            dayEnd,
            listOf(TaskScoreManager.EventType.NotificationReportViewed.eventKey),
        ) > 0
        val positiveActionsThisWeek = taskScoreEventDao.countEventsBetween(
            weekStart,
            weekEnd,
            positiveOnly = true,
        )

        // 8. Snapshot.
        return MissionMetricSnapshot(
            capturedAt = nowMillis,
            screenTimeMinutesToday = screenTimeMinutesToday,
            unlockCountToday = unlockCountToday,
            usedAfter23Today = usedAfter23Today,
            firstUseAfter23At = firstUseAfter23At,
            screenTimeMinutesThisWeek = screenTimeMinutesThisWeek,
            screenTimeMinutesPreviousWeek = screenTimeMinutesPreviousWeek,
            classificationActionsToday = classificationActionsToday,
            notificationReportViewedToday = notificationReportViewedToday,
            positiveActionsThisWeek = positiveActionsThisWeek,
            freshness = dataFreshnessResolver.resolve(nowMillis),
        )
    }
}

/** [MissionMetricSnapshot] -> [MissionEngine.MissionCheckInput] koprusu (M02). */
fun MissionMetricSnapshot.toMissionCheckInput(): MissionEngine.MissionCheckInput =
    MissionEngine.MissionCheckInput(
        screenTimeMinutesToday = screenTimeMinutesToday,
        usedAfter23Today = usedAfter23Today,
        unlockCountToday = unlockCountToday,
        weeklyScreenTimeMinutes = screenTimeMinutesThisWeek,
        previousWeeklyScreenTimeMinutes = screenTimeMinutesPreviousWeek,
        taskEvents = MissionEngine.TaskEventInput(
            positiveEventsToday = 0,
            positiveEventsThisWeek = positiveActionsThisWeek,
            classificationActionsToday = classificationActionsToday,
            notificationReportViewedToday = notificationReportViewedToday,
        ),
    )

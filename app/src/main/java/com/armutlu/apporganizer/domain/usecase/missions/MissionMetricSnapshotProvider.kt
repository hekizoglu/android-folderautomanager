package com.armutlu.apporganizer.domain.usecase.missions

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.TaskScoreEventDao
import com.armutlu.apporganizer.domain.common.DataFreshnessResolver
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.time.PeriodBoundary
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import com.armutlu.apporganizer.utils.AppPrefs
import java.time.LocalDate
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
    private val appDao: AppDao,
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

        // 5b. Dongu G1 — kisisel hedef icin bugun HARIC son 7 TAMAMLANMIS gunun gecmisi.
        // Bugun donem bitmeden dahil edilmezse hedef gun ortasinda kaymaz (M01 sabitlik ilkesi).
        val screenTimeMinutesLast7CompletedDays = minutesByDay
            ?.filterKeys { it < epochDay && it >= epochDay - 7 }
            ?.toSortedMap()
            ?.values
            ?.toList()
            ?: emptyList()
        val unlockCountByDay = usageStatsSource.getUnlockCountPerDay(context, days = 8)
        val unlockCountLast7CompletedDays = unlockCountByDay
            ?.filterKeys { it < epochDay && it >= epochDay - 7 }
            ?.toSortedMap()
            ?.values
            ?.map { it.toLong() }
            ?: emptyList()

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

        // 7b. Dongu G3a — yeni eylem sayaclari (ayni TaskScore event deseni).
        val folderCustomizedToday = taskScoreEventDao.countEventsBetweenByKeys(
            dayStart,
            dayEnd,
            listOf(TaskScoreManager.EventType.FolderCustomized.eventKey),
        ) > 0
        val wrappedReportViewedThisWeek = taskScoreEventDao.countEventsBetweenByKeys(
            weekStart,
            weekEnd,
            listOf(TaskScoreManager.EventType.WrappedReportViewed.eventKey),
        ) > 0

        // 7c. Dongu G3a — DAILY_FOCUS_SESSION: AppPrefs basit prefs sayaci (izin bagimsiz,
        // UsageStats gerektirmez).
        val focusModeMinutesToday = AppPrefs.getFocusMinutesToday(context, nowMillis, zoneId)

        // 7d. Dongu G3a — DAILY_MORNING_CALM: gunun ilk kullaniminin paketi CAT_SOCIAL
        // kategorisindeyse (ilk 30dk penceresi icinde) gorev ihlal edilmis sayilir. Sadece
        // izin VARSA ve bugun en az bir kullanim gerceklestiyse hesaplanir; aksi halde null
        // (veri yok, pencere henuz acilmadi) — sahte basari/basarisizlik UYDURULMAZ.
        val socialAppOpenedInFirst30MinToday = if (todayEntries.isNullOrEmpty()) {
            null
        } else {
            runCatching {
                val socialPackages = appDao.getPackageNamesByCategory(Category.CAT_SOCIAL).toSet()
                detectSocialInMorningWindow(todayEntries, socialPackages)
            }.getOrNull()
        }

        // 8c. Dongu G3b — uygulama-spesifik gorev (DAILY_APP_LIMIT). Sadece izin VARSA
        // (sessions != null) hesaplanir; aksi halde her ikisi de bos/null kalir (sahte
        // veri UYDURULMAZ, MissionEngine.isEligible() zaten appLimitTargetMinutes==null
        // durumunda gorevi havuza almaz).
        val appLimitCandidates = if (sessions != null) {
            runCatching { buildAppLimitCandidates(sessions, epochDay) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val pinnedAppLimitPackage = AppPrefs.getAppLimitTargetPackage(context, epochDay)
        val appLimitUsageMinutesToday = if (pinnedAppLimitPackage != null && minutesByDay != null) {
            todayEntries
                ?.filter { it.packageName == pinnedAppLimitPackage }
                ?.maxOfOrNull { it.foregroundDurationMs }
                ?.let { it / TimeUnit.MINUTES.toMillis(1) }
        } else {
            null
        }

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
            screenTimeMinutesLast7CompletedDays = screenTimeMinutesLast7CompletedDays,
            unlockCountLast7CompletedDays = unlockCountLast7CompletedDays,
            folderCustomizedToday = folderCustomizedToday,
            wrappedReportViewedThisWeek = wrappedReportViewedThisWeek,
            socialAppOpenedInFirst30MinToday = socialAppOpenedInFirst30MinToday,
            focusModeMinutesToday = focusModeMinutesToday,
            appLimitCandidates = appLimitCandidates,
            appLimitUsageMinutesToday = appLimitUsageMinutesToday,
        )
    }

    /**
     * Dongu G3b — sessions'daki HER paket icin son 7 TAMAMLANMIS gunun gunluk dakikalarini
     * cikarir, kategorisini AppDao'dan okur (sadece eligible kategoride olanlar tutulur —
     * gereksiz paketler icin kategori sorgusu atlanir, performans). AppLimitCandidateSelector
     * bu listeden aday secip hedef hesaplar; burasi SADECE veri toplar, karar VERMEZ.
     */
    private suspend fun buildAppLimitCandidates(
        sessions: List<DailyPackageUsage>,
        epochDay: Long,
    ): List<AppLimitCandidateSelector.PackageUsageCandidate> {
        val minutesByPackageAndDay = sessions
            .filter { it.epochDay < epochDay && it.epochDay >= epochDay - 7 }
            .groupBy { it.packageName }
            .mapValues { (_, entries) ->
                entries.associate { it.epochDay to it.foregroundDurationMs / TimeUnit.MINUTES.toMillis(1) }
            }
        if (minutesByPackageAndDay.isEmpty()) return emptyList()

        val eligiblePackages = AppLimitCandidateSelector.ELIGIBLE_CATEGORY_IDS
            .flatMap { categoryId -> appDao.getPackageNamesByCategory(categoryId).map { it to categoryId } }
            .toMap()

        return minutesByPackageAndDay.mapNotNull { (packageName, dayMap) ->
            val categoryId = eligiblePackages[packageName] ?: return@mapNotNull null
            AppLimitCandidateSelector.PackageUsageCandidate(
                packageName = packageName,
                categoryId = categoryId,
                dailyMinutesLast7Days = dayMap.values.toList(),
            )
        }
    }

    /**
     * Dongu G3a — bugunun paket-bazli saatlik kovalarindan ("hourlyForegroundMs", 24 eleman)
     * ilk kullanimin gerceklestigi saati bulur, o saatin ILK YARIM SAATINDA ("first-hour-half
     * heuristic" — kova cozunurlugu saatlik oldugundan dakika hassasiyeti yoktur) hangi
     * paketlerin aktif oldugunu belirler ve bunlardan herhangi biri sosyal kategorideyse true
     * doner. Kova cozunurlugu saatlik oldugu icin tam "ilk 30 dakika" degil "ilk kullanimin
     * gerceklestigi saat dilimi" olarak yorumlanir — kabul edilebilir yaklaşıklık (roadmap G3a
     * kapsaminda dakika hassasiyetli event-stream analizi asiri mühendislik olurdu).
     */
    private fun detectSocialInMorningWindow(
        todayEntries: List<DailyPackageUsage>,
        socialPackages: Set<String>,
    ): Boolean? {
        if (socialPackages.isEmpty()) return null
        val firstActiveHour = (0..23).firstOrNull { hour ->
            todayEntries.any { it.hourlyForegroundMs.getOrNull(hour)?.let { ms -> ms > 0L } == true }
        } ?: return null
        val activePackagesInFirstHour = todayEntries
            .filter { it.hourlyForegroundMs.getOrNull(firstActiveHour)?.let { ms -> ms > 0L } == true }
            .map { it.packageName }
            .toSet()
        return activePackagesInFirstHour.any { it in socialPackages }
    }
}

/** [MissionMetricSnapshot] -> [MissionEngine.MissionCheckInput] koprusu (M02, G3a genisletildi). */
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
            folderCustomizedToday = folderCustomizedToday,
            wrappedReportViewedThisWeek = wrappedReportViewedThisWeek,
        ),
        socialAppOpenedInFirst30MinToday = socialAppOpenedInFirst30MinToday,
        focusModeMinutesToday = focusModeMinutesToday,
        // appLimitTargetMinutes BURADA DEGIL — MissionSummaryUseCase.compute() aday secimini
        // (AppLimitCandidateSelector + AppPrefs pin) yaptiktan SONRA .copy() ile ekler (bkz.
        // personalScreenTarget/personalUnlockTarget ile AYNI desen).
        appLimitUsageMinutesToday = appLimitUsageMinutesToday,
    )

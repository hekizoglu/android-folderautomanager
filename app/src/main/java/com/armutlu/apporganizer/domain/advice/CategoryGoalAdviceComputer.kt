package com.armutlu.apporganizer.domain.advice

import android.content.Context
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import com.armutlu.apporganizer.domain.usecase.goals.CategoryUsageSnapshot
import com.armutlu.apporganizer.domain.usecase.missions.MissionUsageStatsSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * P8 — [DashboardViewModel][com.armutlu.apporganizer.presentation.viewmodel.DashboardViewModel]
 * VE [MissionsViewModel][com.armutlu.apporganizer.presentation.viewmodel.MissionsViewModel] AYNI
 * bu fonksiyonu çağırır — Dashboard'daki TodayCard'ın ADVICE'ı ile Görevler ekranındaki
 * "Bugünün Tavsiyesi" kartı FARKLI hesaplama yapmasın diye (roadmap P8 "ikinci bir kart deseni
 * yazılmaz" ilkesinin doğal uzantısı: hesaplama da tekil olmalı).
 */
data class CategoryGoalForAdvice(
    val categoryId: String,
    val goal: WeeklyGoal,
    val previousWeekMinutes: Long?,
    val currentWeekMinutesSoFar: Long?,
)

/**
 * @param context/usageStatsSource/notificationEventDao/zoneId — P9-takip: bildirim gürültüsü
 * (roadmap §11.1 "en baskın kaynağın payı") ve sabah/gece kullanım paterni (§11.1 "sabah ilk
 * kullanımında sosyal", "gece 23:00 sonrası") sinyallerini gerçek veriden besler. Hepsi
 * NULLABLE — herhangi biri eksikse (test ortamı, izin yok) o sinyal sessizce `null` kalır,
 * motor zaten null-güvenli (sahte veri üretmez, roadmap §4.1/§11 ilkesi).
 */
suspend fun computeDigitalAdvice(
    snapshot: CategoryUsageSnapshot,
    goalsUi: List<CategoryGoalForAdvice>,
    appDao: AppDao,
    clock: Clock,
    context: Context? = null,
    usageStatsSource: MissionUsageStatsSource? = null,
    notificationEventDao: NotificationEventDao? = null,
    zoneId: ZoneId = ZoneId.systemDefault(),
): DigitalAdvice? {
    val exceeded = goalsUi.count { g ->
        val used = g.currentWeekMinutesSoFar
        used != null && used > g.goal.targetMinutes
    }
    val atRisk = goalsUi.count { g ->
        val used = g.currentWeekMinutesSoFar
        used != null && used <= g.goal.targetMinutes && used >= g.goal.targetMinutes * 0.8
    }
    val allOnTrack = goalsUi.isNotEmpty() && goalsUi.all { g ->
        val used = g.currentWeekMinutesSoFar
        used != null && used <= g.goal.targetMinutes
    }
    val categoryChanges = goalsUi.mapNotNull { g ->
        val previous = g.previousWeekMinutes
        val current = g.currentWeekMinutesSoFar
        if (previous == null || current == null) return@mapNotNull null
        CategoryUsageChange(
            // Kategori adı burada çözülmez (kimlik motora taşınmaz) — mesaj şablonu genel
            // "bir kategoride" ifadesi kullanır, spesifik ad ileride UI katmanında zenginleştirilebilir.
            categoryNameRes = R.string.advice_usage_increase_generic_category,
            previousWeekMinutes = previous,
            currentWeekMinutes = current,
        )
    }

    val apps = runCatching { appDao.getAllApps() }.getOrDefault(emptyList())
    val now = clock.millis()
    val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
    val unusedCount = apps.count { app ->
        !app.isHidden && !app.isSystemApp &&
            app.lastUsedTimestamp > 0L && (now - app.lastUsedTimestamp) > thirtyDaysMs
    }
    val uncategorizedCount = apps.count { app ->
        !app.isHidden && !app.isSystemApp && app.categoryId == Category.CAT_UNCATEGORIZED
    }

    val notificationNoiseShare = if (notificationEventDao != null) {
        runCatching {
            val since = now - TimeUnit.DAYS.toMillis(7)
            val counts = notificationEventDao.countsSince(since)
            val total = counts.sumOf { it.count }
            if (total > 0) counts.maxOf { it.count }.toFloat() / total else null
        }.getOrNull()
    } else {
        null
    }

    val (morningDays, nightDays) = if (context != null && usageStatsSource != null) {
        runCatching { computeUsagePatternDays(context, usageStatsSource, appDao, now, zoneId) }
            .getOrDefault(null to null)
    } else {
        null to null
    }

    val input = DigitalAdviceInput(
        permissionFreshness = snapshot.freshness,
        categoryUsageChanges = categoryChanges,
        exceededCategoryGoalCount = exceeded,
        atRiskCategoryGoalCount = atRisk,
        allCategoryGoalsOnTrack = allOnTrack,
        hasAnyCategoryGoal = goalsUi.isNotEmpty(),
        notificationNoiseTopSourceShare = notificationNoiseShare,
        morningSocialOpenDaysLast7 = morningDays,
        lateNightUsageDaysLast7 = nightDays,
        unusedAppCount = unusedCount,
        uncategorizedAppCount = uncategorizedCount,
    )
    return DigitalAdviceEngine.evaluate(input, now)
}

/**
 * P9-takip — son 7 TAMAMLANMIŞ günde (bugün hariç, dönem ortasında kayma olmasın diye
 * [MissionMetricSnapshotProvider][com.armutlu.apporganizer.domain.usecase.missions.MissionMetricSnapshotProvider]
 * ile aynı "G1 sabitlik ilkesi") sabah ilk 30dk içinde sosyal uygulama açılan gün sayısı VE
 * 23:00 sonrası kullanım gerçekleşen gün sayısı. İzin yoksa (`getDailySessionUsage` null döner)
 * ikisi de null — sahte 0 üretilmez.
 */
private suspend fun computeUsagePatternDays(
    context: Context,
    usageStatsSource: MissionUsageStatsSource,
    appDao: AppDao,
    nowMillis: Long,
    zoneId: ZoneId,
): Pair<Int?, Int?> {
    val sessions = usageStatsSource.getDailySessionUsage(context, days = 8, nowMillis = nowMillis) ?: return null to null
    val todayEpochDay = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().toEpochDay()
    val completedDays = sessions.filter { it.epochDay < todayEpochDay && it.epochDay >= todayEpochDay - 7 }
    if (completedDays.isEmpty()) return null to null

    val socialPackages = runCatching { appDao.getPackageNamesByCategory(Category.CAT_SOCIAL).toSet() }.getOrDefault(emptySet())

    val byDay = completedDays.groupBy { it.epochDay }
    var morningCount = 0
    var nightCount = 0
    byDay.forEach { (_, entries) ->
        val firstActiveHour = (0..23).firstOrNull { hour ->
            entries.any { it.hourlyForegroundMs.getOrNull(hour)?.let { ms -> ms > 0L } == true }
        }
        if (firstActiveHour != null && socialPackages.isNotEmpty()) {
            val activeInFirstHour = entries
                .filter { it.hourlyForegroundMs.getOrNull(firstActiveHour)?.let { ms -> ms > 0L } == true }
                .map { it.packageName }
                .toSet()
            if (activeInFirstHour.any { it in socialPackages }) morningCount++
        }
        val usedAfter23 = entries.any { it.hourlyForegroundMs.getOrNull(23)?.let { ms -> ms > 0L } == true }
        if (usedAfter23) nightCount++
    }
    return morningCount to nightCount
}

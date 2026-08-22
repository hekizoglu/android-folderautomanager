package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.NotificationEvent
import java.util.Calendar

/**
 * Bildirim davranış analizi. Yalnız içeriksiz `notification_events` metadata'sını kullanır;
 * bildirim başlığı, gövdesi, göndereni veya hassas içerik rapora taşınmaz.
 */
object NotificationAnalyzer {

    private const val HIGH_PRIORITY_MIN_SCORE = 80

    data class AppNotifStats(
        val packageName: String,
        val appName: String,
        val total: Int,
        val nightCount: Int,
        val maxBurstPerHour: Int,
        val usageMinutes: Long,
        val dailyCounts: List<Int>,
        val actionableCount: Int = total,
        val suppressedCount: Int = 0,
        val highPriorityCount: Int = 0,
        val categoryCounts: Map<NotificationCategory, Int> = emptyMap(),
    ) {
        val nightRatio: Float get() = if (total == 0) 0f else nightCount.toFloat() / total
        val distractionScore: Float get() = total.toFloat() / (usageMinutes + 1)
        val promotionCount: Int get() = categoryCounts[NotificationCategory.PROMOTION] ?: 0
    }

    data class Report(
        val totalNotifications: Int,
        val appStats: List<AppNotifStats>,
        val mostTalkative: List<AppNotifStats>,
        val disturbing: List<AppNotifStats>,
        val distracting: List<AppNotifStats>,
        val totalReceived: Int = totalNotifications,
        val actionableCount: Int = totalNotifications,
        val suppressedCount: Int = 0,
        val categoryDistribution: Map<NotificationCategory, Int> = emptyMap(),
        val highPriorityCount: Int = 0,
        val nightCount: Int = 0,
        val topPromotionSources: List<AppNotifStats> = emptyList(),
    )

    /**
     * @param events son 7 günün içeriksiz bildirim olayları
     * @param appNames paket → görünen isim
     * @param usageMs paket → son 7 gün ön plan süresi (ms)
     * @param nowMillis günlük dağılım için referans zaman; testlerde sabitlenebilir
     */
    fun analyze(
        events: List<NotificationEvent>,
        appNames: Map<String, String>,
        usageMs: Map<String, Long>,
        nowMillis: Long = System.currentTimeMillis(),
    ): Report {
        if (events.isEmpty()) {
            return Report(
                totalNotifications = 0,
                appStats = emptyList(),
                mostTalkative = emptyList(),
                disturbing = emptyList(),
                distracting = emptyList(),
            )
        }

        val dayMs = 24L * 60 * 60 * 1000
        val cal = Calendar.getInstance()
        val categoryDistribution = mutableMapOf<NotificationCategory, Int>()
        var reportActionable = 0
        var reportSuppressed = 0
        var reportHighPriority = 0
        var reportNight = 0

        val stats = events.groupBy { it.packageName }.map { (pkg, list) ->
            var night = 0
            var actionable = 0
            var suppressed = 0
            var highPriority = 0
            val appCategoryCounts = mutableMapOf<NotificationCategory, Int>()
            val hourBuckets = HashMap<Long, Int>()
            val daily = IntArray(7)

            list.forEach { event ->
                val category = parseCategory(event.category)
                appCategoryCounts[category] = (appCategoryCounts[category] ?: 0) + 1
                categoryDistribution[category] = (categoryDistribution[category] ?: 0) + 1

                if (event.wasSuppressed) {
                    suppressed++
                    reportSuppressed++
                } else {
                    actionable++
                    reportActionable++
                }
                if (event.importanceScore >= HIGH_PRIORITY_MIN_SCORE) {
                    highPriority++
                    reportHighPriority++
                }

                cal.timeInMillis = event.postedAt
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                if (hour >= 23 || hour < 7) {
                    night++
                    reportNight++
                }
                val hourKey = event.postedAt / (60L * 60 * 1000)
                hourBuckets[hourKey] = (hourBuckets[hourKey] ?: 0) + 1

                val ageDays = ((nowMillis - event.postedAt) / dayMs).toInt().coerceIn(0, 6)
                daily[6 - ageDays]++
            }

            AppNotifStats(
                packageName = pkg,
                appName = appNames[pkg] ?: pkg.substringAfterLast('.'),
                total = list.size,
                nightCount = night,
                maxBurstPerHour = hourBuckets.values.maxOrNull() ?: 0,
                usageMinutes = (usageMs[pkg] ?: 0L) / 60_000,
                dailyCounts = daily.toList(),
                actionableCount = actionable,
                suppressedCount = suppressed,
                highPriorityCount = highPriority,
                categoryCounts = appCategoryCounts.toMap(),
            )
        }.sortedByDescending { it.total }

        val disturbing = stats.filter { statsItem ->
            (statsItem.total >= 10 && statsItem.nightRatio > 0.3f) ||
                statsItem.maxBurstPerHour >= 5
        }.sortedByDescending { it.nightCount + it.maxBurstPerHour }

        val distracting = stats.filter { statsItem ->
            statsItem.total >= 15 &&
                statsItem.usageMinutes < 30 &&
                statsItem.distractionScore > 1f
        }.sortedByDescending { it.distractionScore }

        val topPromotionSources = stats
            .filter { it.promotionCount > 0 }
            .sortedWith(
                compareByDescending<AppNotifStats> { it.promotionCount }
                    .thenByDescending { it.total },
            )
            .take(10)

        return Report(
            totalNotifications = events.size,
            appStats = stats,
            mostTalkative = stats.take(10),
            disturbing = disturbing.take(10),
            distracting = distracting.take(10),
            totalReceived = events.size,
            actionableCount = reportActionable,
            suppressedCount = reportSuppressed,
            categoryDistribution = categoryDistribution.toMap(),
            highPriorityCount = reportHighPriority,
            nightCount = reportNight,
            topPromotionSources = topPromotionSources,
        )
    }

    private fun parseCategory(value: String): NotificationCategory =
        runCatching { NotificationCategory.valueOf(value) }
            .getOrDefault(NotificationCategory.OTHER)
}

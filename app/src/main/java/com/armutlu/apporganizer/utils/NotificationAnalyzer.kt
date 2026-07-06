package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.NotificationEvent
import java.util.Calendar

/**
 * Bildirim davranış analizi — notification_events kayıtlarından üç kategori üretir:
 *
 * - **Çok Konuşan:** 7 günde en fazla bildirim gönderen uygulamalar ("Instagram 200 bildirim gönderdi")
 * - **Rahatsız Eden:** gece (23:00-07:00) bildirim oranı yüksek VEYA tek saatte 5+ bildirim patlaması yapan
 * - **Dikkat Dağıtan:** çok bildirim gönderen AMA az kullanılan — "seni çağırıyor ama işine yaramıyor" skoru
 *
 * Tüm analiz cihazda çalışır; bildirim içeriği hiç saklanmaz (yalnızca paket + zaman).
 */
object NotificationAnalyzer {

    data class AppNotifStats(
        val packageName: String,
        val appName: String,
        val total: Int,
        val nightCount: Int,        // 23:00-07:00 arası bildirim sayısı
        val maxBurstPerHour: Int,   // tek saat diliminde en yüksek bildirim sayısı
        val usageMinutes: Long,     // son 7 gün ön plan kullanımı (dakika)
        val dailyCounts: List<Int>, // son 7 günün gün-gün sayıları (eski→yeni)
    ) {
        val nightRatio: Float get() = if (total == 0) 0f else nightCount.toFloat() / total
        /** Dikkat dağıtma skoru: bildirim/kullanım oranı — yüksek = çağırıyor ama kullanılmıyor. */
        val distractionScore: Float get() = total.toFloat() / (usageMinutes + 1)
    }

    data class Report(
        val totalNotifications: Int,
        val appStats: List<AppNotifStats>,          // toplam bildirime göre sıralı
        val mostTalkative: List<AppNotifStats>,     // çok konuşanlar (top 10)
        val disturbing: List<AppNotifStats>,        // rahatsız edenler
        val distracting: List<AppNotifStats>,       // dikkat dağıtanlar
    )

    /**
     * @param events son 7 günün bildirim olayları
     * @param appNames paket → görünen isim (bilinmeyenler paket adıyla gösterilir)
     * @param usageMs paket → son 7 gün ön plan süresi (ms), UsageStatsHelper.getUsageCounts
     */
    fun analyze(
        events: List<NotificationEvent>,
        appNames: Map<String, String>,
        usageMs: Map<String, Long>,
    ): Report {
        if (events.isEmpty()) {
            return Report(0, emptyList(), emptyList(), emptyList(), emptyList())
        }
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val cal = Calendar.getInstance()

        val stats = events.groupBy { it.packageName }.map { (pkg, list) ->
            var night = 0
            val hourBuckets = HashMap<Long, Int>()   // epoch-saat → sayı
            val daily = IntArray(7)
            list.forEach { e ->
                cal.timeInMillis = e.postedAt
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                if (hour >= 23 || hour < 7) night++
                val hourKey = e.postedAt / (60L * 60 * 1000)
                hourBuckets[hourKey] = (hourBuckets[hourKey] ?: 0) + 1
                // Gün indeksi: 0 = 6 gün önce ... 6 = bugün
                val dayIdx = 6 - ((now - e.postedAt) / dayMs).toInt().coerceIn(0, 6)
                daily[dayIdx]++
            }
            AppNotifStats(
                packageName = pkg,
                appName = appNames[pkg] ?: pkg.substringAfterLast('.'),
                total = list.size,
                nightCount = night,
                maxBurstPerHour = hourBuckets.values.maxOrNull() ?: 0,
                usageMinutes = (usageMs[pkg] ?: 0L) / 60_000,
                dailyCounts = daily.toList(),
            )
        }.sortedByDescending { it.total }

        val disturbing = stats.filter { s ->
            (s.total >= 10 && s.nightRatio > 0.3f) || s.maxBurstPerHour >= 5
        }.sortedByDescending { it.nightCount + it.maxBurstPerHour }

        val distracting = stats.filter { s ->
            s.total >= 15 && s.usageMinutes < 30 && s.distractionScore > 1f
        }.sortedByDescending { it.distractionScore }

        return Report(
            totalNotifications = events.size,
            appStats = stats,
            mostTalkative = stats.take(10),
            disturbing = disturbing.take(10),
            distracting = distracting.take(10),
        )
    }
}

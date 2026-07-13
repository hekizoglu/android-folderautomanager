package com.armutlu.apporganizer.domain.usecase.pulse

import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pulse içgörü motoru — saf Kotlin. Ana ekranda AYNI ANDA yalnızca BİR içgörü gösterilir;
 * seçim [pickInsight] ile yapılır (son gösterilen id atlanarak dönüşümlü gösterim).
 *
 * İlkeler:
 * - Veri yoksa UYDURMA içgörü üretilmez (boş liste dönebilir).
 * - Yargılayıcı dil yok — metinler UI'da resource'tan çözülür, burada yalnızca tür+arg üretilir.
 * - Öncelik: bildirim sorunu > olumlu gelişme > kullanılmayan uygulama > kategori değişimi
 *   > kilit açma trendi > düzen başarısı > genel bilgi.
 */
object PulseInsightEngine {

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val UNUSED_THRESHOLD_DAYS = 60

    // Öncelik sabitleri (küçük = önce)
    const val PRIORITY_NOTIF_ISSUE = 0
    const val PRIORITY_NOTIF_CALM = 1
    const val PRIORITY_UNUSED = 2
    const val PRIORITY_CATEGORY_SHIFT = 3
    const val PRIORITY_UNLOCK_TREND = 4
    const val PRIORITY_ORGANIZED = 5
    const val PRIORITY_GENERAL = 6

    /**
     * Sinyallerden öncelik sıralı içgörü listesi üretir. Liste boş olabilir (veri yoksa).
     */
    fun generate(input: PulseInput, score: DigitalPulseScore): List<PulseInsightSpec> {
        val apps = input.apps.filter { !it.isHidden }
        val specs = mutableListOf<PulseInsightSpec>()

        // 1) Ciddi bildirim sorunu
        val notif = input.notification
        if (notif != null && notif.disturbingCount + notif.distractingCount >= 3) {
            specs += PulseInsightSpec(
                id = "notif_issue",
                type = PulseInsightType.NOTIF_ISSUE,
                priority = PRIORITY_NOTIF_ISSUE,
                positive = false,
                routeKey = "NOTIFICATION_REPORT",
                intArg = notif.disturbingCount + notif.distractingCount,
            )
        }

        // 2) Belirgin olumlu gelişme — bildirim yükü sakin
        if (notif != null && notif.totalNotifications > 0 &&
            notif.disturbingCount == 0 && notif.distractingCount == 0
        ) {
            specs += PulseInsightSpec(
                id = "notif_calm",
                type = PulseInsightType.NOTIF_CALM,
                priority = PRIORITY_NOTIF_CALM,
                positive = true,
                routeKey = "NOTIFICATION_REPORT",
            )
        }

        // 3) Kullanılmayan uygulama önerisi (sistem ve yeni kurulanlar hariç)
        val unusedCount = apps.count { app ->
            if (app.isSystemApp) return@count false
            val last = if (app.lastUsedTimestamp > 0L) app.lastUsedTimestamp else app.installTime
            last > 0L && (input.nowMillis - last) >= UNUSED_THRESHOLD_DAYS * DAY_MS
        }
        if (unusedCount >= 5) {
            specs += PulseInsightSpec(
                id = "unused_apps",
                type = PulseInsightType.UNUSED_APPS,
                priority = PRIORITY_UNUSED,
                positive = false,
                routeKey = "USAGE_REPORT",
                intArg = unusedCount,
            )
        }

        // 4) Haftalık kategori değişimi — en çok büyüyen kategori (baseline varsa)
        val prev = input.previousCategoryUsage
        if (prev != null && prev.isNotEmpty()) {
            val currentUsage = apps.groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.usageCount } }
            val topGrowth = currentUsage.mapNotNull { (cat, curr) ->
                val p = prev[cat] ?: return@mapNotNull null
                if (p <= 0L) return@mapNotNull null
                val pct = (((curr - p).toDouble() / p) * 100).roundToInt()
                cat to pct
            }.maxByOrNull { it.second }
            if (topGrowth != null && topGrowth.second >= 30) {
                specs += PulseInsightSpec(
                    id = "category_shift_${topGrowth.first}",
                    type = PulseInsightType.CATEGORY_SHIFT,
                    priority = PRIORITY_CATEGORY_SHIFT,
                    positive = null,
                    routeKey = "WRAPPED_REPORT",
                    intArg = topGrowth.second,
                    textArg = topGrowth.first, // categoryId — UI görünen ada çevirir
                )
            }
        }

        // 5) Kilit açma trendi
        val unlock = input.unlockCount
        val prevUnlock = input.previousUnlockCount
        if (unlock != null && prevUnlock != null && prevUnlock > 0) {
            val deltaPct = ((unlock - prevUnlock) * 100.0 / prevUnlock).roundToInt()
            if (abs(deltaPct) >= 15) {
                specs += PulseInsightSpec(
                    id = "unlock_trend",
                    type = PulseInsightType.UNLOCK_TREND,
                    priority = PRIORITY_UNLOCK_TREND,
                    positive = deltaPct < 0,
                    routeKey = "WRAPPED_REPORT",
                    intArg = abs(deltaPct),
                )
            }
        }

        // 6) Düzen başarısı
        if (apps.isNotEmpty() && score.organization >= 85) {
            specs += PulseInsightSpec(
                id = "organized_well",
                type = PulseInsightType.ORGANIZED_WELL,
                priority = PRIORITY_ORGANIZED,
                positive = true,
                routeKey = "REPORTS_CENTER",
            )
        }

        // 7) Genel bilgi — yalnızca gerçek veri varken (uydurma mesaj yasak)
        if (specs.isEmpty() && apps.isNotEmpty()) {
            specs += PulseInsightSpec(
                id = "general_report_ready",
                type = PulseInsightType.GENERAL,
                priority = PRIORITY_GENERAL,
                positive = null,
                routeKey = "WRAPPED_REPORT",
            )
        }

        return specs.sortedBy { it.priority }
    }

    /**
     * Gösterilecek TEK içgörüyü seçer — son gösterilen id atlanır (dönüşümlü gösterim),
     * tek aday varsa o gösterilir.
     */
    fun pickInsight(specs: List<PulseInsightSpec>, lastShownId: String?): PulseInsightSpec? {
        if (specs.isEmpty()) return null
        if (specs.size == 1) return specs.first()
        return specs.firstOrNull { it.id != lastShownId } ?: specs.first()
    }
}

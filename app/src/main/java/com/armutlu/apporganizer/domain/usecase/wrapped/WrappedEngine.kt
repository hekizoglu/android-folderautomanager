package com.armutlu.apporganizer.domain.usecase.wrapped

import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseEngine
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInput
import com.armutlu.apporganizer.domain.usecase.pulse.PulseNotificationSignals
import kotlin.math.roundToInt

/**
 * Haftalık Rapor ("Wrapped") motoru — saf Kotlin, Android bağımlılığı yok, unit test edilebilir.
 * Tüm hesaplar AppInfo/NotificationEvent tabanlı hafif snapshot'lardan türetilir; hiçbir veri
 * sunucuya gitmez, uydurma metrik gösterilmez (veri yoksa ilgili bölüm null/boş döner).
 *
 * Skor: V2'den itibaren TEK motor [DigitalPulseEngine] kullanılır — ana ekran (Pulse Clock),
 * Rapor Merkezi ve bu rapor farklı skor hesaplamaz.
 */
object WrappedEngine {

    // ── Girdi modelleri (Android bağımlılığı yok) ──────────────────────────

    /** AppInfo'dan map'lenen hafif snapshot. */
    data class AppSnapshot(
        val packageName: String,
        val appName: String,
        val categoryId: String,
        val usageCount: Long,
        val lastUsedTimestamp: Long,
        val installTime: Long,
        val firstInstalledTime: Long,
        val appSizeBytes: Long,
        val isHidden: Boolean,
        val isSystemApp: Boolean,
    )

    /** Bildirim özeti — NotificationAnalyzer.Report'tan türetilir, içerik taşımaz. */
    data class NotificationSummary(
        val totalNotifications: Int,
        val disturbingCount: Int,
        val distractingCount: Int,
        val nightCount: Int = 0,
    )

    /** Geçen haftanın kategori bazlı kullanım agregatı — WrappedSnapshotPrefs'ten okunur. */
    data class PreviousSnapshot(
        val categoryUsage: Map<String, Long>,
        val totalApps: Int,
        val savedAtEpochDay: Long,
    )

    data class WrappedInput(
        val apps: List<AppSnapshot>,
        val notificationSummary: NotificationSummary?,
        val previousSnapshot: PreviousSnapshot?,
        val folderCount: Int = 0,
        val launcherInstalledDays: Int = 0,
        val nowMillis: Long = System.currentTimeMillis(),
        val unlockCount: Int? = null,
        val previousUnlockCount: Int? = null,
        val hasUsageAccess: Boolean = true,
    )

    // ── Çıktı modelleri ─────────────────────────────────────────────────────

    data class ScoreReason(val label: String, val delta: Int)

    data class DigitalLifeScore(
        val score: Int,       // 0-100
        val reasons: List<ScoreReason>,
    )

    enum class PersonalityType(val emoji: String, val label: String) {
        PRODUCER("🎯", "Uretici"),
        SOCIAL_BUTTERFLY("📱", "Sosyal Kelebek"),
        GAMER("🎮", "Oyuncu"),
        FINANCE_WOLF("💰", "Finans Kurdu"),
        STUDENT("📚", "Ogrenci"),
        BALANCED("⚖️", "Dengeli"),
    }

    data class PersonalityResult(
        val type: PersonalityType,
        val categoryPercentages: Map<String, Int>, // categoryId -> yuzde (0-100)
        val dominantPercentage: Int,
    )

    data class InterestingStats(
        val mostOpenedApp: AppSnapshot?,
        val leastOpenedApp: AppSnapshot?,   // usageCount > 0 olanlar icinde min
        val largestApp: AppSnapshot?,
        val oldestInstalledApp: AppSnapshot?,
        val newestInstalledApp: AppSnapshot?,
        val longestUnusedApp: AppSnapshot?,
    )

    data class Badge(
        val id: String,
        val emoji: String,
        val title: String,
        val criteriaDescription: String,
        val earned: Boolean,
    )

    data class CategoryGrowth(
        val categoryId: String,
        val deltaPercent: Int, // pozitif = büyüme, negatif = azalma
    )

    // NOT: previousScore alanı KALDIRILDI (D244 bug fix) — skor karşılaştırması
    // WrappedSnapshotPrefs → ViewModel.previousScore akışıyla taşınır; engine hep null
    // döndürdüğü için UI'da "geçen haftaya göre" rozeti hiç görünmüyordu.
    data class WeeklyComparison(
        val topGrowingCategories: List<CategoryGrowth>, // en cok buyuyen 3
    )

    data class WrappedReport(
        val score: DigitalLifeScore,
        val personality: PersonalityResult,
        val stats: InterestingStats,
        val badges: List<Badge>,
        val weeklyComparison: WeeklyComparison?, // null = "veri birikiyor"
        val pulse: DigitalPulseScore, // V2 alt skorlar + confidence — tek motor
    )

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val UNUSED_THRESHOLD_DAYS = 60

    // Kişilik profillerinde kullanılan kategori gruplamaları — Category.kt sabitleriyle uyumlu.
    private val SOCIAL_CATEGORIES = setOf("social", "communication", "dating")
    private val GAME_CATEGORIES = setOf("games")
    private val FINANCE_CATEGORIES = setOf("finance", "business")
    private val PRODUCTIVITY_CATEGORIES = setOf("productivity", "utilities")
    private val EDUCATION_CATEGORIES = setOf("education", "books")

    fun compute(input: WrappedInput): WrappedReport {
        val relevantApps = input.apps.filter { !it.isHidden }
        val pulse = DigitalPulseEngine.compute(
            PulseInput(
                apps = relevantApps,
                notification = input.notificationSummary?.let {
                    PulseNotificationSignals(
                        totalNotifications = it.totalNotifications,
                        disturbingCount = it.disturbingCount,
                        distractingCount = it.distractingCount,
                        nightCount = it.nightCount,
                    )
                },
                previousCategoryUsage = input.previousSnapshot?.categoryUsage,
                folderCount = input.folderCount,
                unlockCount = input.unlockCount,
                previousUnlockCount = input.previousUnlockCount,
                hasUsageAccess = input.hasUsageAccess,
                nowMillis = input.nowMillis,
            )
        )
        // Geriye uyumluluk: DigitalLifeScore.reasons AI Coach prompt'u için ASCII log
        // etiketleriyle doldurulur (kullanıcıya gösterilmez — UI pulse.reasons'ı resource ile çözer).
        val score = DigitalLifeScore(
            score = pulse.total,
            reasons = pulse.reasons.map { ScoreReason(it.id.logLabel, it.delta) },
        )
        val personality = computePersonality(relevantApps)
        val stats = computeInterestingStats(relevantApps, input.nowMillis)
        val badges = computeBadges(relevantApps, personality, input)
        val comparison = computeWeeklyComparison(relevantApps, input.previousSnapshot)
        return WrappedReport(
            score = score,
            personality = personality,
            stats = stats,
            badges = badges,
            weeklyComparison = comparison,
            pulse = pulse,
        )
    }

    // ── a) Dijital Yaşam Skoru ──────────────────────────────────────────────
    // V2: skor hesabı DigitalPulseEngine'e taşındı (tek motor kuralı) — sosyal/oyun
    // kullanımına otomatik ceza veren V1 computeScore KALDIRILDI.

    // ── b) Kişilik tipi ──────────────────────────────────────────────────────

    private fun computePersonality(apps: List<AppSnapshot>): PersonalityResult {
        val totalUsage = apps.sumOf { it.usageCount }
        if (totalUsage <= 0L) {
            return PersonalityResult(PersonalityType.BALANCED, emptyMap(), 0)
        }

        fun ratioOf(categories: Set<String>): Int =
            ((apps.filter { it.categoryId in categories }.sumOf { it.usageCount }.toDouble() / totalUsage) * 100).roundToInt()

        val percentages = mapOf(
            "productivity" to ratioOf(PRODUCTIVITY_CATEGORIES),
            "social" to ratioOf(SOCIAL_CATEGORIES),
            "games" to ratioOf(GAME_CATEGORIES),
            "finance" to ratioOf(FINANCE_CATEGORIES),
            "education" to ratioOf(EDUCATION_CATEGORIES),
        )

        val (dominantKey, dominantValue) = percentages.entries.maxByOrNull { it.value }
            ?.let { it.key to it.value } ?: ("balanced" to 0)

        val type = if (dominantValue < 30) {
            PersonalityType.BALANCED
        } else when (dominantKey) {
            "productivity" -> PersonalityType.PRODUCER
            "social" -> PersonalityType.SOCIAL_BUTTERFLY
            "games" -> PersonalityType.GAMER
            "finance" -> PersonalityType.FINANCE_WOLF
            "education" -> PersonalityType.STUDENT
            else -> PersonalityType.BALANCED
        }

        return PersonalityResult(
            type = type,
            categoryPercentages = percentages,
            dominantPercentage = dominantValue,
        )
    }

    // ── c) İlginç istatistikler ──────────────────────────────────────────────

    private fun computeInterestingStats(apps: List<AppSnapshot>, now: Long): InterestingStats {
        val mostOpened = apps.maxByOrNull { it.usageCount }
        val leastOpened = apps.filter { it.usageCount > 0 }.minByOrNull { it.usageCount }
        val largest = apps.filter { it.appSizeBytes > 0 }.maxByOrNull { it.appSizeBytes }

        fun installEpoch(a: AppSnapshot): Long =
            if (a.firstInstalledTime > 0L) a.firstInstalledTime else a.installTime

        val oldest = apps.filter { installEpoch(it) > 0L }.minByOrNull { installEpoch(it) }
        val newest = apps.filter { installEpoch(it) > 0L }.maxByOrNull { installEpoch(it) }

        val longestUnused = apps.filter { it.lastUsedTimestamp > 0L }
            .minByOrNull { it.lastUsedTimestamp }

        return InterestingStats(
            mostOpenedApp = mostOpened?.takeIf { it.usageCount > 0 },
            leastOpenedApp = leastOpened,
            largestApp = largest,
            oldestInstalledApp = oldest,
            newestInstalledApp = newest,
            longestUnusedApp = longestUnused,
        )
    }

    // ── d) Rozetler ──────────────────────────────────────────────────────────

    private fun computeBadges(
        apps: List<AppSnapshot>,
        personality: PersonalityResult,
        input: WrappedInput,
    ): List<Badge> {
        val now = input.nowMillis
        val uncategorizedCount = apps.count { it.categoryId == "uncategorized" || it.categoryId == "other" }
        val unusedLongCount = apps.count { app ->
            val last = if (app.lastUsedTimestamp > 0L) app.lastUsedTimestamp else app.installTime
            last > 0L && (now - last) >= UNUSED_THRESHOLD_DAYS * DAY_MS
        }
        val newAppsLast7Days = apps.count { app ->
            val installed = if (app.firstInstalledTime > 0L) app.firstInstalledTime else app.installTime
            installed > 0L && (now - installed) <= 7 * DAY_MS
        }
        val socialRatio = personality.categoryPercentages["social"] ?: 0
        val avgSizeMb = if (apps.isNotEmpty()) {
            (apps.sumOf { it.appSizeBytes }.toDouble() / apps.size) / (1024.0 * 1024.0)
        } else 0.0

        return listOf(
            Badge(
                id = "organizer",
                emoji = "🗂️",
                title = "Duzen Ustasi",
                criteriaDescription = "Kategorisiz uygulama sayisi 0",
                earned = apps.isNotEmpty() && uncategorizedCount == 0,
            ),
            Badge(
                id = "minimalist",
                emoji = "🧘",
                title = "Dijital Minimalist",
                criteriaDescription = "60+ gundur acilmamis uygulama sayisi 3'ten az",
                earned = unusedLongCount < 3,
            ),
            Badge(
                id = "explorer",
                emoji = "🔭",
                title = "Kasif",
                criteriaDescription = "Son 7 gunde yeni uygulama yuklendi",
                earned = newAppsLast7Days > 0,
            ),
            Badge(
                id = "social_detox",
                emoji = "🌿",
                title = "Sosyal Detoks",
                criteriaDescription = "Sosyal kategori kullanim payi %15'in altinda",
                earned = socialRatio < 15,
            ),
            Badge(
                id = "loyal_user",
                emoji = "🏠",
                title = "Sadik Kullanici",
                criteriaDescription = "Launcher 30+ gundur kurulu",
                earned = input.launcherInstalledDays >= 30,
            ),
            Badge(
                id = "folder_collector",
                emoji = "📚",
                title = "Klasor Koleksiyoncusu",
                criteriaDescription = "8 veya daha fazla dolu klasor",
                earned = input.folderCount >= 8,
            ),
            Badge(
                id = "memory_friendly",
                emoji = "💾",
                title = "Hafiza Dostu",
                criteriaDescription = "Ortalama uygulama boyutu 50MB'in altinda",
                earned = apps.any { it.appSizeBytes > 0 } && avgSizeMb < 50.0,
            ),
        )
        // Not: "Gece Kuşu / Erken Kalkan" rozeti getWeightedScores timeSlot verisinden
        // guvenilir turetilemedigi icin (bu katmanda erisilebilir degil) BILEREK eklenmedi —
        // uydurma metrik gosterme kurali geregi.
    }

    // ── e) Haftalık karşılaştırma ────────────────────────────────────────────

    private fun computeWeeklyComparison(
        apps: List<AppSnapshot>,
        previous: PreviousSnapshot?,
    ): WeeklyComparison? {
        if (previous == null) return null

        val currentUsage = apps.groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.usageCount } }

        val deltas = currentUsage.keys.union(previous.categoryUsage.keys).mapNotNull { catId ->
            val curr = currentUsage[catId] ?: 0L
            val prev = previous.categoryUsage[catId] ?: 0L
            if (prev <= 0L) {
                if (curr > 0L) CategoryGrowth(catId, 100) else null
            } else {
                val pct = (((curr - prev).toDouble() / prev) * 100).roundToInt()
                CategoryGrowth(catId, pct)
            }
        }

        val topGrowing = deltas.sortedByDescending { it.deltaPercent }.take(3)

        return WeeklyComparison(topGrowingCategories = topGrowing)
    }
}

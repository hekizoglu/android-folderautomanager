package com.armutlu.apporganizer.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Ana ekran haber şeridi (ticker) icin klasör anlik goruntusu.
 * LauncherViewModel.AppFolder'dan bagimsiz — pure Kotlin, unit test edilebilir.
 */
data class FolderSnapshot(
    val categoryId: String,
    val categoryName: String,
    val emoji: String,
    val appCount: Int,
)

/**
 * Tek bir uygulamanin ticker icin gerekli minimum alanlari.
 * AppInfo'dan turetilir (packageName, appName, usageCount, lastUsedTimestamp).
 */
data class AppSnapshot(
    val packageName: String,
    val appName: String,
    val usageCount: Long,
    val lastUsedTimestamp: Long,
)

/** InsightCard'dan turetilen hafif ozet — categoryId varsa klasore, yoksa Dashboard'a yonlendirilir. */
data class InsightSnapshot(
    val id: String,
    val message: String,
    val categoryId: String? = null,
    val packageName: String? = null,
)

/**
 * TickerComposer'in urettigi tek haber — LauncherViewModel bunu TickerItem'a map eder.
 * routeKey: "DASHBOARD" | "NOTIFICATION_REPORT" | "APP_LIST" | "SETTINGS" | "WRAPPED_REPORT" | null — Routes sabitlerine
 * ViewModel tarafinda baglanir (bu dosya Routes'a bagimli degil).
 */
data class TickerSpec(
    val text: String,
    val emoji: String,
    val categoryId: String? = null,
    val routeKey: String? = null,
    val packageName: String? = null,
    val suggestionKey: String? = null,
    /** Karistirma sirasinda ilk sirada tutulmasi gereken "taze" sinyaller icin (bildirim, selamlama). */
    val priority: Int = 0,
)

/**
 * Ana ekran haber seridi icin cesitli, gunluk rotasyonlu haber metinleri uretir.
 *
 * Tasarim ilkeleri:
 * - Ayni gun icinde ayni haber icin ayni sablon secilir (deterministik seed), ertesi gun degisir.
 * - Seed = LocalDate.toEpochDay() + item'a ozgu hash — farkli haberler ayni gun farkli sablonlarla eslesir.
 * - Sonuc listesi gunluk seed'li Random ile karistirilir; ilk eleman en "taze" sinyal (bildirim > selamlama > digerleri).
 */
object TickerComposer {

    private const val FORGOTTEN_APP_THRESHOLD_DAYS = 45L
    private const val MS_PER_DAY = 24L * 3600 * 1000

    // ---- Sablon havuzlari ----

    private val folderTemplates: List<(FolderSnapshot) -> String> = listOf(
        { f -> "${f.categoryName} klasöründe ${f.appCount} uygulama var" },
        { f -> "${f.categoryName} tarafında ${f.appCount} uygulama seni bekliyor" },
        { f -> "En kalabalık köşen: ${f.categoryName} (${f.appCount} uygulama)" },
        { f -> "${f.categoryName} klasörü ${f.appCount} uygulamayla dolu" },
    )

    private val forgottenAppTemplates: List<(String, Long) -> String> = listOf(
        { app, days -> "$app uygulamasını $days gündür açmadın — hâlâ gerekli mi?" },
        { app, days -> "$app son $days gündür sessiz — silmeyi düşünür müsün?" },
        { app, days -> "$days gündür dokunmadığın bir uygulama: $app" },
    )

    private val championTemplates: List<(String) -> String> = listOf(
        { app -> "Bu aralar favorin: $app" },
        { app -> "En çok $app uygulamasını kullanıyorsun" },
        { app -> "Gözde uygulaman belli oldu: $app" },
    )

    /** İpucu havuzu — statik, 6+ madde, gunluk rotasyon. routeKey hepsinde null (SETTINGS istenirse ViewModel eslestirebilir). */
    private val tips: List<TickerSpec> = listOf(
        TickerSpec("İpucu: Klasöre uzun basarak yeniden adlandırabilirsin", "💡", routeKey = "SETTINGS_APPEARANCE"),
        TickerSpec("Arama çubuğuna 2 harf yaz — kişilerini de bulur", "🔍", routeKey = "SEARCH_SETTINGS"),
        TickerSpec("Dock'a 5 uygulama veya klasor sabitleyebilirsin", "📌", routeKey = "SETTINGS_LAUNCHER"),
        TickerSpec("Bildirim rozetlerini ayarlardan kapatabilirsin", "🔔", routeKey = "SETTINGS_NOTIFICATIONS"),
        TickerSpec("Klasör rengini ve emojisini özelleştirebilirsin", "🎨", routeKey = "SETTINGS_APPEARANCE"),
        TickerSpec("Sık kullandığın uygulamalar dock'a otomatik önerilir", "⚡", routeKey = "SETTINGS_LAUNCHER"),
        TickerSpec("Uygulamayı sürükleyip başka bir klasöre taşıyabilirsin", "📁", routeKey = "APP_LIST"),
    )

    private val morningTemplates: List<String> = listOf(
        "Günaydın! Güne hazır mısın?",
        "Yeni bir gün başladı — klasörlerine göz at",
        "Günaydın! Bugün seni neler bekliyor?",
    )
    private val afternoonTemplates: List<String> = listOf(
        "İyi öğlenler! Kısa bir mola zamanı",
        "Gün ortası — en çok kullandığın uygulamalar hazır",
        "Öğle arası bir göz atmaya ne dersin?",
    )
    private val eveningTemplates: List<String> = listOf(
        "İyi akşamlar! Günü nasıl geçirdin?",
        "Akşam oldu — bugünün özetine bakmak ister misin?",
        "Günün yorgunluğunu at, bir göz gezdir",
    )
    private val nightTemplates: List<String> = listOf(
        "İyi geceler! Telefonu bırakma vakti gelmedi mi?",
        "Gece geç oldu — yarın için erken kalkmayı unutma",
        "Sessiz bir gece — son bir tur mu atıyorsun?",
    )

    private val weeklyTemplates: List<(Int, String, Int) -> String> = listOf(
        { total, biggest, count -> "Haftalık özet: $total uygulaman var, en büyük klasör $biggest ($count uygulama)" },
        { total, biggest, count -> "Bu hafta: toplam $total uygulama, lider klasör $biggest ($count uygulama)" },
        { total, biggest, count -> "Haftaya bakış: $total uygulama kayıtlı, $biggest klasörü zirvede ($count uygulama)" },
    )

    /** Dijital yaşam skoru sablonlari — arrow parametresi trend oku ("" | " ↑" | " ↓" | " →"). */
    private val scoreTemplates: List<(Int, String) -> String> = listOf(
        { score, arrow -> "Dijital yaşam skorun: $score/100$arrow" },
        { score, arrow -> "Bu haftaki dijital skorun: $score/100$arrow — rapora dokun" },
        { score, arrow -> "Dijital denge puanın: $score/100$arrow" },
    )

    // Dijital yasam skoru turetimi icin kategori gruplari (WrappedEngine.computeScore ile ayni ruh).
    private const val SCORE_UNUSED_THRESHOLD_DAYS = 60L
    private val SCORE_SOCIAL_GAME_CATEGORIES = setOf("social", "communication", "dating", "games")
    private val SCORE_UNCATEGORIZED = setOf("other", "uncategorized")

    /**
     * Ana ekran ticker'i icin hafif "Dijital Yaşam Skoru" (0-100) — GERCEK sinyallerden turetilir,
     * uydurma metrik yok. WrappedEngine.computeScore erisilemedigi (private) icin ticker'in elindeki
     * hafif snapshot'lardan (folders: kategori dagilimi + appCount, apps: lastUsedTimestamp) hesaplanir.
     *
     * Girdi yoksa (apps bos) null doner — bu durumda skor haberi hic gosterilmez.
     * @return 0-100 arasi skor, veya yeterli veri yoksa null.
     */
    fun computeDigitalLifeScore(
        folders: List<FolderSnapshot>,
        apps: List<AppSnapshot>,
        nowMillis: Long,
    ): Int? {
        if (apps.isEmpty()) return null
        var total = 50 // notr baslangic (WrappedEngine ile ayni)

        // Sinyal A — kullanilmayan oran (yalnizca lastUsedTimestamp'i olan uygulamalar, gercek veri)
        val usedApps = apps.filter { it.lastUsedTimestamp > 0L }
        if (usedApps.isNotEmpty()) {
            val cutoff = SCORE_UNUSED_THRESHOLD_DAYS * MS_PER_DAY
            val unusedCount = usedApps.count { (nowMillis - it.lastUsedTimestamp) >= cutoff }
            val unusedRatio = unusedCount.toFloat() / usedApps.size
            if (unusedRatio < 0.15f) total += 15
            else if (unusedRatio > 0.4f) total -= 15
        }

        val totalApps = folders.sumOf { it.appCount }
        if (totalApps > 0) {
            // Sinyal B — sosyal/oyun yogunlugu (kategori dagilimi)
            val socialGameApps = folders.filter { it.categoryId in SCORE_SOCIAL_GAME_CATEGORIES }.sumOf { it.appCount }
            val socialGameRatio = socialGameApps.toFloat() / totalApps
            if (socialGameRatio > 0.5f) total -= 15
            else if (socialGameRatio < 0.2f) total += 10

            // Sinyal C — kategorileme duzeni
            val categorizedApps = folders.filter { it.categoryId !in SCORE_UNCATEGORIZED }.sumOf { it.appCount }
            val categorizedRatio = categorizedApps.toFloat() / totalApps
            if (categorizedRatio > 0.85f) total += 10
            else if (categorizedRatio < 0.5f) total -= 5
        }

        return total.coerceIn(0, 100)
    }

    // ---- Yardimci: gunluk seed'li deterministik secim ----

    /** Ayni gun + ayni item key -> ayni indeks. Ertesi gun degisir. */
    private fun pickIndex(daySeed: Long, itemKey: String, poolSize: Int): Int {
        if (poolSize <= 0) return 0
        val combined = daySeed * 31 + itemKey.hashCode()
        // Negatif olabilecek hash'i pozitif araliga cek.
        val normalized = (combined xor (combined ushr 32)) and 0x7FFFFFFF
        return (normalized % poolSize).toInt()
    }

    /**
     * Ana kompozisyon fonksiyonu.
     * @param nowMillis test edilebilirlik icin disaridan verilir (System.currentTimeMillis() yerine).
     */
    fun compose(
        folders: List<FolderSnapshot>,
        apps: List<AppSnapshot>,
        badgeTotal: Int,
        insights: List<InsightSnapshot>,
        lowConfidenceCount: Int,
        nowMillis: Long,
        /** Onceden hesaplanmis dijital yasam skoru (0-100) — null ise skor haberi eklenmez. */
        digitalLifeScore: Int? = null,
        /** Bir onceki gunun skoru — trend oku (↑/↓/→) icin. null ise ok gosterilmez. */
        digitalLifeScorePrevious: Int? = null,
        epochDay: Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay(),
        zone: ZoneId = ZoneId.systemDefault(),
        random: Random = Random(epochDay),
    ): List<TickerSpec> {
        val daySeed = epochDay
        val specs = mutableListOf<TickerSpec>()

        // 1) Bildirim ozeti — en yuksek oncelik (taze sinyal)
        if (badgeTotal > 0) {
            specs.add(
                TickerSpec(
                    text = "$badgeTotal aktif bildirim — analiz raporu için dokun",
                    emoji = "🔔",
                    routeKey = "NOTIFICATION_REPORT",
                    suggestionKey = "notification_summary",
                    priority = 100,
                )
            )
        }

        // 2) Saat bazli selamlama + baglamsal klasor onerisi
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val hour = zdt.hour
        val (greetingPool, contextCategoryHints) = when {
            hour in 5..10 -> morningTemplates to listOf("news", "finance", "productivity")
            hour in 11..16 -> afternoonTemplates to listOf("productivity", "communication")
            hour in 17..21 -> eveningTemplates to listOf("social", "entertainment")
            else -> nightTemplates to listOf("entertainment", "social")
        }
        val greetingKey = "greeting_$hour"
        var greetingText = greetingPool[pickIndex(daySeed, greetingKey, greetingPool.size)]
        val hintFolder = folders.firstOrNull { f -> contextCategoryHints.any { hint -> f.categoryId.contains(hint, ignoreCase = true) } }
        if (hintFolder != null) {
            greetingText = "$greetingText ${hintFolder.emoji} ${hintFolder.categoryName} klasörüne göz at"
        }
        val greetingEmoji = when {
            hour in 5..10 -> "☀️"
            hour in 11..16 -> "🌤️"
            hour in 17..21 -> "🌇"
            else -> "🌙"
        }
        specs.add(
            TickerSpec(
                text = greetingText,
                emoji = greetingEmoji,
                categoryId = hintFolder?.categoryId,
                routeKey = if (hintFolder == null) "REPORTS_CENTER" else null,
                priority = 90,
            )
        )

        // 3) Unutulan uygulamalar (45+ gun acilmamis)
        val forgottenCutoff = nowMillis - FORGOTTEN_APP_THRESHOLD_DAYS * MS_PER_DAY
        apps.filter { it.lastUsedTimestamp in 1 until forgottenCutoff }
            .sortedBy { it.lastUsedTimestamp }
            .take(3)
            .forEach { app ->
                val daysSince = (nowMillis - app.lastUsedTimestamp) / MS_PER_DAY
                val pool = forgottenAppTemplates
                val idx = pickIndex(daySeed, "forgotten_${app.packageName}", pool.size)
                specs.add(
                    TickerSpec(
                        text = pool[idx](app.appName, daysSince),
                        emoji = "🕰️",
                        packageName = app.packageName,
                        priority = 40,
                    )
                )
            }

        // 4) Gunun sampiyonu — en yuksek usageCount/lastUsed
        apps.filter { it.usageCount > 0 }
            .maxByOrNull { it.usageCount }
            ?.let { champion ->
                val pool = championTemplates
                val idx = pickIndex(daySeed, "champion_${champion.packageName}", pool.size)
                specs.add(
                    TickerSpec(
                        text = pool[idx](champion.appName),
                        emoji = "🏆",
                        packageName = champion.packageName,
                        priority = 50,
                    )
                )
            }

        // 5) Klasor istatistikleri — en kalabalik 5 klasor, sablon rotasyonlu
        folders.sortedByDescending { it.appCount }.take(5).forEach { f ->
            val pool = folderTemplates
            val idx = pickIndex(daySeed, "folder_${f.categoryId}", pool.size)
            specs.add(
                TickerSpec(
                    text = pool[idx](f),
                    emoji = f.emoji.ifBlank { "📁" },
                    categoryId = f.categoryId,
                    priority = 20,
                )
            )
        }

        // 6) Icgoru kartlari (InsightEngine'den gelen)
        insights.forEach { insight ->
            specs.add(
                TickerSpec(
                    text = insight.message,
                    emoji = "💡",
                    categoryId = insight.categoryId,
                    packageName = insight.packageName,
                    routeKey = if (insight.categoryId == null && insight.packageName == null) "DASHBOARD" else null,
                    suggestionKey = insight.id,
                    priority = 30,
                )
            )
        }

        // 7) Dusuk guvenli otomatik kategorileme uyarisi
        if (lowConfidenceCount > 0) {
            specs.add(
                TickerSpec(
                    text = "$lowConfidenceCount uygulamanın kategorisi belirsiz — gözden geçirmek ister misin?",
                    emoji = "🤔",
                    routeKey = "APP_LIST_UNCERTAIN",
                    suggestionKey = "low_confidence_review",
                    priority = 35,
                )
            )
        }

        // 8) Ozellik kesif ipucu — statik havuzdan gunluk rotasyon
        val tipIdx = pickIndex(daySeed, "tip_of_day", tips.size)
        specs.add(tips[tipIdx].copy(priority = 5))

        // 9) Haftalik ozet — sadece pazartesi (dayOfWeek == 1)
        if (zdt.dayOfWeek.value == 1 && folders.isNotEmpty()) {
            val totalApps = folders.sumOf { it.appCount }
            val biggest = folders.maxByOrNull { it.appCount }
            if (biggest != null) {
                val pool = weeklyTemplates
                val idx = pickIndex(daySeed, "weekly_summary", pool.size)
                specs.add(
                    TickerSpec(
                        text = pool[idx](totalApps, biggest.categoryName, biggest.appCount),
                        emoji = "📊",
                        routeKey = "REPORTS_CENTER",
                        priority = 60,
                    )
                )
            }
        }

        // 10) Dijital yasam skoru — ara sira (deterministik gun kapisi ile ~3 gunde 1), gununde 1 kez.
        // Skor ViewModel'de gercek sinyallerden hesaplanip verilir; burada sadece haber olarak eklenir.
        if (digitalLifeScore != null && pickIndex(daySeed, "score_gate", 3) == 0) {
            val arrow = when {
                digitalLifeScorePrevious == null -> ""
                digitalLifeScore > digitalLifeScorePrevious -> " ↑"
                digitalLifeScore < digitalLifeScorePrevious -> " ↓"
                else -> " →"
            }
            val pool = scoreTemplates
            val idx = pickIndex(daySeed, "digital_life_score", pool.size)
            specs.add(
                TickerSpec(
                    text = pool[idx](digitalLifeScore, arrow),
                    emoji = "📈",
                    routeKey = "WRAPPED_REPORT",
                    priority = 70,
                )
            )
        }

        // Karistirma: gunluk seed'li Random ile shuffle, sonra oncelige gore stabil sirala
        // (yuksek priority basa gelsin, ayni priority icinde shuffle sirasi korunsun).
        val shuffled = specs.shuffled(random)
        return shuffled.sortedByDescending { it.priority }
    }
}

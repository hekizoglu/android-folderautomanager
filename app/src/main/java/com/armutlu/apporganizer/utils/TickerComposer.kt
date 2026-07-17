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
 * - Sonuc listesi gunluk seed'li Random ile karistirilir; ilk eleman en "taze" sinyal (bildirim > unutulan uygulama > digerleri).
 *
 * Dongu T00 (Akilli Nabiz Seridi P0): dusuk degerli/tekrarli uretici fonksiyonlar kaldirildi —
 * sabah/ogle/aksam/gece selamlamalari, "gunun sampiyonu" ham mesaji, en kalabalik 5 klasorun
 * uygulama sayisi istatistigi. Bu icerikler T01+ dongulerinde SmartTickerItem modeliyle
 * kosullu/degerli formlara donusturulecek (roadmap bolum 8, Dongu T00).
 */
object TickerComposer {

    private const val FORGOTTEN_APP_THRESHOLD_DAYS = 45L
    private const val MS_PER_DAY = 24L * 3600 * 1000

    // ---- Sablon havuzlari ----

    private val forgottenAppTemplates: List<(String, Long) -> String> = listOf(
        { app, days -> "$app uygulamasını $days gündür açmadın — hâlâ gerekli mi?" },
        { app, days -> "$app son $days gündür sessiz — silmeyi düşünür müsün?" },
        { app, days -> "$days gündür dokunmadığın bir uygulama: $app" },
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

    private val weeklyTemplates: List<(Int, String, Int) -> String> = listOf(
        { total, biggest, count -> "Haftalık özet: $total uygulaman var, en büyük klasör $biggest ($count uygulama)" },
        { total, biggest, count -> "Bu hafta: toplam $total uygulama, lider klasör $biggest ($count uygulama)" },
        { total, biggest, count -> "Haftaya bakış: $total uygulama kayıtlı, $biggest klasörü zirvede ($count uygulama)" },
    )

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

        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)

        // 2) Unutulan uygulamalar (45+ gun acilmamis)
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

        // 3) Icgoru kartlari (InsightEngine'den gelen)
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

        // 4) Dusuk guvenli otomatik kategorileme uyarisi
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

        // 5) Ozellik kesif ipucu — statik havuzdan gunluk rotasyon
        val tipIdx = pickIndex(daySeed, "tip_of_day", tips.size)
        specs.add(tips[tipIdx].copy(priority = 5))

        // 6) Haftalik ozet — sadece pazartesi (dayOfWeek == 1)
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

        // Dijital yasam skoru haberi KALDIRILDI (Dongu D00, P0 2.1) — TickerComposer artik
        // kendi skorunu hesaplamiyor. Skor artik yalniz DigitalLifeCard'da (ayri kart,
        // LauncherViewModel.homePulseSummary -> HomeIntelligenceCoordinator -> DigitalPulseRepository)
        // gosteriliyor; ticker'a skor haberi eklemek T donguleri kapsaminda yeniden ele alinacak.

        // Karistirma: gunluk seed'li Random ile shuffle, sonra oncelige gore stabil sirala
        // (yuksek priority basa gelsin, ayni priority icinde shuffle sirasi korunsun).
        val shuffled = specs.shuffled(random)
        return shuffled.sortedByDescending { it.priority }
    }
}

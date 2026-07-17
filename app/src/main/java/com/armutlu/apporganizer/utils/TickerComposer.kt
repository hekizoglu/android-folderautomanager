package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.home.SettingsSection
import com.armutlu.apporganizer.domain.home.SmartTickerItem
import com.armutlu.apporganizer.domain.home.SmartTickerType
import com.armutlu.apporganizer.domain.home.TickerAction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
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
 * Ana ekran haber seridi icin cesitli, gunluk rotasyonlu haber metinleri uretir.
 *
 * Tasarim ilkeleri:
 * - Ayni gun icinde ayni haber icin ayni sablon secilir (deterministik seed), ertesi gun degisir.
 * - Seed = LocalDate.toEpochDay() + item'a ozgu hash — farkli haberler ayni gun farkli sablonlarla eslesir.
 * - Sonuc listesi gunluk seed'li Random ile karistirilir; ilk eleman en "taze" sinyal (bildirim > unutulan uygulama > digerleri).
 *
 * Dongu T00 (Akilli Nabiz Seridi P0): dusuk degerli/tekrarli uretici fonksiyonlar kaldirildi —
 * sabah/ogle/aksam/gece selamlamalari, "gunun sampiyonu" ham mesaji, en kalabalik 5 klasorun
 * uygulama sayisi istatistigi.
 *
 * Dongu T01 (roadmap §3.3): cikti tipi metin+priority'den ibaret TickerSpec yerine davranis
 * tasiyan [SmartTickerItem] oldu — her ogenin turu ([SmartTickerType]), son gecerlilik zamani
 * (expiresAt) ve dokunma eylemi ([TickerAction]) acikca modellenir. LauncherViewModel bu ciktiyi
 * eski UI modeline (`presentation.ui.launcher.TickerItem`) bir kopru fonksiyonuyla esler — T04
 * dongusu HomeTickerRow'u dogrudan SmartTickerItem tuketecek sekilde yeniden yazacak.
 */
object TickerComposer {

    private const val FORGOTTEN_APP_THRESHOLD_DAYS = 45L
    private const val MS_PER_DAY = 24L * 3600 * 1000

    /** Ozellik ipucu her gosterimde 24 saat sonra "bayatlar" — autoAdvance disinda tazelik siniri. */
    private const val TIP_EXPIRY_MS = MS_PER_DAY
    /** Haftalik ozet sadece o gun (pazartesi) icin gecerli. */
    private const val WEEKLY_SUMMARY_EXPIRY_MS = MS_PER_DAY
    /** Bildirim ozeti hizla bayatlar — yeni bildirim gelince zaten yeniden uretilir. */
    private const val NOTIFICATION_EXPIRY_MS = 6L * 3600 * 1000

    // ---- Sablon havuzlari ----

    private val forgottenAppTemplates: List<(String, Long) -> String> = listOf(
        { app, days -> "$app uygulamasını $days gündür açmadın — hâlâ gerekli mi?" },
        { app, days -> "$app son $days gündür sessiz — silmeyi düşünür müsün?" },
        { app, days -> "$days gündür dokunmadığın bir uygulama: $app" },
    )

    private data class TipTemplate(val text: String, val emoji: String, val section: SettingsSection?)

    /** İpucu havuzu — statik, 6+ madde, gunluk rotasyon. section null ise APP_LIST'e yonlendirilir. */
    private val tips: List<TipTemplate> = listOf(
        TipTemplate("İpucu: Klasöre uzun basarak yeniden adlandırabilirsin", "💡", SettingsSection.APPEARANCE),
        TipTemplate("Arama çubuğuna 2 harf yaz — kişilerini de bulur", "🔍", SettingsSection.SEARCH),
        TipTemplate("Dock'a 5 uygulama veya klasor sabitleyebilirsin", "📌", SettingsSection.LAUNCHER),
        TipTemplate("Bildirim rozetlerini ayarlardan kapatabilirsin", "🔔", SettingsSection.NOTIFICATIONS),
        TipTemplate("Klasör rengini ve emojisini özelleştirebilirsin", "🎨", SettingsSection.APPEARANCE),
        TipTemplate("Sık kullandığın uygulamalar dock'a otomatik önerilir", "⚡", SettingsSection.LAUNCHER),
        TipTemplate("Uygulamayı sürükleyip başka bir klasöre taşıyabilirsin", "📁", null),
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
    ): List<SmartTickerItem> {
        val daySeed = epochDay
        val items = mutableListOf<SmartTickerItem>()

        // 1) Bildirim ozeti — en yuksek oncelik (taze sinyal). sensitive=true: bildirim
        // sayisi kilit ekrani/gizlilik senaryolarinda gizlenebilecek bir sinyal tasir.
        if (badgeTotal > 0) {
            items.add(
                SmartTickerItem(
                    id = "notification_summary_$daySeed",
                    type = SmartTickerType.ACTION_REQUIRED,
                    title = "$badgeTotal aktif bildirim",
                    subtitle = "Analiz raporu için dokun",
                    icon = "🔔",
                    priority = 100,
                    createdAt = nowMillis,
                    expiresAt = nowMillis + NOTIFICATION_EXPIRY_MS,
                    action = TickerAction.OpenNotificationReport,
                    suggestionKey = "notification_summary",
                    sensitive = true,
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
                items.add(
                    SmartTickerItem(
                        id = "forgotten_${app.packageName}_$daySeed",
                        type = SmartTickerType.CONTEXTUAL_SUGGESTION,
                        title = pool[idx](app.appName, daysSince),
                        icon = "🕰️",
                        priority = 40,
                        createdAt = nowMillis,
                        action = TickerAction.OpenApp(app.packageName),
                        suggestionKey = "forgotten_${app.packageName}",
                    )
                )
            }

        // 3) Icgoru kartlari (InsightEngine'den gelen)
        insights.forEach { insight ->
            val action = when {
                insight.packageName != null -> TickerAction.OpenApp(insight.packageName)
                insight.categoryId != null -> TickerAction.OpenFolder(insight.categoryId)
                else -> TickerAction.OpenDashboard
            }
            items.add(
                SmartTickerItem(
                    id = "insight_${insight.id}",
                    type = SmartTickerType.CONTEXTUAL_SUGGESTION,
                    title = insight.message,
                    icon = "💡",
                    priority = 30,
                    createdAt = nowMillis,
                    action = action,
                    suggestionKey = insight.id,
                )
            )
        }

        // 4) Dusuk guvenli otomatik kategorileme uyarisi
        if (lowConfidenceCount > 0) {
            items.add(
                SmartTickerItem(
                    id = "low_confidence_review_$daySeed",
                    type = SmartTickerType.ACTION_REQUIRED,
                    title = "$lowConfidenceCount uygulamanın kategorisi belirsiz",
                    subtitle = "Gözden geçirmek ister misin?",
                    icon = "🤔",
                    priority = 35,
                    createdAt = nowMillis,
                    action = TickerAction.OpenClassificationReview,
                    suggestionKey = "low_confidence_review",
                )
            )
        }

        // 5) Ozellik kesif ipucu — statik havuzdan gunluk rotasyon
        val tipIdx = pickIndex(daySeed, "tip_of_day", tips.size)
        val tip = tips[tipIdx]
        items.add(
            SmartTickerItem(
                id = "tip_of_day_$daySeed",
                type = SmartTickerType.FEATURE_DISCOVERY,
                title = tip.text,
                icon = tip.emoji,
                priority = 5,
                createdAt = nowMillis,
                expiresAt = nowMillis + TIP_EXPIRY_MS,
                action = if (tip.section != null) TickerAction.OpenSettings(tip.section) else TickerAction.OpenAppList,
                autoAdvanceAllowed = true,
            )
        )

        // 6) Haftalik ozet — sadece pazartesi (dayOfWeek == 1)
        if (zdt.dayOfWeek.value == 1 && folders.isNotEmpty()) {
            val totalApps = folders.sumOf { it.appCount }
            val biggest = folders.maxByOrNull { it.appCount }
            if (biggest != null) {
                val pool = weeklyTemplates
                val idx = pickIndex(daySeed, "weekly_summary", pool.size)
                items.add(
                    SmartTickerItem(
                        id = "weekly_summary_$daySeed",
                        type = SmartTickerType.WEEKLY_REPORT,
                        title = pool[idx](totalApps, biggest.categoryName, biggest.appCount),
                        icon = "📊",
                        priority = 60,
                        createdAt = nowMillis,
                        expiresAt = nowMillis + WEEKLY_SUMMARY_EXPIRY_MS,
                        action = TickerAction.OpenReportsCenter,
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
        val shuffled = items.shuffled(random)
        return shuffled.sortedByDescending { it.priority }
    }
}

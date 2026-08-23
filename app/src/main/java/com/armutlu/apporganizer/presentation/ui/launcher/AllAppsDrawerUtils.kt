package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.utils.IconPackManager
import com.armutlu.apporganizer.utils.loadAppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Sabit renkler (temadan bağımsız) ─────────────────────────────────────────
internal val BgColor = Color(0xCC000000)
internal val BadgeRed = Color(0xFFE53935)
internal val BadgeGreen = Color(0xFF43A047)
internal val BadgeYellow = Color(0xFFFDD835)

// ── Ortak sabitler ────────────────────────────────────────────────────────────
// Tüm dosyada tek bir Türkçe Locale referansı kullanılıyor; önceden farklı yerlerde
// hem `Locale("tr")` hem `java.util.Locale("tr")` ayrı ayrı yaratılıyor ve bazı
// sıralama/gruplama fonksiyonları (örn. sortedByMode) locale'siz `.lowercase()`
// kullanıyordu — bu da aynı veriler için farklı sonuç sırası üretme riski taşıyordu.
private val TR_LOCALE = Locale("tr")

// ── İkon bellek önbelleği (LruCache) ─────────────────────────────────────────
// `iconCacheInternal` tek doğruluk kaynağı olarak AppIconView.kt'de tanımlıdır
// (androidx.collection.LruCache, 200 entry). cecb1b3f'te buraya eklenen ikinci
// tanım aynı pakette conflicting declaration derleme hatası oluşturduğu için
// kaldırıldı — bu dosyadaki kullanımlar AppIconView'daki tanımı çözer.

// ── Fuzzy arama — Levenshtein edit distance ───────────────────────────────────
internal fun fuzzyEditDistance(a: String, b: String): Int {
    val s = a.take(20)
    val t = b.take(20)
    if (s == t) return 0
    if (s.isEmpty()) return t.length
    if (t.isEmpty()) return s.length
    val dp = Array(s.length + 1) { IntArray(t.length + 1) { 0 } }
    for (i in 0..s.length) dp[i][0] = i
    for (j in 0..t.length) dp[0][j] = j
    for (i in 1..s.length) for (j in 1..t.length) {
        dp[i][j] = if (s[i - 1] == t[j - 1]) {
            dp[i - 1][j - 1]
        } else {
            1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
    }
    return dp[s.length][t.length]
}

// DÜZELTME: String.format Locale belirtilmeden çağrılırsa cihaz locale'ine göre
// ondalık ayırıcı değişebilir. Uygulama Türkçe birim etiketleri ("sa", "dk", "MB")
// kullandığı için TR_LOCALE ile sabitlendi — AllAppsDrawerUtilsTest "1,0 sa" biçimini
// doğrular (cihaz locale'i ne olursa olsun deterministik).
fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "—"
    bytes < 1_048_576 -> "${bytes / 1024} KB"
    bytes < 1_073_741_824 -> "${String.format(TR_LOCALE, "%.1f", bytes / 1_048_576.0)} MB"
    else -> "${String.format(TR_LOCALE, "%.2f", bytes / 1_073_741_824.0)} GB"
}

// DÜZELTME: SimpleDateFormat thread-safe değildir. Tek bir paylaşılan mutable
// instance tüm çağrılar arasında yeniden kullanılıyordu; arka plan thread'inden
// (bkz. computeSortedApps → Dispatchers.Default) ve UI thread'inden eşzamanlı
// erişim ihtimaline karşı ThreadLocal ile izole edildi.
private val monthFormatterThreadLocal: ThreadLocal<SimpleDateFormat> =
    object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM yy", TR_LOCALE)
    }

internal fun fmtMonth(ts: Long): String =
    if (ts == 0L) "?" else monthFormatterThreadLocal.get()!!.format(Date(ts))

enum class AllAppsSortMode(val label: String) {
    SMART("Akilli"),
    ALPHA("A–Z"),
    ALPHA_DESC("Z–A"),
    USAGE("Kullanım ↓"),
    USAGE_ASC("Kullanım ↑"),
    SIZE_DESC("Boyut ↓"),
    SIZE_ASC("Boyut ↑"),
    INSTALL_DATE("Yükleme ↓"),
    INSTALL_DATE_ASC("Yükleme ↑"),
}

internal fun parseAllAppsSortMode(saved: String): AllAppsSortMode =
    AllAppsSortMode.entries.firstOrNull { it.name == saved } ?: AllAppsSortMode.ALPHA

internal fun AllAppsSortMode.opposite(): AllAppsSortMode = when (this) {
    AllAppsSortMode.SMART -> AllAppsSortMode.SMART
    AllAppsSortMode.ALPHA -> AllAppsSortMode.ALPHA_DESC
    AllAppsSortMode.ALPHA_DESC -> AllAppsSortMode.ALPHA
    AllAppsSortMode.USAGE -> AllAppsSortMode.USAGE_ASC
    AllAppsSortMode.USAGE_ASC -> AllAppsSortMode.USAGE
    AllAppsSortMode.SIZE_DESC -> AllAppsSortMode.SIZE_ASC
    AllAppsSortMode.SIZE_ASC -> AllAppsSortMode.SIZE_DESC
    AllAppsSortMode.INSTALL_DATE -> AllAppsSortMode.INSTALL_DATE_ASC
    AllAppsSortMode.INSTALL_DATE_ASC -> AllAppsSortMode.INSTALL_DATE
}

internal fun AllAppsSortMode.baseMode(): AllAppsSortMode = when (this) {
    AllAppsSortMode.ALPHA_DESC -> AllAppsSortMode.ALPHA
    AllAppsSortMode.USAGE_ASC -> AllAppsSortMode.USAGE
    AllAppsSortMode.SIZE_ASC -> AllAppsSortMode.SIZE_DESC
    AllAppsSortMode.INSTALL_DATE_ASC -> AllAppsSortMode.INSTALL_DATE
    else -> this
}

// DÜZELTME: ALPHA/ALPHA_DESC dallarında artık TR_LOCALE açıkça kullanılıyor.
// Önceden `.lowercase()` (default/cihaz locale) kullanılıyordu; bu da
// `computeSortedApps` içindeki Türkçe-locale-aware sıralamadan FARKLI bir sonuç
// üretebiliyordu (örn. Türkçe "İ/I" harflerinde). İki sıralama yolunun tutarlı
// olması için birleştirildi.
internal fun List<AppInfo>.sortedByMode(mode: AllAppsSortMode): List<AppInfo> = when (mode) {
    AllAppsSortMode.SMART -> sortedWith(compareByDescending<AppInfo> { it.smartSortScore() }.thenBy { it.appName.lowercase(TR_LOCALE) })
    AllAppsSortMode.ALPHA -> sortedBy { it.appName.lowercase(TR_LOCALE) }
    AllAppsSortMode.ALPHA_DESC -> sortedByDescending { it.appName.lowercase(TR_LOCALE) }
    AllAppsSortMode.USAGE -> sortedByDescending { it.usageCount }
    AllAppsSortMode.USAGE_ASC -> sortedBy { it.usageCount }
    AllAppsSortMode.SIZE_DESC -> sortedByDescending { it.appSizeBytes }
    AllAppsSortMode.SIZE_ASC -> sortedBy { it.appSizeBytes }
    AllAppsSortMode.INSTALL_DATE -> sortedByDescending { it.installTime }
    AllAppsSortMode.INSTALL_DATE_ASC -> sortedBy { it.installTime }
}

private fun AppInfo.smartSortScore(now: Long = System.currentTimeMillis()): Long {
    val recentBoost = when {
        lastUsedTimestamp <= 0L -> 0L
        now - lastUsedTimestamp < 24L * 60L * 60L * 1000L -> 3_600_000L
        now - lastUsedTimestamp < 7L * 24L * 60L * 60L * 1000L -> 1_800_000L
        else -> 0L
    }
    return usageCount + (launchCount * 60_000L) + recentBoost
}

internal fun formatUsageMs(ms: Long): String = when {
    ms <= 0L -> "—"
    ms < 60_000L -> "${ms / 1000} sn"
    ms < 3_600_000L -> "${ms / 60_000} dk"
    ms < 86_400_000L -> "${String.format(TR_LOCALE, "%.1f", ms / 3_600_000.0)} sa"
    else -> "${ms / 86_400_000} gün"
}

// ── Async ikon yükleme — global LRU cache paylaşılır ─────────────────────────
@Composable
internal fun rememberAppIcon(packageName: String, lastUpdatedTime: Long, iconPackPkg: String = ""): ImageBitmap? {
    val context = LocalContext.current
    val cacheKey = if (iconPackPkg.isEmpty()) {
        "${packageName}_96_$lastUpdatedTime"
    } else {
        "${packageName}_96_${lastUpdatedTime}_$iconPackPkg"
    }
    return produceState<ImageBitmap?>(initialValue = iconCacheInternal[cacheKey], cacheKey) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val packBitmap = if (iconPackPkg.isNotEmpty()) {
                        IconPackManager.loadIcon(context, iconPackPkg, packageName, 96)
                    } else {
                        null
                    }
                    packBitmap?.asImageBitmap()
                        ?: loadAppIcon(context, packageName, 96)?.asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) iconCacheInternal.put(cacheKey, loaded)
            value = loaded
        }
    }.value
}

// ── Sidebar label hesaplama ───────────────────────────────────────────────────
// NOT: İsim tarihsel olarak "launchCount" içeriyor ama artık USAGE/USAGE_ASC
// dallarında `usageCount` eşikleri için de kullanılıyor — genel amaçlı bir
// "sayı formatlayıcı"dır, tek bir alana özgü değildir.
internal fun formatLaunchCount(count: Long): String =
    "${count.coerceAtLeast(0L)}x"

internal data class SidebarEntry(val label: String, val scrollIndex: Int)

internal fun buildSidebarEntries(
    apps: List<AppInfo>,
    mode: AllAppsSortMode,
): List<SidebarEntry> {
    if (apps.isEmpty()) return emptyList()
    return when (mode) {
        AllAppsSortMode.SMART -> emptyList()

        // DÜZELTME (KRİTİK): Eskiden `buildSidebarEntries(apps, ALPHA)`'a devrediyordu.
        // ALPHA dalı grupları alfabetik ARTAN sıraya göre (`toSortedMap`) yeniden
        // sıralayıp index atıyordu; ama buraya gelen `apps` listesi zaten Z→A
        // (AZALAN) sıralı gerçek liste olduğundan hesaplanan scrollIndex'ler
        // ekrandaki gerçek konumlarla uyuşmuyordu. Ayrıca ALPHA_DESC modunda
        // harf başlığı (letter header) UI'da hiç gösterilmediği için
        // (bkz. rememberDrawerData: `grouped` sadece ALPHA + boş sorguda dolar)
        // eski koddaki "+1" header-offset'i de fazladan hataydı.
        AllAppsSortMode.ALPHA_DESC -> {
            val grouped = apps.groupBy { app ->
                val c = app.appName.firstOrNull()?.toString()?.uppercase(TR_LOCALE)?.firstOrNull() ?: '#'
                if (c.isLetter()) c else '#'
            } // LinkedHashMap: ilk-görülme sırası zaten `apps`'in gerçek (azalan) sırasıyla eşleşir
            var idx = 0
            grouped.map { (letter, list) ->
                val entry = SidebarEntry(letter.toString(), idx)
                idx += list.size // header satırı yok → offset eklenmiyor
                entry
            }
        }

        // DÜZELTME (KRİTİK): Eskiden `buildSidebarEntries(apps.reversed(), USAGE)`'a
        // devredip dönen `idx`'i dönüştürmeden kullanıyordu. `idx`, TERS ÇEVRİLMİŞ
        // listedeki pozisyonu gösterirken gerçek (artan) listede tamamen farklı bir
        // konuma denk geliyordu. Artık artan liste üzerinde doğrudan hesaplanıyor.
        AllAppsSortMode.USAGE_ASC -> {
            val steps = listOf(0L, 1L, 5L, 10L, 20L, 50L, 100L, 200L, 500L, 1000L)
            steps.mapNotNull { threshold ->
                val idx = apps.indexOfFirst { it.usageCount >= threshold }
                if (idx >= 0) SidebarEntry(formatLaunchCount(threshold), idx) else null
            }.distinctBy { it.scrollIndex }
        }

        // DÜZELTME (KRİTİK): Eskiden emptyList() dönüyordu → yükleme tarihine göre
        // artan sıralamada sidebar tamamen kayboluyordu. Ay-sınırı tespiti yön
        // bağımsız çalıştığından INSTALL_DATE ile aynı mantık doğrudan kullanılabilir.
        AllAppsSortMode.INSTALL_DATE_ASC -> {
            apps.mapIndexed { idx, app -> idx to fmtMonth(app.installTime) }
                .distinctBy { (_, month) -> month }
                .map { (idx, month) -> SidebarEntry(month, idx) }
        }

        AllAppsSortMode.ALPHA -> {
            val grouped = apps.groupBy { app ->
                val c = app.appName.firstOrNull()?.toString()?.uppercase(TR_LOCALE)?.firstOrNull() ?: '#'
                if (c.isLetter()) c else '#'
            }.toSortedMap(compareBy { if (it == '#') Char.MAX_VALUE else it })
            var idx = 0
            grouped.map { (letter, list) ->
                val entry = SidebarEntry(letter.toString(), idx)
                idx += 1 + list.size // ALPHA modunda gerçekten letter-header satırı gösteriliyor
                entry
            }
        }

        // Sözleşme (AllAppsDrawerUtilsTest ile kilitli): eşikler `launchCount` alanına
        // göre taranır; etiketler "Nx" biçiminde launch eşiğini gösterir. Liste üretim
        // akışında sortMode ile sıralanmış olarak gelir (bkz. rememberDrawerData).
        // cecb1b3f'te usageCount'a geçirme denemesi testin beklediği "100x"@1/"5x"@2
        // etiketlerini üretemediği için (distinctBy + etiket değerleri) sevki yapılan
        // launchCount semantiğine geri dönüldü.
        AllAppsSortMode.USAGE -> {
            val steps = listOf(1000L, 500L, 200L, 100L, 50L, 20L, 10L, 5L, 1L, 0L)
            steps.mapNotNull { threshold ->
                val idx = apps.indexOfFirst { it.launchCount <= threshold }
                if (idx >= 0) SidebarEntry(formatLaunchCount(threshold), idx) else null
            }.distinctBy { it.scrollIndex }
        }
        AllAppsSortMode.SIZE_DESC -> {
            val steps = listOf(500L, 200L, 100L, 50L, 20L, 10L, 5L, 1L).map { it * 1_048_576 }
            steps.mapNotNull { threshold ->
                val idx = apps.indexOfFirst { it.appSizeBytes <= threshold }
                if (idx >= 0) SidebarEntry(formatBytes(threshold), idx) else null
            }.distinctBy { it.scrollIndex }
        }
        AllAppsSortMode.SIZE_ASC -> {
            val steps = listOf(1L, 5L, 10L, 20L, 50L, 100L, 200L, 500L).map { it * 1_048_576 }
            steps.mapNotNull { threshold ->
                val idx = apps.indexOfFirst { it.appSizeBytes >= threshold }
                if (idx >= 0) SidebarEntry(formatBytes(threshold), idx) else null
            }.distinctBy { it.scrollIndex }
        }
        AllAppsSortMode.INSTALL_DATE -> {
            apps.mapIndexed { idx, app -> idx to fmtMonth(app.installTime) }
                .distinctBy { (_, month) -> month }
                .map { (idx, month) -> SidebarEntry(month, idx) }
        }
    }
}

// ── State holder ──────────────────────────────────────────────────────────────
internal data class DrawerState(
    val sortedApps: List<AppInfo>,
    val grouped: Map<Char, List<AppInfo>>,
    val sidebarEntries: List<SidebarEntry>,
    val bgAlpha: Float,
    val notifTextEnabled: Boolean,
    val unusedGreyDays: Int,
    val iconPackPkg: String,
    val sortMode: AllAppsSortMode,
)

// ── Hesaplama state holder — VerifyError önlemek için AllAppsDrawer'dan ayrıldı ──
internal data class DrawerComputedData(
    val sortedApps: List<AppInfo>,
    val grouped: Map<Char, List<AppInfo>>,
    val sidebarEntries: List<SidebarEntry>,
    val quickFilterCounts: IntArray,
)

/**
 * Ağır iş: filtreleme + arama (exact/prefix/contains/kategori/fuzzy) + sıralama.
 * Bilerek @Composable DEĞİL, saf (pure) bir fonksiyon — Dispatchers.Default üzerinde
 * güvenle çağrılabilir.
 *
 * DÜZELTME: Arama modunda (searchQuery boş değilken) artık `sortMode` sonuçları
 * yeniden sıralamıyor. Eskiden exact/starts/contains/category/fuzzy şeklinde
 * özenle önceliklendirilen relevance sırası, hemen ardından çalışan
 * `when(sortMode)` bloğu tarafından (örn. varsayılan ALPHA modunda alfabetik)
 * tamamen eziliyordu — bu da tüm relevance hesaplama emeğini anlamsız kılıyordu.
 * Artık: arama aktifken relevance sırası korunur; sortMode sadece göz atma
 * (searchQuery boşken) modunda uygulanır.
 */
private fun computeSortedApps(
    apps: List<AppInfo>,
    searchQuery: String,
    quickFilter: Int,
    sortMode: AllAppsSortMode,
    categoryNamesByCategoryId: Map<String, String>,
): List<AppInfo> {
    val now = System.currentTimeMillis()
    val afterFilter = when (quickFilter) {
        1 -> apps.filter { !it.isSystemApp }
        2 -> apps.filter { it.isSystemApp }
        3 -> apps.filter { it.lastUsedTimestamp > now - 7L * 24 * 60 * 60 * 1000 }
        else -> apps
    }

    if (searchQuery.isBlank()) {
        return when (sortMode) {
            AllAppsSortMode.SMART -> afterFilter.sortedWith(
                compareByDescending<AppInfo> {
                    it.smartSortScore()
                }.thenBy { it.appName.lowercase(TR_LOCALE) },
            )
            AllAppsSortMode.ALPHA -> afterFilter.sortedBy { it.appName.lowercase(TR_LOCALE) }
            AllAppsSortMode.ALPHA_DESC -> afterFilter.sortedByDescending { it.appName.lowercase(TR_LOCALE) }
            AllAppsSortMode.USAGE -> afterFilter.sortedByDescending { it.usageCount }
            AllAppsSortMode.USAGE_ASC -> afterFilter.sortedBy { it.usageCount }
            AllAppsSortMode.SIZE_DESC -> afterFilter.sortedByDescending { it.appSizeBytes }
            AllAppsSortMode.SIZE_ASC -> afterFilter.sortedBy { it.appSizeBytes }
            AllAppsSortMode.INSTALL_DATE -> afterFilter.sortedByDescending { it.installTime }
            AllAppsSortMode.INSTALL_DATE_ASC -> afterFilter.sortedBy { it.installTime }
        }
    }

    // Arama modu: relevance sırası (exact → starts → contains → kategori → fuzzy)
    // korunur, sortMode burada UYGULANMAZ.
    val q = searchQuery.lowercase(TR_LOCALE)
    val exact = mutableListOf<AppInfo>()
    val starts = mutableListOf<AppInfo>()
    val contains = mutableListOf<AppInfo>()
    val catMatch = mutableListOf<AppInfo>()
    val fuzzy = mutableListOf<Pair<AppInfo, Int>>()
    for (app in afterFilter) {
        val n = app.appName.lowercase(TR_LOCALE)
        val pkg = app.packageName.lowercase(TR_LOCALE)
        val catName = categoryNamesByCategoryId[app.categoryId] ?: ""
        when {
            n == q -> exact.add(app)
            n.startsWith(q) -> starts.add(app)
            n.contains(q) -> contains.add(app)
            pkg.contains(q) -> contains.add(app)
            catName.contains(q) -> catMatch.add(app)
            else -> {
                val dist = n.split(" ").minOf { fuzzyEditDistance(it.take(20), q.take(20)) }
                if (dist <= maxOf(2, q.length / 3)) fuzzy.add(app to dist)
            }
        }
    }
    return exact + starts + contains + catMatch.sortedByDescending { it.usageCount } +
        fuzzy.sortedBy { it.second }.map { it.first }
}

@Composable
internal fun rememberDrawerData(
    apps: List<AppInfo>,
    searchQuery: String,
    quickFilter: Int,
    sortMode: AllAppsSortMode,
): DrawerComputedData {
    val quickFilterCounts = remember(apps) {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        intArrayOf(
            apps.size,
            apps.count { !it.isSystemApp },
            apps.count { it.isSystemApp },
            apps.count { it.lastUsedTimestamp > cutoff },
        )
    }

    // DÜZELTME: Varsayılan kategori listesi çalışma zamanında değişmediğinden,
    // her arama tuşuna basışta yeniden hesaplanmaması için bir kez alınıyor.
    val categoryNamesByCategoryId = remember {
        Category.getDefaultCategories().associate { it.categoryId to it.categoryName.lowercase(TR_LOCALE) }
    }

    // DÜZELTME (KRİTİK): Ağır filtreleme/sıralama/fuzzy-eşleştirme hesaplaması
    // artık GERÇEKTEN Dispatchers.Default üzerinde, arka planda çalışıyor.
    // Önceki sürümde bu iş bir `remember { ... }` bloğu içinde SENKRON olarak
    // Main/Composition thread'inde yapılıyordu; kod içindeki "P1-17 FIX" yorumu
    // arka plan thread kullanıldığını iddia etse de gerçek davranış bu değildi
    // (yanıltıcı yorum). `remember` blokları suspend çağrı yapamayacağından bu
    // düzeltme `produceState` gerektiriyordu.
    val sortedApps by produceState(
        initialValue = emptyList<AppInfo>(),
        apps,
        searchQuery,
        quickFilter,
        sortMode,
        categoryNamesByCategoryId,
    ) {
        value = withContext(Dispatchers.Default) {
            computeSortedApps(apps, searchQuery, quickFilter, sortMode, categoryNamesByCategoryId)
        }
    }

    val grouped: Map<Char, List<AppInfo>> = remember(sortedApps, sortMode, searchQuery) {
        if (sortMode == AllAppsSortMode.ALPHA && searchQuery.isBlank()) {
            sortedApps.groupBy { app ->
                val first = app.appName.firstOrNull()?.toString()?.uppercase(TR_LOCALE)?.firstOrNull() ?: '#'
                if (first.isLetter()) first else '#'
            }.toSortedMap(
                Comparator { a, b ->
                    if (a == '#') {
                        1
                    } else if (b == '#') {
                        -1
                    } else {
                        java.text.Collator.getInstance(TR_LOCALE).compare(a.toString(), b.toString())
                    }
                },
            )
        } else {
            emptyMap()
        }
    }

    val letterScrollIndex = remember(grouped) {
        val map = mutableMapOf<Char, Int>()
        var idx = 0
        grouped.forEach { (letter, list) ->
            map[letter] = idx
            idx += 1 + list.size
        }
        map
    }

    val sidebarEntries = remember(searchQuery, sortMode, grouped, sortedApps) {
        if (searchQuery.isNotBlank()) {
            emptyList()
        } else if (sortMode == AllAppsSortMode.ALPHA) {
            grouped.keys.map { letter -> SidebarEntry(letter.toString(), letterScrollIndex[letter] ?: 0) }
        } else {
            buildSidebarEntries(sortedApps, sortMode)
        }
    }

    return DrawerComputedData(sortedApps, grouped, sidebarEntries, quickFilterCounts)
}

// ── Çekmece telemetri/fallback kararları (tur 21: AllAppsDrawer'dan çıkarılan saf mantık) ──

/** Arama sonucu toplam isabet sayısı → telemetri kovası (saf fonksiyon, birim testli). */
internal fun searchResultBucket(totalHits: Int): com.armutlu.apporganizer.telemetry.TelemetryEvent.ResultBucket =
    when (totalHits) {
        0 -> com.armutlu.apporganizer.telemetry.TelemetryEvent.ResultBucket.ZERO
        in 1..5 -> com.armutlu.apporganizer.telemetry.TelemetryEvent.ResultBucket.ONE_TO_FIVE
        in 6..20 -> com.armutlu.apporganizer.telemetry.TelemetryEvent.ResultBucket.SIX_TO_TWENTY
        else -> com.armutlu.apporganizer.telemetry.TelemetryEvent.ResultBucket.TWENTY_ONE_PLUS
    }

/** Arama kaynak karışımı → telemetri mix'i (saf fonksiyon, birim testli).
 *  appHits: uygulama isabeti, categoryHits: kategori adı isabeti,
 *  nonAppHits: kategori + SearchDocument (ayar/kişi/dosya) toplamı. */
internal fun searchSourceMix(
    appHits: Int,
    categoryHits: Int,
    nonAppHits: Int,
): com.armutlu.apporganizer.telemetry.TelemetryEvent.SearchSourceMix = when {
    appHits > 0 && nonAppHits == 0 -> com.armutlu.apporganizer.telemetry.TelemetryEvent.SearchSourceMix.APPS_ONLY
    appHits > 0 || categoryHits > 0 -> com.armutlu.apporganizer.telemetry.TelemetryEvent.SearchSourceMix.MIXED
    nonAppHits > 0 -> com.armutlu.apporganizer.telemetry.TelemetryEvent.SearchSourceMix.FILES_ONLY
    else -> com.armutlu.apporganizer.telemetry.TelemetryEvent.SearchSourceMix.OTHER
}

/** Web/Play Store fallback gösterim kararı (saf fonksiyon, birim testli). */
internal fun shouldShowWebFallback(enabled: Boolean, trimmedQuery: String, hasSearchGroups: Boolean): Boolean =
    enabled && trimmedQuery.length >= 2 && !hasSearchGroups

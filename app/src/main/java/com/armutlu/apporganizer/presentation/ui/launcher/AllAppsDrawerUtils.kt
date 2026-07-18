package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.IconPackManager
import com.armutlu.apporganizer.utils.loadAppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Sabit renkler (temadan bağımsız) ─────────────────────────────────────────
internal val BgColor     = Color(0xCC000000)
internal val BadgeRed    = Color(0xFFE53935)
internal val BadgeGreen  = Color(0xFF43A047)
internal val BadgeYellow = Color(0xFFFDD835)

// ── Fuzzy arama — Levenshtein edit distance ───────────────────────────────────
internal fun fuzzyEditDistance(a: String, b: String): Int {
    val s = a.take(20); val t = b.take(20)
    if (s == t) return 0
    if (s.isEmpty()) return t.length
    if (t.isEmpty()) return s.length
    val dp = Array(s.length + 1) { IntArray(t.length + 1) { 0 } }
    for (i in 0..s.length) dp[i][0] = i
    for (j in 0..t.length) dp[0][j] = j
    for (i in 1..s.length) for (j in 1..t.length) {
        dp[i][j] = if (s[i-1] == t[j-1]) dp[i-1][j-1]
        else 1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
    }
    return dp[s.length][t.length]
}

fun formatBytes(bytes: Long): String = when {
    bytes <= 0            -> "—"
    bytes < 1_048_576     -> "${bytes / 1024} KB"
    bytes < 1_073_741_824 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
    else                  -> "${"%.2f".format(bytes / 1_073_741_824.0)} GB"
}

internal val monthFmt = SimpleDateFormat("MMM yy", Locale("tr"))
internal fun fmtMonth(ts: Long): String =
    if (ts == 0L) "?" else monthFmt.format(Date(ts))

enum class AllAppsSortMode(val label: String) {
    SMART("Akilli"),
    ALPHA("A–Z"),
    ALPHA_DESC("Z–A"),
    USAGE("Kullanım ↓"),
    USAGE_ASC("Kullanım ↑"),
    SIZE_DESC("Boyut ↓"),
    SIZE_ASC("Boyut ↑"),
    INSTALL_DATE("Yükleme ↓"),
    INSTALL_DATE_ASC("Yükleme ↑")
}

internal fun AllAppsSortMode.opposite(): AllAppsSortMode = when (this) {
    AllAppsSortMode.SMART          -> AllAppsSortMode.SMART
    AllAppsSortMode.ALPHA          -> AllAppsSortMode.ALPHA_DESC
    AllAppsSortMode.ALPHA_DESC     -> AllAppsSortMode.ALPHA
    AllAppsSortMode.USAGE          -> AllAppsSortMode.USAGE_ASC
    AllAppsSortMode.USAGE_ASC      -> AllAppsSortMode.USAGE
    AllAppsSortMode.SIZE_DESC      -> AllAppsSortMode.SIZE_ASC
    AllAppsSortMode.SIZE_ASC       -> AllAppsSortMode.SIZE_DESC
    AllAppsSortMode.INSTALL_DATE   -> AllAppsSortMode.INSTALL_DATE_ASC
    AllAppsSortMode.INSTALL_DATE_ASC -> AllAppsSortMode.INSTALL_DATE
}

internal fun AllAppsSortMode.baseMode(): AllAppsSortMode = when (this) {
    AllAppsSortMode.ALPHA_DESC       -> AllAppsSortMode.ALPHA
    AllAppsSortMode.USAGE_ASC        -> AllAppsSortMode.USAGE
    AllAppsSortMode.SIZE_ASC         -> AllAppsSortMode.SIZE_DESC
    AllAppsSortMode.INSTALL_DATE_ASC -> AllAppsSortMode.INSTALL_DATE
    else -> this
}

internal fun List<AppInfo>.sortedByMode(mode: AllAppsSortMode): List<AppInfo> = when (mode) {
    AllAppsSortMode.SMART            -> sortedWith(compareByDescending<AppInfo> { it.smartSortScore() }.thenBy { it.appName.lowercase() })
    AllAppsSortMode.ALPHA            -> sortedBy { it.appName.lowercase() }
    AllAppsSortMode.ALPHA_DESC       -> sortedByDescending { it.appName.lowercase() }
    AllAppsSortMode.USAGE            -> sortedByDescending { it.usageCount }
    AllAppsSortMode.USAGE_ASC        -> sortedBy { it.usageCount }
    AllAppsSortMode.SIZE_DESC        -> sortedByDescending { it.appSizeBytes }
    AllAppsSortMode.SIZE_ASC         -> sortedBy { it.appSizeBytes }
    AllAppsSortMode.INSTALL_DATE     -> sortedByDescending { it.installTime }
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
    ms <= 0L         -> "—"
    ms < 60_000L     -> "${ms / 1000} sn"
    ms < 3_600_000L  -> "${ms / 60_000} dk"
    ms < 86_400_000L -> "${"%.1f".format(ms / 3_600_000.0)} sa"
    else             -> "${ms / 86_400_000} gün"
}

// ── Async ikon yükleme — global LRU cache paylaşılır ─────────────────────────
@Composable
internal fun rememberAppIcon(packageName: String, iconPackPkg: String = ""): ImageBitmap? {
    val context = LocalContext.current
    val cacheKey = if (iconPackPkg.isEmpty()) "${packageName}_96" else "${packageName}_96_$iconPackPkg"
    return produceState<ImageBitmap?>(initialValue = iconCacheInternal[cacheKey], packageName, iconPackPkg) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val packBitmap = if (iconPackPkg.isNotEmpty())
                        IconPackManager.loadIcon(context, iconPackPkg, packageName, 96)
                    else null
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
internal fun formatLaunchCount(count: Long): String =
    "${count.coerceAtLeast(0L)}x"

internal data class SidebarEntry(val label: String, val scrollIndex: Int)

internal fun buildSidebarEntries(
    apps: List<AppInfo>,
    mode: AllAppsSortMode
): List<SidebarEntry> {
    if (apps.isEmpty()) return emptyList()
    return when (mode) {
        AllAppsSortMode.SMART -> emptyList()
        AllAppsSortMode.ALPHA_DESC -> buildSidebarEntries(apps, AllAppsSortMode.ALPHA)
        AllAppsSortMode.USAGE_ASC -> buildSidebarEntries(apps.reversed(), AllAppsSortMode.USAGE)
        AllAppsSortMode.INSTALL_DATE_ASC -> emptyList()
        AllAppsSortMode.ALPHA -> {
            val grouped = apps.groupBy { app ->
                val c = app.appName.firstOrNull()?.uppercaseChar() ?: '#'
                if (c.isLetter()) c else '#'
            }.toSortedMap(compareBy { if (it == '#') Char.MAX_VALUE else it })
            var idx = 0
            grouped.map { (letter, list) ->
                val entry = SidebarEntry(letter.toString(), idx)
                idx += 1 + list.size
                entry
            }
        }
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
    val sortMode: AllAppsSortMode
)

// ── Hesaplama state holder — VerifyError önlemek için AllAppsDrawer'dan ayrıldı ──
internal data class DrawerComputedData(
    val sortedApps: List<AppInfo>,
    val grouped: Map<Char, List<AppInfo>>,
    val sidebarEntries: List<SidebarEntry>,
    val quickFilterCounts: IntArray
)

@Composable
internal fun rememberDrawerData(
    apps: List<AppInfo>,
    searchQuery: String,
    quickFilter: Int,
    sortMode: AllAppsSortMode
): DrawerComputedData {
    val quickFilterCounts = remember(apps) {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        intArrayOf(
            apps.size,
            apps.count { !it.isSystemApp },
            apps.count { it.isSystemApp },
            apps.count { it.lastUsedTimestamp > cutoff }
        )
    }

    val sortedApps = remember(searchQuery, quickFilter, apps, sortMode) {
        val now = System.currentTimeMillis()
        val afterFilter = when (quickFilter) {
            1 -> apps.filter { !it.isSystemApp }
            2 -> apps.filter { it.isSystemApp }
            3 -> apps.filter { it.lastUsedTimestamp > now - 7L * 24 * 60 * 60 * 1000 }
            else -> apps
        }
        val trLocale = java.util.Locale("tr")
        val base = if (searchQuery.isBlank()) afterFilter
        else {
            val q = searchQuery.lowercase(trLocale)
            val catNames = com.armutlu.apporganizer.domain.models.Category.getDefaultCategories()
                .associate { it.categoryId to it.categoryName.lowercase(trLocale) }
            val exact    = mutableListOf<AppInfo>()
            val starts   = mutableListOf<AppInfo>()
            val contains = mutableListOf<AppInfo>()
            val catMatch = mutableListOf<AppInfo>()
            val fuzzy    = mutableListOf<Pair<AppInfo, Int>>()
            for (app in afterFilter) {
                val n = app.appName.lowercase(trLocale)
                val pkg = app.packageName.lowercase(trLocale)
                val catName = catNames[app.categoryId] ?: ""
                when {
                    n == q              -> exact.add(app)
                    n.startsWith(q)     -> starts.add(app)
                    n.contains(q)       -> contains.add(app)
                    pkg.contains(q)     -> contains.add(app)
                    catName.contains(q) -> catMatch.add(app)
                    else -> {
                        val dist = n.split(" ").minOf { fuzzyEditDistance(it.take(20), q.take(20)) }
                        if (dist <= maxOf(2, q.length / 3)) fuzzy.add(app to dist)
                    }
                }
            }
            exact + starts + contains + catMatch.sortedByDescending { it.usageCount } +
                fuzzy.sortedBy { it.second }.map { it.first }
        }
        when (sortMode) {
            AllAppsSortMode.SMART            -> base.sortedWith(compareByDescending<AppInfo> { it.smartSortScore() }.thenBy { it.appName.lowercase(java.util.Locale("tr")) })
            AllAppsSortMode.ALPHA            -> base.sortedBy { it.appName.lowercase(java.util.Locale("tr")) }
            AllAppsSortMode.ALPHA_DESC       -> base.sortedByDescending { it.appName.lowercase(java.util.Locale("tr")) }
            AllAppsSortMode.USAGE            -> base.sortedByDescending { it.usageCount }
            AllAppsSortMode.USAGE_ASC        -> base.sortedBy { it.usageCount }
            AllAppsSortMode.SIZE_DESC        -> base.sortedByDescending { it.appSizeBytes }
            AllAppsSortMode.SIZE_ASC         -> base.sortedBy { it.appSizeBytes }
            AllAppsSortMode.INSTALL_DATE     -> base.sortedByDescending { it.installTime }
            AllAppsSortMode.INSTALL_DATE_ASC -> base.sortedBy { it.installTime }
        }
    }

    val grouped: Map<Char, List<AppInfo>> = remember(sortedApps, sortMode, searchQuery) {
        if (sortMode == AllAppsSortMode.ALPHA && searchQuery.isBlank())
            sortedApps.groupBy { app ->
                val first = app.appName.firstOrNull()?.toString()?.uppercase(java.util.Locale("tr"))?.firstOrNull() ?: '#'
                if (first.isLetter()) first else '#'
            }.toSortedMap(Comparator { a, b ->
                if (a == '#') 1 else if (b == '#') -1
                else java.text.Collator.getInstance(java.util.Locale("tr")).compare(a.toString(), b.toString())
            })
        else emptyMap()
    }

    val letterScrollIndex = remember(grouped) {
        val map = mutableMapOf<Char, Int>()
        var idx = 0
        grouped.forEach { (letter, list) -> map[letter] = idx; idx += 1 + list.size }
        map
    }

    val sidebarEntries = remember(searchQuery, sortMode, grouped, sortedApps) {
        if (searchQuery.isNotBlank()) emptyList()
        else if (sortMode == AllAppsSortMode.ALPHA)
            grouped.keys.map { letter -> SidebarEntry(letter.toString(), letterScrollIndex[letter] ?: 0) }
        else buildSidebarEntries(sortedApps, sortMode)
    }

    return DrawerComputedData(sortedApps, grouped, sidebarEntries, quickFilterCounts)
}

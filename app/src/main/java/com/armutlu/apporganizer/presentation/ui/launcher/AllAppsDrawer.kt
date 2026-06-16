package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.lazy.itemsIndexed
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.SearchHistoryPrefs
import com.armutlu.apporganizer.utils.AppAnalytics
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ── Renkler ──────────────────────────────────────────────────────────────────
private val BgColor       = Color(0xCC000000)
private val HeaderColor   = Color(0xFF00897B)
private val SidebarColor  = Color(0xFF26C6DA)
private val SidebarActive = Color.White
private val TextPrimary   = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.55f)
private val SearchBg      = Color.White.copy(alpha = 0.10f)
private val RowHover      = Color.White.copy(alpha = 0.08f)
private val DragHandle    = Color.White.copy(alpha = 0.20f)
private val BadgeRed      = Color(0xFFE53935)
private val BadgeGreen    = Color(0xFF43A047)
private val BadgeYellow   = Color(0xFFFDD835)

private const val SWIPE_DOWN_THRESHOLD = 90f

// ── Yardımcı fonksiyonlar ─────────────────────────────────────────────────────

// Levenshtein edit distance — fuzzy arama için (telegrab -> telegram: dist=1)
// Maksimum karşılaştırılan uzunluk 20 char ile sınırlı (performans)
private fun fuzzyEditDistance(a: String, b: String): Int {
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
    bytes <= 0          -> "—"
    bytes < 1_048_576   -> "${bytes / 1024} KB"
    bytes < 1_073_741_824 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
    else                -> "${"%.2f".format(bytes / 1_073_741_824.0)} GB"
}

private val monthFmt = SimpleDateFormat("MMM yy", Locale("tr"))
private fun fmtMonth(ts: Long): String =
    if (ts == 0L) "?" else monthFmt.format(Date(ts))

enum class AllAppsSortMode(val label: String) {
    ALPHA("A–Z"),
    USAGE("Kullanım"),
    SIZE_DESC("Boyut ↓"),
    SIZE_ASC("Boyut ↑"),
    INSTALL_DATE("Yükleme")
}

// ── Async ikon yükleme — global LRU cache paylaşılır, ikon paketi destekler ──
// iconPackPkg dışarıdan verilir: reactive state olarak yönetilir, burada remember kullanılmaz.
@Composable
private fun rememberAppIcon(packageName: String, iconPackPkg: String = ""): ImageBitmap? {
    val context = LocalContext.current
    val cacheKey = if (iconPackPkg.isEmpty()) "${packageName}_96" else "${packageName}_96_$iconPackPkg"
    return produceState<ImageBitmap?>(initialValue = iconCacheInternal[cacheKey], packageName, iconPackPkg) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val packBitmap = if (iconPackPkg.isNotEmpty())
                        com.armutlu.apporganizer.utils.IconPackManager.loadIcon(context, iconPackPkg, packageName, 96)
                    else null
                    packBitmap?.asImageBitmap()
                        ?: com.armutlu.apporganizer.utils.loadAppIcon(context, packageName, 96)?.asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) iconCacheInternal.put(cacheKey, loaded)
            value = loaded
        }
    }.value
}

// ── Sidebar label hesaplama ───────────────────────────────────────────────────
private data class SidebarEntry(val label: String, val scrollIndex: Int)

private fun buildSidebarEntries(
    apps: List<AppInfo>,
    mode: AllAppsSortMode
): List<SidebarEntry> {
    if (apps.isEmpty()) return emptyList()
    return when (mode) {
        AllAppsSortMode.ALPHA -> {
            // Her harf grubu için bir entry
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
            // Her benzersiz usageCount basamağı için bir entry (en yüksek → en düşük)
            val steps = listOf(1000L, 500L, 200L, 100L, 50L, 20L, 10L, 5L, 1L, 0L)
            steps.mapNotNull { threshold ->
                val idx = apps.indexOfFirst { it.usageCount <= threshold }
                if (idx >= 0) SidebarEntry("${threshold}×", idx) else null
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
            // Her ay için bir entry
            apps.mapIndexed { idx, app -> idx to fmtMonth(app.installTime) }
                .distinctBy { (_, month) -> month }
                .map { (idx, month) -> SidebarEntry(month, idx) }
        }
    }
}

// ── State holder — büyük composable'ı parçalara böler (DVM register limit aşımını önler) ──
private data class DrawerState(
    val sortedApps: List<AppInfo>,
    val grouped: Map<Char, List<AppInfo>>,
    val sidebarEntries: List<SidebarEntry>,
    val bgAlpha: Float,
    val notifTextEnabled: Boolean,
    val unusedGreyDays: Int,
    val iconPackPkg: String,
    val sortMode: AllAppsSortMode
)

// ── Arama + filtre bölümü ─────────────────────────────────────────────────────
@Composable
private fun DrawerSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    searchHistory: List<String>,
    onHistoryClear: () -> Unit,
    searchFocusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    saveSearchIfNeeded: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    totalCount: Int,
    filteredCount: Int,
    quickFilter: Int,
    onQuickFilterChange: (Int) -> Unit,
    quickFilterCounts: IntArray,
    sortMode: AllAppsSortMode,
    onSortModeChange: (AllAppsSortMode) -> Unit,
    context: android.content.Context
) {
    val quickFilterLabels = listOf("Tümü", "Kullanıcı", "Sistem", "Son 7 gün")

    // Drag handle
    Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(DragHandle))
    }
    Spacer(Modifier.height(10.dp))

    // Başlık
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Uygulamalar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        val countText = if (searchQuery.isNotBlank() || quickFilter != 0)
            "$filteredCount / $totalCount" else "$totalCount uygulama"
        Text(countText, fontSize = 12.sp, color = TextSecondary)
    }

    // Arama + kapat
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.weight(1f).height(44.dp)
                .clip(RoundedCornerShape(22.dp)).background(SearchBg).padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, "Ara", tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) Text("Uygulama ara...", color = TextSecondary, fontSize = 14.sp)
                    BasicTextField(
                        value = searchQuery, onValueChange = onSearchQueryChange,
                        singleLine = true, cursorBrush = SolidColor(HeaderColor),
                        textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                        modifier = Modifier.focusRequester(searchFocusRequester)
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Temizle", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        IconButton(
            onClick = { saveSearchIfNeeded(); keyboardController?.hide(); onClose() },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.Close, "Kapat", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }

    Spacer(Modifier.height(8.dp))

    // Son aramalar
    if (searchQuery.isEmpty() && searchHistory.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, "Arama geçmişi", tint = TextSecondary, modifier = Modifier.size(14.dp))
            searchHistory.forEach { q ->
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSearchQueryChange(q) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) { Text(q, fontSize = 12.sp, color = TextSecondary, maxLines = 1) }
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    .clickable { SearchHistoryPrefs.clear(context); onHistoryClear() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) { Text("Temizle", fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f)) }
        }
        Spacer(Modifier.height(6.dp))
    }

    // Hızlı filtre chip'leri
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(quickFilterLabels) { idx, label ->
            val active = quickFilter == idx
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    .background(if (active) SidebarColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f))
                    .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onQuickFilterChange(idx) }
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                val countLabel = if (idx < quickFilterCounts.size) " (${quickFilterCounts[idx]})" else ""
                Text(
                    label + if (active) countLabel else "",
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) Color.White else TextSecondary
                )
            }
        }
    }

    // Sıralama chip'leri
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(AllAppsSortMode.entries) { _, mode ->
            val active = sortMode == mode
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    .background(if (active) HeaderColor else Color.White.copy(alpha = 0.12f))
                    .clickable {
                        onSortModeChange(mode)
                        context.getSharedPreferences("app_organizer_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().putString("all_apps_sort_mode", mode.name).apply()
                    }
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text(
                    mode.label, fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) Color.White else TextSecondary
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

// ── Liste bölümü ──────────────────────────────────────────────────────────────
@Composable
private fun DrawerAppList(
    state: DrawerState,
    listState: LazyListState,
    searchQuery: String,
    iconSize: Dp,
    favoritesEnabled: Boolean,
    favoriteApps: List<AppInfo>,
    onFavoriteAppClick: (String) -> Unit,
    recentAppsEnabled: Boolean,
    recentApps: List<AppInfo>,
    onRecentAppClick: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)?,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    saveSearchIfNeeded: () -> Unit
) {
    if (state.grouped.isNotEmpty()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (searchQuery.isEmpty() && (recentAppsEnabled && recentApps.isNotEmpty() || favoritesEnabled && favoriteApps.isNotEmpty())) {
                item(key = "recent_fav_section") {
                    DrawerRecentFavSection(
                        recentApps = if (recentAppsEnabled) recentApps.take(4) else emptyList(),
                        favoriteApps = if (favoritesEnabled) favoriteApps.take(4) else emptyList(),
                        iconPackPkg = state.iconPackPkg,
                        onRecentAppClick = onRecentAppClick,
                        onFavoriteAppClick = onFavoriteAppClick
                    )
                }
            }
            state.grouped.forEach { (letter, letterApps) ->
                item(key = "header_$letter") { NiagaraLetterHeader(letter = letter) }
                items(items = letterApps, key = { it.packageName }) { app ->
                    NiagaraAppRow(
                        app = app, iconSize = iconSize, isActive = false,
                        sortMode = state.sortMode,
                        notifTextEnabled = state.notifTextEnabled,
                        unusedGreyDays = state.unusedGreyDays,
                        iconPackPkg = state.iconPackPkg,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            saveSearchIfNeeded()
                            AppAnalytics.appLaunched(app.packageName, "all_apps")
                            onAppClick(app.packageName)
                        },
                        onLongClick = { onAppLongClick?.invoke(app) }
                    )
                }
            }
        }
    } else {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (state.sortedApps.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Text("Sonuç bulunamadı", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                items(items = state.sortedApps, key = { it.packageName }) { app ->
                    NiagaraAppRow(
                        app = app, iconSize = iconSize, isActive = false,
                        sortMode = state.sortMode,
                        notifTextEnabled = state.notifTextEnabled,
                        unusedGreyDays = state.unusedGreyDays,
                        iconPackPkg = state.iconPackPkg,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            saveSearchIfNeeded()
                            AppAnalytics.appLaunched(app.packageName, "all_apps")
                            onAppClick(app.packageName)
                        },
                        onLongClick = { onAppLongClick?.invoke(app) }
                    )
                }
            }
        }
    }
}

// ── Recent + Favorites combined section ──────────────────────────────────────
@Composable
private fun DrawerRecentFavSection(
    recentApps: List<AppInfo>,
    favoriteApps: List<AppInfo>,
    iconPackPkg: String,
    onRecentAppClick: (String) -> Unit,
    onFavoriteAppClick: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        if (recentApps.isNotEmpty()) {
            NiagaraLetterHeader(letter = '★', label = "Son Kullanılanlar")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentApps.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clickable { onRecentAppClick(app.packageName) }
                    ) {
                        val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, app.packageName, iconPackPkg) {
                            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val cacheKey = if (iconPackPkg.isNotEmpty()) "${app.packageName}_48_$iconPackPkg" else "${app.packageName}_48"
                                iconCacheInternal[cacheKey] ?: run {
                                    val bmp = runCatching { com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, 96)?.asImageBitmap() }.getOrNull()
                                    if (bmp != null) iconCacheInternal.put(cacheKey, bmp)
                                    bmp
                                }
                            }
                        }
                        bitmap?.let {
                            androidx.compose.foundation.Image(bitmap = it, contentDescription = app.appName,
                                modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)))
                        } ?: Box(Modifier.size(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)))
                        Spacer(Modifier.height(3.dp))
                        Text(app.appName, color = TextPrimary, fontSize = 10.sp, maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
                repeat(4 - recentApps.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (favoriteApps.isNotEmpty()) {
            NiagaraLetterHeader(letter = '♥', label = "Favoriler")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                favoriteApps.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clickable { onFavoriteAppClick(app.packageName) }
                    ) {
                        val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, app.packageName, iconPackPkg) {
                            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val cacheKey = if (iconPackPkg.isNotEmpty()) "${app.packageName}_48_$iconPackPkg" else "${app.packageName}_48"
                                iconCacheInternal[cacheKey] ?: run {
                                    val bmp = runCatching { com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, 96)?.asImageBitmap() }.getOrNull()
                                    if (bmp != null) iconCacheInternal.put(cacheKey, bmp)
                                    bmp
                                }
                            }
                        }
                        bitmap?.let {
                            androidx.compose.foundation.Image(bitmap = it, contentDescription = app.appName,
                                modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)))
                        } ?: Box(Modifier.size(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)))
                        Spacer(Modifier.height(3.dp))
                        Text(app.appName, color = TextPrimary, fontSize = 10.sp, maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
                repeat(4 - favoriteApps.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ── Sidebar ───────────────────────────────────────────────────────────────────
@Composable
private fun DrawerSidebar(
    sidebarEntries: List<SidebarEntry>,
    activeSidebarIdx: Int,
    onActivate: (Int) -> Unit,
    onDeactivate: () -> Unit,
    listState: LazyListState,
    scope: kotlinx.coroutines.CoroutineScope,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val sidebarPaddingDp = 56.dp
    var boxHeightPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(52.dp)
            .padding(vertical = sidebarPaddingDp)
            .onSizeChanged { boxHeightPx = it.height.toFloat() }
            .pointerInput(sidebarEntries) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val n = sidebarEntries.size
                        if (n > 0 && boxHeightPx > 0f) {
                            val idx = (offset.y / (boxHeightPx / n)).toInt().coerceIn(0, sidebarEntries.lastIndex)
                            onActivate(idx)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { listState.scrollToItem(sidebarEntries[idx].scrollIndex) }
                        }
                    },
                    onDragEnd = { onDeactivate() },
                    onDragCancel = { onDeactivate() },
                    onDrag = { change, _ ->
                        change.consume()
                        val n = sidebarEntries.size
                        if (n > 0 && boxHeightPx > 0f) {
                            val idx = (change.position.y / (boxHeightPx / n)).toInt().coerceIn(0, sidebarEntries.lastIndex)
                            if (activeSidebarIdx != idx) {
                                onActivate(idx)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { listState.scrollToItem(sidebarEntries[idx].scrollIndex) }
                            }
                        }
                    }
                )
            }
            .pointerInput(sidebarEntries) {
                detectTapGestures { offset ->
                    val n = sidebarEntries.size
                    if (n > 0 && boxHeightPx > 0f) {
                        val idx = (offset.y / (boxHeightPx / n)).toInt().coerceIn(0, sidebarEntries.lastIndex)
                        onActivate(idx)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { listState.scrollToItem(sidebarEntries[idx].scrollIndex) }
                    }
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            sidebarEntries.forEachIndexed { idx, entry ->
                val isActive = activeSidebarIdx == idx
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.5f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "sidebar_scale_$idx"
                )
                Text(
                    text = entry.label,
                    fontSize = if (isActive) 16.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) SidebarActive else SidebarColor,
                    modifier = Modifier.scale(scale).padding(horizontal = 2.dp),
                    maxLines = 1
                )
            }
        }
    }
}

// ── Ana Drawer ────────────────────────────────────────────────────────────────
@Composable
fun AllAppsDrawer(
    apps: List<AppInfo>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onClose: () -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
    iconSize: Dp = 40.dp,
    favoriteApps: List<AppInfo> = emptyList(),
    favoritesEnabled: Boolean = false,
    onFavoriteAppClick: (String) -> Unit = {},
    recentApps: List<AppInfo> = emptyList(),
    recentAppsEnabled: Boolean = false,
    onRecentAppClick: (String) -> Unit = {},
) {
    var dragOffset        by remember { mutableFloatStateOf(0f) }
    val context           = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val haptic            = LocalHapticFeedback.current
    val listState         = rememberLazyListState()
    val scope             = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        runCatching { searchFocusRequester.requestFocus() }
        keyboardController?.show()
    }

    var sortMode by remember {
        val saved = context.getSharedPreferences("app_organizer_prefs", android.content.Context.MODE_PRIVATE)
            .getString("all_apps_sort_mode", AllAppsSortMode.ALPHA.name)
        mutableStateOf(AllAppsSortMode.entries.firstOrNull { it.name == saved } ?: AllAppsSortMode.ALPHA)
    }
    var activeSidebarIdx by remember { mutableIntStateOf(-1) }
    var searchHistory    by remember { mutableStateOf(SearchHistoryPrefs.getHistory(context)) }
    var quickFilter      by remember { mutableStateOf(0) }

    val saveSearchIfNeeded = {
        if (searchQuery.trim().length >= 2) {
            SearchHistoryPrefs.addQuery(context, searchQuery)
            searchHistory = SearchHistoryPrefs.getHistory(context)
        }
    }

    val quickFilterCounts by remember {
        derivedStateOf {
            val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            intArrayOf(apps.size, apps.count { !it.isSystemApp }, apps.count { it.isSystemApp }, apps.count { it.lastUsedTimestamp > cutoff })
        }
    }

    var bgAlpha          by remember { mutableFloatStateOf(com.armutlu.apporganizer.utils.AppPrefs.getAllAppsBgAlpha(context)) }
    var notifTextEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)) }
    var unusedGreyDays   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getUnusedGreyDays(context)) }
    var iconPackPkg      by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context)) }

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(com.armutlu.apporganizer.utils.AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                com.armutlu.apporganizer.utils.AppPrefs.KEY_ALLAPPS_BG_ALPHA -> bgAlpha = com.armutlu.apporganizer.utils.AppPrefs.getAllAppsBgAlpha(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_NOTIFICATION_TEXT_ENABLED -> notifTextEnabled = com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_UNUSED_GREY_DAYS -> unusedGreyDays = com.armutlu.apporganizer.utils.AppPrefs.getUnusedGreyDays(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_ICON_PACK -> iconPackPkg = com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val sortedApps = remember(searchQuery, quickFilter, apps, sortMode) {
        val now = System.currentTimeMillis()
        val afterFilter = when (quickFilter) {
            1 -> apps.filter { !it.isSystemApp }
            2 -> apps.filter { it.isSystemApp }
            3 -> apps.filter { it.lastUsedTimestamp > now - 7L * 24 * 60 * 60 * 1000 }
            else -> apps
        }
        val trLocale = Locale("tr")
        val base = if (searchQuery.isBlank()) afterFilter
        else {
            val q = searchQuery.lowercase(trLocale)
            val exact    = mutableListOf<AppInfo>()
            val starts   = mutableListOf<AppInfo>()
            val contains = mutableListOf<AppInfo>()
            val fuzzy    = mutableListOf<Pair<AppInfo, Int>>()
            for (app in afterFilter) {
                val n = app.appName.lowercase(trLocale)
                val pkg = app.packageName.lowercase(trLocale)
                when {
                    n == q                    -> exact.add(app)
                    n.startsWith(q)           -> starts.add(app)
                    n.contains(q)             -> contains.add(app)
                    pkg.contains(q)           -> contains.add(app)
                    else -> {
                        val dist = n.split(" ").minOf { fuzzyEditDistance(it.take(20), q.take(20)) }
                        if (dist <= maxOf(2, q.length / 3))
                            fuzzy.add(app to dist)
                    }
                }
            }
            exact + starts + contains + fuzzy.sortedBy { it.second }.map { it.first }
        }
        when (sortMode) {
            AllAppsSortMode.ALPHA        -> base.sortedBy { it.appName.lowercase(Locale("tr")) }
            AllAppsSortMode.USAGE        -> base.sortedByDescending { it.usageCount }
            AllAppsSortMode.SIZE_DESC    -> base.sortedByDescending { it.appSizeBytes }
            AllAppsSortMode.SIZE_ASC     -> base.sortedBy { it.appSizeBytes }
            AllAppsSortMode.INSTALL_DATE -> base.sortedByDescending { it.installTime }
        }
    }

    LaunchedEffect(searchQuery, sortedApps.size) {
        if (searchQuery.trim().length >= 2) AppAnalytics.searchPerformed(searchQuery.trim(), sortedApps.size)
    }

    val grouped: Map<Char, List<AppInfo>> = remember(sortedApps, sortMode, searchQuery) {
        if (sortMode == AllAppsSortMode.ALPHA && searchQuery.isBlank())
            sortedApps.groupBy { app ->
                val first = app.appName.firstOrNull()?.toString()?.uppercase(Locale("tr"))?.firstOrNull() ?: '#'
                if (first.isLetter()) first else '#'
            }.toSortedMap(Comparator { a, b ->
                if (a == '#') 1 else if (b == '#') -1
                else java.text.Collator.getInstance(Locale("tr")).compare(a.toString(), b.toString())
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

    val drawerState = DrawerState(
        sortedApps = sortedApps,
        grouped = grouped,
        sidebarEntries = sidebarEntries,
        bgAlpha = bgAlpha,
        notifTextEnabled = notifTextEnabled,
        unusedGreyDays = unusedGreyDays,
        iconPackPkg = iconPackPkg,
        sortMode = sortMode
    )

    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    if (dragOffset > SWIPE_DOWN_THRESHOLD) { saveSearchIfNeeded(); keyboardController?.hide(); onClose() }
                    dragOffset = 0f
                },
                onDragCancel = { dragOffset = 0f },
                onVerticalDrag = { _, delta -> if (delta > 0) dragOffset += delta else dragOffset = 0f }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().blur(20.dp).background(Color.Black.copy(alpha = bgAlpha)))
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    DrawerSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onClose = onClose,
                        searchHistory = searchHistory,
                        onHistoryClear = { searchHistory = emptyList() },
                        searchFocusRequester = searchFocusRequester,
                        keyboardController = keyboardController,
                        saveSearchIfNeeded = saveSearchIfNeeded,
                        haptic = haptic,
                        totalCount = apps.size,
                        filteredCount = sortedApps.size,
                        quickFilter = quickFilter,
                        onQuickFilterChange = { quickFilter = it },
                        quickFilterCounts = quickFilterCounts,
                        sortMode = sortMode,
                        onSortModeChange = { sortMode = it },
                        context = context
                    )
                    DrawerAppList(
                        state = drawerState,
                        listState = listState,
                        searchQuery = searchQuery,
                        iconSize = iconSize,
                        favoritesEnabled = favoritesEnabled,
                        favoriteApps = favoriteApps,
                        onFavoriteAppClick = onFavoriteAppClick,
                        recentAppsEnabled = recentAppsEnabled,
                        recentApps = recentApps,
                        onRecentAppClick = onRecentAppClick,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        haptic = haptic,
                        saveSearchIfNeeded = saveSearchIfNeeded
                    )
                }
                if (sidebarEntries.isNotEmpty()) {
                    DrawerSidebar(
                        sidebarEntries = sidebarEntries,
                        activeSidebarIdx = activeSidebarIdx,
                        onActivate = { activeSidebarIdx = it },
                        onDeactivate = { activeSidebarIdx = -1 },
                        listState = listState,
                        scope = scope,
                        haptic = haptic
                    )
                }
            }
        }
    }
}

// ── Harf başlığı ─────────────────────────────────────────────────────────────
@Composable
private fun NiagaraLetterHeader(letter: Char, label: String? = null) {
    Text(
        text = label ?: letter.toString(),
        fontSize = if (label != null) 13.sp else 34.sp,
        fontWeight = FontWeight.Black, color = HeaderColor,
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 18.dp, bottom = 2.dp)
    )
}

// ── Uygulama satırı ───────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NiagaraAppRow(
    app: AppInfo,
    iconSize: Dp = 40.dp,
    isActive: Boolean = false,
    sortMode: AllAppsSortMode = AllAppsSortMode.ALPHA,
    notifTextEnabled: Boolean = false,
    unusedGreyDays: Int = 0,
    iconPackPkg: String = "",
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val icon = rememberAppIcon(app.packageName, iconPackPkg)
    val notifColor = when {
        app.notificationCount == 0 -> null
        app.notificationImportance >= 4 -> BadgeRed
        app.notificationImportance >= 3 -> BadgeGreen
        else -> BadgeYellow
    }

    // Sağ taraf bilgisi — moda göre değişir
    val trailingText: String? = when (sortMode) {
        AllAppsSortMode.SIZE_DESC, AllAppsSortMode.SIZE_ASC ->
            if (app.appSizeBytes > 0) formatBytes(app.appSizeBytes) else null
        AllAppsSortMode.INSTALL_DATE ->
            if (app.installTime > 0) fmtMonth(app.installTime) else null
        AllAppsSortMode.USAGE ->
            if (app.usageCount > 0) "${app.usageCount}×" else null
        AllAppsSortMode.ALPHA -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .semantics {
                contentDescription = "${app.appName}, ${app.categoryId.ifBlank { "Uygulama" }}"
                onClick(label = "Aç") { onClick(); true }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (isActive) RowHover else Color.Transparent)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // İkon + rozet (greyscale: sadece unusedGreyDays > 0 ise aktif)
        val saturation = when {
            unusedGreyDays <= 0  -> 1f   // ayar kapalı → her zaman renkli
            app.usageCount == 0L -> 0f
            app.usageCount < 5L  -> 0.4f + (app.usageCount * 0.12f)
            else                 -> 1f
        }
        val iconAlpha = if (unusedGreyDays > 0 && app.usageCount == 0L) 0.5f else 1f
        val greyFilter = if (saturation < 1f)
            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(saturation) }
            )
        else null
        Box(modifier = Modifier.size(iconSize + 8.dp), contentAlignment = Alignment.Center) {
            icon?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = app.appName,
                    modifier = Modifier.size(iconSize).clip(RoundedCornerShape(10.dp)),
                    alpha = iconAlpha,
                    colorFilter = greyFilter
                )
            } ?: run {
                Box(
                    modifier = Modifier.size(iconSize).clip(RoundedCornerShape(10.dp))
                        .background(HeaderColor.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.appName.firstOrNull()?.toString() ?: "?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (notifColor != null) {
                val count = app.notificationCount
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (count > 9) 18.dp else 14.dp)
                        .clip(CircleShape).background(notifColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (count > 0) Text(if (count > 99) "99+" else count.toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        // Isim + alt bilgi
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.appName, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (notifTextEnabled && app.notificationText.isNotBlank()) {
                Text(
                    app.notificationText,
                    fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.8f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                val catLabel = remember(app.categoryId) {
                    com.armutlu.apporganizer.domain.models.Category.getDefaultCategories()
                        .firstOrNull { it.categoryId == app.categoryId }?.categoryName
                }
                if (!catLabel.isNullOrBlank() && app.categoryId != "other" && app.categoryId != "uncategorized") {
                    Text(
                        catLabel,
                        fontSize = 11.sp, color = TextSecondary, maxLines = 1
                    )
                }
            }
        }

        // Sağ bilgi (boyut / tarih / kullanım)
        if (trailingText != null) {
            Text(
                trailingText, fontSize = 11.sp, color = SidebarColor, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

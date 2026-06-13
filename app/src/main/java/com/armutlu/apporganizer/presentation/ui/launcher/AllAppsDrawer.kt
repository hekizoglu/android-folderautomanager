package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.lazy.itemsIndexed
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.SearchHistoryPrefs
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
@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val iconPackPkg = remember { com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context) }
    val cacheKey = if (iconPackPkg.isEmpty()) "${packageName}_96" else "${packageName}_96_$iconPackPkg"
    return produceState<ImageBitmap?>(initialValue = iconCacheInternal[cacheKey], packageName, iconPackPkg) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val packBitmap = if (iconPackPkg.isNotEmpty())
                        com.armutlu.apporganizer.utils.IconPackManager.loadIcon(context, iconPackPkg, packageName, 96)
                    else null
                    packBitmap?.asImageBitmap()
                        ?: context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
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

// ── Ana Drawer ────────────────────────────────────────────────────────────────
@Composable
fun AllAppsDrawer(
    apps: List<AppInfo>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onClose: () -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
    iconSize: Dp = 40.dp
) {
    var dragOffset      by remember { mutableFloatStateOf(0f) }
    val context         = LocalContext.current
    val bgAlpha = com.armutlu.apporganizer.utils.AppPrefs.getAllAppsBgAlpha(context)
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    // Drawer açıldığında 300ms sonra klavyeyi aç (animasyon bitmeden açma)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        runCatching { searchFocusRequester.requestFocus() }
        keyboardController?.show()
    }

    var sortMode        by remember {
        val saved = context.getSharedPreferences("app_organizer_prefs", android.content.Context.MODE_PRIVATE)
            .getString("all_apps_sort_mode", AllAppsSortMode.ALPHA.name)
        mutableStateOf(AllAppsSortMode.entries.firstOrNull { it.name == saved } ?: AllAppsSortMode.ALPHA)
    }
    var activeSidebarIdx by remember { mutableIntStateOf(-1) }
    val haptic          = LocalHapticFeedback.current
    val listState       = rememberLazyListState()
    val scope           = rememberCoroutineScope()
    val density         = LocalDensity.current
    var searchHistory   by remember { mutableStateOf(SearchHistoryPrefs.getHistory(context)) }
    var quickFilter     by remember { mutableStateOf(0) } // 0=Tümü 1=Kullanıcı 2=Sistem 3=Son7gün

    // Arama geçmişini güncelle: sorgu boşken drawer kapanmadan önce kaydet
    val saveSearchIfNeeded = {
        if (searchQuery.trim().length >= 2) {
            SearchHistoryPrefs.addQuery(context, searchQuery)
            searchHistory = SearchHistoryPrefs.getHistory(context)
        }
    }

    val quickFilterLabels = listOf("Tümü", "Kullanıcı", "Sistem", "Son 7 gün")

    // Her chip'in sayımı — apps değişince yeniden hesapla, scroll/rekomposisyonda değil
    val quickFilterCounts = remember(apps) {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        intArrayOf(
            apps.size,
            apps.count { !it.isSystemApp },
            apps.count { it.isSystemApp },
            apps.count { it.lastUsedTimestamp > cutoff }
        )
    }

    // Bildirim metni ayarı — drawer açıldığında bir kere oku, her satırda değil
    val notifTextEnabled = remember { com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context) }

    // Sırala + filtrele
    val sortedApps = remember(apps, sortMode, searchQuery, quickFilter) {
        val now = System.currentTimeMillis()
        val afterQuickFilter = when (quickFilter) {
            1 -> apps.filter { !it.isSystemApp }
            2 -> apps.filter { it.isSystemApp }
            3 -> apps.filter { it.lastUsedTimestamp > now - 7L * 24 * 60 * 60 * 1000 }
            else -> apps
        }
        val base = if (searchQuery.isBlank()) afterQuickFilter
        else afterQuickFilter.filter { it.appName.contains(searchQuery, ignoreCase = true) }
        when (sortMode) {
            AllAppsSortMode.ALPHA        -> base.sortedBy { it.appName.lowercase() }
            AllAppsSortMode.USAGE        -> base.sortedByDescending { it.usageCount }
            AllAppsSortMode.SIZE_DESC    -> base.sortedByDescending { it.appSizeBytes }
            AllAppsSortMode.SIZE_ASC     -> base.sortedBy { it.appSizeBytes }
            AllAppsSortMode.INSTALL_DATE -> base.sortedByDescending { it.installTime }
        }
    }

    // Alfa gruplama sadece ALPHA modunda
    val grouped: Map<Char, List<AppInfo>> = remember(sortedApps, sortMode, searchQuery, quickFilter) {
        if (sortMode == AllAppsSortMode.ALPHA && searchQuery.isBlank())
            sortedApps.groupBy { app ->
                val c = app.appName.firstOrNull()?.uppercaseChar() ?: '#'
                if (c.isLetter()) c else '#'
            }.toSortedMap(compareBy { if (it == '#') Char.MAX_VALUE else it })
        else emptyMap()
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

    // Sidebar entries (contextual)
    val sidebarEntries = remember(sortedApps, sortMode, searchQuery, quickFilter) {
        if (searchQuery.isNotBlank()) emptyList()
        else if (sortMode == AllAppsSortMode.ALPHA) {
            grouped.keys.mapIndexed { i, letter ->
                SidebarEntry(letter.toString(), letterScrollIndex[letter] ?: 0)
            }
        } else {
            buildSidebarEntries(sortedApps, sortMode)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > SWIPE_DOWN_THRESHOLD) {
                            saveSearchIfNeeded()
                            keyboardController?.hide()
                            onClose()
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onVerticalDrag = { _, delta ->
                        if (delta > 0) dragOffset += delta else dragOffset = 0f
                    }
                )
            }
    ) {
        // Arka plan
        Box(modifier = Modifier.fillMaxSize().blur(20.dp).background(Color.Black.copy(alpha = bgAlpha)))

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {

                // ── Sol: Liste ─────────────────────────────────────────────
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

                    // Drag handle
                    Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(DragHandle))
                    }

                    Spacer(Modifier.height(10.dp))

                    // Başlık — uygulama sayısı
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Uygulamalar",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                        )
                        val countText = if (searchQuery.isNotBlank() || quickFilter != 0)
                            "${sortedApps.size} / ${apps.size}"
                        else
                            "${apps.size} uygulama"
                        Text(countText, fontSize = 12.sp, color = TextSecondary)
                    }

                    // Arama + kapat
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).height(44.dp)
                                .clip(RoundedCornerShape(22.dp)).background(SearchBg)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
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
                            onClick = {
                                saveSearchIfNeeded()
                                keyboardController?.hide()
                                onClose()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Close, "Kapat", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Son aramalar — sadece arama boşken ve geçmiş varken göster
                    if (searchQuery.isEmpty() && searchHistory.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            searchHistory.forEach { q ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSearchQueryChange(q)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(q, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        SearchHistoryPrefs.clear(context)
                                        searchHistory = emptyList()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text("Temizle", fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f))
                            }
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
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (active) SidebarColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        quickFilter = idx
                                    }
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

                    // Sıralama chip'leri (5 mod)
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(AllAppsSortMode.entries) { _, mode ->
                            val active = sortMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (active) HeaderColor else Color.White.copy(alpha = 0.12f))
                                    .clickable {
                                        sortMode = mode
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

                    // ── Liste ──────────────────────────────────────────────────
                    if (grouped.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            grouped.forEach { (letter, letterApps) ->
                                item(key = "header_$letter") {
                                    NiagaraLetterHeader(letter = letter)
                                }
                                items(items = letterApps, key = { it.packageName }) { app ->
                                    NiagaraAppRow(
                                        app = app, iconSize = iconSize, isActive = false,
                                        sortMode = sortMode,
                                        notifTextEnabled = notifTextEnabled,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            saveSearchIfNeeded()
                                            onAppClick(app.packageName)
                                        },
                                        onLongClick = { onAppLongClick?.invoke(app) }
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            if (sortedApps.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                                        Text("Sonuç bulunamadı", color = TextSecondary, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                items(items = sortedApps, key = { it.packageName }) { app ->
                                    NiagaraAppRow(
                                        app = app, iconSize = iconSize, isActive = false,
                                        sortMode = sortMode,
                                        notifTextEnabled = notifTextEnabled,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            saveSearchIfNeeded()
                                            onAppClick(app.packageName)
                                        },
                                        onLongClick = { onAppLongClick?.invoke(app) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Sağ: Contextual Sidebar ─────────────────────────────────
                if (sidebarEntries.isNotEmpty()) {
                    val sidebarPaddingDp = 56.dp
                    val sidebarPaddingPx = with(density) { sidebarPaddingDp.toPx() }
                    var boxHeightPx by remember { mutableStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(44.dp)
                            .padding(vertical = sidebarPaddingDp)
                            .onSizeChanged { boxHeightPx = it.height.toFloat() }
                            .pointerInput(sidebarEntries) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val pos = event.changes.firstOrNull()?.position ?: continue
                                        val n = sidebarEntries.size
                                        if (n == 0 || boxHeightPx == 0f) continue
                                        // pos.y is within padded box (0..boxHeightPx)
                                        val itemSlotPx = boxHeightPx / n
                                        val idx = (pos.y / itemSlotPx)
                                            .toInt()
                                            .coerceIn(0, sidebarEntries.lastIndex)
                                        if (activeSidebarIdx != idx) {
                                            activeSidebarIdx = idx
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            scope.launch {
                                                listState.scrollToItem(sidebarEntries[idx].scrollIndex)
                                            }
                                        }
                                        event.changes.forEach { it.consume() }
                                    } while (event.changes.none { it.changedToUp() })
                                    activeSidebarIdx = -1
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
                                    fontSize = if (isActive) 14.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) SidebarActive else SidebarColor,
                                    modifier = Modifier.scale(scale).padding(horizontal = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Harf başlığı ─────────────────────────────────────────────────────────────
@Composable
private fun NiagaraLetterHeader(letter: Char) {
    Text(
        text = letter.toString(),
        fontSize = 34.sp, fontWeight = FontWeight.Black, color = HeaderColor,
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
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val icon = rememberAppIcon(app.packageName)
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (isActive) RowHover else Color.Transparent)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // İkon + rozet (greyscale: usageCount=0 → gri+yarı saydam)
        val saturation = when {
            app.usageCount == 0L -> 0f
            app.usageCount < 5L  -> 0.4f + (app.usageCount * 0.12f)
            else                 -> 1f
        }
        val iconAlpha = if (app.usageCount == 0L) 0.5f else 1f
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

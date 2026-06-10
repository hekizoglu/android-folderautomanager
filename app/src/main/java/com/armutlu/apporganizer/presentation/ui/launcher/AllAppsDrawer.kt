package com.armutlu.apporganizer.presentation.ui.launcher

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Renkler ──────────────────────────────────────────────────────────────────
private val BgColor       = Color(0xCC000000)   // %80 siyah — duvar kağıdı görünür
private val HeaderColor   = Color(0xFF00897B)   // Teal — harf başlıkları
private val SidebarColor  = Color(0xFF26C6DA)   // Cyan — sidebar harfleri
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

enum class SortMode(val label: String) {
    ALPHA("A–Z"), USAGE("Kullanım"), RECENT("Son Açılan")
}

// ── Async ikon yükleme ────────────────────────────────────────────────────────
@Composable
private fun rememberAppIcon(packageName: String): Drawable? {
    val context = LocalContext.current
    return produceState<Drawable?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
        }
    }.value
}

// ── Ana Drawer ────────────────────────────────────────────────────────────────
@Composable
fun AllAppsDrawer(
    apps: List<AppInfo>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onClose: () -> Unit,
    onAppClick: (String) -> Unit,
    iconSize: Dp = 40.dp
) {
    var dragOffset      by remember { mutableFloatStateOf(0f) }
    var sortMode        by remember { mutableStateOf(SortMode.ALPHA) }
    var activeLetter    by remember { mutableStateOf<Char?>(null) }
    val haptic          = LocalHapticFeedback.current
    val listState       = rememberLazyListState()
    val scope           = rememberCoroutineScope()
    val density         = LocalDensity.current

    // Sırala + filtrele
    val sortedApps = remember(apps, sortMode, searchQuery) {
        val base = if (searchQuery.isBlank()) apps
        else apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
        when (sortMode) {
            SortMode.ALPHA   -> base.sortedBy { it.appName.lowercase() }
            SortMode.USAGE   -> base.sortedByDescending { it.usageCount }
            SortMode.RECENT  -> base.sortedByDescending { it.lastUsedTimestamp }
        }
    }

    // A-Z gruplama (sadece ALPHA + arama yok)
    val grouped: Map<Char, List<AppInfo>> = remember(sortedApps, sortMode, searchQuery) {
        if (sortMode == SortMode.ALPHA && searchQuery.isBlank())
            sortedApps.groupBy { app ->
                val c = app.appName.firstOrNull()?.uppercaseChar() ?: '#'
                if (c.isLetter()) c else '#'
            }.toSortedMap(compareBy { if (it == '#') Char.MAX_VALUE else it })
        else emptyMap()
    }

    // Sidebar harfleri + scroll-offset hesabı
    val sidebarLetters = remember(grouped) { grouped.keys.toList() }

    // grouped'dan her harfin LazyColumn indeksi: header + item sayıları
    val letterScrollIndex = remember(grouped) {
        val map = mutableMapOf<Char, Int>()
        var idx = 0
        grouped.forEach { (letter, list) ->
            map[letter] = idx
            idx += 1 + list.size  // 1 header + N item
        }
        map
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { if (dragOffset > SWIPE_DOWN_THRESHOLD) onClose(); dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f },
                    onVerticalDrag = { _, delta ->
                        if (delta > 0) dragOffset += delta else dragOffset = 0f
                    }
                )
            }
    ) {
        // Katman 1: arka plan blur
        Box(modifier = Modifier.fillMaxSize().blur(20.dp).background(BgColor))

        // Katman 2: içerik
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {

                // ── Sol: Liste ─────────────────────────────────────────────
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

                    // Drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp).height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DragHandle)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Arama + kapat satırı
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(SearchBg)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text("Uygulama ara...", color = TextSecondary, fontSize = 14.sp)
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        singleLine = true,
                                        cursorBrush = SolidColor(HeaderColor),
                                        textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp)
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChange("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Temizle", tint = TextSecondary, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, "Kapat", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Sıralama chip'leri
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortMode.entries.forEach { mode ->
                            val active = sortMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (active) HeaderColor else Color.White.copy(alpha = 0.12f)
                                    )
                                    .clickable { sortMode = mode }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    mode.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    color = if (active) Color.White else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── Niagara-stili liste ──────────────────────────────────
                    if (grouped.isNotEmpty()) {
                        // Gruplu alfabetik liste — sticky harfler
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            grouped.forEach { (letter, letterApps) ->
                                // Harf başlığı (sticky değil — stickyHeader API stable değil tüm versiyonlarda)
                                item(key = "header_$letter") {
                                    NiagaraLetterHeader(letter = letter)
                                }
                                items(items = letterApps, key = { it.packageName }) { app ->
                                    NiagaraAppRow(
                                        app = app,
                                        iconSize = iconSize,
                                        isActive = activeLetter == app.appName.firstOrNull()?.uppercaseChar(),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onAppClick(app.packageName)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Arama sonucu veya kullanım/son sıralaması — düz liste
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            if (sortedApps.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Sonuç bulunamadı", color = TextSecondary, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                items(items = sortedApps, key = { it.packageName }) { app ->
                                    NiagaraAppRow(
                                        app = app,
                                        iconSize = iconSize,
                                        isActive = false,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onAppClick(app.packageName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Sağ: A-Z Sidebar ────────────────────────────────────────
                if (sortMode == SortMode.ALPHA && searchQuery.isEmpty() && sidebarLetters.isNotEmpty()) {
                    val itemHeightDp = 22.dp

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(30.dp)
                            .padding(vertical = 56.dp)
                            .pointerInput(sidebarLetters) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val pos = event.changes.firstOrNull()?.position ?: continue
                                        val itemPx = with(density) { itemHeightDp.toPx() }
                                        val idx = (pos.y / itemPx)
                                            .toInt()
                                            .coerceIn(0, sidebarLetters.lastIndex)
                                        val letter = sidebarLetters[idx]
                                        if (activeLetter != letter) {
                                            activeLetter = letter
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val scrollIdx = letterScrollIndex[letter] ?: 0
                                            scope.launch { listState.scrollToItem(scrollIdx) }
                                        }
                                        event.changes.forEach { it.consume() }
                                    } while (event.changes.none { it.changedToUp() })
                                    activeLetter = null
                                }
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            sidebarLetters.forEach { letter ->
                                val isActive = activeLetter == letter
                                val scale by animateFloatAsState(
                                    targetValue = if (isActive) 1.5f else 1f,
                                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                    label = "sidebar_scale"
                                )
                                Text(
                                    text = letter.toString(),
                                    fontSize = if (isActive) 13.sp else 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) SidebarActive else SidebarColor,
                                    modifier = Modifier
                                        .height(itemHeightDp)
                                        .scale(scale)
                                        .padding(horizontal = 2.dp)
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
        fontSize = 34.sp,
        fontWeight = FontWeight.Black,
        color = HeaderColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 18.dp, bottom = 2.dp)
    )
}

// ── Uygulama satırı ───────────────────────────────────────────────────────────
@Composable
fun NiagaraAppRow(
    app: AppInfo,
    iconSize: Dp = 40.dp,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val icon = rememberAppIcon(app.packageName)
    val notifColor = when {
        (app.notificationCount ?: 0) == 0 -> null
        (app.notificationImportance ?: 0) >= 4 -> BadgeRed
        (app.notificationImportance ?: 0) >= 3 -> BadgeGreen
        else -> BadgeYellow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
            .background(if (isActive) RowHover else Color.Transparent)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // İkon + renk kodlu badge
        Box(modifier = Modifier.size(iconSize + 8.dp), contentAlignment = Alignment.Center) {
            if (icon != null) {
                androidx.compose.foundation.Image(
                    painter = rememberDrawablePainter(icon),
                    contentDescription = app.appName,
                    modifier = Modifier.size(iconSize).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HeaderColor.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.firstOrNull()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Renk kodlu bildirim rozeti
            if (notifColor != null) {
                val count = app.notificationCount ?: 0
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (count > 9) 18.dp else 14.dp)
                        .clip(CircleShape)
                        .background(notifColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (count > 0) {
                        Text(
                            text = if (count > 99) "99+" else count.toString(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        // Uygulama adı — Niagara'da dominant
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (app.categoryId.isNotBlank() && app.categoryId != "other") {
                Text(
                    text = app.categoryId.replaceFirstChar { it.uppercase() },
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }

        // Kullanım sayacı (USAGE modunda bilgi)
        if (app.usageCount > 0) {
            Text(
                text = "${app.usageCount}×",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

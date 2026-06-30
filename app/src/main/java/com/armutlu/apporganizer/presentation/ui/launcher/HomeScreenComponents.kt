package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.SearchHistoryPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.armutlu.apporganizer.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun PixelClockWidget(modifier: Modifier = Modifier) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale("tr")) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            fontSize = 84.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-3).sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Tarih + hava durumu yan yana
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                .clickable {
                    // Sistem hava durumu uygulamasını aç
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity")
                        putExtra("query", "hava durumu")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }.onFailure {
                        val fallback = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=hava+durumu")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(fallback) }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = dateFormat.format(now).replaceFirstChar { it.uppercase() },
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )
            Text(
                text = "  ·  🌤️",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp
            )
            Text(
                text = " Hava",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 13.sp
            )
        }
    }
}

/** Pixel Launcher stili Google arama cubugu — tikklayinca Google web aramasi acar. */
@Composable
internal fun GoogleSearchBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .height(50.dp)
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(25.dp))
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(25.dp)
            )
            .padding(horizontal = 16.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("G", color = Color(0xFF4285F4), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("o", color = Color(0xFFEA4335), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("o", color = Color(0xFFFBBC04), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("g", color = Color(0xFF4285F4), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("l", color = Color(0xFF34A853), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("e", color = Color(0xFFEA4335), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Ara veya URL gir",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Sesli ara",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(22.dp)
                .clickable {
                    val voiceIntent = Intent("android.speech.action.WEB_SEARCH").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(voiceIntent) }
                }
        )
    }
}

/** Frosted pill dock — packages listesi DockPrefs'ten gelir, kullanici tarafindan secilebilir. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PixelDock(
    packages: List<String>,
    iconPackPkg: String = "",
    onLaunchApp: (String) -> Unit,
    onLongPress: () -> Unit = {},
    onAppLongPress: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val visiblePkgs = remember(packages) {
        packages.filter { pm.getLaunchIntentForPackage(it) != null }
    }

    Box(
        modifier = modifier
            .height(72.dp)
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50)
            )
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visiblePkgs.forEach { pkg ->
                val label = remember(pkg) {
                    runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                }
                DockIcon(
                    packageName = pkg,
                    label = label,
                    iconPackPkg = iconPackPkg,
                    iconSize = 48.dp,
                    onClick = { onLaunchApp(pkg) },
                    onLongClick = { onAppLongPress(pkg) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DockIcon(
    packageName: String,
    label: String,
    iconPackPkg: String = "",
    iconSize: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { iconSize.roundToPx() }
    val cacheKey = if (iconPackPkg.isEmpty()) "${packageName}_$px" else "${packageName}_${px}_$iconPackPkg"
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = iconCacheInternal[cacheKey],
        key1 = cacheKey
    ) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val packBitmap = if (iconPackPkg.isNotEmpty())
                        com.armutlu.apporganizer.utils.IconPackManager.loadIcon(context, iconPackPkg, packageName, px)
                    else null
                    packBitmap?.asImageBitmap()
                        ?: com.armutlu.apporganizer.utils.loadAppIcon(context, packageName, px)?.asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) iconCacheInternal.put(cacheKey, loaded)
            value = loaded
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp)
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = label,
                modifier = Modifier.size(iconSize)
            )
        } ?: Box(
            modifier = Modifier
                .size(iconSize)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .semantics { contentDescription = label }
        )
    }
}

/** Pixel Launcher tarz sonKullanilan/onerileri satiri — 4 ikon yatay dizilir. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppSuggestionsRow(
    apps: List<AppInfo>,
    iconPackPkg: String = "",
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val labelRes = remember(hour) {
        when {
            hour in 6..10  -> R.string.suggestions_label_morning
            hour in 11..13 -> R.string.suggestions_label_noon
            hour in 14..17 -> R.string.suggestions_label_afternoon
            else            -> R.string.suggestions_label_evening  // 18-05 arası
        }
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = 20.dp,
        backgroundAlpha = 0.13f,
        borderAlpha = 0.22f
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(labelRes),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                apps.forEach { app ->
                    SuggestionAppItem(
                        app = app,
                        iconPackPkg = iconPackPkg,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionAppItem(
    app: AppInfo,
    iconPackPkg: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val iconSize = 48.dp
    val px = with(LocalDensity.current) { iconSize.roundToPx() }
    val cacheKey = if (iconPackPkg.isEmpty()) "${app.packageName}_$px" else "${app.packageName}_${px}_$iconPackPkg"
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = iconCacheInternal[cacheKey],
        key1 = cacheKey
    ) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val packBitmap = if (iconPackPkg.isNotEmpty())
                        com.armutlu.apporganizer.utils.IconPackManager.loadIcon(context, iconPackPkg, app.packageName, px)
                    else null
                    packBitmap?.asImageBitmap()
                        ?: com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, px)?.asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) iconCacheInternal.put(cacheKey, loaded)
            value = loaded
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = app.appName,
                modifier = Modifier.size(iconSize)
            )
        } ?: Box(
            modifier = Modifier
                .size(iconSize)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .semantics { contentDescription = app.appName }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.appName,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(60.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FavoritesRow(
    apps: List<AppInfo>,
    iconPackPkg: String = "",
    onAppClick: (String) -> Unit,
    onAppLongClick: ((String) -> Unit)? = null,
) {
    if (apps.isEmpty()) return
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            "Favoriler",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps, key = { it.packageName }) { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = app.appName
                        }
                        .combinedClickable(
                            onClick = { onAppClick(app.packageName) },
                            onLongClick = { onAppLongClick?.invoke(app.packageName) }
                        )
                ) {
                    val cacheKey = remember(app.packageName, app.lastUpdatedTime, iconPackPkg) {
                        if (iconPackPkg.isNotEmpty()) "${app.packageName}_48_${app.lastUpdatedTime}_$iconPackPkg"
                        else "${app.packageName}_48_${app.lastUpdatedTime}"
                    }
                    val bitmap by produceState<ImageBitmap?>(null, cacheKey) {
                        value = withContext(Dispatchers.IO) {
                            iconCacheInternal[cacheKey] ?: run {
                                val bmp = runCatching {
                                    com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, 96)?.asImageBitmap()
                                }.getOrNull()
                                if (bmp != null) iconCacheInternal.put(cacheKey, bmp)
                                bmp
                            }
                        }
                    }
                    bitmap?.let {
                        Image(bitmap = it, contentDescription = app.appName,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                    } ?: Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)))
                    Spacer(Modifier.height(4.dp))
                    Text(app.appName, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp,
                        maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
internal fun RecentAppsRow(
    apps: List<AppInfo>,
    iconPackPkg: String = "",
    onAppClick: (String) -> Unit,
) {
    if (apps.isEmpty()) return
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            "Son Kullanılanlar",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps.take(8), key = { it.packageName }) { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = app.appName
                        }
                        .clickable { onAppClick(app.packageName) }
                ) {
                    val cacheKey = remember(app.packageName, app.lastUpdatedTime, iconPackPkg) {
                        if (iconPackPkg.isNotEmpty()) "${app.packageName}_48_${app.lastUpdatedTime}_$iconPackPkg"
                        else "${app.packageName}_48_${app.lastUpdatedTime}"
                    }
                    val bitmap by produceState<ImageBitmap?>(null, cacheKey) {
                        value = withContext(Dispatchers.IO) {
                            iconCacheInternal[cacheKey] ?: run {
                                val bmp = runCatching {
                                    com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, 96)?.asImageBitmap()
                                }.getOrNull()
                                if (bmp != null) iconCacheInternal.put(cacheKey, bmp)
                                bmp
                            }
                        }
                    }
                    bitmap?.let {
                        Image(bitmap = it, contentDescription = app.appName,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                    } ?: Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)))
                    Spacer(Modifier.height(4.dp))
                    Text(app.appName, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp,
                        maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
internal fun SwipeHint(context: Context, visible: Boolean) {
    // swipeHintEnabled — SharedPrefs'ten reaktif okuma; incrementSwipeHintCount sonrası state güncellenir
    var hintAllowed by remember { mutableStateOf(AppPrefs.shouldShowSwipeHint(context)) }
    val showSwipeHint = visible && hintAllowed
    val infiniteTransition = rememberInfiniteTransition(label = "swipe_hint")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swipe_y"
    )
    LaunchedEffect(showSwipeHint) {
        if (showSwipeHint) {
            AppPrefs.incrementSwipeHintCount(context)
            hintAllowed = AppPrefs.shouldShowSwipeHint(context)
        }
    }
    if (showSwipeHint && visible) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Yukarı kaydırarak tüm uygulamaları aç"
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Yukarı kaydırma ipucu",
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp).offset(y = offsetY.dp)
            )
            Text(
                text = "Tüm uygulamalar",
                color = Color.White.copy(alpha = 0.40f),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Ana ekran uygulama arama çubuğu — allApps içinde isim arar, sonuçlar anlık gösterilir.
 * Long-press (300ms) → drag handle görünür + scale(1.04f); bırakınca snap noktasına oturur.
 */
@Composable
internal fun HomeAppSearchBar(
    allApps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPositionSnap: ((String) -> Unit)? = null,
    folderQuery: String? = null,
    onFolderQueryChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    // Klasör arama modu: false=uygulama, true=klasör
    var folderMode by remember { mutableStateOf(false) }
    val activeFolderMode = folderMode && folderQuery != null

    val results = remember(query, allApps, activeFolderMode) {
        if (activeFolderMode || query.isBlank()) emptyList()
        else allApps
            .filter { it.appName.lowercase(Locale("tr")).contains(query.lowercase(Locale("tr"))) }
            .sortedBy { it.appName }
            .take(6)
    }

    // Klasör arama modundayken folderQuery'yi güncelle
    LaunchedEffect(query, activeFolderMode) {
        if (activeFolderMode) onFolderQueryChange?.invoke(query)
        else if (!activeFolderMode && folderQuery != null) onFolderQueryChange?.invoke("")
    }
    val historyItems = remember(query) {
        if (query.isNotBlank()) emptyList()
        else SearchHistoryPrefs.getHistory(context)
    }

    // Drag handle state
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var showGhostZones by remember { mutableStateOf(false) }
    val barScale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "search_bar_scale"
    )

    Column(modifier = modifier) {
        // Ghost zones — TOP / BOTTOM snap hedefleri
        if (showGhostZones) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (dragOffsetY < 0) Color.White.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.07f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↑ Üst",
                    color = Color.White.copy(alpha = if (dragOffsetY < 0) 0.80f else 0.30f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        // Arama alanı — glass kart stilinde + drag handle
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .scale(barScale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            isDragging = true
                            showGhostZones = true
                        }
                    )
                }
                .then(
                    if (isDragging) Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                val snapPos = if (dragOffsetY < 0)
                                    AppPrefs.SEARCH_BAR_POS_TOP
                                else
                                    AppPrefs.SEARCH_BAR_POS_BOTTOM
                                AppPrefs.setSearchBarPosition(context, snapPos)
                                onPositionSnap?.invoke(snapPos)
                                isDragging = false
                                showGhostZones = false
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                showGhostZones = false
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                            }
                        )
                    } else Modifier
                ),
            cornerRadius = 28.dp,
            backgroundAlpha = if (isDragging) 0.22f else 0.12f,
            borderAlpha = if (isDragging) 0.45f else 0.25f
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Sekme row — sadece klasör araması da etkinse göster
                if (folderQuery != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(false to "Uygulama", true to "Klasör").forEach { (isFolderTab, label) ->
                            val selected = activeFolderMode == isFolderTab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) Color.White.copy(alpha = 0.22f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        folderMode = isFolderTab
                                        query = ""
                                    }
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    label,
                                    color = Color.White.copy(alpha = if (selected) 1f else 0.45f),
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Ara",
                        tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text(
                                    if (activeFolderMode) "Klasör ara…" else "Uygulama, kategori ara…",
                                    color = Color.White.copy(alpha = 0.40f), fontSize = 14.sp
                                )
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        Icon(Icons.Default.Close, contentDescription = null,
                            tint = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.size(18.dp).clickable { query = "" })
                    }
                }
            }
        }

        // Ghost zone — BOTTOM
        if (showGhostZones) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (dragOffsetY > 0) Color.White.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.07f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↓ Alt",
                    color = Color.White.copy(alpha = if (dragOffsetY > 0) 0.80f else 0.30f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Arama geçmişi chip row — sorgu boşken gösterilir
        if (historyItems.isNotEmpty() && query.isBlank() && !isDragging) {
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(historyItems) { histQuery ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { query = histQuery }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Search, null,
                            tint = Color.White.copy(alpha = 0.50f), modifier = Modifier.size(12.dp))
                        Text(histQuery, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        Icon(Icons.Default.Close, null,
                            tint = Color.White.copy(alpha = 0.40f),
                            modifier = Modifier.size(12.dp).clickable {
                                SearchHistoryPrefs.clear(context)
                            })
                    }
                }
            }
        }

        // Sonuç listesi
        if (results.isNotEmpty() && !isDragging) {
            Spacer(Modifier.height(4.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, backgroundAlpha = 0.18f) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    results.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SearchHistoryPrefs.addQuery(context, query)
                                    query = ""
                                    onAppClick(app.packageName)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val cacheKey = "${app.packageName}_32_${app.lastUpdatedTime}"
                            val icon by produceState<ImageBitmap?>(null, cacheKey) {
                                value = withContext(Dispatchers.IO) {
                                    iconCacheInternal[cacheKey] ?: run {
                                        val bmp = runCatching {
                                            com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, 64)?.asImageBitmap()
                                        }.getOrNull()
                                        if (bmp != null) iconCacheInternal.put(cacheKey, bmp)
                                        bmp
                                    }
                                }
                            }
                            if (icon != null) {
                                Image(bitmap = icon!!, contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                            } else {
                                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f)))
                            }
                            Text(app.appName, color = Color.White.copy(alpha = 0.90f),
                                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ana ekran klasör arama çubuğu.
 * [query] boş değilken aktif — 30s hareketsizlikte [onClear] tetiklenir.
 * [countdown] dışarıdan yönetilir (HomeScreen LaunchedEffect).
 */
@Composable
internal fun FolderSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    countdown: Int,
    modifier: Modifier = Modifier
) {
    val active = query.isNotEmpty() || countdown < 30
    Row(
        modifier = modifier
            .height(44.dp)
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else Color.White.copy(alpha = 0.18f),
                RoundedCornerShape(22.dp)
            )
            .background(
                color = Color.White.copy(alpha = if (active) 0.18f else 0.10f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Klasör ara",
            tint = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            decorationBox = { inner ->
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Klasör ara...",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.weight(1f)
        )
        if (active) {
            Text(
                text = "${countdown}s",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier.wrapContentWidth()
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Aramayı temizle",
                tint = Color.White.copy(alpha = 0.70f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onClear() }
            )
        }
    }
}

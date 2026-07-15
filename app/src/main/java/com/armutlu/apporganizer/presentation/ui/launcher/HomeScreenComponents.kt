package com.armutlu.apporganizer.presentation.ui.launcher

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.provider.Settings
import com.armutlu.apporganizer.presentation.ui.MainActivity
import com.armutlu.apporganizer.presentation.navigation.Routes
import com.armutlu.apporganizer.presentation.ui.screens.isNotificationListenerGranted
import com.armutlu.apporganizer.utils.UsageStatsHelper
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType
import com.armutlu.apporganizer.presentation.ui.common.diamondShine
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.ContactActionPrefs
import com.armutlu.apporganizer.utils.DockPrefs
import com.armutlu.apporganizer.utils.SearchCache
import com.armutlu.apporganizer.utils.SearchHistoryPrefs
import com.armutlu.apporganizer.utils.SearchOverlayDecisions
import com.armutlu.apporganizer.utils.SearchStatsPrefs
import com.armutlu.apporganizer.utils.SystemSettingsCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.armutlu.apporganizer.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * P1.3: contactId bossa (numara yoksa/parse hatasi) sessizce atlar.
 * Ayar kapaliysa (KEY_CONTACT_SUGGESTIONS_ENABLED=false) hicbir kayit yazilmaz.
 */
private fun homeLogContactAction(
    context: Context,
    contactId: String,
    action: com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType
) {
    if (contactId.isBlank()) return
    if (!AppPrefs.isContactSuggestionsEnabled(context)) return
    com.armutlu.apporganizer.utils.ContactActionPrefs.logAction(context, contactId, action)
}

/**
 * @param compact Dar ekran / kalabalık ana ekranda saat küçülür (84sp→56sp) ve tarih pili gizlenir
 *                — klasörler kaybolmasın diye alan saate değil klasörlere verilir.
 */
@Composable
internal fun PixelClockWidget(modifier: Modifier = Modifier, compact: Boolean = false) {
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
            fontSize = if (compact) 56.sp else 84.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = if (compact) (-2).sp else (-3).sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Tarih + hava durumu yan yana — compact modda gizli
        if (!compact) Row(
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
    folders: List<AppFolder> = emptyList(),
    iconPackPkg: String = "",
    onLaunchApp: (String) -> Unit,
    onOpenFolder: (AppFolder) -> Unit = {},
    onLongPress: () -> Unit = {},
    onAppLongPress: (String) -> Unit = {},
    onFolderLongPress: (AppFolder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val visibleItems = remember(packages, folders) {
        packages.mapNotNull { item ->
            val folderId = DockPrefs.folderId(item)
            if (folderId != null) {
                folders
                    .firstOrNull { it.category.categoryId == folderId && it.apps.isNotEmpty() }
                    ?.let { DockDisplayItem.Folder(item, it) }
            } else if (pm.getLaunchIntentForPackage(item) != null) {
                DockDisplayItem.App(item)
            } else {
                null
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compactDock = maxWidth < 360.dp
        val dockHeight = if (compactDock) 64.dp else 72.dp
        val iconSize = if (compactDock) 42.dp else 48.dp
        val horizontalPadding = if (compactDock) 8.dp else 12.dp
        val slotSpacing = if (compactDock) 2.dp else 6.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dockHeight)
                .background(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                )
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onLongPress() })
                }
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(slotSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleItems.forEach { item ->
                    when (item) {
                        is DockDisplayItem.App -> {
                            val pkg = item.packageName
                            val label = remember(pkg) {
                                runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                DockIcon(
                                    packageName = pkg,
                                    label = label,
                                    iconPackPkg = iconPackPkg,
                                    iconSize = iconSize,
                                    onClick = { onLaunchApp(pkg) },
                                    onLongClick = { onAppLongPress(pkg) }
                                )
                            }
                        }
                        is DockDisplayItem.Folder -> {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                DockFolderIcon(
                                    folder = item.folder,
                                    iconSize = iconSize,
                                    onClick = { onOpenFolder(item.folder) },
                                    onLongClick = { onFolderLongPress(item.folder) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class DockDisplayItem {
    data class App(val packageName: String) : DockDisplayItem()
    data class Folder(val item: String, val folder: AppFolder) : DockDisplayItem()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockFolderIcon(
    folder: AppFolder,
    iconSize: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val label = folder.category.categoryName
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
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = folder.category.iconEmoji,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 3.dp, bottom = 1.dp)
            )
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
    // lastUpdateTime key'de: uygulama güncellenince eski logo cache'i geçersizleşir
    val lastUpdated = remember(packageName) {
        runCatching { context.packageManager.getPackageInfo(packageName, 0).lastUpdateTime }.getOrDefault(0L)
    }
    val cacheKey = if (iconPackPkg.isEmpty()) "${packageName}_${lastUpdated}_$px"
        else "${packageName}_${lastUpdated}_${px}_$iconPackPkg"
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
    iconSizeDp: Int = 40,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return
    // ROADMAP #29: 4→3'ten sonra tersine 5 öneriye çıkarıldı (kullanıcı isteği).
    val visibleApps = remember(apps) { apps.take(5) }
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val labelRes = remember(hour) {
        when {
            hour in 6..10  -> R.string.suggestions_label_morning
            hour in 11..13 -> R.string.suggestions_label_noon
            hour in 14..17 -> R.string.suggestions_label_afternoon
            else            -> R.string.suggestions_label_evening  // 18-05 arası
        }
    }
    // D244: bölüm çok yer kaplıyordu — dış/iç padding sıkılaştırıldı, ikon boyutu
    // artık Ayarlar'dan gelen iconSizeDp parametresi (varsayılan 40dp, önceki 48dp).
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.dp),
        cornerRadius = 16.dp,
        backgroundAlpha = 0.13f,
        borderAlpha = 0.22f
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ROADMAP #29: teknik detay ("Son 28 gün + bu saat") artık başlığın hemen yanında,
            // aynı satırda küçük fontla — eskiden sağdaki ayrı rozette gizliydi.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 3.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(labelRes),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.suggestions_meta_signal),
                    color = Color.White.copy(alpha = 0.38f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // key(packageName): forEach'te kararlı kimlik — liste değişince produceState'in
                // önceki ikonu tutup yeni etiketle eşleşmesini (Instagram logo + Akbank yazı) önler.
                visibleApps.forEach { app ->
                    key(app.packageName) {
                        SuggestionAppItem(
                            app = app,
                            iconPackPkg = iconPackPkg,
                            iconSize = iconSizeDp.dp,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) }
                        )
                    }
                }
                SuggestionSignalPill(count = visibleApps.size)
            }
        }
    }
}

// D278'de burada ayrıca "Son 28 gün + bu saat" sinyal metni de vardı — ROADMAP #29 ile
// başlığın yanına taşındı, burada tekrar etmesin diye kaldırıldı (sadece öneri sayısı kalır).
@Composable
private fun SuggestionSignalPill(count: Int) {
    Box(
        modifier = Modifier
            .widthIn(min = 56.dp, max = 80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.suggestions_meta_count, count),
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
internal fun RecentNotificationAppsRow(
    apps: List<AppInfo>,
    notificationCounts: Map<String, Int>,
    iconPackPkg: String = "",
    iconSizeDp: Int = 38,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.dp),
        cornerRadius = 16.dp,
        backgroundAlpha = 0.12f,
        borderAlpha = 0.20f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.recent_notifications_row_title),
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    apps.take(4).forEach { app ->
                        key(app.packageName) {
                            SuggestionAppItem(
                                app = app,
                                iconPackPkg = iconPackPkg,
                                iconSize = iconSizeDp.dp,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                        }
                    }
                }
            }
            val total = apps.sumOf { notificationCounts[it.packageName] ?: 0 }
            Column(
                modifier = Modifier
                    .widthIn(min = 88.dp, max = 112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.recent_notifications_row_count, total),
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.recent_notifications_row_signal),
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionAppItem(
    app: AppInfo,
    iconPackPkg: String,
    iconSize: Dp = 40.dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { iconSize.roundToPx() }
    // lastUpdatedTime key'de: uygulama güncellenince eski logo cache'i geçersizleşir (isim/logo uyumsuzluğu fix)
    val cacheKey = if (iconPackPkg.isEmpty()) "${app.packageName}_${app.lastUpdatedTime}_$px"
        else "${app.packageName}_${app.lastUpdatedTime}_${px}_$iconPackPkg"
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
            .padding(horizontal = 6.dp, vertical = 2.dp)
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
        Spacer(Modifier.height(2.dp))
        Text(
            text = app.appName,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
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
 * Ana ekran birleşik arama çubuğu (S1) — tek sorguda uygulama + klasör + kişi + dosya
 * sonuçları kaynak gruplarıyla gösterilir (AllAppsDrawer'daki SourceGroupHeader pattern'i).
 * "Uygulama / Klasör" sekmesi kaldırıldı; klasör eşleşmeleri "Klasörler" sonuç grubudur.
 * Kişi araması izin verilmişse varsayılan etkindir; izin yoksa "Kişiler" grubunda
 * "izin ver" kısayolu görünür (S2).
 * Long-press (300ms) → drag handle görünür + scale(1.04f); bırakınca snap noktasına oturur.
 */
@Composable
internal fun HomeAppSearchBar(
    allApps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPositionSnap: ((String) -> Unit)? = null,
    folders: List<AppFolder> = emptyList(),
    folderCustomNames: Map<String, String> = emptyMap(),
    folderCustomEmojis: Map<String, String> = emptyMap(),
    onFolderClick: (AppFolder) -> Unit = {},
    searchResults: Map<SourceType, List<SearchDocument>> = emptyMap(),
    onQueryChange: (String) -> Unit = {},
    onEnableContactsSource: () -> Unit = {},
    onEnableFilesSource: () -> Unit = {},
    // P0.3: dosya kaynağı izin/indeks durumu — PermissionRequired iken "izin gerekli" satırı gösterilir,
    // sahte "0 sonuç" izlenimi verilmez.
    filesIndexState: com.armutlu.apporganizer.domain.models.FileIndexState =
        com.armutlu.apporganizer.domain.models.FileIndexState.Disabled,
    fullScreenEnabled: Boolean = false,
    onOpenFullScreen: () -> Unit = {},
    homeResumeTrigger: Int = 0,
    // Çubuk alttayken sonuçlar ÜSTTE (yukarı doğru) açılır — sayfa kaymaz (D258)
    resultsAbove: Boolean = false
) {
    val context = LocalContext.current

    if (fullScreenEnabled) {
        var isDragging by remember { mutableStateOf(false) }
        var dragOffsetY by remember { mutableStateOf(0f) }
        var showGhostZones by remember { mutableStateOf(false) }
        val shineEnabled = AppPrefs.isSearchShineEnabled(context)
        val barScale by animateFloatAsState(
            targetValue = if (isDragging) 1.04f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "fullscreen_search_bar_scale"
        )

        Column(modifier = modifier) {
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
                    Text("↑ Üst", color = Color.White.copy(alpha = if (dragOffsetY < 0) 0.80f else 0.30f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(barScale)
                    .diamondShine(shineEnabled, RoundedCornerShape(28.dp), trigger = homeResumeTrigger)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (!isDragging) onOpenFullScreen() },
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
                                    val snapPos = if (dragOffsetY < 0) AppPrefs.SEARCH_BAR_POS_TOP else AppPrefs.SEARCH_BAR_POS_BOTTOM
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
                borderAlpha = if (isDragging) 0.45f else 0.25f,
                borderColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.search_overlay_title),
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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
                    Text("↓ Alt", color = Color.White.copy(alpha = if (dragOffsetY > 0) 0.80f else 0.30f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    var query by rememberSaveable { mutableStateOf("") }

    // Arama ayarları — Ayarlar ekranından dönünce canlı güncellensin (Reaktif AppPrefs pattern'i, LEARNINGS)
    var fuzzy         by remember { mutableStateOf(AppPrefs.isSearchFuzzyEnabled(context)) }
    var phonetic      by remember { mutableStateOf(AppPrefs.isSearchPhoneticEnabled(context)) }
    var sortByUsage   by remember { mutableStateOf(AppPrefs.isSearchSortByUsage(context)) }
    var maxResults    by remember { mutableStateOf(AppPrefs.getSearchMaxResults(context)) }
    var showIcons     by remember { mutableStateOf(AppPrefs.isSearchShowIcons(context)) }
    var showAvatar    by remember { mutableStateOf(AppPrefs.isSearchShowContactAvatar(context)) }
    var shineEnabled  by remember { mutableStateOf(AppPrefs.isSearchShineEnabled(context)) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_SEARCH_FUZZY -> fuzzy = AppPrefs.isSearchFuzzyEnabled(context)
                AppPrefs.KEY_SEARCH_PHONETIC -> phonetic = AppPrefs.isSearchPhoneticEnabled(context)
                AppPrefs.KEY_SEARCH_SORT_BY_USAGE -> sortByUsage = AppPrefs.isSearchSortByUsage(context)
                AppPrefs.KEY_SEARCH_MAX_RESULTS -> maxResults = AppPrefs.getSearchMaxResults(context)
                AppPrefs.KEY_SEARCH_SHOW_ICONS -> showIcons = AppPrefs.isSearchShowIcons(context)
                AppPrefs.KEY_SEARCH_SHOW_CONTACT_AVATAR -> showAvatar = AppPrefs.isSearchShowContactAvatar(context)
                AppPrefs.KEY_SEARCH_SHINE_ENABLED -> shineEnabled = AppPrefs.isSearchShineEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Kişi kaynağı — Settings'ten dönünce güncellensin (Reaktif AppPrefs pattern'i, LEARNINGS)
    var contactsOn by remember { mutableStateOf(AppPrefs.isSearchSourceContactsEnabled(context)) }
    var filesOn by remember { mutableStateOf(AppPrefs.isSearchSourceFilesEnabled(context)) }
    // Kullanıcı Ayarlar'dan kişi kaynağını BİLİNÇLİ kapattıysa "izin ver" kısayolu da gizlenir
    var contactsOptedOut by remember {
        mutableStateOf(
            AppPrefs.hasSearchSourceContactsPreference(context) &&
                !AppPrefs.isSearchSourceContactsEnabled(context)
        )
    }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_SEARCH_SOURCE_CONTACTS -> {
                    contactsOn = AppPrefs.isSearchSourceContactsEnabled(context)
                    contactsOptedOut = AppPrefs.hasSearchSourceContactsPreference(context) && !contactsOn
                }
                AppPrefs.KEY_SEARCH_SOURCE_FILES -> filesOn = AppPrefs.isSearchSourceFilesEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // READ_CONTACTS izin durumu + istek launcher'ı (S2: sonuç grubunda "izin ver" kısayolu)
    var contactsPermGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    // Spec §5: kullanıcı bir kez reddederse o oturumda tekrar sorulmaz
    var contactsPermDeniedSession by remember { mutableStateOf(false) }
    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactsPermGranted = true
            AppPrefs.setSearchSourceContactsEnabled(context, true)
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
            onEnableContactsSource() // FTS indeksi (ContactsIndexer) arka planda başlar
        } else {
            contactsPermDeniedSession = true
        }
    }

    // P0.3: dosya izinleri (READ_MEDIA_* / READ_EXTERNAL_STORAGE) + istek launcher'ı —
    // "Dosya araması için izin gerekli" satırına dokununca doğrudan sistem izin diyaloğu açılır.
    var filesPermDeniedSession by remember { mutableStateOf(false) }
    val filesPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            AppPrefs.setSearchSourceFilesEnabled(context, true)
            onEnableFilesSource() // FilesIndexer.indexAll() arka planda başlar (SearchRepository.enableFilesSource)
        } else {
            filesPermDeniedSession = true
        }
    }

    // Cache'i allApps değişince güncelle
    LaunchedEffect(allApps) {
        withContext(Dispatchers.IO) { SearchCache.warmApps(allApps) }
    }

    // Kişi cache'ini başlat (kaynak açık + izin varsa)
    LaunchedEffect(contactsOn, contactsPermGranted) {
        if (contactsOn && contactsPermGranted) {
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
        }
    }

    // Sonuç grupları: uygulama + klasör + kişi + dosya (S1 — birleşik arama)
    val appResults = remember(query, allApps, fuzzy, phonetic, sortByUsage, maxResults) {
        if (query.isBlank()) emptyList()
        else SearchCache.searchApps(query, maxResults, phonetic, fuzzy, sortByUsage)
    }
    // Klasörler — eski "Klasör" sekmesinin yerine sonuç grubu; özel klasör adı varsa onunla eşleşir
    val folderResults = remember(query, folders, folderCustomNames) {
        if (query.isBlank() || folders.isEmpty()) emptyList()
        else {
            val q = query.trim().lowercase(Locale("tr"))
            folders.filter { folder ->
                val displayName = folderCustomNames[folder.category.categoryId]
                    ?: folder.category.categoryName
                displayName.lowercase(Locale("tr")).contains(q)
            }.take(4)
        }
    }
    fun SearchDocument.matchesCurrentQuery(): Boolean {
        val q = query.trim().lowercase(Locale("tr"))
        if (q.isBlank()) return false
        return title.lowercase(Locale("tr")).contains(q) ||
            subtitle.lowercase(Locale("tr")).contains(q) ||
            sourceId.lowercase(Locale("tr")).contains(q)
    }
    val contactResults = remember(query, contactsOn, contactsPermGranted) {
        if (!contactsOn || !contactsPermGranted || query.isBlank()) emptyList()
        else SearchCache.searchContacts(query, 3, phonetic = true, fuzzy = true)
    }
    // Dosya adları — SearchRepository FTS5 indeksinden (LauncherViewModel.searchResults akışı)
    val fileResults = if (query.isBlank()) emptyList()
        else searchResults[SourceType.FILE].orEmpty().filter { it.matchesCurrentQuery() }.take(4)
    val settingResults = if (query.isBlank()) emptyList()
        else searchResults[SourceType.SETTING].orEmpty().filter { it.matchesCurrentQuery() }.take(4)
    val searchHintRes = if ((contactsOn && contactsPermGranted) || filesOn) {
        R.string.home_search_hint_full
    } else {
        R.string.home_search_hint_basic
    }
    // İzin yoksa "Kişiler" grubunda izin kısayolu göster (kullanıcı kaynağı kapatmadıysa)
    val showContactsPermissionHint = query.isNotBlank() && !contactsPermGranted &&
        !contactsOptedOut && !contactsPermDeniedSession
    // P0.3: dosya kaynağı açık ama izin yoksa "izin gerekli" göster — Ready değilken
    // sahte "0 sonuç" izlenimi verilmez (spec madde 4). Aksiyon Ayarlar > Arama'ya yönlendirir
    // (izin isteği zaten SearchSettingsScreen'deki ContextualPermissionDialog akışında var).
    val showFilesPermissionHint = query.isNotBlank() && filesOn && !filesPermDeniedSession &&
        filesIndexState is com.armutlu.apporganizer.domain.models.FileIndexState.PermissionRequired

    // Web/Play Store fallback — sorgu >= 2 karakter, tüm kaynaklar sıfır sonuç verince gösterilir (Ayarlar > Arama'dan kapatılabilir)
    var webFallbackEnabled by remember { mutableStateOf(AppPrefs.isSearchWebFallbackEnabled(context)) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPrefs.KEY_SEARCH_WEB_FALLBACK_ENABLED) {
                webFallbackEnabled = AppPrefs.isSearchWebFallbackEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val showWebFallback = webFallbackEnabled && query.trim().length >= 2 &&
        appResults.isEmpty() && folderResults.isEmpty() && contactResults.isEmpty() &&
        settingResults.isEmpty() && fileResults.isEmpty() &&
        !showContactsPermissionHint && !showFilesPermissionHint

    // Sorguyu ViewModel'e ilet — FTS5 çok-kaynak araması (dosyalar) debounce ile orada çalışır
    LaunchedEffect(query) { onQueryChange(query) }

    // İzin ipucu (E10) — Kullanım Erişimi veya Bildirim Erişimi eksikse arama çubuğu altında göster.
    // ON_RESUME'da yeniden kontrol: Ayarlar'dan izin verilince ipucu anında kaybolur.
    var usageGranted by remember { mutableStateOf(UsageStatsHelper.hasPermission(context)) }
    var notifListenerGranted by remember { mutableStateOf(isNotificationListenerGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageGranted = UsageStatsHelper.hasPermission(context)
                notifListenerGranted = isNotificationListenerGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val permMissing = !usageGranted || !notifListenerGranted
    // Bu oturumda X ile kapatıldı mı (aktif ipucu için geçici gizleme)
    var permHintSessionDismissed by remember { mutableStateOf(false) }
    // Kaç açılışta gösterildi — MAX'a ulaşınca rahatsız etmeyen pasif linke döner
    var permHintCount by remember { mutableStateOf(AppPrefs.getSearchPermHintCount(context)) }
    val permHintPassiveMode = permHintCount >= AppPrefs.SEARCH_PERM_HINT_MAX
    // Pasif link kalıcı kapatıldıysa bir daha hiç gösterme
    var permHintPermDismissed by remember { mutableStateOf(AppPrefs.isSearchPermHintDismissed(context)) }
    val showPermHint = permMissing && !permHintSessionDismissed && !permHintPermDismissed
    // Aktif modda ilk gösterimde sayaç artır — birkaç açılış sonra pasif moda geçsin
    LaunchedEffect(showPermHint, permHintPassiveMode) {
        if (showPermHint && !permHintPassiveMode) {
            AppPrefs.incrementSearchPermHintCount(context)
            permHintCount = AppPrefs.getSearchPermHintCount(context)
        }
    }

    // Drag handle state
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    // Focus state — arama alanı seçilince kart belirginleşir (E9)
    var isFocused by remember { mutableStateOf(false) }
    var showGhostZones by remember { mutableStateOf(false) }
    val barScale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "search_bar_scale"
    )

    // Sonuc bolumu tek yerde tanimlanir; resultsAbove=true iken cubugun USTUNDE render
    // edilir (alttan yukari acilir, sayfa kaymaz — D258), aksi halde eski gibi altta.
    val searchResultsSection: @Composable () -> Unit = {
        // Sonuç listesi — kaynak grupları: Uygulamalar / Klasörler / Kişiler / Dosyalar (S1)
        val hasAnyResult = appResults.isNotEmpty() || folderResults.isNotEmpty() ||
            settingResults.isNotEmpty() || contactResults.isNotEmpty() || fileResults.isNotEmpty() ||
            showContactsPermissionHint || showFilesPermissionHint ||
            showWebFallback
        if (hasAnyResult && !isDragging) {
            // Tek grup varsa başlık gereksiz kalabalık — yalnızca çoklu grupta göster
            val multiGroup = listOf(
                appResults.isNotEmpty(),
                folderResults.isNotEmpty(),
                settingResults.isNotEmpty(),
                contactResults.isNotEmpty() || showContactsPermissionHint,
                fileResults.isNotEmpty() || showFilesPermissionHint
            ).count { it } > 1
            Spacer(Modifier.height(4.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, backgroundAlpha = 0.18f) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .then(
                            // Yukari acilirken ekrani kaplamasin — sinirli yukseklik + ic scroll
                            if (resultsAbove) Modifier
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                            else Modifier
                        )
                ) {

                    // App sonuçları
                    if (appResults.isNotEmpty() && multiGroup) {
                        HomeSearchGroupHeader(label = "Uygulamalar", icon = Icons.Default.Search)
                    }
                    appResults.forEachIndexed { index, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SearchStatsPrefs.logClick(context, SourceType.APP.key, index)
                                    query = ""
                                    onAppClick(app.packageName)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (showIcons) {
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
                            }
                            Text(app.appName, color = Color.White.copy(alpha = 0.90f),
                                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                        }
                    }

                    // Klasör sonuçları — eski "Klasör" sekmesinin yerini alan sonuç grubu (S1)
                    if (folderResults.isNotEmpty()) {
                        if (appResults.isNotEmpty()) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Klasörler", icon = Icons.Default.Folder)
                        folderResults.forEachIndexed { index, folder ->
                            val displayName = folderCustomNames[folder.category.categoryId]
                                ?: folder.category.categoryName
                            val emoji = (folderCustomEmojis[folder.category.categoryId]
                                ?: folder.category.iconEmoji).ifBlank { "📁" }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SearchStatsPrefs.logClick(context, SourceType.CATEGORY.key, index)
                                        query = ""
                                        onFolderClick(folder)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 16.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(displayName, color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${folder.apps.size} uygulama",
                                        color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Android ayarları sonuçları — Wi-Fi, bildirim erişimi, kullanım erişimi vb.
                    if (settingResults.isNotEmpty()) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty()) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Ayarlar", icon = Icons.Default.Search)
                        settingResults.forEachIndexed { index, document ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SearchStatsPrefs.logClick(context, SourceType.SETTING.key, index)
                                        query = ""
                                        SystemSettingsCatalog.open(context, document)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(document.title, color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(document.subtitle.substringBefore(" | "), color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // Kişi sonuçları — ayraç
                    if (contactResults.isNotEmpty()) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty() || settingResults.isNotEmpty()) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Kişiler", icon = Icons.Default.Person)
                        contactResults.forEachIndexed { index, contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SearchStatsPrefs.logClick(context, SourceType.CONTACT.key, index)
                                        query = ""
                                        // Kişi tıklaması: arama ekranına veya telefon dialer'a
                                        val dialIntent = android.content.Intent(
                                            android.content.Intent.ACTION_DIAL,
                                            android.net.Uri.parse("tel:${android.net.Uri.encode(contact.phone)}")
                                        ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                        runCatching { context.startActivity(dialIntent) }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (showAvatar) {
                                    val avatarBitmap by produceState<ImageBitmap?>(null, contact.photoUri) {
                                        value = withContext(Dispatchers.IO) {
                                            if (contact.photoUri != null) {
                                                runCatching {
                                                    val uri = android.net.Uri.parse(contact.photoUri)
                                                    context.contentResolver.openInputStream(uri)?.use {
                                                        android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
                                                    }
                                                }.getOrNull()
                                            } else null
                                        }
                                    }
                                    if (avatarBitmap != null) {
                                        Image(bitmap = avatarBitmap!!, contentDescription = null,
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Crop)
                                    } else {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.20f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                contact.displayName.take(1).uppercase(),
                                                color = Color.White.copy(alpha = 0.80f),
                                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.displayName, color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (contact.phone.isNotBlank()) {
                                        Text(contact.phone, color = Color.White.copy(alpha = 0.45f),
                                            fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                // Hizli aksiyonlar - numara varsa gosterilir; satirin kendisi ayri
                                // olarak dialer'i acmaya devam eder (mevcut davranis korunur)
                                if (contact.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            SearchStatsPrefs.logAction(context, "CALL")
                                            homeLogContactAction(context, contact.id.toString(),
                                                com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType.CALL)
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_DIAL,
                                                android.net.Uri.parse("tel:${android.net.Uri.encode(contact.phone)}")
                                            ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                            runCatching { context.startActivity(intent) }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Ara",
                                            tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val normalized = contact.phone.filter { it.isDigit() || it == '+' }
                                            runCatching {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("https://wa.me/$normalized")
                                                ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                                context.startActivity(intent)
                                                SearchStatsPrefs.logAction(context, "WHATSAPP")
                                                homeLogContactAction(context, contact.id.toString(),
                                                    com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType.WHATSAPP)
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp",
                                            tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            SearchStatsPrefs.logAction(context, "SMS")
                                            homeLogContactAction(context, contact.id.toString(),
                                                com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType.SMS)
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_SENDTO,
                                                android.net.Uri.parse("smsto:${android.net.Uri.encode(contact.phone)}")
                                            ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                            runCatching { context.startActivity(intent) }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS",
                                            tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }

                    // İzin kısayolu — kişi araması etkin ama READ_CONTACTS yok (S2)
                    if (showContactsPermissionHint) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty() || settingResults.isNotEmpty()) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Kişiler", icon = Icons.Default.Person)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    contactsPermLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.home_search_contacts_enable_title), color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 14.sp)
                                Text(stringResource(R.string.home_search_contacts_enable_desc),
                                    color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                            }
                        }
                    }

                    // P0.3: dosya kaynağı açık ama Android izni yok — "0 sonuç" yerine izin kısayolu
                    if (showFilesPermissionHint) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty() ||
                            settingResults.isNotEmpty() || contactResults.isNotEmpty() || showContactsPermissionHint
                        ) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Dosyalar", icon = Icons.Default.Description)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(
                                            android.Manifest.permission.READ_MEDIA_IMAGES,
                                            android.Manifest.permission.READ_MEDIA_VIDEO,
                                            android.Manifest.permission.READ_MEDIA_AUDIO,
                                        )
                                    } else {
                                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    filesPermLauncher.launch(permissions)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.home_search_files_permission_required), color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 14.sp)
                                Text(stringResource(R.string.home_search_files_permission_required_desc),
                                    color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                            }
                        }
                    }

                    // Dosya sonuçları — FTS5 indeksinden (kaynak Ayarlar > Arama'dan kapatılabilir)
                    if (fileResults.isNotEmpty()) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty() ||
                            settingResults.isNotEmpty() || contactResults.isNotEmpty() ||
                            showContactsPermissionHint || showFilesPermissionHint
                        ) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Dosyalar", icon = Icons.Default.Description)
                        fileResults.forEachIndexed { index, document ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SearchStatsPrefs.logClick(context, SourceType.FILE.key, index)
                                        query = ""
                                        // AllAppsDrawer.openSearchDocument ile aynı pattern
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(document.sourceId))
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        runCatching { context.startActivity(intent) }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(document.title, color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val subtitle = document.subtitle.ifBlank { document.sourceId }
                                    Text(subtitle, color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // Web/Play Store fallback — sıfır sonuçta arama devam ettirilebilsin (Ayarlar > Arama)
                    if (showWebFallback) {
                        SearchFallbackRows(context = context, query = query.trim())
                    }
                }
            }
        }
    }

    Column(modifier = modifier) {
        if (resultsAbove) searchResultsSection()

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

        // Arama alanı — glass kart stilinde + drag handle + elmas parlaması
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .scale(barScale)
                .diamondShine(shineEnabled, RoundedCornerShape(28.dp), trigger = homeResumeTrigger)
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
            // Focus'ta da belirginleş — seçili olduğu net görünsün (E9)
            backgroundAlpha = if (isDragging || isFocused) 0.22f else 0.12f,
            borderAlpha = if (isDragging) 0.45f else if (isFocused) 0.70f else 0.25f,
            borderColor = if (isFocused) Color(0xFF26C6DA) else Color.White
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // "Uygulama / Klasör" sekmesi kaldırıldı (S1) — klasörler artık sonuç grubu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Ara",
                        tint = if (isFocused) Color.White else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isFocused = it.isFocused },
                        decorationBox = { inner ->
                            Box(Modifier.weight(1f)) {
                                // Spec §5: placeholder kalabalıklaşmasın — kişi/dosya eklenmez
                                if (query.isEmpty()) Text(
                                    stringResource(searchHintRes),
                                    color = Color.White.copy(alpha = 0.40f), fontSize = 14.sp
                                )
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        Icon(Icons.Default.Close, contentDescription = "Aramayı temizle",
                            tint = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.size(18.dp).clickable { query = "" })
                    }
                }
            }
        }

        // İzin ipucu satırı (E10) — arama çubuğunun hemen altında; sürükleme sırasında gizli
        if (showPermHint && !isDragging) {
            Spacer(Modifier.height(4.dp))
            val permHintText = when {
                permHintPassiveMode -> "İzinler ayarlardan yönetilebilir →"
                !usageGranted -> "🔍 Daha iyi arama ve öneriler için kullanım erişimi gerekli — Ver"
                else -> "🔔 Bildirim rozetleri için erişim gerekli — Ver"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable {
                        if (permHintPassiveMode) {
                            // Rahatsız etmeden İzinler rehberine yönlendir (MainActivity → PERMISSIONS_GUIDE)
                            val intent = Intent(context, MainActivity::class.java).apply {
                                putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.PERMISSIONS_GUIDE)
                                addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }
                            runCatching { context.startActivity(intent) }
                        } else {
                            // Eksik olan izni doğrudan sistem ayarında aç
                            if (!usageGranted) {
                                UsageStatsHelper.openPermissionSettings(context)
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    permHintText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "İpucunu kapat",
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            permHintSessionDismissed = true
                            if (permHintPassiveMode) {
                                // Pasif link kapatıldıysa kalıcı gizle — bir daha rahatsız etme
                                AppPrefs.setSearchPermHintDismissed(context, true)
                                permHintPermDismissed = true
                            } else {
                                // Aktif ipucu kapatıldıysa sayacı ilerlet (pasif moda daha çabuk geçsin)
                                AppPrefs.incrementSearchPermHintCount(context)
                                permHintCount = AppPrefs.getSearchPermHintCount(context)
                            }
                        }
                )
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

        if (!resultsAbove) searchResultsSection()
    }
}

/**
 * P1.1 + P1.2 - Tam ekran "Her seyi ara" overlay'i.
 * Bos sorguda baglamsal top apps + kisi onerileri + son aramalar gosterir.
 */
@Composable
internal fun FullScreenSearchOverlay(
    allApps: List<AppInfo>,
    folders: List<AppFolder>,
    folderCustomNames: Map<String, String>,
    searchResults: Map<SourceType, List<SearchDocument>>,
    filesIndexState: com.armutlu.apporganizer.domain.models.FileIndexState,
    suggestedContacts: List<SearchCache.ContactEntry>,
    onClose: () -> Unit,
    onAppClick: (String) -> Unit,
    onFolderClick: (AppFolder) -> Unit,
    onEnableContactsSource: () -> Unit,
    onEnableFilesSource: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun closeOverlay(clearQuery: Boolean = true) {
        if (clearQuery) {
            onQueryChange("")
            keyboardController?.hide()
        }
        onClose()
    }

    BackHandler(enabled = true) { closeOverlay() }

    var query by rememberSaveable { mutableStateOf("") }
    var fuzzy by remember { mutableStateOf(AppPrefs.isSearchFuzzyEnabled(context)) }
    var phonetic by remember { mutableStateOf(AppPrefs.isSearchPhoneticEnabled(context)) }
    var sortByUsage by remember { mutableStateOf(AppPrefs.isSearchSortByUsage(context)) }
    var maxResults by remember { mutableStateOf(AppPrefs.getSearchMaxResults(context)) }
    var showIcons by remember { mutableStateOf(AppPrefs.isSearchShowIcons(context)) }
    var showAvatar by remember { mutableStateOf(AppPrefs.isSearchShowContactAvatar(context)) }
    var contactsOn by remember { mutableStateOf(AppPrefs.isSearchSourceContactsEnabled(context)) }
    var filesOn by remember { mutableStateOf(AppPrefs.isSearchSourceFilesEnabled(context)) }
    var webFallbackEnabled by remember { mutableStateOf(AppPrefs.isSearchWebFallbackEnabled(context)) }
    val focusRequester = remember { FocusRequester() }

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_SEARCH_FUZZY -> fuzzy = AppPrefs.isSearchFuzzyEnabled(context)
                AppPrefs.KEY_SEARCH_PHONETIC -> phonetic = AppPrefs.isSearchPhoneticEnabled(context)
                AppPrefs.KEY_SEARCH_SORT_BY_USAGE -> sortByUsage = AppPrefs.isSearchSortByUsage(context)
                AppPrefs.KEY_SEARCH_MAX_RESULTS -> maxResults = AppPrefs.getSearchMaxResults(context)
                AppPrefs.KEY_SEARCH_SHOW_ICONS -> showIcons = AppPrefs.isSearchShowIcons(context)
                AppPrefs.KEY_SEARCH_SHOW_CONTACT_AVATAR -> showAvatar = AppPrefs.isSearchShowContactAvatar(context)
                AppPrefs.KEY_SEARCH_SOURCE_CONTACTS -> contactsOn = AppPrefs.isSearchSourceContactsEnabled(context)
                AppPrefs.KEY_SEARCH_SOURCE_FILES -> filesOn = AppPrefs.isSearchSourceFilesEnabled(context)
                AppPrefs.KEY_SEARCH_WEB_FALLBACK_ENABLED -> webFallbackEnabled = AppPrefs.isSearchWebFallbackEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var contactsPermGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactsPermGranted = true
            AppPrefs.setSearchSourceContactsEnabled(context, true)
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
            onEnableContactsSource()
        }
    }
    val filesPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            AppPrefs.setSearchSourceFilesEnabled(context, true)
            onEnableFilesSource()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        onQueryChange("")
    }
    LaunchedEffect(allApps) {
        withContext(Dispatchers.IO) { SearchCache.warmApps(allApps) }
    }
    LaunchedEffect(contactsOn, contactsPermGranted) {
        if (contactsOn && contactsPermGranted) {
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
        }
    }
    LaunchedEffect(query) { onQueryChange(query) }

    val appResults = remember(query, allApps, fuzzy, phonetic, sortByUsage, maxResults) {
        if (query.isBlank()) emptyList()
        else SearchCache.searchApps(query, maxResults.coerceAtLeast(8), phonetic, fuzzy, sortByUsage)
    }
    val folderResults = remember(query, folders, folderCustomNames) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase(Locale("tr"))
            folders.filter { folder ->
                val displayName = folderCustomNames[folder.category.categoryId] ?: folder.category.categoryName
                displayName.lowercase(Locale("tr")).contains(q)
            }.take(8)
        }
    }
    fun SearchDocument.matchesCurrentQuery(): Boolean {
        val q = query.trim().lowercase(Locale("tr"))
        if (q.isBlank()) return false
        return title.lowercase(Locale("tr")).contains(q) ||
            subtitle.lowercase(Locale("tr")).contains(q) ||
            sourceId.lowercase(Locale("tr")).contains(q)
    }
    val contactResults = remember(query, contactsOn, contactsPermGranted) {
        if (!contactsOn || !contactsPermGranted || query.isBlank()) emptyList()
        else SearchCache.searchContacts(query, 5, phonetic = true, fuzzy = true)
    }
    val fileResults = if (query.isBlank()) emptyList()
        else searchResults[SourceType.FILE].orEmpty().filter { it.matchesCurrentQuery() }.take(8)
    val settingResults = if (query.isBlank()) emptyList()
        else searchResults[SourceType.SETTING].orEmpty().filter { it.matchesCurrentQuery() }.take(8)
    val showFilesPermissionHint = SearchOverlayDecisions.shouldShowFilesPermissionHint(
        query = query,
        filesOn = filesOn,
        filesIndexState = filesIndexState,
    )
    val showWebFallback = SearchOverlayDecisions.shouldShowWebFallback(
        query = query,
        webFallbackEnabled = webFallbackEnabled,
        appCount = appResults.size,
        folderCount = folderResults.size,
        contactCount = contactResults.size,
        settingCount = settingResults.size,
        fileCount = fileResults.size,
        showFilesPermissionHint = showFilesPermissionHint,
    )

    val zeroStateApps = remember(allApps) {
        val visibleByPkg = allApps.filterNot { it.isHidden }.associateBy { it.packageName }
        UsageStatsHelper.getCurrentSlotTopApps(context, days = 28)
            .mapNotNull { visibleByPkg[it] }
            .take(5)
    }
    val historyItems = remember(query) {
        if (query.isBlank()) SearchHistoryPrefs.getAll(context) else emptyList()
    }

    fun recordSearch(queryText: String, title: String, sourceType: SourceType, sourceId: String) {
        SearchHistoryPrefs.record(
            context = context,
            query = queryText,
            title = title,
            sourceType = sourceType,
            sourceId = sourceId,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .semantics { isTraversalGroup = true }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { traversalIndex = 0f },
                    cornerRadius = 24.dp,
                    backgroundAlpha = 0.16f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = { closeOverlay() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_overlay_close),
                                tint = Color.White
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .semantics {
                                    traversalIndex = 1f
                                    contentDescription = context.getString(R.string.search_overlay_field_content_desc)
                                },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Search,
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() }
                            ),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isBlank()) {
                                        Text(stringResource(R.string.search_overlay_title), color = Color.White.copy(alpha = 0.48f), fontSize = 16.sp)
                                    }
                                    inner()
                                }
                            }
                        )
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.search_overlay_clear_query),
                                    tint = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }
            }

            if (query.isBlank()) {
                if (zeroStateApps.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_zero_state_apps_title), Icons.Default.Search) }
                    items(zeroStateApps) { app ->
                        SearchAppRow(app = app, showIcons = showIcons) {
                            recordSearch(app.appName, app.appName, SourceType.APP, app.packageName)
                            closeOverlay(clearQuery = false)
                            onAppClick(app.packageName)
                        }
                    }
                }
                if (suggestedContacts.take(3).isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_zero_state_contacts_title), Icons.Default.Person) }
                    items(suggestedContacts.take(3)) { contact ->
                        SearchContactRow(context = context, contact = contact, showAvatar = showAvatar, query = contact.displayName)
                    }
                }
                if (historyItems.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_zero_state_history_title), Icons.Default.Search) }
                    items(historyItems) { item ->
                        SearchHistoryRow(item = item) {
                            when (item.sourceType) {
                                SourceType.APP.key -> {
                                    closeOverlay(clearQuery = false)
                                    onAppClick(item.sourceId)
                                }
                                SourceType.CATEGORY.key -> folders.firstOrNull { it.category.categoryId == item.sourceId }?.let {
                                    closeOverlay(clearQuery = false)
                                    onFolderClick(it)
                                }
                                SourceType.FILE.key -> {
                                    closeOverlay(clearQuery = false)
                                    openSearchDocument(context, item.sourceId)
                                }
                                SourceType.SETTING.key -> searchResults[SourceType.SETTING].orEmpty()
                                    .firstOrNull { it.sourceId == item.sourceId }
                                    ?.let {
                                        closeOverlay(clearQuery = false)
                                        SystemSettingsCatalog.open(context, it)
                                    }
                                SourceType.CONTACT.key -> SearchCache.getContactList()
                                    .firstOrNull { it.id.toString() == item.sourceId }
                                    ?.let {
                                        closeOverlay(clearQuery = false)
                                        launchDial(context, it.phone)
                                    }
                            }
                        }
                    }
                }
                if (zeroStateApps.isEmpty() && suggestedContacts.isEmpty() && historyItems.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_zero_state_empty),
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            } else {
                if (appResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_apps), Icons.Default.Search) }
                    items(appResults) { app ->
                        SearchAppRow(app = app, showIcons = showIcons) {
                            recordSearch(query, app.appName, SourceType.APP, app.packageName)
                            SearchStatsPrefs.logClick(context, SourceType.APP.key, 0)
                            closeOverlay(clearQuery = false)
                            onAppClick(app.packageName)
                        }
                    }
                }
                if (folderResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader("Klasörler", Icons.Default.Folder) }
                    items(folderResults) { folder ->
                        SearchSimpleRow(
                            leading = { Text("📁", fontSize = 18.sp) },
                            title = folderCustomNames[folder.category.categoryId] ?: folder.category.categoryName,
                            subtitle = folder.apps.size.toString() + " uygulama"
                        ) {
                            recordSearch(query, folder.category.categoryName, SourceType.CATEGORY, folder.category.categoryId)
                            onFolderClick(folder)
                        }
                    }
                }
                if (settingResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader("Ayarlar", Icons.Default.Search) }
                    items(settingResults) { document ->
                        SearchDocumentRow(document = document, icon = Icons.Default.Search) {
                            recordSearch(query, document.title, SourceType.SETTING, document.sourceId)
                            SystemSettingsCatalog.open(context, document)
                        }
                    }
                }
                if (contactResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader("Kişiler", Icons.Default.Person) }
                    items(contactResults) { contact ->
                        SearchContactRow(context = context, contact = contact, showAvatar = showAvatar, query = query)
                    }
                }
                if (showFilesPermissionHint) {
                    item {
                        SearchSimpleRow(
                            leading = { Icon(Icons.Default.Description, contentDescription = null, tint = Color.White.copy(alpha = 0.70f)) },
                            title = stringResource(R.string.home_search_files_permission_required),
                            subtitle = stringResource(R.string.home_search_files_permission_required_desc)
                        ) {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    android.Manifest.permission.READ_MEDIA_IMAGES,
                                    android.Manifest.permission.READ_MEDIA_VIDEO,
                                    android.Manifest.permission.READ_MEDIA_AUDIO,
                                )
                            } else arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            filesPermLauncher.launch(permissions)
                        }
                    }
                }
                if (fileResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader("Dosyalar", Icons.Default.Description) }
                    items(fileResults) { document ->
                        SearchDocumentRow(document = document, icon = Icons.Default.Description) {
                            recordSearch(query, document.title, SourceType.FILE, document.sourceId)
                            openSearchDocument(context, document.sourceId)
                        }
                    }
                }
                if (!contactsPermGranted && contactsOn) {
                    item {
                        SearchSimpleRow(
                            leading = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.70f)) },
                            title = "Kişi izni gerekli",
                            subtitle = "Dokunup Android kişi izni ver"
                        ) {
                            contactsPermLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    }
                }
                if (showWebFallback) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, backgroundAlpha = 0.14f) {
                            Column { SearchFallbackRows(context = context, query = query.trim()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FullScreenSearchOverlayV2(
    allApps: List<AppInfo>,
    folders: List<AppFolder>,
    folderCustomNames: Map<String, String>,
    searchResults: Map<SourceType, List<SearchDocument>>,
    filesIndexState: com.armutlu.apporganizer.domain.models.FileIndexState,
    suggestedContacts: List<SearchCache.ContactEntry>,
    onClose: () -> Unit,
    onAppClick: (String) -> Unit,
    onFolderClick: (AppFolder) -> Unit,
    onEnableContactsSource: () -> Unit,
    onEnableFilesSource: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var query by rememberSaveable { mutableStateOf("") }
    var fuzzy by remember { mutableStateOf(AppPrefs.isSearchFuzzyEnabled(context)) }
    var phonetic by remember { mutableStateOf(AppPrefs.isSearchPhoneticEnabled(context)) }
    var sortByUsage by remember { mutableStateOf(AppPrefs.isSearchSortByUsage(context)) }
    var maxResults by remember { mutableStateOf(AppPrefs.getSearchMaxResults(context)) }
    var showIcons by remember { mutableStateOf(AppPrefs.isSearchShowIcons(context)) }
    var showAvatar by remember { mutableStateOf(AppPrefs.isSearchShowContactAvatar(context)) }
    var contactsOn by remember { mutableStateOf(AppPrefs.isSearchSourceContactsEnabled(context)) }
    var filesOn by remember { mutableStateOf(AppPrefs.isSearchSourceFilesEnabled(context)) }
    var webFallbackEnabled by remember { mutableStateOf(AppPrefs.isSearchWebFallbackEnabled(context)) }

    fun closeOverlay(clearQuery: Boolean = true) {
        if (clearQuery) {
            query = ""
            onQueryChange("")
            keyboardController?.hide()
        }
        onClose()
    }

    BackHandler(enabled = true) { closeOverlay() }

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_SEARCH_FUZZY -> fuzzy = AppPrefs.isSearchFuzzyEnabled(context)
                AppPrefs.KEY_SEARCH_PHONETIC -> phonetic = AppPrefs.isSearchPhoneticEnabled(context)
                AppPrefs.KEY_SEARCH_SORT_BY_USAGE -> sortByUsage = AppPrefs.isSearchSortByUsage(context)
                AppPrefs.KEY_SEARCH_MAX_RESULTS -> maxResults = AppPrefs.getSearchMaxResults(context)
                AppPrefs.KEY_SEARCH_SHOW_ICONS -> showIcons = AppPrefs.isSearchShowIcons(context)
                AppPrefs.KEY_SEARCH_SHOW_CONTACT_AVATAR -> showAvatar = AppPrefs.isSearchShowContactAvatar(context)
                AppPrefs.KEY_SEARCH_SOURCE_CONTACTS -> contactsOn = AppPrefs.isSearchSourceContactsEnabled(context)
                AppPrefs.KEY_SEARCH_SOURCE_FILES -> filesOn = AppPrefs.isSearchSourceFilesEnabled(context)
                AppPrefs.KEY_SEARCH_WEB_FALLBACK_ENABLED -> webFallbackEnabled = AppPrefs.isSearchWebFallbackEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var contactsPermGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactsPermGranted = true
            AppPrefs.setSearchSourceContactsEnabled(context, true)
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
            onEnableContactsSource()
        }
    }
    val filesPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            AppPrefs.setSearchSourceFilesEnabled(context, true)
            onEnableFilesSource()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        onQueryChange("")
    }
    LaunchedEffect(allApps) {
        withContext(Dispatchers.IO) { SearchCache.warmApps(allApps) }
    }
    LaunchedEffect(contactsOn, contactsPermGranted) {
        if (contactsOn && contactsPermGranted) {
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
        }
    }
    LaunchedEffect(query) { onQueryChange(query) }

    fun SearchDocument.matchesCurrentQuery(): Boolean {
        val q = query.trim().lowercase(Locale("tr"))
        if (q.isBlank()) return false
        return title.lowercase(Locale("tr")).contains(q) ||
            subtitle.lowercase(Locale("tr")).contains(q) ||
            sourceId.lowercase(Locale("tr")).contains(q)
    }

    val appResults = remember(query, allApps, fuzzy, phonetic, sortByUsage, maxResults) {
        if (query.isBlank()) emptyList()
        else SearchCache.searchApps(query, maxResults.coerceAtLeast(8), phonetic, fuzzy, sortByUsage)
    }
    val folderResults = remember(query, folders, folderCustomNames) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase(Locale("tr"))
            folders.filter { folder ->
                val displayName = folderCustomNames[folder.category.categoryId] ?: folder.category.categoryName
                displayName.lowercase(Locale("tr")).contains(q)
            }.take(8)
        }
    }
    val contactResults = remember(query, contactsOn, contactsPermGranted) {
        if (!contactsOn || !contactsPermGranted || query.isBlank()) emptyList()
        else SearchCache.searchContacts(query, 5, phonetic = true, fuzzy = true)
    }
    val fileResults = if (query.isBlank()) emptyList()
    else searchResults[SourceType.FILE].orEmpty().filter { it.matchesCurrentQuery() }.take(8)
    val settingResults = if (query.isBlank()) emptyList()
    else searchResults[SourceType.SETTING].orEmpty().filter { it.matchesCurrentQuery() }.take(8)

    val showFilesPermissionHint = SearchOverlayDecisions.shouldShowFilesPermissionHint(
        query = query,
        filesOn = filesOn,
        filesIndexState = filesIndexState,
    )
    val showWebFallback = SearchOverlayDecisions.shouldShowWebFallback(
        query = query,
        webFallbackEnabled = webFallbackEnabled,
        appCount = appResults.size,
        folderCount = folderResults.size,
        contactCount = contactResults.size,
        settingCount = settingResults.size,
        fileCount = fileResults.size,
        showFilesPermissionHint = showFilesPermissionHint,
    )

    val zeroStateApps = remember(allApps) {
        val visibleByPkg = allApps.filterNot { it.isHidden }.associateBy { it.packageName }
        UsageStatsHelper.getCurrentSlotTopApps(context, days = 28)
            .mapNotNull { visibleByPkg[it] }
            .take(5)
    }
    val historyItems = remember(query) {
        if (query.isBlank()) SearchHistoryPrefs.getAll(context) else emptyList()
    }

    fun recordSearch(queryText: String, title: String, sourceType: SourceType, sourceId: String) {
        SearchHistoryPrefs.record(context, queryText, title, sourceType, sourceId)
    }

    fun openHistoryItem(item: SearchHistoryPrefs.SearchHistoryItem) {
        when (item.sourceType) {
            SourceType.APP.key -> {
                closeOverlay(clearQuery = false)
                onAppClick(item.sourceId)
            }
            SourceType.CATEGORY.key -> folders.firstOrNull { it.category.categoryId == item.sourceId }?.let {
                closeOverlay(clearQuery = false)
                onFolderClick(it)
            }
            SourceType.FILE.key -> {
                closeOverlay(clearQuery = false)
                openSearchDocument(context, item.sourceId)
            }
            SourceType.SETTING.key -> searchResults[SourceType.SETTING].orEmpty()
                .firstOrNull { it.sourceId == item.sourceId }
                ?.let {
                    closeOverlay(clearQuery = false)
                    SystemSettingsCatalog.open(context, it)
                }
            SourceType.CONTACT.key -> SearchCache.getContactList()
                .firstOrNull { it.id.toString() == item.sourceId }
                ?.let {
                    closeOverlay(clearQuery = false)
                    launchDial(context, it.phone)
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .semantics { isTraversalGroup = true }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { traversalIndex = 0f },
                    cornerRadius = 24.dp,
                    backgroundAlpha = 0.16f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = { closeOverlay() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_overlay_close),
                                tint = Color.White
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .semantics {
                                    traversalIndex = 1f
                                    contentDescription = context.getString(R.string.search_overlay_field_content_desc)
                                },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Search,
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() }
                            ),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.search_overlay_title),
                                            color = Color.White.copy(alpha = 0.48f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.search_overlay_clear_query),
                                    tint = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }
            }

            if (query.isBlank()) {
                if (zeroStateApps.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_zero_state_apps_title), Icons.Default.Search) }
                    items(zeroStateApps) { app ->
                        SearchAppRow(app = app, showIcons = showIcons) {
                            recordSearch(app.appName, app.appName, SourceType.APP, app.packageName)
                            closeOverlay(clearQuery = false)
                            onAppClick(app.packageName)
                        }
                    }
                }
                if (suggestedContacts.take(3).isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_zero_state_contacts_title), Icons.Default.Person) }
                    items(suggestedContacts.take(3)) { contact ->
                        SearchContactRow(context = context, contact = contact, showAvatar = showAvatar, query = contact.displayName)
                    }
                }
                if (historyItems.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_zero_state_history_title), Icons.Default.Search) }
                    items(historyItems) { item ->
                        SearchHistoryRow(item = item) { openHistoryItem(item) }
                    }
                }
                if (zeroStateApps.isEmpty() && suggestedContacts.isEmpty() && historyItems.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_zero_state_empty),
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            } else {
                if (appResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_apps), Icons.Default.Search) }
                    items(appResults) { app ->
                        SearchAppRow(app = app, showIcons = showIcons) {
                            recordSearch(query, app.appName, SourceType.APP, app.packageName)
                            SearchStatsPrefs.logClick(context, SourceType.APP.key, 0)
                            closeOverlay(clearQuery = false)
                            onAppClick(app.packageName)
                        }
                    }
                }
                if (folderResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_folders), Icons.Default.Folder) }
                    items(folderResults) { folder ->
                        SearchSimpleRow(
                            leading = {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            title = folderCustomNames[folder.category.categoryId] ?: folder.category.categoryName,
                            subtitle = stringResource(R.string.search_folder_result_count, folder.apps.size)
                        ) {
                            recordSearch(query, folder.category.categoryName, SourceType.CATEGORY, folder.category.categoryId)
                            closeOverlay(clearQuery = false)
                            onFolderClick(folder)
                        }
                    }
                }
                if (settingResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_settings), Icons.Default.Search) }
                    items(settingResults) { document ->
                        SearchDocumentRow(document = document, icon = Icons.Default.Search) {
                            recordSearch(query, document.title, SourceType.SETTING, document.sourceId)
                            closeOverlay(clearQuery = false)
                            SystemSettingsCatalog.open(context, document)
                        }
                    }
                }
                if (contactResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_contacts), Icons.Default.Person) }
                    items(contactResults) { contact ->
                        SearchContactRow(context = context, contact = contact, showAvatar = showAvatar, query = query)
                    }
                }
                if (showFilesPermissionHint) {
                    item {
                        SearchSimpleRow(
                            leading = { Icon(Icons.Default.Description, contentDescription = null, tint = Color.White.copy(alpha = 0.70f)) },
                            title = stringResource(R.string.home_search_files_permission_required),
                            subtitle = stringResource(R.string.home_search_files_permission_required_desc)
                        ) {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    android.Manifest.permission.READ_MEDIA_IMAGES,
                                    android.Manifest.permission.READ_MEDIA_VIDEO,
                                    android.Manifest.permission.READ_MEDIA_AUDIO,
                                )
                            } else {
                                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            filesPermLauncher.launch(permissions)
                        }
                    }
                }
                if (fileResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_files), Icons.Default.Description) }
                    items(fileResults) { document ->
                        SearchDocumentRow(document = document, icon = Icons.Default.Description) {
                            recordSearch(query, document.title, SourceType.FILE, document.sourceId)
                            closeOverlay(clearQuery = false)
                            openSearchDocument(context, document.sourceId)
                        }
                    }
                }
                if (!contactsPermGranted && contactsOn) {
                    item {
                        SearchSimpleRow(
                            leading = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.70f)) },
                            title = stringResource(R.string.search_contacts_permission_required_title),
                            subtitle = stringResource(R.string.search_contacts_permission_required_desc)
                        ) {
                            contactsPermLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    }
                }
                if (showWebFallback) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, backgroundAlpha = 0.14f) {
                            Column { SearchFallbackRows(context = context, query = query.trim()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAppRow(
    app: AppInfo,
    showIcons: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    SearchSimpleRow(
        leading = {
            if (showIcons) {
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
                    Image(bitmap = icon!!, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.16f)))
                }
            } else {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f))
            }
        },
        title = app.appName,
        subtitle = app.packageName,
        onClick = onClick
    )
}

@Composable
private fun SearchDocumentRow(
    document: SearchDocument,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    SearchSimpleRow(
        leading = { Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.72f)) },
        title = document.title,
        subtitle = document.subtitle.ifBlank { document.sourceId }.substringBefore(" | "),
        onClick = onClick
    )
}

@Composable
private fun SearchContactRow(
    context: Context,
    contact: SearchCache.ContactEntry,
    showAvatar: Boolean,
    query: String,
) {
    SearchSimpleRow(
        leading = {
            if (showAvatar && contact.photoUri != null) {
                val avatarBitmap by produceState<ImageBitmap?>(null, contact.photoUri) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(Uri.parse(contact.photoUri))?.use {
                                android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
                            }
                        }.getOrNull()
                    }
                }
                if (avatarBitmap != null) {
                    Image(bitmap = avatarBitmap!!, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.70f))
                    }
                }
            } else {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.70f))
                }
            }
        },
        title = contact.displayName,
        subtitle = contact.phone,
    ) {
        SearchHistoryPrefs.record(context, query, contact.displayName, SourceType.CONTACT, contact.id.toString())
        SearchStatsPrefs.logClick(context, SourceType.CONTACT.key, 0)
        ContactActionPrefs.logAction(context, contact.id.toString(), ContactActionPrefs.ActionType.CALL)
        launchDial(context, contact.phone)
    }
}

@Composable
private fun SearchHistoryRow(
    item: SearchHistoryPrefs.SearchHistoryItem,
    onClick: () -> Unit,
) {
    val subtitle = if (item.query.isBlank()) {
        stringResource(R.string.search_history_recent_result)
    } else {
        stringResource(R.string.search_history_query_prefix, item.query)
    }
    SearchSimpleRow(
        leading = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f)) },
        title = item.title,
        subtitle = subtitle,
        onClick = onClick
    )
}

@Composable
private fun SearchSimpleRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, backgroundAlpha = 0.12f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) { leading() }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Color.White.copy(alpha = 0.52f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun launchDial(context: Context, phone: String) {
    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(dialIntent) }
}

private fun openSearchDocument(context: Context, sourceId: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceId))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/**
 * Sıfır sonuçta gösterilen iki fallback satırı — Web'de ara / Play Store'da ara.
 * ACTION_WEB_SEARCH / market:// başarısız olursa https:// ACTION_VIEW'a düşer.
 */
@Composable
private fun SearchFallbackRows(context: Context, query: String) {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = Color.White.copy(alpha = 0.10f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SearchStatsPrefs.logAction(context, "WEB_FALLBACK")
                val webIntent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
                runCatching { context.startActivity(webIntent) }.onFailure {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(fallback) }
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🌐", fontSize = 16.sp)
        Text(
            stringResource(R.string.search_fallback_google, query),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SearchStatsPrefs.logAction(context, "PLAY_FALLBACK")
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(query)))
                runCatching { context.startActivity(marketIntent) }.onFailure {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(query))
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(fallback) }
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("▶️", fontSize = 16.sp)
        Text(
            stringResource(R.string.search_fallback_play_store, query),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Ana ekran arama sonuç grubu başlığı — glass stilinde küçük kaynak etiketi
 * (AllAppsDrawer.SourceGroupHeader'ın home karşılığı).
 */
@Composable
private fun HomeSearchGroupHeader(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null,
                tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(11.dp))
        }
        Text(label, color = Color.White.copy(alpha = 0.40f), fontSize = 11.sp)
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

package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.armutlu.apporganizer.utils.AppAnalytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onLaunchWidgetPicker: () -> Unit = {}
) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsState()
    val openFolder by viewModel.openFolder.collectAsState()
    val allAppsOpen by viewModel.allAppsOpen.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val widgetIds by viewModel.widgetIds.collectAsState()
    var widgetAreaEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetAreaEnabled(context)) }
    var bgType by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgType(context)) }
    var bgColorInt by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgColor(context)) }
    var textAlpha by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getTextAlpha(context)) }
    var suggestionsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSuggestionsEnabled(context)) }
    var recentAppsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabled(context)) }
    var favoritesEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabled(context)) }
    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    var suggestionIconPack by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context)) }
    var customFolderNames by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomNames(context)) }
    var customFolderEmojis by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomEmojis(context)) }
    var customFolderColors by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomColors(context)) }
    var folderSizeDp by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderSizeDp(context)) }
    var labelColorHex by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getLabelColor(context)) }
    // Ayar toggle'ları — DisposableEffect listener ile reaktif
    var swipeHintEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSwipeHintEnabled(context)) }
    var newBadgeEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNewBadgeEnabled(context)) }
    var folderCountVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderCountVisible(context)) }
    var folderSwipeHint    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderSwipeHintEnabled(context)) }
    var notifTextEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)) }
    val labelColor = remember(labelColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(labelColorHex)) }.getOrDefault(Color.White)
    }
    // Settings'de değiştirilen arka plan/widget ayarları launcher'a dönerken anında yansısın
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(
            com.armutlu.apporganizer.utils.AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE
        )
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                com.armutlu.apporganizer.utils.AppPrefs.KEY_BG_TYPE ->
                    bgType = com.armutlu.apporganizer.utils.AppPrefs.getBgType(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_BG_COLOR ->
                    bgColorInt = com.armutlu.apporganizer.utils.AppPrefs.getBgColor(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_TEXT_ALPHA ->
                    textAlpha = com.armutlu.apporganizer.utils.AppPrefs.getTextAlpha(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_WIDGET_AREA_ENABLED ->
                    widgetAreaEnabled = com.armutlu.apporganizer.utils.AppPrefs.isWidgetAreaEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_SUGGESTIONS_ENABLED ->
                    suggestionsEnabled = com.armutlu.apporganizer.utils.AppPrefs.isSuggestionsEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_RECENT_APPS_ENABLED ->
                    recentAppsEnabled = com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FAVORITES_ENABLED ->
                    favoritesEnabled = com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_CUSTOM_NAMES ->
                    customFolderNames = com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomNames(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_CUSTOM_EMOJIS ->
                    customFolderEmojis = com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomEmojis(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_CUSTOM_COLORS ->
                    customFolderColors = com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomColors(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_SIZE ->
                    folderSizeDp = com.armutlu.apporganizer.utils.AppPrefs.getFolderSizeDp(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_LABEL_COLOR ->
                    labelColorHex = com.armutlu.apporganizer.utils.AppPrefs.getLabelColor(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_ICON_PACK ->
                    suggestionIconPack = com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_SWIPE_HINT_ENABLED ->
                    swipeHintEnabled = com.armutlu.apporganizer.utils.AppPrefs.isSwipeHintEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_NEW_BADGE_ENABLED ->
                    newBadgeEnabled = com.armutlu.apporganizer.utils.AppPrefs.isNewBadgeEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_COUNT_VISIBLE ->
                    folderCountVisible = com.armutlu.apporganizer.utils.AppPrefs.isFolderCountVisible(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_SWIPE_HINT_ENABLED ->
                    folderSwipeHint = com.armutlu.apporganizer.utils.AppPrefs.isFolderSwipeHintEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_NOTIFICATION_TEXT_ENABLED ->
                    notifTextEnabled = com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val haptic = LocalHapticFeedback.current
    val composeView = LocalView.current
    val dockPackages by viewModel.dockPackages.collectAsState()
    var dockEditOpen by remember { mutableStateOf(false) }
    var contextMenuPkg by remember { mutableStateOf<String?>(null) }
    // allApps flow'undan güncel app al — isHidden, notificationCount vs. stale olmaz
    val contextMenuApp = contextMenuPkg?.let { pkg -> allApps.find { it.packageName == pkg } }
    var categoryPickerApp by remember { mutableStateOf<com.armutlu.apporganizer.domain.models.AppInfo?>(null) }
    var folderContextMenu by remember { mutableStateOf<AppFolder?>(null) }
    var homeLongPressOpen by remember { mutableStateOf(false) }

    // Drag & drop state
    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragToIndex   by remember { mutableStateOf<Int?>(null) }
    var draggingFolders by remember { mutableStateOf<List<AppFolder>?>(null) }

    // Dock sistem gesture exclusion rect — sadece deger degisince guncellenir, her layout'ta degil
    val dockRectHolder = remember { object { var rect: android.graphics.Rect? = null } }

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    var swipeDelta = 0f  // mutableFloatStateOf değil — sadece scroll callback'te kullanılır, recomposition tetiklemez

    // rememberUpdatedState ile closure'lar her zaman güncel değeri okur
    val currentAllAppsOpen by rememberUpdatedState(allAppsOpen)
    LaunchedEffect(allAppsOpen) { if (allAppsOpen) swipeDelta = 0f }

    // Çift tetikleme önlemi: nestedScroll ve pointerInput aynı gesture'ı tetiklemesin
    var swipeLock by remember { mutableStateOf(false) }
    LaunchedEffect(allAppsOpen) {
        if (allAppsOpen) {
            swipeLock = true
            delay(300)
            swipeLock = false
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!currentAllAppsOpen && !swipeLock && available.y < -200f) {
                    AppAnalytics.allAppsOpened()
                    viewModel.openAllApps()
                }
                return Velocity.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (!currentAllAppsOpen && !swipeLock && available.y < 0f) {
                    swipeDelta += available.y
                    if (swipeDelta < -swipeThresholdPx) {
                        AppAnalytics.allAppsOpened()
                        viewModel.openAllApps()
                        swipeDelta = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    val isLoading = folders.isEmpty() && allApps.isEmpty()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // İzin verilmeden launcher seçildiyse veya veriler henüz yüklenmediyse güvenli fallback
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "AppOrganizer",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = "Uygulamalar yükleniyor...",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                ) {
                    Text("Launcher Ayarları")
                }
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(intent) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Uygulama Ayarları")
                }
            }
        }
        return
    }

    val scope = rememberCoroutineScope()
    val folderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler(enabled = allAppsOpen || openFolder != null) {
        if (allAppsOpen) viewModel.closeAllApps()
        else {
            scope.launch {
                folderSheetState.hide()
                viewModel.closeFolder()
            }
        }
    }

    // Root box — duvar kağıdı (transparent) veya düz renk arka plan
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (bgType == "solid")
                    Modifier.background(Color(bgColorInt))
                else Modifier
            )
            .nestedScroll(nestedScrollConnection)
            .pointerInput(allAppsOpen) {
                if (!allAppsOpen) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openAllApps()
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            homeLongPressOpen = true
                        }
                    )
                }
            }
            .pointerInput(allAppsOpen) {
                if (!allAppsOpen) {
                    var accumulated = 0f
                    var dragStartY = 0f
                    // Xiaomi/Samsung alt gesture zone (~80dp) — oradan başlayan swipe'ı yok say
                    val gestureZonePx = with(density) { 80.dp.toPx() }
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragStartY = offset.y
                            accumulated = 0f
                        },
                        onDragEnd = { accumulated = 0f },
                        onDragCancel = { accumulated = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            if (dragStartY < size.height - gestureZonePx) {
                                accumulated += dragAmount
                                if (!swipeLock && accumulated < -120f) {
                                    accumulated = 0f
                                    AppAnalytics.allAppsOpened()
                                    viewModel.openAllApps()
                                }
                            }
                        }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // İzin uyarı banner'ı (kapatılabilir)
            PermissionsBanner()

            // Clock widget — top center, Pixel style (uzun bas → yönetim ekranı)
            PixelClockWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { viewModel.openManager(context) })
                    }
            )

            // Google arama çubuğu — Pixel Launcher stili, tıklayınca Google'ı açar
            GoogleSearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Favori uygulamalar satiri
            if (favoritesEnabled && favoriteApps.isNotEmpty()) {
                FavoritesRow(
                    apps = favoriteApps,
                    iconPackPkg = suggestionIconPack,
                    onAppClick = { pkg ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        AppAnalytics.appLaunched(pkg, "favorites")
                        viewModel.launchApp(context, pkg)
                    },
                    onAppLongClick = { pkg ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        contextMenuPkg = pkg
                    }
                )
            }

            // Uygulama önerileri — son kullanılan 4 uygulama, toggle ile kapatılabilir
            if (suggestionsEnabled && suggestedApps.isNotEmpty()) {
                AppSuggestionsRow(
                    apps = suggestedApps,
                    iconPackPkg = suggestionIconPack,
                    onAppClick = { app ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.launchApp(context, app.packageName)
                    },
                    onAppLongClick = { app ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        contextMenuPkg = app.packageName
                    }
                )
            }

            // Son kullanılan uygulamalar satırı (varsayılan kapalı)
            if (recentAppsEnabled && recentApps.isNotEmpty()) {
                RecentAppsRow(
                    apps = recentApps,
                    iconPackPkg = suggestionIconPack,
                    onAppClick = { pkg ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.launchApp(context, pkg)
                    }
                )
            }

            // Widget alanı — arama çubuğu ile klasör gridi arasında
            if (widgetAreaEnabled && widgetIds.isNotEmpty()) {
                WidgetArea(
                    widgetIds = widgetIds,
                    onRemoveWidget = { id -> viewModel.removeWidgetId(context, id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // İstatistik bandı — toplam klasör ve uygulama sayısı
            val totalApps   = folders.sumOf { it.apps.size }
            val totalFolders = folders.size
            if (totalFolders > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$totalFolders klasör  ·  $totalApps uygulama",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Folder grid — sayfa basina 8 klasor (4 sutun x 2 satir), fazlasi sonraki sayfaya
            val displayFolders = draggingFolders ?: folders
            val pageSize = 8
            val pageCount = maxOf(1, (displayFolders.size + pageSize - 1) / pageSize)
            val pagerState = rememberPagerState(pageCount = { pageCount })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { page ->
                val pageStart = page * pageSize
                val pageFolders = displayFolders.drop(pageStart).take(pageSize)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    userScrollEnabled = false
                ) {
                items(pageFolders.size) { pageIndex ->
                    val index = pageStart + pageIndex
                    val folder = pageFolders[pageIndex]
                    val isDragging = dragFromIndex == index
                    FolderTile(
                        folder = folder,
                        onClick = {
                            if (dragFromIndex == null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                AppAnalytics.folderOpened(folder.category.categoryId, folder.category.categoryName)
                                viewModel.openFolder(folder)
                            }
                        },
                        onLongClick = {
                            if (dragFromIndex == null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                folderContextMenu = folder
                            }
                        },
                        onSwipeUp = { pkg -> viewModel.launchApp(context, pkg) },
                        onNotificationTap = { pkg -> viewModel.launchApp(context, pkg) },
                        textAlpha = textAlpha,
                        folderSizeDp = folderSizeDp,
                        labelColor = labelColor,
                        customName = customFolderNames[folder.category.categoryId],
                        customEmoji = customFolderEmojis[folder.category.categoryId],
                        customColor = customFolderColors[folder.category.categoryId],
                        folderCountVisible = folderCountVisible,
                        folderSwipeHintEnabled = folderSwipeHint,
                        notifTextEnabled = notifTextEnabled,
                        modifier = Modifier
                            .pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragFromIndex = index
                                        draggingFolders = folders.toMutableList()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val from = dragFromIndex ?: return@detectDragGesturesAfterLongPress
                                        val tileWidthPx = with(density) { 90.dp.toPx() }
                                        val dx = change.position.x
                                        val dy = change.position.y
                                        val colCount = 4
                                        val colOffset = (dx / tileWidthPx).toInt().coerceIn(-1, 1)
                                        val rowOffset = (dy / tileWidthPx).toInt().coerceIn(-1, 1)
                                        val to = (from + rowOffset * colCount + colOffset)
                                            .coerceIn(0, (draggingFolders?.lastIndex ?: 0))
                                        if (to != dragToIndex) {
                                            dragToIndex = to
                                            draggingFolders = draggingFolders?.toMutableList()?.also { list ->
                                                if (from != to && from in list.indices && to in list.indices) {
                                                    val item = list.removeAt(from)
                                                    list.add(to, item)
                                                    dragFromIndex = to
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggingFolders?.let { viewModel.reorderFolders(context, it) }
                                        dragFromIndex = null
                                        dragToIndex = null
                                        draggingFolders = null
                                    },
                                    onDragCancel = {
                                        dragFromIndex = null
                                        dragToIndex = null
                                        draggingFolders = null
                                    }
                                )
                            }
                            .then(if (isDragging) Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)) else Modifier)
                    )
                }
                // Bos slotlar — folder'lardan sonra, uzun basinca HomeLongPressSheet ac
                val emptySlots = pageSize - pageFolders.size
                if (emptySlots > 0) {
                    items(emptySlots) { _ ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            homeLongPressOpen = true
                                        }
                                    )
                                }
                        )
                    }
                }
                } // LazyVerticalGrid
            } // HorizontalPager

            // Sayfa noktaciklari — birden fazla sayfa varsa goster
            if (pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { idx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (pagerState.currentPage == idx) 8.dp else 5.dp)
                                .background(
                                    if (pagerState.currentPage == idx) Color.White.copy(alpha = 0.9f)
                                    else Color.White.copy(alpha = 0.3f),
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            }

            // Swipe-up ipucu — ilk 5 acilista goster
            SwipeHint(context = context, visible = !allAppsOpen && swipeHintEnabled)

            // Drag pill handle — above dock, pure Pixel style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            // Bottom dock — frosted pill (uzun bas → düzenle)
            PixelDock(
                packages = dockPackages,
                iconPackPkg = suggestionIconPack,
                onLaunchApp = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.launchApp(context, pkg)
                },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dockEditOpen = true
                },
                onAppLongPress = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    contextMenuPkg = pkg
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .onGloballyPositioned { coords ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val pos = coords.positionInWindow()
                            val l = pos.x.toInt()
                            val t = pos.y.toInt()
                            val r = (pos.x + coords.size.width).toInt()
                            val b = (pos.y + coords.size.height).toInt()
                            val prev = dockRectHolder.rect
                            if (prev == null || prev.left != l || prev.top != t || prev.right != r || prev.bottom != b) {
                                val newRect = android.graphics.Rect(l, t, r, b)
                                dockRectHolder.rect = newRect
                                composeView.systemGestureExclusionRects = listOf(newRect)
                            }
                        }
                    }
            )
        }

        // All Apps Drawer overlay — LinearOutSlowIn acilirken ani baslatip yavaslatir,
        // FastOutLinearIn kapatirken hizli bitis verir (Material motion standardlari)
        AnimatedVisibility(
            visible = allAppsOpen,
            enter = slideInVertically(
                animationSpec = tween(300, easing = LinearOutSlowInEasing),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(220, easing = FastOutLinearInEasing),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = tween(180))
        ) {
            AllAppsDrawer(
                apps = allApps,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                onAppClick = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.launchApp(context, pkg)
                },
                onAppLongClick = { app ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    contextMenuPkg = app.packageName
                },
                onClose = viewModel::closeAllApps,
                favoriteApps = favoriteApps,
                favoritesEnabled = favoritesEnabled,
                onFavoriteAppClick = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.launchApp(context, pkg)
                },
                recentApps = recentApps,
                recentAppsEnabled = recentAppsEnabled,
                onRecentAppClick = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.launchApp(context, pkg)
                }
            )
        }
    }

    // Dock düzenleme sheet
    if (dockEditOpen) {
        DockEditSheet(
            allApps = allApps,
            dockPackages = dockPackages,
            onAdd = { viewModel.addToDock(context, it) },
            onRemove = { viewModel.removeFromDock(context, it) },
            onDismiss = { dockEditOpen = false }
        )
    }

    // App context menu (long press)
    contextMenuApp?.let { app ->
        AppContextMenu(
            app = app,
            isDocked = app.packageName in dockPackages,
            onDismiss = { contextMenuPkg = null },
            onLaunch = { viewModel.launchApp(context, app.packageName) },
            onAddToDock = { viewModel.addToDock(context, app.packageName) },
            onRemoveFromDock = { viewModel.removeFromDock(context, app.packageName) },
            onChangeCategory = {
                categoryPickerApp = app
                contextMenuPkg = null
            },
            onHideApp = { hidden ->
                viewModel.setAppHidden(app.packageName, hidden)
                contextMenuPkg = null
            },
            onSaveNote = { note ->
                viewModel.saveAppNote(app.packageName, note)
            },
            onToggleFavorite = { _ ->
                viewModel.toggleFavorite(context, app.packageName)
            }
        )
    }

    // Kategori picker sheet
    categoryPickerApp?.let { app ->
        CategoryPickerSheet(
            app = app,
            onDismiss = { categoryPickerApp = null },
            onCategorySelected = { catId ->
                viewModel.updateAppCategory(app.packageName, catId)
                // Klasör açıksa kapat — kullanıcı ana ekranda yeni kategoriyi hemen görsün
                viewModel.closeFolder()
            }
        )
    }

    // Folder bottom sheet
    openFolder?.let { folder ->
        FolderSheet(
            folder = folder,
            sheetState = folderSheetState,
            onDismiss = {
                scope.launch {
                    folderSheetState.hide()
                    viewModel.closeFolder()
                }
            },
            onAppClick = { pkg -> viewModel.launchApp(context, pkg) },
            onAppLongClick = { app ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                contextMenuPkg = app.packageName
            }
        )
    }

    // Ana ekran uzun basma menüsü
    if (homeLongPressOpen) {
        HomeLongPressSheet(
            onDismiss = { homeLongPressOpen = false },
            onWallpaper = {
                homeLongPressOpen = false
                val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(Intent.createChooser(intent, "Duvar Kagidi Sec").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }) }
            },
            onSettings = {
                homeLongPressOpen = false
                viewModel.openManager(context)
            },
            onDockEdit = {
                homeLongPressOpen = false
                dockEditOpen = true
            },
            onAddWidget = {
                homeLongPressOpen = false
                onLaunchWidgetPicker()
            }
        )
    }

    // Klasör uzun basınca context menu
    folderContextMenu?.let { folder ->
        FolderContextMenuSheet(
            folder = folder,
            onDismiss = { folderContextMenu = null },
            onOpenFolder = {
                folderContextMenu = null
                viewModel.openFolder(folder)
            },
            onOpenAllApps = {
                folderContextMenu = null
                viewModel.openAllApps()
            }
        )
    }
}

// PixelClockWidget, GoogleSearchBar, PixelDock, DockIcon, SwipeHint
// HomeScreenComponents.kt dosyasına taşındı — aynı paket, internal görünürlük.


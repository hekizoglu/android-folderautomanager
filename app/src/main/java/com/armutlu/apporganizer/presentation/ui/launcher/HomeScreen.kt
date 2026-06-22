package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.utils.AppAnalytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onLaunchWidgetPicker: () -> Unit = {}
) {
    val context = LocalContext.current
    var folderBlurEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderBlurEnabled(context)) }
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
    var recentAppsEnabledAllApps by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabledAllApps(context)) }
    var favoritesEnabledAllApps by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabledAllApps(context)) }
    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    var suggestionIconPack by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context)) }
    var customFolderNames by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomNames(context)) }
    var customFolderEmojis by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomEmojis(context)) }
    var customFolderColors by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomColors(context)) }
    var folderSizeDp by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderSizeDp(context)) }
    var pageFolderCount by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getPageSize(context)) }
    var labelColorHex by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getLabelColor(context)) }
    // Ayar toggle'ları — DisposableEffect listener ile reaktif
    var swipeHintEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSwipeHintEnabled(context)) }
    var newBadgeEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNewBadgeEnabled(context)) }
    var folderCountVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderCountVisible(context)) }
    var folderSwipeHint    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderSwipeHintEnabled(context)) }
    var notifTextEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)) }
    var folderShape        by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderShape(context)) }
    var homeSearchEnabled  by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeSearchEnabled(context)) }
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
                com.armutlu.apporganizer.utils.AppPrefs.KEY_RECENT_APPS_ENABLED_ALLAPPS ->
                    recentAppsEnabledAllApps = com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabledAllApps(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FAVORITES_ENABLED_ALLAPPS ->
                    favoritesEnabledAllApps = com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabledAllApps(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_CUSTOM_NAMES ->
                    customFolderNames = com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomNames(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_CUSTOM_EMOJIS ->
                    customFolderEmojis = com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomEmojis(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_CUSTOM_COLORS ->
                    customFolderColors = com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomColors(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_SIZE ->
                    folderSizeDp = com.armutlu.apporganizer.utils.AppPrefs.getFolderSizeDp(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_PAGE_SIZE ->
                    pageFolderCount = com.armutlu.apporganizer.utils.AppPrefs.getPageSize(context)
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
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_SHAPE ->
                    folderShape = com.armutlu.apporganizer.utils.AppPrefs.getFolderShape(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_BLUR ->
                    folderBlurEnabled = com.armutlu.apporganizer.utils.AppPrefs.isFolderBlurEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_HOME_SEARCH_ENABLED ->
                    homeSearchEnabled = com.armutlu.apporganizer.utils.AppPrefs.isHomeSearchEnabled(context)
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
    var folderSearchQuery by remember { mutableStateOf("") }
    var folderSearchCountdown by remember { mutableStateOf(30) }

    // Drag & drop state
    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragToIndex   by remember { mutableStateOf<Int?>(null) }
    var draggingFolders by remember { mutableStateOf<List<AppFolder>?>(null) }
    // Kümülatif drag offset — change.position tile-local, dragAmount ekran-delta verir
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }

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
            delay(150)
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

    // Klasör arama 30s otomatik sıfırlama
    LaunchedEffect(folderSearchQuery) {
        folderSearchCountdown = 30
        if (folderSearchQuery.isNotEmpty()) {
            repeat(30) {
                delay(1_000)
                folderSearchCountdown--
            }
            folderSearchQuery = ""
            folderSearchCountdown = 30
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.launcher_settings))
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
                    Text(stringResource(R.string.app_settings))
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
    // NOT: .haze() gesture'lardan SONRA — Haze 0.7.3 önce gelince pointer event tüketiyor
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (bgType == "solid")
                    Modifier.background(Color(bgColorInt))
                else Modifier
            )
            .nestedScroll(nestedScrollConnection)
            .pointerInput("tap") {
                detectTapGestures(
                    onDoubleTap = {
                        if (!currentAllAppsOpen) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openAllApps()
                        }
                    },
                    onLongPress = {
                        if (!currentAllAppsOpen) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            homeLongPressOpen = true
                        }
                    }
                )
            }
            .pointerInput("drag") {
                var accumulated = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        accumulated = 0f
                    },
                    onDragEnd = { accumulated = 0f },
                    onDragCancel = { accumulated = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        // gestureZonePx kontrolünü kaldırdık — tüm ekrandan swipe çalışsın
                        if (!currentAllAppsOpen) {
                            accumulated += dragAmount
                            if (!swipeLock && accumulated < -60f) {
                                change.consume()
                                accumulated = 0f
                                AppAnalytics.allAppsOpened()
                                viewModel.openAllApps()
                            }
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Top
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

            // Klasör arama çubuğu — HomeScreenComponents.FolderSearchBar
            if (homeSearchEnabled) {
                FolderSearchBar(
                    query = folderSearchQuery,
                    onQueryChange = { folderSearchQuery = it },
                    onClear = { folderSearchQuery = ""; folderSearchCountdown = 30 },
                    countdown = folderSearchCountdown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Favori, öneri ve son kullanılan satırları — HomeScreenFavorites.kt
            HomeFavoritesSection(
                favoritesEnabled = favoritesEnabled,
                favoriteApps = favoriteApps,
                suggestionsEnabled = suggestionsEnabled,
                suggestedApps = suggestedApps,
                recentAppsEnabled = recentAppsEnabled,
                recentApps = recentApps,
                iconPackPkg = suggestionIconPack,
                haptic = haptic,
                onLaunchApp = { pkg -> viewModel.launchApp(context, pkg) },
                onAppLongClick = { pkg -> contextMenuPkg = pkg }
            )

            // Widget alanı — arama çubuğu ile klasör gridi arasında
            if (widgetAreaEnabled && widgetIds.isNotEmpty()) {
                WidgetArea(
                    widgetIds = widgetIds,
                    onRemoveWidget = { id -> viewModel.removeWidgetId(context, id) },
                    onReorderWidgets = { newOrder -> viewModel.reorderWidgets(context, newOrder) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // İstatistik bandı — toplam klasör ve uygulama sayısı
            FolderStatsRow(folders = folders)

            // Folder grid — sayfa başına klasör sayısı ayarlanabilir (AppPrefs.KEY_PAGE_SIZE)
            val baseFolders = draggingFolders ?: folders
            val displayFolders = if (homeSearchEnabled && folderSearchQuery.isNotEmpty()) {
                val q = folderSearchQuery.lowercase(java.util.Locale("tr"))
                baseFolders.filter { folder ->
                    folder.category.categoryName.lowercase(java.util.Locale("tr")).contains(q) ||
                    folder.apps.any { it.appName.lowercase(java.util.Locale("tr")).contains(q) }
                }
            } else baseFolders
            val pageSize = pageFolderCount
            val pageCount = maxOf(1, (displayFolders.size + pageSize - 1) / pageSize)
            val pagerState = rememberPagerState(pageCount = { pageCount })

            FolderPager(
                pagerState = pagerState,
                displayFolders = displayFolders,
                pageSize = pageSize,
                dragFromIndex = dragFromIndex,
                dragToIndex = dragToIndex,
                dragOffsetX = dragOffsetX,
                dragOffsetY = dragOffsetY,
                textAlpha = textAlpha,
                folderSizeDp = folderSizeDp,
                labelColor = labelColor,
                customFolderNames = customFolderNames,
                customFolderEmojis = customFolderEmojis,
                customFolderColors = customFolderColors,
                folderCountVisible = folderCountVisible,
                folderSwipeHint = folderSwipeHint,
                notifTextEnabled = notifTextEnabled,
                folderShape = folderShape,
                haptic = haptic,
                onFolderClick = { viewModel.openFolder(it) },
                onFolderLongClick = { folderContextMenu = it },
                onSwipeUp = { pkg -> viewModel.launchApp(context, pkg) },
                onNotificationTap = { pkg -> viewModel.launchApp(context, pkg) },
                onDragStart = { index ->
                    dragFromIndex = index
                    dragOffsetX = 0f
                    dragOffsetY = 0f
                    draggingFolders = folders.toMutableList()
                },
                onDrag = { dragAmount, page ->
                    val from = dragFromIndex ?: return@FolderPager
                    dragOffsetX += dragAmount.x
                    dragOffsetY += dragAmount.y
                    val colCount = 4
                    val screenWidthPx = with(density) { android.content.res.Resources.getSystem().displayMetrics.widthPixels.toFloat() }
                    val tileWidthPx = screenWidthPx / colCount
                    val tileHeightPx = with(density) { 100.dp.toPx() }
                    val colOffset = (dragOffsetX / tileWidthPx).toInt()
                    val rowOffset = (dragOffsetY / tileHeightPx).toInt()
                    val pageOffset = page * pageSize
                    val localFrom = from - pageOffset
                    val pageFoldersCnt = displayFolders.drop(pageOffset).take(pageSize).size
                    val localCol = localFrom % colCount + colOffset
                    val localRow = localFrom / colCount + rowOffset
                    val localTo = (localRow * colCount + localCol).coerceIn(0, pageFoldersCnt - 1)
                    val globalTo = pageOffset + localTo
                    if (globalTo != dragToIndex) {
                        dragToIndex = globalTo
                        draggingFolders = draggingFolders?.toMutableList()?.also { list ->
                            if (from != globalTo && from in list.indices && globalTo in list.indices) {
                                val item = list.removeAt(from)
                                list.add(globalTo, item)
                                dragFromIndex = globalTo
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            }
                        }
                    }
                },
                onDragEnd = {
                    draggingFolders?.let { viewModel.reorderFolders(context, it) }
                    dragFromIndex = null
                    dragToIndex = null
                    draggingFolders = null
                    dragOffsetX = 0f
                    dragOffsetY = 0f
                },
                onDragCancel = {
                    dragFromIndex = null
                    dragToIndex = null
                    draggingFolders = null
                    dragOffsetX = 0f
                    dragOffsetY = 0f
                },
                onHomeLongPress = { homeLongPressOpen = true },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            // Sayfa noktaciklari — HomeScreenPageIndicator.kt
            HomePageIndicator(pageCount = pageCount, pagerState = pagerState)

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
                favoritesEnabled = favoritesEnabledAllApps,
                onFavoriteAppClick = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.launchApp(context, pkg)
                },
                recentApps = recentApps,
                recentAppsEnabled = recentAppsEnabledAllApps,
                onRecentAppClick = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.launchApp(context, pkg)
                }
            )
        }
    }

    HomeScreenOverlays(
        allApps = allApps,
        folders = folders,
        dockPackages = dockPackages,
        openFolder = openFolder,
        folderSheetState = folderSheetState,
        dockEditOpen = dockEditOpen,
        contextMenuApp = contextMenuApp,
        categoryPickerApp = categoryPickerApp,
        homeLongPressOpen = homeLongPressOpen,
        folderContextMenu = folderContextMenu,
        haptic = haptic,
        scope = scope,
        onDockEditDismiss = { dockEditOpen = false },
        onContextMenuDismiss = { contextMenuPkg = null },
        onCategoryPickerDismiss = { categoryPickerApp = null },
        onHomeLongPressDismiss = { homeLongPressOpen = false },
        onFolderContextMenuDismiss = { folderContextMenu = null },
        onDockAdd = { viewModel.addToDock(context, it) },
        onDockRemove = { viewModel.removeFromDock(context, it) },
        onLaunchApp = { pkg -> viewModel.launchApp(context, pkg) },
        onAddToDock = { pkg -> viewModel.addToDock(context, pkg) },
        onRemoveFromDock = { pkg -> viewModel.removeFromDock(context, pkg) },
        onChangeCategory = { app ->
            categoryPickerApp = app
            contextMenuPkg = null
        },
        onHideApp = { app, hidden ->
            viewModel.setAppHidden(app.packageName, hidden)
            contextMenuPkg = null
        },
        onSaveNote = { app, note -> viewModel.saveAppNote(app.packageName, note) },
        onToggleFavorite = { app -> viewModel.toggleFavorite(context, app.packageName) },
        onCategorySelected = { app, catId ->
            viewModel.updateAppCategory(app.packageName, catId)
            viewModel.closeFolder()
        },
        onFolderDismiss = {
            scope.launch {
                folderSheetState.hide()
                viewModel.closeFolder()
            }
        },
        onFolderAppLongClick = { app ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            contextMenuPkg = app.packageName
        },
        onOpenFolder = { folder ->
            folderContextMenu = null
            viewModel.openFolder(folder)
        },
        onOpenAllApps = {
            folderContextMenu = null
            viewModel.openAllApps()
        },
        onMoveFolder = { folder, newIndex ->
            val currentList = folders.toMutableList()
            val fromIndex = currentList.indexOfFirst { it.category.categoryId == folder.category.categoryId }
            if (fromIndex >= 0 && newIndex in currentList.indices) {
                val item = currentList.removeAt(fromIndex)
                currentList.add(newIndex, item)
                viewModel.reorderFolders(context, currentList)
            }
            folderContextMenu = null
        },
        onWallpaper = {
            homeLongPressOpen = false
            val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                context.startActivity(Intent.createChooser(intent, "Duvar Kagidi Sec").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
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

@Composable
private fun FolderStatsRow(folders: List<AppFolder>) {
    val totalApps = folders.sumOf { it.apps.size }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenOverlays(
    allApps: List<com.armutlu.apporganizer.domain.models.AppInfo>,
    folders: List<AppFolder>,
    dockPackages: List<String>,
    openFolder: AppFolder?,
    folderSheetState: androidx.compose.material3.SheetState,
    dockEditOpen: Boolean,
    contextMenuApp: com.armutlu.apporganizer.domain.models.AppInfo?,
    categoryPickerApp: com.armutlu.apporganizer.domain.models.AppInfo?,
    homeLongPressOpen: Boolean,
    folderContextMenu: AppFolder?,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    scope: kotlinx.coroutines.CoroutineScope,
    onDockEditDismiss: () -> Unit,
    onContextMenuDismiss: () -> Unit,
    onCategoryPickerDismiss: () -> Unit,
    onHomeLongPressDismiss: () -> Unit,
    onFolderContextMenuDismiss: () -> Unit,
    onDockAdd: (String) -> Unit,
    onDockRemove: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAddToDock: (String) -> Unit,
    onRemoveFromDock: (String) -> Unit,
    onChangeCategory: (com.armutlu.apporganizer.domain.models.AppInfo) -> Unit,
    onHideApp: (com.armutlu.apporganizer.domain.models.AppInfo, Boolean) -> Unit,
    onSaveNote: (com.armutlu.apporganizer.domain.models.AppInfo, String) -> Unit,
    onToggleFavorite: (com.armutlu.apporganizer.domain.models.AppInfo) -> Unit,
    onCategorySelected: (com.armutlu.apporganizer.domain.models.AppInfo, String) -> Unit,
    onFolderDismiss: () -> Unit,
    onFolderAppLongClick: (com.armutlu.apporganizer.domain.models.AppInfo) -> Unit,
    onOpenFolder: (AppFolder) -> Unit,
    onOpenAllApps: () -> Unit,
    onMoveFolder: (AppFolder, Int) -> Unit,
    onWallpaper: () -> Unit,
    onSettings: () -> Unit,
    onDockEdit: () -> Unit,
    onAddWidget: () -> Unit
) {
    if (dockEditOpen) {
        DockEditSheet(
            allApps = allApps,
            dockPackages = dockPackages,
            onAdd = onDockAdd,
            onRemove = onDockRemove,
            onDismiss = onDockEditDismiss
        )
    }

    contextMenuApp?.let { app ->
        AppContextMenu(
            app = app,
            isDocked = app.packageName in dockPackages,
            onDismiss = onContextMenuDismiss,
            onLaunch = { onLaunchApp(app.packageName) },
            onAddToDock = { onAddToDock(app.packageName) },
            onRemoveFromDock = { onRemoveFromDock(app.packageName) },
            onChangeCategory = { onChangeCategory(app) },
            onHideApp = { hidden -> onHideApp(app, hidden) },
            onSaveNote = { note -> onSaveNote(app, note) },
            onToggleFavorite = { _ -> onToggleFavorite(app) }
        )
    }

    categoryPickerApp?.let { app ->
        CategoryPickerSheet(
            app = app,
            onDismiss = onCategoryPickerDismiss,
            onCategorySelected = { catId -> onCategorySelected(app, catId) }
        )
    }

    openFolder?.let { folder ->
        FolderSheet(
            folder = folder,
            sheetState = folderSheetState,
            onDismiss = onFolderDismiss,
            onAppClick = onLaunchApp,
            onAppLongClick = onFolderAppLongClick,
        )
    }

    if (homeLongPressOpen) {
        HomeLongPressSheet(
            onDismiss = onHomeLongPressDismiss,
            onWallpaper = onWallpaper,
            onSettings = onSettings,
            onDockEdit = onDockEdit,
            onAddWidget = onAddWidget
        )
    }

    folderContextMenu?.let { folder ->
        FolderContextMenuSheet(
            folder = folder,
            allFolders = folders,
            onDismiss = onFolderContextMenuDismiss,
            onOpenFolder = { onOpenFolder(folder) },
            onOpenAllApps = onOpenAllApps,
            onMove = { newIndex -> onMoveFolder(folder, newIndex) }
        )
    }
}

// PixelClockWidget, GoogleSearchBar, PixelDock, DockIcon, SwipeHint
// HomeScreenComponents.kt dosyasına taşındı — aynı paket, internal görünürlük.


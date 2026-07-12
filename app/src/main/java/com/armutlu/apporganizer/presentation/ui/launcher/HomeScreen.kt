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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.navigation.Routes
import com.armutlu.apporganizer.presentation.ui.MainActivity
import com.armutlu.apporganizer.utils.AppAnalytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onLaunchWidgetPicker: () -> Unit = {},
    onNavigateToFolder: (AppFolder) -> Unit = {},
) {
    val context = LocalContext.current
    var folderBlurEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderBlurEnabled(context)) }
    val folders by viewModel.folders.collectAsState()
    val initialLoadDone by viewModel.initialLoadDone.collectAsState()
    val allAppsOpen by viewModel.allAppsOpen.collectAsState()
    val focusSearchOnOpen by viewModel.focusSearchOnOpen.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val widgetIds by viewModel.widgetIds.collectAsState()
    var widgetAreaEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetAreaEnabled(context)) }
    var widgetAutoResize by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetAutoResizeEnabled(context)) }
    var bgType by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgType(context)) }
    var bgColorInt by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgColor(context)) }
    var textAlpha by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getTextAlpha(context)) }
    var suggestionsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSuggestionsEnabled(context)) }
    var recentAppsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabled(context)) }
    var favoritesEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabled(context)) }
    var recentAppsEnabledAllApps by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabledAllApps(context)) }
    var favoritesEnabledAllApps by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabledAllApps(context)) }
    var doubleTapSearchEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isDoubleTapSearchEnabled(context)) }
    var assistantCardsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isAssistantCardsEnabled(context)) }
    var tickerEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isTickerEnabled(context)) }
    val tickerItems by viewModel.tickerItems.collectAsState()
    val insightCards by viewModel.insightCards.collectAsState()
    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var suggestionIconPack by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context)) }
    var customFolderNames by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomNames(context)) }
    var customFolderEmojis by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomEmojis(context)) }
    var customFolderColors by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomColors(context)) }
    var folderSizeDp by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderSizeDp(context)) }
    var autoFolderSizeEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isAutoFolderSizeEnabled(context)) }
    var pageFolderCount by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getPageSize(context)) }
    val configuration = LocalConfiguration.current
    val isTablet = remember(configuration.screenWidthDp) { configuration.screenWidthDp >= 600 }
    val screenColumns = remember(configuration.screenWidthDp) {
        when {
            configuration.screenWidthDp >= 840 -> 6
            configuration.screenWidthDp >= 600 -> 5
            else -> 4
        }
    }
    val maxFolderSizeDp = remember(configuration.screenWidthDp, screenColumns) {
        ((configuration.screenWidthDp - 32) / screenColumns).coerceIn(48, 96)
    }
    val effectiveFolderSizeDp = remember(autoFolderSizeEnabled, maxFolderSizeDp, folderSizeDp) {
        if (autoFolderSizeEnabled) maxFolderSizeDp else folderSizeDp.coerceAtMost(maxFolderSizeDp)
    }
    var labelColorHex by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getLabelColor(context)) }
    // Ayar toggle'ları — DisposableEffect listener ile reaktif
    var swipeHintEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSwipeHintEnabled(context)) }
    var newBadgeEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNewBadgeEnabled(context)) }
    var folderCountVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderCountVisible(context)) }
    var folderSwipeHint    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderSwipeHintEnabled(context)) }
    var notifTextEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)) }
    var unusedInfoEnabled  by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isUnusedInfoEnabled(context)) }
    var folderBadgeEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderBadgeEnabled(context)) }
    var folderShape        by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderShape(context)) }
    var homeSearchEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeSearchEnabled(context)) }
    var homeAppSearchEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeAppSearchEnabled(context)) }
    var searchBarPosition    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getSearchBarPosition(context)) }
    var quickWheelEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isQuickWheelEnabled(context)) }
    var focusModeEnabled     by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFocusModeEnabled(context)) }
    var gestureDoubleTap by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getGestureDoubleTap(context)) }
    var gestureLongPress by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getGestureLongPress(context)) }
    var gestureSwipeUp   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getGestureSwipeUp(context)) }
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
                com.armutlu.apporganizer.utils.AppPrefs.KEY_AUTO_FOLDER_SIZE ->
                    autoFolderSizeEnabled = com.armutlu.apporganizer.utils.AppPrefs.isAutoFolderSizeEnabled(context)
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
                com.armutlu.apporganizer.utils.AppPrefs.KEY_UNUSED_INFO_ENABLED ->
                    unusedInfoEnabled = com.armutlu.apporganizer.utils.AppPrefs.isUnusedInfoEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_BADGE_ENABLED ->
                    folderBadgeEnabled = com.armutlu.apporganizer.utils.AppPrefs.isFolderBadgeEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_SHAPE ->
                    folderShape = com.armutlu.apporganizer.utils.AppPrefs.getFolderShape(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_BLUR ->
                    folderBlurEnabled = com.armutlu.apporganizer.utils.AppPrefs.isFolderBlurEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_HOME_SEARCH_ENABLED ->
                    homeSearchEnabled = com.armutlu.apporganizer.utils.AppPrefs.isHomeSearchEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_HOME_APP_SEARCH_ENABLED ->
                    homeAppSearchEnabled = com.armutlu.apporganizer.utils.AppPrefs.isHomeAppSearchEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_SEARCH_BAR_POSITION ->
                    searchBarPosition = com.armutlu.apporganizer.utils.AppPrefs.getSearchBarPosition(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_WIDGET_AUTO_RESIZE ->
                    widgetAutoResize = com.armutlu.apporganizer.utils.AppPrefs.isWidgetAutoResizeEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_QUICK_WHEEL ->
                    quickWheelEnabled = com.armutlu.apporganizer.utils.AppPrefs.isQuickWheelEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOCUS_MODE ->
                    focusModeEnabled = com.armutlu.apporganizer.utils.AppPrefs.isFocusModeEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_DOUBLE_TAP_SEARCH ->
                    doubleTapSearchEnabled = com.armutlu.apporganizer.utils.AppPrefs.isDoubleTapSearchEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_ASSISTANT_CARDS ->
                    assistantCardsEnabled = com.armutlu.apporganizer.utils.AppPrefs.isAssistantCardsEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_TICKER_ENABLED ->
                    tickerEnabled = com.armutlu.apporganizer.utils.AppPrefs.isTickerEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_GESTURE_DOUBLE_TAP ->
                    gestureDoubleTap = com.armutlu.apporganizer.utils.AppPrefs.getGestureDoubleTap(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_GESTURE_LONG_PRESS ->
                    gestureLongPress = com.armutlu.apporganizer.utils.AppPrefs.getGestureLongPress(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_GESTURE_SWIPE_UP ->
                    gestureSwipeUp = com.armutlu.apporganizer.utils.AppPrefs.getGestureSwipeUp(context)
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
    var quickWheelVisible by remember { mutableStateOf(false) }
    var quickWheelX by remember { mutableStateOf(0f) }
    var quickWheelY by remember { mutableStateOf(0f) }
    var folderSearchQuery by remember { mutableStateOf("") }
    var folderSearchCountdown by remember { mutableStateOf(30) }
    // Kapasite aşımı uyarısı — aynı (istek, kapasite) çifti için bir kez gösterilir
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var lastCapacityWarning by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) }

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
    var swipeDelta by rememberSaveable { mutableStateOf(0f) }

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
                    viewModel.dispatchGestureAction(context, gestureSwipeUp)
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

    // Fix 3: initialLoadDone false iken Room'dan henuz emit gelmedi demektir — cold resume'da
    // gercekten bos DB ile ilk-emit-bekleniyor durumunu ayirt eder, yanlis "yukleniyor" flasini onler.
    val isLoading = !initialLoadDone && folders.isEmpty() && allApps.isEmpty()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Klasör arama 30s otomatik sıfırlama — yeni sorgu gelince eski sayaç iptal edilir
    LaunchedEffect(Unit) {
        snapshotFlow { folderSearchQuery }
            .collectLatest { query ->
                folderSearchCountdown = 30
                if (query.isBlank()) return@collectLatest
                repeat(30) {
                    delay(1_000)
                    folderSearchCountdown--
                }
                if (folderSearchQuery == query) {
                    folderSearchQuery = ""
                }
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

    BackHandler(enabled = allAppsOpen) {
        viewModel.closeAllApps()
    }

    // Ana ekrana her gelişte (ON_RESUME) 1 kez elmas parlaması tetiklensin — sürekli
    // tekrarlayan zamanlayıcı değil (D210 fix, bkz. ShineEffect.kt).
    var homeResumeTrigger by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) homeResumeTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                            val isSearchAction = gestureDoubleTap == com.armutlu.apporganizer.utils.AppPrefs.GestureAction.OPEN_SEARCH
                            if (!isSearchAction || doubleTapSearchEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.dispatchGestureAction(context, gestureDoubleTap)
                            }
                        }
                    },
                    onLongPress = { pressOffset ->
                        if (!currentAllAppsOpen) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (quickWheelEnabled) {
                                quickWheelX = pressOffset.x
                                quickWheelY = pressOffset.y
                                quickWheelVisible = true
                            } else if (gestureLongPress == com.armutlu.apporganizer.utils.AppPrefs.GestureAction.OPEN_APP_MANAGER) {
                                homeLongPressOpen = true
                            } else {
                                viewModel.dispatchGestureAction(context, gestureLongPress)
                            }
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
                                viewModel.dispatchGestureAction(context, gestureSwipeUp)
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
            // İzin uyarıları artık ana ekranda değil — Ayarlar > Eksik İzinler bölümünde

            // Clock widget — top center, Pixel style (uzun bas → yönetim ekranı)
            // Dar ekran / kalabalık grid'de kompakt: klasörler kaybolmasın diye saat küçülür
            val compactClock = pageFolderCount > 8 || configuration.screenHeightDp < 700
            PixelClockWidget(
                compact = compactClock,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (compactClock) 16.dp else 32.dp, bottom = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { viewModel.openManager(context) })
                    }
            )

            // Birleşik arama çubuğu bölümü (S1) — uygulama + klasör + kişi + dosya tek çubukta,
            // sonuçlar kaynak gruplarıyla gösterilir; "Uygulama / Klasör" sekmesi kaldırıldı.
            // Konum AppPrefs.KEY_SEARCH_BAR_POSITION'a göre: TOP = saat widget'ının altı,
            // BOTTOM = below the Google search affordance.
            val searchBarSection: @Composable () -> Unit = {
                if (homeAppSearchEnabled) {
                    HomeAppSearchBar(
                        allApps = allApps,
                        onAppClick = { pkg -> viewModel.launchApp(context, pkg) },
                        // Klasör grubu "Klasor ve Kategori Aramasi" toggle'ına bağlı kalır
                        folders = if (homeSearchEnabled) folders else emptyList(),
                        folderCustomNames = customFolderNames,
                        folderCustomEmojis = customFolderEmojis,
                        onFolderClick = { folder ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToFolder(folder)
                        },
                        searchResults = searchResults,
                        onQueryChange = viewModel::setSearchQuery,
                        onEnableContactsSource = viewModel::enableContactsSearchSource,
                        homeResumeTrigger = homeResumeTrigger,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                } else if (homeSearchEnabled) {
                    // Uygulama araması kapalı ama klasör araması açık — sadece klasör filtresi
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
            }

            // TOP konumu: arama çubuğu saat widget'ının hemen altında
            if (searchBarPosition == com.armutlu.apporganizer.utils.AppPrefs.SEARCH_BAR_POS_TOP) {
                searchBarSection()
            }

            // Google arama çubuğu — Pixel Launcher stili
            GoogleSearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // BOTTOM (varsayılan) konumu: Google aramasının altında
            if (searchBarPosition != com.armutlu.apporganizer.utils.AppPrefs.SEARCH_BAR_POS_TOP) {
                searchBarSection()
            }

            // Favori, öneri ve son kullanılan satırları — HomeScreenFavorites.kt
            HomeFavoritesSection(
                favoritesEnabled = favoritesEnabled,
                favoriteApps = favoriteApps,
                suggestionsEnabled = suggestionsEnabled,
                suggestedApps = suggestedApps,
                recentAppsEnabled = recentAppsEnabled,
                recentApps = recentApps,
                dockPackages = dockPackages,
                iconPackPkg = suggestionIconPack,
                haptic = haptic,
                onLaunchApp = { pkg -> viewModel.launchApp(context, pkg) },
                onAppLongClick = { pkg -> contextMenuPkg = pkg },
                screenHeightDp = LocalConfiguration.current.screenHeightDp,
                showSecondaryRowsInCompactMode = focusModeEnabled,
            )

            // Widget alanı — arama çubuğu ile klasör gridi arasında
            if (widgetAreaEnabled && widgetIds.isNotEmpty()) {
                WidgetArea(
                    widgetIds = widgetIds,
                    onRemoveWidget = { id -> viewModel.removeWidgetId(context, id) },
                    onReorderWidgets = { newOrder -> viewModel.reorderWidgets(context, newOrder) },
                    autoResize = widgetAutoResize,
                    screenHeightDp = LocalConfiguration.current.screenHeightDp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // İçgörüler ticker açıkken de gerekli (haber şeridine akar) — her açılışta yenile
            LaunchedEffect(Unit) {
                viewModel.refreshInsights(context)
            }

            // Assistant kartları — ticker açıkken gizli (içerik haber şeridinde akar)
            if (assistantCardsEnabled && !tickerEnabled) {
                if (insightCards.isNotEmpty()) {
                    // Dashboard kısayolu — FolderStatsRow.onOpenDashboard ile aynı intent
                    val openDashboard = {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.DASHBOARD)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(intent) }
                        Unit
                    }
                    AssistantInsightRow(
                        cards = insightCards,
                        onCardClick = { card ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            when {
                                card.packageName != null -> viewModel.launchApp(context, card.packageName)
                                card.categoryId != null -> {
                                    val folder = folders.find { it.category.categoryId == card.categoryId }
                                    // Klasör bulunamazsa sessiz kalma — Dashboard'a düş (B4)
                                    if (folder != null) onNavigateToFolder(folder) else openDashboard()
                                }
                                // Hedefi olmayan kartlar (motivasyon vb.) Dashboard'ı açar
                                else -> openDashboard()
                            }
                        },
                        onOpenDashboard = openDashboard,
                    )
                }
            }
            // Odak Modu aktifken klasör grid ve istatistik gizlenir
            if (focusModeEnabled) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "Search-first Home modu aktif",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 14.sp
                    )
                }
            } else {
            // Haber şeridi — "Alışveriş klasöründe 5 uygulama var" tarzı akan bilgiler.
            // Dokunma → hedef açılır; kaydırma → önceki/sonraki haber; basılı tut → sessize al (8s/1g/7g).
            // Kapalıysa eski istatistik bandı döner. Sessizdeyken hiçbir şey gösterilmez, süre dolunca geri gelir.
            var tickerMutedUntil by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getTickerMutedUntil(context)) }
            val tickerMuted = tickerMutedUntil > System.currentTimeMillis()
            if (tickerMuted) {
                // Süre dolduğunda ana ekran açıkken bile şerit kendiliğinden geri gelsin
                LaunchedEffect(tickerMutedUntil) {
                    kotlinx.coroutines.delay((tickerMutedUntil - System.currentTimeMillis()).coerceAtLeast(0L))
                    tickerMutedUntil = 0L
                }
            }
            if (tickerEnabled && !tickerMuted) {
                HomeTickerRow(
                    onMute = { duration ->
                        val until = System.currentTimeMillis() + duration
                        com.armutlu.apporganizer.utils.AppPrefs.setTickerMutedUntil(context, until)
                        tickerMutedUntil = until
                    },
                    items = tickerItems,
                    onItemClick = { item ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Dokunulan haber bu oturumda tekrar gösterilmesin — ticker çeşitlensin (D226)
                        viewModel.dismissTickerItem(item.key)
                        when {
                            item.packageName != null -> viewModel.launchApp(context, item.packageName)
                            item.categoryId != null -> {
                                val folder = folders.find { it.category.categoryId == item.categoryId }
                                if (folder != null) onNavigateToFolder(folder)
                            }
                            item.route != null -> {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_OPEN_ROUTE, item.route)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        }
                    }
                )
            } else if (!tickerEnabled)
            // İstatistik bandı — toplam klasör ve uygulama sayısı (sessize alınmışsa hiçbiri gösterilmez)
            FolderStatsRow(
                folders = folders,
                onOpenFolderStats = {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.REPORTS_CENTER)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                },
                onOpenAppStats = {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.APP_LIST)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                },
                onOpenDashboard = {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.DASHBOARD)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                },
                onOpenUsageReport = {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.USAGE_REPORT)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                },
            )

            // Folder grid — sayfa başına klasör sayısı: kullanıcı tercihi veya ekran yüksekliğine göre adaptif
            val screenHeightDp = LocalConfiguration.current.screenHeightDp
            // Aktif özellik sayısı — fazla içerik varsa daha az klasör göster
            val activeFeatureCount = listOf(favoritesEnabled, suggestionsEnabled, recentAppsEnabled).count { it }
            val requestedPageSize = if (pageFolderCount == 8) {
                // Varsayılan değerdeyse otomatik adaptif hesapla
                when {
                    screenHeightDp < 640 -> 4
                    screenHeightDp < 720 && activeFeatureCount >= 2 -> 4
                    screenHeightDp < 800 && activeFeatureCount >= 2 -> 8
                    else -> 8
                }
            } else pageFolderCount  // Kullanıcı manuel ayarladıysa dokunma
            val baseFolders = draggingFolders ?: folders
            val displayFolders = if (homeSearchEnabled && folderSearchQuery.isNotEmpty()) {
                val q = folderSearchQuery.lowercase(java.util.Locale("tr"))
                baseFolders.filter { folder ->
                    folder.category.categoryName.lowercase(java.util.Locale("tr")).contains(q) ||
                    folder.apps.any { it.appName.lowercase(java.util.Locale("tr")).contains(q) }
                }
            } else baseFolders

            // Kalan gerçek yükseklik ölçülür; sığmayan klasör KIRPILMAZ, sonraki sayfaya taşar
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
            val availableHeightDp = with(density) { constraints.maxHeight.toDp().value.toInt() }
            val folderCapacity = remember(availableHeightDp, effectiveFolderSizeDp, screenColumns) {
                HomeLayoutMath.folderCapacity(availableHeightDp, effectiveFolderSizeDp, screenColumns)
            }
            val pageSize = minOf(requestedPageSize, folderCapacity)
            // Kullanıcının manuel seçtiği sayfa boyutu ekrana sığmıyorsa görüntüyü bozmadan uyar
            LaunchedEffect(pageFolderCount, folderCapacity) {
                if (pageFolderCount != 8 && pageFolderCount > folderCapacity &&
                    lastCapacityWarning != pageFolderCount to folderCapacity
                ) {
                    lastCapacityWarning = pageFolderCount to folderCapacity
                    snackbarHostState.showSnackbar(
                        "Bu ekranda en fazla $folderCapacity klasör sığıyor — Ayarlar'dan sayfa boyutunu küçültebilirsin"
                    )
                }
            }
            val pageCount = maxOf(1, (displayFolders.size + pageSize - 1) / pageSize)
            // Son görüntülenen sayfa AppPrefs'ten okunur — geri tuşu/process death sonrası
            // ilk sayfaya sıfırlanmasın (D210 fix). pageCount değişirse sınır dışına taşmasın.
            val initialPage = remember { com.armutlu.apporganizer.utils.AppPrefs.getLastHomePage(context).coerceIn(0, pageCount - 1) }
            val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .collect { page -> com.armutlu.apporganizer.utils.AppPrefs.setLastHomePage(context, page) }
            }

            Column(modifier = Modifier.fillMaxSize()) {

            FolderPager(
                pagerState = pagerState,
                displayFolders = displayFolders,
                pageSize = pageSize,
                columnsCount = screenColumns,
                dragFromIndex = dragFromIndex,
                dragToIndex = dragToIndex,
                dragOffsetX = dragOffsetX,
                dragOffsetY = dragOffsetY,
                textAlpha = textAlpha,
                folderSizeDp = effectiveFolderSizeDp,
                labelColor = labelColor,
                customFolderNames = customFolderNames,
                customFolderEmojis = customFolderEmojis,
                customFolderColors = customFolderColors,
                folderCountVisible = folderCountVisible,
                folderSwipeHint = folderSwipeHint,
                notifTextEnabled = notifTextEnabled,
                unusedInfoEnabled = unusedInfoEnabled,
                folderBadgeEnabled = folderBadgeEnabled,
                folderShape = folderShape,
                folderGlassEnabled = folderBlurEnabled,
                haptic = haptic,
                onFolderClick = { onNavigateToFolder(it) },
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
            } // end inner Column
            } // end BoxWithConstraints
            } // end else !focusModeEnabled

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

        // All Apps Drawer — telefonda tam ekran overlay, tablette sağ side panel
        AnimatedVisibility(
            visible = allAppsOpen,
            modifier = if (isTablet)
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(380.dp)
            else Modifier,
            enter = if (isTablet)
                slideInHorizontally(tween(280, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(200))
            else
                slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = if (isTablet)
                slideOutHorizontally(tween(220, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(180))
            else
                slideOutVertically(tween(220, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(180))
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
                },
                focusSearchOnOpen = focusSearchOnOpen,
                onFocusSearchConsumed = viewModel::resetFocusSearchOnOpen,
                categories = categories,
                searchResults = searchResults
            )
        }

        // Kapasite aşımı snackbar'ı — layout'u bozmadan dock'un üzerinde görünür
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)
        )

        // Quick Wheel overlay — uzun bas ile radyal uygulama çarkı
        if (quickWheelVisible) {
            val metrics = android.content.res.Resources.getSystem().displayMetrics
            QuickWheelOverlay(
                apps = allApps,
                pressX = quickWheelX,
                pressY = quickWheelY,
                screenWidthPx = metrics.widthPixels.toFloat(),
                screenHeightPx = metrics.heightPixels.toFloat(),
                onLaunch = { pkg -> viewModel.launchApp(context, pkg) },
                onDismiss = { quickWheelVisible = false }
            )
        }
    }

    HomeScreenOverlays(
        allApps = allApps,
        folders = folders,
        dockPackages = dockPackages,
        dockEditOpen = dockEditOpen,
        contextMenuApp = contextMenuApp,
        favoritePackages = favoriteApps.mapTo(mutableSetOf()) { it.packageName },
        categories = categories,
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
        },
        onOpenFolder = { folder ->
            folderContextMenu = null
            onNavigateToFolder(folder)
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

// FolderStatsRow, HomeScreenOverlays → HomeScreenOverlays.kt dosyasına taşındı.
// PixelClockWidget, GoogleSearchBar, PixelDock, DockIcon, SwipeHint → HomeScreenComponents.kt


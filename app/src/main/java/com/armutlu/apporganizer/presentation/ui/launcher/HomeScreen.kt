package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.pager.rememberPagerState
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageAnchor
import com.armutlu.apporganizer.utils.HomePagePrefs
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.armutlu.apporganizer.utils.DockPrefs
import com.armutlu.apporganizer.domain.home.PulseActionRouter
import com.armutlu.apporganizer.domain.models.HomeLayoutItem
import com.armutlu.apporganizer.domain.models.HomeSectionId
import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.utils.HomeLayoutPrefs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onLaunchWidgetPicker: () -> Unit = {},
    onNavigateToFolder: (AppFolder) -> Unit = {},
    onEditHomeLayout: () -> Unit = {},
) {
    val context = LocalContext.current
    // ROADMAP #24 kok neden: kok Column fillMaxSize+imePadding() ile sinirli, kaydirilmiyor.
    // Klavye acildiginda favoriler/oneriler/widget alani gibi ikincil satirlar yer kaplamaya
    // devam edince arama sonuclari (ozellikle BOTTOM konumunda, agirlikli klasor gridinden
    // SONRA render edilen) ekran/klavye sinirinin disina tasip gorunmez oluyordu. Klavye
    // acikken bu ikincil satirlari gecici gizleyerek arama + sonuc alani her zaman gorunur
    // kalir.
    val imeVisible = WindowInsets.isImeVisible
    val folders by viewModel.folders.collectAsState()
    val initialLoadDone by viewModel.initialLoadDone.collectAsState()
    val allAppsOpen by viewModel.allAppsOpen.collectAsState()
    val focusSearchOnOpen by viewModel.focusSearchOnOpen.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val filesIndexState by viewModel.filesIndexState.collectAsState()
    val widgetIds by viewModel.widgetIds.collectAsState()
    var widgetAreaEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetAreaEnabled(context)) }
    var widgetAutoResize by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetAutoResizeEnabled(context)) }
    var bgType by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgType(context)) }
    var bgColorInt by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgColor(context)) }
    var bgGradientStyle by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getHomeBackgroundStyle(context)) }
    var textAlpha by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getTextAlpha(context)) }
    var suggestionsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSuggestionsEnabled(context)) }
    var recentNotificationAppsRowEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentNotificationAppsRowEnabled(context)) }
    var recentAppsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabled(context)) }
    var favoritesEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabled(context)) }
    var recentAppsEnabledAllApps by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabledAllApps(context)) }
    var favoritesEnabledAllApps by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabledAllApps(context)) }
    var doubleTapSearchEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isDoubleTapSearchEnabled(context)) }
    var assistantCardsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isAssistantCardsEnabled(context)) }
    var tickerEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isTickerEnabled(context)) }
    // T05 (Akıllı Nabız ayarları) — otomatik geçiş aç/kapat + geçiş süresi Ayarlar'dan gelir.
    var tickerAutoAdvance by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isTickerAutoAdvanceEnabled(context)) }
    var tickerIntervalSeconds by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getTickerIntervalSeconds(context)) }
    var missionsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isMissionsEnabled(context)) }
    // D03: skor artık tek yerde — DigitalLifeCard. isDigitalLifeCardVisible() eski
    // KEY_HOME_SCORE_VISIBLE tercihini ilk çağrıda bir kez migrate eder.
    var digitalLifeCardVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isDigitalLifeCardVisible(context)) }
    var recentInstallsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentInstallsEnabled(context)) }
    // Görev S1 — tek "BUGÜN" kartı ayarı; açıkken SmartDashboardPage intelligence/recentInstalls/
    // insight bölümlerini TodayCard ile değiştirir.
    var todayCardEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isTodayCardEnabled(context)) }
    val todayInstalledApps by viewModel.todayInstalledApps.collectAsState()
    val tickerItems by viewModel.tickerItems.collectAsState()
    val homePulseSummary by viewModel.homePulseSummary.collectAsState()
    val homeMissionSummary by viewModel.homeMissionSummary.collectAsState()
    val insightCards by viewModel.insightCards.collectAsState()
    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val recentNotificationCounts by viewModel.recentNotificationCounts.collectAsState()
    val recentNotificationApps by viewModel.recentNotificationApps.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    val smartAccessState by viewModel.smartAccessState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val suggestedContacts by viewModel.suggestedContacts.collectAsState()
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
    var suggestionsIconSizeDp by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getSuggestionsIconSizeDp(context)) }
    var folderBadgeEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderBadgeEnabled(context)) }
    var folderShape        by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderShape(context)) }
    var pixelLookEnabled     by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isPixelLookEnabled(context)) }
    // Faz S3 — deneysel, varsayılan KAPALI. Açıkken Dashboard widget alanı WidgetFreeGrid'e döner.
    var widgetFreeGridEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetFreeGridEnabled(context)) }
    var folderGlassBorderEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderGlassBorderEnabled(context)) }
    var homeSearchEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeSearchEnabled(context)) }
    var homeAppSearchEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeAppSearchEnabled(context)) }
    var fullscreenSearchEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFullscreenSearchEnabled(context)) }
    val homeLayoutConfig = remember(context) { HomeLayoutPrefs.read(context).config }
    val homeZonePlan = remember(homeLayoutConfig) { homeZoneRenderPlan(homeLayoutConfig) }
    var searchBarPosition by remember(homeZonePlan) {
        mutableStateOf(
            if (homeLayoutConfig.items.first { it.sectionId == HomeSectionId.MAIN_SEARCH }.zone == HomeLayoutZone.FOOTER) {
                com.armutlu.apporganizer.utils.AppPrefs.SEARCH_BAR_POS_BOTTOM
            } else com.armutlu.apporganizer.utils.AppPrefs.SEARCH_BAR_POS_TOP
        )
    }
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
                com.armutlu.apporganizer.utils.AppPrefs.KEY_PIXEL_LOOK_ENABLED ->
                    pixelLookEnabled = com.armutlu.apporganizer.utils.AppPrefs.isPixelLookEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_WIDGET_FREE_GRID_ENABLED ->
                    widgetFreeGridEnabled = com.armutlu.apporganizer.utils.AppPrefs.isWidgetFreeGridEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_GLASS_BORDER_ENABLED ->
                    folderGlassBorderEnabled = com.armutlu.apporganizer.utils.AppPrefs.isFolderGlassBorderEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_BG_TYPE ->
                    bgType = com.armutlu.apporganizer.utils.AppPrefs.getBgType(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_BG_COLOR ->
                    bgColorInt = com.armutlu.apporganizer.utils.AppPrefs.getBgColor(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_HOME_BACKGROUND_STYLE ->
                    bgGradientStyle = com.armutlu.apporganizer.utils.AppPrefs.getHomeBackgroundStyle(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_TEXT_ALPHA ->
                    textAlpha = com.armutlu.apporganizer.utils.AppPrefs.getTextAlpha(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_WIDGET_AREA_ENABLED ->
                    widgetAreaEnabled = com.armutlu.apporganizer.utils.AppPrefs.isWidgetAreaEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_SUGGESTIONS_ENABLED ->
                    suggestionsEnabled = com.armutlu.apporganizer.utils.AppPrefs.isSuggestionsEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_RECENT_NOTIFICATION_APPS_ROW ->
                    recentNotificationAppsRowEnabled = com.armutlu.apporganizer.utils.AppPrefs.isRecentNotificationAppsRowEnabled(context)
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
                com.armutlu.apporganizer.utils.AppPrefs.KEY_SUGGESTIONS_ICON_SIZE ->
                    suggestionsIconSizeDp = com.armutlu.apporganizer.utils.AppPrefs.getSuggestionsIconSizeDp(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_BADGE_ENABLED ->
                    folderBadgeEnabled = com.armutlu.apporganizer.utils.AppPrefs.isFolderBadgeEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FOLDER_SHAPE ->
                    folderShape = com.armutlu.apporganizer.utils.AppPrefs.getFolderShape(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_HOME_SEARCH_ENABLED ->
                    homeSearchEnabled = com.armutlu.apporganizer.utils.AppPrefs.isHomeSearchEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_HOME_APP_SEARCH_ENABLED ->
                    homeAppSearchEnabled = com.armutlu.apporganizer.utils.AppPrefs.isHomeAppSearchEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_FULLSCREEN_SEARCH_ENABLED ->
                    fullscreenSearchEnabled = com.armutlu.apporganizer.utils.AppPrefs.isFullscreenSearchEnabled(context)
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
                com.armutlu.apporganizer.utils.AppPrefs.KEY_SMART_TICKER_ENABLED ->
                    tickerEnabled = com.armutlu.apporganizer.utils.AppPrefs.isTickerEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_TICKER_AUTO_ADVANCE ->
                    tickerAutoAdvance = com.armutlu.apporganizer.utils.AppPrefs.isTickerAutoAdvanceEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_TICKER_INTERVAL_SECONDS ->
                    tickerIntervalSeconds = com.armutlu.apporganizer.utils.AppPrefs.getTickerIntervalSeconds(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_MISSIONS_ENABLED ->
                    missionsEnabled = com.armutlu.apporganizer.utils.AppPrefs.isMissionsEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_DIGITAL_LIFE_CARD_VISIBLE ->
                    digitalLifeCardVisible = com.armutlu.apporganizer.utils.AppPrefs.isDigitalLifeCardVisible(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_RECENT_INSTALLS_ENABLED -> {
                    recentInstallsEnabled = com.armutlu.apporganizer.utils.AppPrefs.isRecentInstallsEnabled(context)
                    viewModel.refreshTodayInstalled()
                }
                com.armutlu.apporganizer.utils.AppPrefs.KEY_TODAY_CARD_ENABLED ->
                    todayCardEnabled = com.armutlu.apporganizer.utils.AppPrefs.isTodayCardEnabled(context)
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

    // P24 — Dashboard tercihi ve pager rollout/safe-mode bayrakları reaktif okunur; ayarlardan
    // yapılan değişiklikler yeni sayfa planına yeniden bağlanır.
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
    // Döngü P08 — fullscreen arama açık/kapalı bayrağı tek gerçek kaynak olarak burada kalır
    // (davranış DEĞİŞMEDİ — eskiden de HomeScreen'in local `rememberSaveable` state'iydi).
    // GlobalSearchHost bu değeri okur ve arama çubuğuna tıklanınca `onFullScreenSearchOpenChanged`
    // ile günceller; overlay'in kendisi (FullScreenSearchOverlayV2) hâlâ HomeShell'in `overlays`
    // (Box) slotunda, bu değere göre render edilir — mutlak konumlandırma bozulmaz.
    var fullScreenSearchOpen by rememberSaveable { mutableStateOf(false) }
    // Döngü P12 — Home tuşu çift-basış penceresi artık burada tutulur (eskiden LauncherActivity
    // alan değişkeniydi, bkz. LauncherActivity.kt yorumu). Search/modal state'i (fullScreenSearchOpen,
    // folderSearchQuery, dockEditOpen vb.) yalnız burada bilindiği için HomeCommandPolicy.
    // resolveHomeCommand() çağrısı da burada yapılır — LauncherActivity.onNewIntent artık yalnızca
    // ham sinyali (viewModel.onHomePressed()) yayınlar.
    var lastHomePressMs by rememberSaveable { mutableStateOf(0L) }
    // HomePagerHost.kt'deki aynı desen — "azaltılmış hareket" açıkken GoToStartPage komutu da
    // animasyonsuz scrollToPage kullanır (roadmap madde 5: animateScrollToPage veya reduce
    // motion'da scrollToPage).
    val reduceMotionEnabled = remember { !android.animation.ValueAnimator.areAnimatorsEnabled() }
    var quickWheelVisible by remember { mutableStateOf(false) }
    var quickWheelX by remember { mutableStateOf(0f) }
    var quickWheelY by remember { mutableStateOf(0f) }
    var folderSearchQuery by remember { mutableStateOf("") }
    var folderSearchCountdown by remember { mutableStateOf(30) }
    // Kapasite aşımı uyarısı — aynı (istek, kapasite) çifti için bir kez gösterilir
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var lastCapacityWarning by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) }

    // Döngü P05 — indicator hoisting: pageCount/pagerState artık `pager` slotu içinde
    // (BoxWithConstraints ölçümünden sonra) hesaplanıyor ama HomeShell'in `indicator` slotu
    // aynı üst `HomeScreen` recomposition kapsamında AYRI bir lambda — parametre olarak
    // doğrudan iletilemez. `pager` slotu her zaman `indicator` slotundan ÖNCE compose edildiği
    // için (HomeShell.kt: pager() -> indicator() -> ...) bu `remember` holder'a yazıp
    // `indicator` slotunda okumak güvenlidir (aynı frame, tek composition geçişi).
    var homePagerPageCount by remember { mutableStateOf(1) }
    var homePagerState by remember { mutableStateOf<androidx.compose.foundation.pager.PagerState?>(null) }
    // Döngü P12 — GoToStartPage komutu StartPageMode'a göre doğru index'i hesaplayabilsin diye
    // güncel `pages` listesi de aynı hoisting deseniyle (yukarıdaki yorum) dışarı taşınır.
    var homePages by remember { mutableStateOf<List<com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec>>(emptyList()) }
    var homePageSize by remember { mutableStateOf(8) }

    // Drag & drop state
    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragToIndex   by remember { mutableStateOf<Int?>(null) }
    var draggingFolders by remember { mutableStateOf<List<AppFolder>?>(null) }
    // Faz S3 — WidgetFreeGrid kendi serbest sürüklemesi aktifken de kök jestler (pager scroll vb.)
    // klasör reorder'ıyla aynı şekilde kilitlenmeli (aşağıdaki `reorderActive` hesabına OR'lanır).
    var widgetDragActive by remember { mutableStateOf(false) }
    // Kümülatif drag offset — change.position tile-local, dragAmount ekran-delta verir
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Dock sistem gesture exclusion rect — sadece deger degisince guncellenir, her layout'ta degil
    val dockRectHolder = remember { object { var rect: android.graphics.Rect? = null } }

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    // Döngü P10 — kök pointerInput("drag") dikey eşiği artık density-bağımsız (roadmap madde 3).
    // Eskiden ham `-60f` px sabitiydi; HomeGestureArbiter.VERTICAL_SWIPE_THRESHOLD_DP (60dp) buraya
    // taşındı ve density ile px'e çevrildi — mdpi cihazda sayısal olarak birebir aynı (60dp == 60px).
    val rootVerticalDragThresholdPx = with(density) { HomeGestureArbiter.VERTICAL_SWIPE_THRESHOLD_DP.dp.toPx() }
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

    // Döngü P10 — nestedScroll'un dikey-eşik kararı HomeGestureArbiter.decide(NESTED_VERTICAL_SCROLL)
    // çekirdeğine delege edilir (davranış DEĞİŞMEZ — aynı 200f/80dp eşikleri, aynı sıra).
    // swipeLock (çift tetikleme önleyici debounce) arbiter'ın UI-durum modelinde YOKTUR — roadmap
    // kural tablosunda karşılığı olmayan saf zamanlama korumasıdır, çağrı noktasında kalır.
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!swipeLock && available.y < -200f) {
                    val result = HomeGestureArbiter.decide(
                        kind = HomeGestureKind.NESTED_VERTICAL_SCROLL,
                        context = HomeGestureContext(allAppsOpen = currentAllAppsOpen),
                        verticalDeltaPx = available.y,
                        thresholdPx = 0f,
                    )
                    if (result.decision == HomeGestureDecision.OPEN_ALL_APPS) {
                        AppAnalytics.allAppsOpened()
                        viewModel.dispatchGestureAction(context, gestureSwipeUp)
                    }
                }
                return Velocity.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (!swipeLock && available.y < 0f) {
                    swipeDelta += available.y
                    val result = HomeGestureArbiter.decide(
                        kind = HomeGestureKind.NESTED_VERTICAL_SCROLL,
                        context = HomeGestureContext(allAppsOpen = currentAllAppsOpen),
                        verticalDeltaPx = swipeDelta,
                        thresholdPx = swipeThresholdPx,
                    )
                    if (result.decision == HomeGestureDecision.OPEN_ALL_APPS) {
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

    LaunchedEffect(Unit) {
        com.armutlu.apporganizer.utils.AppPrefs.clearLegacyFolderBlurPreference(context)
    }

    // Döngü P12 — Home komut akışı. LauncherActivity.onNewIntent() All Apps kapalıyken Home
    // basışını viewModel.onHomePressed() ile yayınlar (roadmap madde 1: All Apps açıksa Activity
    // erken döner, bu flow'a hiç emit ETMEZ — bkz. LauncherViewModel.kt yorumu). Search/modal
    // kapatma önceliği + başlangıç sayfasına anında dönüş + çift-basış All Apps kararı tek
    // noktada (HomeCommandPolicy.resolveHomeCommand) çözülür.
    LaunchedEffect(Unit) {
        viewModel.homePressed.collect {
            val result = resolveHomeCommand(
                context = HomeCommandContext(
                    searchActive = fullScreenSearchOpen || (homeSearchEnabled && folderSearchQuery.isNotEmpty()),
                    modalOpen = dockEditOpen || homeLongPressOpen || folderContextMenu != null ||
                        contextMenuPkg != null || categoryPickerApp != null,
                ),
                lastHomePressMs = lastHomePressMs,
                nowMs = System.currentTimeMillis(),
            )
            lastHomePressMs = result.nextLastHomePressMs
            when (result.command) {
                HomeCommand.CloseSearch -> {
                    fullScreenSearchOpen = false
                    folderSearchQuery = ""
                }
                HomeCommand.CloseModal -> {
                    dockEditOpen = false
                    homeLongPressOpen = false
                    folderContextMenu = null
                    contextMenuPkg = null
                    categoryPickerApp = null
                }
                HomeCommand.GoToStartPage -> {
                    val pagerState = homePagerState
                    val pages = homePages
                    if (pagerState != null && pages.isNotEmpty()) {
                        // Roadmap madde 6 — start mode'a göre hedef sayfa:
                        // SMART_DASHBOARD -> Dashboard sayfası (yoksa ilk sayfa, HomePageAnchorResolver
                        //   fallback'i). FIRST_FOLDER_PAGE -> her zaman ilk klasör sayfası (index 0,
                        //   pages listesinde Dashboard varsa index 1 olur — anchor Dashboard DEĞİL,
                        //   ilk sayfa index'i doğrudan kullanılır). RESTORE_LAST_PAGE -> kaydedilmiş
                        //   semantic anchor (HomePagePrefs.getLastHomePageAnchor, P13'ün de kullandığı
                        //   aynı kaynak).
                        val startPageMode = com.armutlu.apporganizer.utils.HomePagePrefs.getStartPageMode(context)
                        val targetIndex = when (startPageMode) {
                            com.armutlu.apporganizer.utils.HomePagePrefs.StartPageMode.SMART_DASHBOARD ->
                                HomePageAnchorResolver.resolve(pages, com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageAnchor.Dashboard)
                            com.armutlu.apporganizer.utils.HomePagePrefs.StartPageMode.FIRST_FOLDER_PAGE ->
                                0
                            com.armutlu.apporganizer.utils.HomePagePrefs.StartPageMode.RESTORE_LAST_PAGE -> {
                                val anchor = com.armutlu.apporganizer.utils.HomePagePrefs.getLastHomePageAnchor(context, folders, homePageSize)
                                HomePageAnchorResolver.resolve(pages, anchor)
                            }
                        }.coerceIn(0, pages.lastIndex)
                        if (pagerState.currentPage != targetIndex) {
                            if (reduceMotionEnabled) {
                                pagerState.scrollToPage(targetIndex)
                            } else {
                                pagerState.animateScrollToPage(targetIndex)
                            }
                        }
                    }
                }
                HomeCommand.OpenAllApps -> {
                    AppAnalytics.allAppsOpened()
                    viewModel.openAllApps()
                }
                HomeCommand.None -> Unit
            }
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

    // D260 reload-bug fix: eskiden yalnizca allAppsOpen==true iken aktifti; ana ekran kokunde
    // (allAppsOpen==false) hicbir BackHandler devrede olmadigindan bazi OEM'lerde (Android 13+
    // predictive back / MIUI-HyperOS) sistem geri tusu LauncherActivity'yi sonlandirabiliyor —
    // bu da sonraki HOME basisinda sifirdan onCreate/reload olarak hissediliyordu. Artik daima
    // aktif: allAppsOpen'da cekmeceyi kapatir, kokte ise hicbir sey yapmadan geri tusunu yutar
    // (launcher'in kendisi "en kok" ekran — geri basinca hicbir yere gidilmez, Activity asla
    // finish edilmez).
    BackHandler(enabled = true) {
        when {
            fullScreenSearchOpen -> fullScreenSearchOpen = false
            allAppsOpen -> viewModel.closeAllApps()
            else -> Unit
        }
        // else: no-op — ana ekranda geri tusu Activity'yi kapatmamali (bkz. yukaridaki not)
    }

    // Ana ekrana her gelişte (ON_RESUME) 1 kez elmas parlaması tetiklensin — sürekli
    // tekrarlayan zamanlayıcı değil (D210 fix, bkz. ShineEffect.kt).
    var homeResumeTrigger by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Şerit görünürlüğü (roadmap T04) — ana ekran arka plandayken (ON_PAUSE) otomatik geçiş
    // timer'ı çalışmamalı; HomeTickerRow'a visible= olarak iletilir.
    var homeTickerVisible by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    homeResumeTrigger++
                    homeTickerVisible = true
                    viewModel.refreshSmartAccessPermissions()
                    viewModel.refreshLastLaunched()
                }
                Lifecycle.Event.ON_PAUSE -> homeTickerVisible = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launcher yüzeyi: Ayarlar > Görünüm > Arka Plan seçimine göre boyanır (Görev 1, D26x).
    // "Duvar Kağıdı" seçiliyken transparan kalır (sistem duvar kağıdı sızar); diğer stillerde
    // opak boyanır. NOT: .haze() gesture'lardan SONRA — Haze 0.7.3 önce gelince pointer event tüketiyor
    Box(
        modifier = Modifier
            .fillMaxSize()
            .homeRootBackground(bgType, bgColorInt, bgGradientStyle)
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
                        // Döngü P10 — eskiden ham `-60f` px sabiti kullanılıyordu (roadmap madde 3:
                        // "sabit raw -60f kaldırılır"); artık HomeGestureArbiter.VERTICAL_SWIPE_THRESHOLD_DP
                        // density'e göre px'e çevrilir (rootVerticalDragThresholdPx, aşağıda tanımlı) —
                        // karar HomeGestureArbiter.decide(VERTICAL_DRAG) çekirdeğine delege edilir.
                        if (!currentAllAppsOpen) {
                            accumulated += dragAmount
                            if (!swipeLock) {
                                val result = HomeGestureArbiter.decide(
                                    kind = HomeGestureKind.VERTICAL_DRAG,
                                    context = HomeGestureContext(allAppsOpen = currentAllAppsOpen),
                                    verticalDeltaPx = accumulated,
                                    thresholdPx = rootVerticalDragThresholdPx,
                                )
                                if (result.decision == HomeGestureDecision.OPEN_ALL_APPS) {
                                    change.consume()
                                    accumulated = 0f
                                    AppAnalytics.allAppsOpened()
                                    viewModel.dispatchGestureAction(context, gestureSwipeUp)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // Birleşik arama çubuğu bölümü (S1) — uygulama + klasör + kişi + dosya tek çubukta,
        // sonuçlar kaynak gruplarıyla gösterilir; "Uygulama / Klasör" sekmesi kaldırıldı.
        // Konum AppPrefs.KEY_SEARCH_BAR_POSITION'a göre: TOP = saat widget'ının altı,
        // BOTTOM = dock'un hemen üstü (tek elle kullanım — D246).
        // Döngü P08 — searchBarSection local lambda GlobalSearchHost'a taşındı (roadmap satır
        // 805-866): arama artık sayfa bağımsız tek bir composable/host üzerinden besleniyor.
        // FullScreenSearchOverlayV2 kendi kökünde fillMaxSize Box olduğu için (bkz.
        // GlobalSearchHost.kt dosya başı NOT'u) Column tabanlı bu slotta DEĞİL, HomeShell'in
        // `overlays` (Box) slotunda render edilmeye devam eder — sadece açık/kapalı state artık
        // host ile HomeScreen arasında `fullScreenSearchOpen`/`onFullScreenSearchOpenChanged`
        // üzerinden senkronize ediliyor (tek gerçek kaynak: HomeScreen'in `fullScreenSearchOpen`).
        val searchBarSection: @Composable () -> Unit = {
            GlobalSearchHost(
                    homeAppSearchEnabled = homeAppSearchEnabled,
                    homeSearchEnabled = homeSearchEnabled,
                    fullscreenSearchEnabled = fullscreenSearchEnabled,
                    allApps = allApps,
                    folders = folders,
                    folderCustomNames = customFolderNames,
                    folderCustomEmojis = customFolderEmojis,
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    filesIndexState = filesIndexState,
                    homeResumeTrigger = homeResumeTrigger,
                    resultsAbove = searchBarPosition == com.armutlu.apporganizer.utils.AppPrefs.SEARCH_BAR_POS_BOTTOM,
                    onAppClick = { pkg -> viewModel.launchApp(context, pkg) },
                    onNavigateToFolder = onNavigateToFolder,
                    onQueryChange = viewModel::setSearchQuery,
                    onEnableContactsSource = viewModel::enableContactsSearchSource,
                    onEnableFilesSource = viewModel::enableFilesSearchSource,
                    folderSearchQuery = folderSearchQuery,
                    onFolderSearchQueryChange = { folderSearchQuery = it },
                    onFolderSearchClear = { folderSearchQuery = ""; folderSearchCountdown = 30 },
                    folderSearchCountdown = folderSearchCountdown,
                    fullScreenSearchOpen = fullScreenSearchOpen,
                    onFullScreenSearchOpenChanged = { fullScreenSearchOpen = it },
                )
        }

        // Döngü P06 — compactClock ve tickerMutedUntilState `pager` slotunun HEM eski (klasör
        // sayfası üstü) konumundaki kod HEM de dashboardContent lambda'sındaki SmartDashboardPage
        // tarafından okunur/yazılır — tek doğruluk kaynağı olsun diye pager slotundan önce,
        // burada tanımlanır (bkz. görev raporu "koşullu ikili yerleşim" kararı).
        // Döngü P18 — Focus Mode (Search-first / Odak Modu) artık ayrı bir paralel ana ekran
        // değil, bu pager slotunun sade bir preset'idir (roadmap Döngü P18, "Önerilen karar: A").
        // Saat kompaklaştırma kuralı DashboardLayoutPolicy.applyFocusMode ile aynı politikayı
        // paylaşır (clock.compact = true) — tek doğruluk kaynağı politika fonksiyonundadır.
        val compactClock = pageFolderCount > 8 || configuration.screenHeightDp < 700 || focusModeEnabled
        var tickerMutedUntilState by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getTickerMutedUntil(context)) }
        val hideSecondaryRowsForIme = homeAppSearchEnabled && imeVisible

        // Döngü P03 — global shell: search/dock/indicator artık HomeShell'de toplanıyor,
        // sayfa içeriği (saat, kartlar, klasör pager'ı, favoriler vb.) `pager` slotunda kalıyor.
        // Sistem bar + IME padding artık yalnız HomeShell'in kök Column'unda uygulanıyor
        // (ROADMAP #4 notu HomeShell.kt'ye taşındı — davranış birebir korunur).
        // Döngü P09 — FullScreenSearchOverlayV2 artık HomeShell'in AYRI `searchOverlay` slotunda
        // (roadmap satır 867-919): z-order sözleşmesi net — pager'ın üzerinde, All Apps'in
        // (genel `overlays` slotu) altında render edilir. Konumlandırma/davranış DEĞİŞMEDİ,
        // sadece hangi slotta render edildiği (bkz. HomeShell.kt doc-comment).
        HomeShell(
            topSearch = if (
                homePagerState?.currentPage?.let { it != 0 } == true &&
                searchBarPosition == com.armutlu.apporganizer.utils.AppPrefs.SEARCH_BAR_POS_TOP
            ) {
                { searchBarSection() }
            } else null,
            bottomSearch = if (
                homePagerState?.currentPage?.let { it != 0 } == true &&
                searchBarPosition == com.armutlu.apporganizer.utils.AppPrefs.SEARCH_BAR_POS_BOTTOM
            ) {
                { searchBarSection() }
            } else null,
            indicator = {
                // Döngü P05 — HomeShell'in indicator slotuna hoist edildi (eskiden `pager`
                // slotu içinde, FolderPager'ın hemen altında render ediliyordu).
                val state = homePagerState
                if (state != null) {
                    HomePageIndicator(pages = homePages, pagerState = state)
                }
            },
            dock = {
                // Döngü P20 — büyük tablette dock'un tüm ekran genişliğine yayılmaması için
                // içerik HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp() ile sınırlanıp
                // ortalanır (roadmap "Büyük tablet" madde 4: "Dock maksimum genişlikle
                // ortalanır"). Telefon/küçük tablette centeredContentMaxWidthDp == null →
                // Modifier.widthIn(max=...) hiçbir etki yapmaz, eski fillMaxWidth davranışı
                // birebir korunur.
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

                // Hero dock: yalnız sabit uygulamalar; klasör/dinamik öneri içermez.
                com.armutlu.apporganizer.presentation.ui.launcher.hero.HeroDock(
                    packages = dockPackages,
                    appsByPackage = remember(allApps) { allApps.associateBy { it.packageName } },
                    onLaunchApp = { pkg ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.launchApp(context, pkg)
                    },
                    onAppLongClick = { pkg ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        contextMenuPkg = pkg
                    },
                    onEditDock = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dockEditOpen = true
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
            },
            searchOverlay = {
                // Döngü P09 — genel `overlays` slotundan taşındı; z-order artık HomeShell
                // tarafından garanti edilir (searchOverlay, overlays'ten ÖNCE çizilir → All Apps
                // her zaman aramanın üzerinde kalır). Görünürlük koşulu ve içerik BİREBİR aynı.
                if (fullScreenSearchOpen && homeAppSearchEnabled && fullscreenSearchEnabled) {
                    key(fullScreenSearchOpen) {
                        FullScreenSearchOverlayV2(
                        allApps = allApps,
                        folders = if (homeSearchEnabled) folders else emptyList(),
                        folderCustomNames = customFolderNames,
                        searchResults = searchResults,
                        filesIndexState = filesIndexState,
                        suggestedContacts = suggestedContacts,
                        onClose = { fullScreenSearchOpen = false },
                        onAppClick = { pkg ->
                            fullScreenSearchOpen = false
                            viewModel.launchApp(context, pkg)
                        },
                        onFolderClick = { folder ->
                            fullScreenSearchOpen = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToFolder(folder)
                        },
                        onEnableContactsSource = viewModel::enableContactsSearchSource,
                        onEnableFilesSource = viewModel::enableFilesSearchSource,
                        onQueryChange = viewModel::setSearchQuery,
                        )
                    }
                }
            },
            overlays = {
                // Döngü P11 (roadmap madde 6) — tablette All Apps yalnız 380dp sağ side panel
                // olarak açılır; solda kalan dock/global search GÖRSEL olarak arkada kalır ama
                // Compose pointer input'u otomatik BLOKLAMAZ (z-order sadece çizim sırasıdır).
                // Bu şeffaf scrim allAppsOpen açıkken tüm ekranı kaplayıp pointer'ı yutar —
                // AllAppsDrawer kendi panel alanında normal şekilde üstte render edildiği için
                // dokunuşlar drawer'a ulaşmaya devam eder (scrim drawer'DAN ÖNCE çizilir, drawer
                // onun üzerinde). Telefonda drawer zaten tam ekran olduğundan bu scrim no-op'tur.
                if (allAppsOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) { detectTapGestures { } }
                    )
                }

                // All Apps Drawer — telefonda tam ekran overlay, tablette sağ side panel.
                // Döngü P20 — panel genişliği artık HomeAdaptiveLayoutPolicy'den: küçük tablet
                // 380dp (eski sabitle birebir aynı), büyük tablette 420dp (roadmap: dashboard
                // içeriği aşırı daralmasın diye telefon genişliğiyle sınırlı kalmaz ama ekranın
                // yarısını da kaplamaz).
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
                        recentNotificationAppsEnabled = recentNotificationAppsRowEnabled,
                        recentNotificationApps = recentNotificationApps,
                        todayInstalledAppsEnabled = recentInstallsEnabled,
                        todayInstalledApps = todayInstalledApps,
                        focusSearchOnOpen = focusSearchOnOpen,
                        onFocusSearchConsumed = viewModel::resetFocusSearchOnOpen,
                        categories = categories,
                        searchResults = searchResults,
                        recentNotificationCounts = recentNotificationCounts,
                        filesIndexState = filesIndexState,
                        onEnableFilesSource = viewModel::enableFilesSearchSource,
                        onCategoryClick = { categoryId ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.closeAllApps()
                            viewModel.openFolderByCategoryId(categoryId)
                        }
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
            },
            pager = {
            // P25: Dashboard içeriğinin eski, pager-dışı kopyası kaldırıldı. Dashboard artık
            // yalnız HomePagerHost.dashboardContent içinde; klasörler yalnız folderPageContent
            // içinde çizilir. Böylece ikinci ve sonraki sayfalar Dashboard'u tekrar etmez.
            // P25/F7: eski pager-dışı Dashboard kopyası kaldırıldı — tek doğruluk kaynağı SmartDashboardPage/HomePagerHost.
            // P18 — Odak Modu artık paralel bir ana ekran (placeholder metni) DEĞİL, bu pager
            // slotunun kendisidir: klasör sayfaları ve global arama HER ZAMAN erişilebilir kalır
            // (roadmap kabul kriteri: "yeni page mimarisini bypass eden paralel bir ana ekran
            // oluşturmaz"). Haber şeridi (ticker) odak modunda kapatılır (dikkat dağıtan içerik
            // minimum, roadmap madde 1) — kapandığında zaten aşağıdaki `else if (!tickerVisibleNow)`
            // dalı klasör/uygulama istatistik bandını (FolderStatsRow) gösterir, bu korunur.
            run {
            // Haber şeridi — "Alışveriş klasöründe 5 uygulama var" tarzı akan bilgiler.
            // Dokunma → hedef açılır; kaydırma → önceki/sonraki haber; basılı tut → sessize al (8s/1g/7g).
            // Kapalıysa eski istatistik bandı döner. Sessizdeyken hiçbir şey gösterilmez, süre dolunca geri gelir.
            // P06: tickerMutedUntilState artık yukarıda (Column'dan önce) tek yerde tanımlı —
            // dashboardContent lambda'sındaki SmartDashboardPage ile aynı state paylaşılır.
            // P18 — Odak Modu ticker'ı kapatır (dikkat dağıtan içerik minimum); kullanıcının kendi
            // tickerEnabled tercihine DOKUNMAZ (Ayarlar'daki gerçek değer korunur, sadece bu
            // pager'da geçici olarak bastırılır) — bu yüzden ayrı bir isimle tutulur.
            val tickerVisibleNow = tickerEnabled && !focusModeEnabled
            val tickerMuted = tickerMutedUntilState > System.currentTimeMillis()
            if (tickerMuted) {
                // Süre dolduğunda ana ekran açıkken bile şerit kendiliğinden geri gelsin
                LaunchedEffect(tickerMutedUntilState) {
                    kotlinx.coroutines.delay((tickerMutedUntilState - System.currentTimeMillis()).coerceAtLeast(0L))
                    tickerMutedUntilState = 0L
                }
            }
            // SmartDashboardPage aynı ticker'ı dashboard slotunda tek kez çizer.
            // P25/F7: eski pager-dışı HomeTickerRow kopyası kaldırıldı — tek doğruluk kaynağı SmartDashboardPage/HomePagerHost.
            if (!tickerVisibleNow)
            // İstatistik bandı — toplam klasör ve uygulama sayısı (sessize alınmışsa hiçbiri gösterilmez).
            // Odak Modunda ticker her zaman bastırıldığı için bu dal devreye girer (istatistik bandı korunur).
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
            val activeFeatureCount = listOf(
                favoritesEnabled,
                suggestionsEnabled,
                recentNotificationAppsRowEnabled,
                recentAppsEnabled
            ).count { it }
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
            val displayFolders = remember(baseFolders, homeSearchEnabled, folderSearchQuery) {
                if (homeSearchEnabled && folderSearchQuery.isNotEmpty()) {
                    val trLocale = java.util.Locale("tr")
                    val q = folderSearchQuery.lowercase(trLocale)
                    baseFolders.filter { folder ->
                        folder.category.categoryName.lowercase(trLocale).contains(q) ||
                            folder.apps.any { it.appName.lowercase(trLocale).contains(q) }
                    }
                } else {
                    baseFolders
                }
            }

            // Kalan gerçek yükseklik ölçülür; sığmayan klasör KIRPILMAZ, sonraki sayfaya taşar
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
            val availableHeightDp = with(density) { constraints.maxHeight.toDp().value.toInt() }
            val folderCapacity = remember(availableHeightDp, effectiveFolderSizeDp, screenColumns) {
                HomeLayoutMath.folderCapacity(availableHeightDp, effectiveFolderSizeDp, screenColumns)
            }
            // 8, yeni kurulumdaki otomatik düzen değeridir; kapasiteyi 8 ile sınırlamak
            // geniş tabletlerde gereksiz klasör sayfaları üretiyordu. Manuel 4/6/8/12
            // tercihlerinde ise kullanıcının seçimi korunur.
            val pageSize = if (pageFolderCount == 8) {
                HomeLayoutMath.adaptivePageSize(folderCapacity)
            } else {
                HomeLayoutMath.pageSize(requestedPageSize, folderCapacity)
            }
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
            // Hero kesin ürün kararı: Sayfa 0 daima Hero Dashboard, paralel eski yol yok.
            val dashboardEnabledForPager = true

            val pages = remember(displayFolders, pageSize, dashboardEnabledForPager) {
                com.armutlu.apporganizer.telemetry.TelemetryManager.trace(
                    com.armutlu.apporganizer.telemetry.PerformanceTraceName.HOME_FOLDER_PAGE_READY
                ) {
                    HomePagePlanner.buildPages(
                        folders = displayFolders,
                        pageSize = pageSize,
                        dashboardEnabled = dashboardEnabledForPager,
                    )
                }
            }
            // Döngü P04: HomeLayoutMath.pageCount() ile hizalandı — tekil kaynak.
            val pageCount = pages.size

            // Açılışta ham index değil semantik anchor'dan çözülür (roadmap Bölüm 9:
            // "Sayfa restore = Ham index değil semantic anchor"). Eski AppPrefs.getLastHomePage
            // deprecated köprü olarak HomePagePrefs.getLastHomePageAnchor() içinde tek seferlik
            // migration ile okunuyor — bu çağrı UI'yı o köprüye bağlar.
            val initialAnchor = remember {
                HomePagePrefs.getLastHomePageAnchor(context, displayFolders, pageSize)
            }
            val initialPage = remember(pages) {
                HomePageAnchorResolver.resolve(pages, initialAnchor).coerceIn(0, pageCount - 1)
            }
            val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

            LaunchedEffect(Unit) {
                com.armutlu.apporganizer.telemetry.TelemetryManager.trace(
                    com.armutlu.apporganizer.telemetry.PerformanceTraceName.HOME_SHELL_READY
                ) { Unit }
            }
            LaunchedEffect(pagerState, pages) {
                var firstImpression = true
                snapshotFlow { pagerState.settledPage }.collect { page ->
                    val pageSpec = pages.getOrNull(page)
                    val traceName = if (pageSpec is HomePageSpec.Dashboard) {
                        com.armutlu.apporganizer.telemetry.PerformanceTraceName.HOME_DASHBOARD_READY
                    } else {
                        com.armutlu.apporganizer.telemetry.PerformanceTraceName.HOME_PAGE_SWITCH
                    }
                    com.armutlu.apporganizer.telemetry.TelemetryManager.trace(traceName) { Unit }
                    val pageType = HomePageTelemetryPolicy.pageType(pageSpec)
                    val position = HomePageTelemetryPolicy.positionBucket(page, pageCount)
                    AppAnalytics.homePageViewed(
                        pageType = pageType,
                        pagePosition = position,
                        navigationSource = if (firstImpression) {
                            com.armutlu.apporganizer.telemetry.TelemetryEvent.HomeNavigationSource.RESTORE
                        } else {
                            com.armutlu.apporganizer.telemetry.TelemetryEvent.HomeNavigationSource.SWIPE
                        },
                        searchPosition = HomePageTelemetryPolicy.searchPosition(searchBarPosition),
                        startMode = HomePageTelemetryPolicy.startMode(
                            HomePagePrefs.getStartPageMode(context)
                        ),
                        deviceClass = HomePageTelemetryPolicy.deviceClass(
                            HomeAdaptiveLayoutPolicy.deviceClass(configuration.screenWidthDp)
                        ),
                    )
                    if (!firstImpression) {
                        AppAnalytics.homePageSwiped(
                            pageType = pageType,
                            pagePosition = position,
                            deviceClass = HomePageTelemetryPolicy.deviceClass(
                                HomeAdaptiveLayoutPolicy.deviceClass(configuration.screenWidthDp)
                            ),
                        )
                    }
                    firstImpression = false
                }
            }

            // Sayfa listesi değiştiğinde (klasör eklendi/silindi, dashboard aç/kapat) current
            // page güvenli sınıra çekilir — pageCount küçülürse pagerState kendiliğinden clamp
            // eder ama biz burada ayrıca en güncel stableKey'e göre anchor'ı tazeleriz.
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .collect { page ->
                        val safePage = clampPageToSafeBounds(pages, page)
                        val anchor = anchorForCurrentPage(pages, safePage)
                        HomePagePrefs.setLastHomePageAnchor(context, anchor)
                    }
            }

            // Döngü P13 — sayfa PLANI değiştiğinde (klasör reorder/silme, page size değişimi,
            // Dashboard aç/kapat) `pagerState.currentPage` Compose'un kendi pageCount clamp'i
            // ile sadece HAM index sınırına çekilir; bu, "3. sayfadaydım" davranışıdır ve
            // reorder sonrası kullanıcıyı yanlış klasöre fırlatabilir (roadmap P13 kabul
            // kriteri: "Ham page index persistence kullanılmaz"). Bu efekt bir önceki planı +
            // current page'i saklar, plan değiştiğinde `resolvePageAfterPlanChange` ile SEMANTİK
            // olarak yeniden çözer ve gerekirse (animasyonsuz, kullanıcı hareketi değil arka
            // plan senkronizasyonu olduğu için) `scrollToPage` ile oraya taşır.
            var previousPagesForReconcile by remember { mutableStateOf(pages) }
            var previousPageIndexForReconcile by remember { mutableStateOf(initialPage) }
            LaunchedEffect(pages) {
                val fromPages = previousPagesForReconcile
                val fromIndex = previousPageIndexForReconcile
                if (fromPages !== pages) {
                    val samePlan = fromPages.size == pages.size &&
                        fromPages.indices.all { fromPages[it].stableKey == pages[it].stableKey }
                    if (!samePlan) {
                        val resolvedIndex = resolvePageAfterPlanChange(
                            previousPages = fromPages,
                            previousPageIndex = fromIndex,
                            newPages = pages,
                        )
                        if (pagerState.currentPage != resolvedIndex) {
                            pagerState.scrollToPage(resolvedIndex)
                        }
                    }
                }
                previousPagesForReconcile = pages
                previousPageIndexForReconcile = pagerState.currentPage
            }

            // İndicator hoisting (HomeShell slotuna) — bkz. yukarıdaki homePagerState/homePagerPageCount.
            homePagerState = pagerState
            homePagerPageCount = pageCount
            homePages = pages
            homePageSize = pageSize

            // Search aktifken, reorder sırasında veya modal/dock edit açıkken pager scroll kilitli
            // kalır — eskiden FolderPager'ın kendi HorizontalPager'ı bu kısıtlamaları hiç
            // uygulamıyordu (roadmap P05 madde 9-11 bu döngüde eklendi).
            // Döngü P10 — karar artık HomeGestureArbiter'a delege edilir (merkezileştirme,
            // davranış DEĞİŞMEZ): searchActive/reorderActive/modalOpen hesapları aynı kalır,
            // sadece "bu üçünden biri açıkken pager kilitlenir" kuralı tek çekirdekte yaşar.
            val searchActive = fullScreenSearchOpen ||
                (homeSearchEnabled && folderSearchQuery.isNotEmpty())
            val reorderActive = dragFromIndex != null || widgetDragActive
            val modalOpen = dockEditOpen || homeLongPressOpen || folderContextMenu != null ||
                contextMenuPkg != null || categoryPickerApp != null
            val pagerScrollEnabled = HomeGestureArbiter.isHorizontalPagerScrollEnabled(
                HomeGestureContext(
                    searchActive = searchActive,
                    modalOpen = modalOpen,
                    folderReorderActive = reorderActive,
                )
            )

            Column(modifier = Modifier.fillMaxSize()) {

            HomePagerHost(
                pages = pages,
                pagerState = pagerState,
                userScrollEnabled = pagerScrollEnabled,
                dashboardContent = {
                    // P06/P24 — gerçek SmartDashboardPage; rollout ve kullanıcı tercihi açıkken
                    // HomePagerHost'un Dashboard sayfasında compose edilir.
                    // Sayfa 0 tek ürün yolu: gerçek Hero Dashboard composition.
                    SmartDashboardPage(
                        state = DashboardUiState(
                            clock = DashboardClockState(compact = compactClock),
                            intelligence = DashboardIntelligenceState(
                                missionsEnabled = missionsEnabled,
                                mission = homeMissionSummary,
                                digitalLifeCardVisible = digitalLifeCardVisible,
                                pulse = homePulseSummary,
                                todayCardEnabled = todayCardEnabled,
                                todayCardSpec = remember(todayCardEnabled, homeMissionSummary, homePulseSummary, tickerItems) {
                                    if (!todayCardEnabled) {
                                        null
                                    } else {
                                        com.armutlu.apporganizer.domain.home.TodayCardSelector.select(
                                            mission = homeMissionSummary,
                                            pulse = homePulseSummary,
                                            weeklyReportReady = tickerItems.any {
                                                it.type == com.armutlu.apporganizer.domain.home.SmartTickerType.WEEKLY_REPORT
                                            },
                                        )
                                    }
                                },
                            ),
                            smartAccess = smartAccessState,
                        ),
                        actions = DashboardActions(
                            onOpenWeeklyReport = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.WRAPPED_REPORT)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            onOpenScoreDetails = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.WRAPPED_REPORT)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            onClockLongPress = { viewModel.openManager(context) },
                            onMissionClick = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.MISSIONS)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            onPulseClick = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.WRAPPED_REPORT)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            onPulseReasonAction = { action ->
                                val target = PulseActionRouter.resolve(action)
                                if (target is PulseActionRouter.RouteTarget.Screen) {
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        putExtra(MainActivity.EXTRA_OPEN_ROUTE, target.route)
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    runCatching { context.startActivity(intent) }
                                }
                            },
                            onOpenFolderStats = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.REPORTS_CENTER)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            onOpenSearch = { fullScreenSearchOpen = true },
                            onOpenSearchSettings = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.SEARCH_SETTINGS)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            onOpenSmartAccessSettings = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.SETTINGS_USAGE_DATA)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            onLaunchApp = { pkg -> viewModel.launchApp(context, pkg) },
                            onAppLongClick = { pkg -> contextMenuPkg = pkg },
                        ),
                        modifier = Modifier.fillMaxSize(),
                        // Faz S4 — widget sürüklerken kenar auto-scroll için gerçek pagerState.
                        pagerState = pagerState,
                    )
                },
                folderPageContent = { spec ->
                    // Döngü P19 madde 5 — indicator'ın kullandığı AYNI hesaplamadan ("Klasör
                    // sayfası N/M") üretilir; iki yerde de aynı cümle okunur (bkz. HomePagerHost.kt
                    // homePagerCurrentPageDescription, HomeScreenPageIndicator.kt buildHomePageIndicatorItems).
                    val pageIndexInPages = pages.indexOf(spec)
                    val folderPageLabel = if (pageIndexInPages >= 0) {
                        val items = buildHomePageIndicatorItems(pages, pageIndexInPages)
                        val current = items.getOrNull(pageIndexInPages)
                        if (current != null && !current.isDashboard) {
                            stringResource(
                                R.string.home_page_indicator_folder_page,
                                current.folderNumber ?: (current.pageIndex + 1),
                                current.folderPageCount.coerceAtLeast(1),
                            )
                        } else null
                    } else null
                    FolderGridPage(
                        pageFolders = spec.folders,
                        globalStartIndex = spec.pageIndex * pageSize,
                        pageSize = pageSize,
                        columnsCount = screenColumns,
                        pageAccessibilityLabel = folderPageLabel,
                        dragFromIndex = dragFromIndex,
                        dragToIndex = dragToIndex,
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
                        pixelLookEnabled = pixelLookEnabled,
                        folderGlassBorderEnabled = folderGlassBorderEnabled,
                        haptic = haptic,
                        onFolderClick = { onNavigateToFolder(it) },
                        onFolderLongClick = { folderContextMenu = it },
                        onSwipeUp = { pkg -> viewModel.launchApp(context, pkg) },
                        onNotificationTap = { pkg -> viewModel.launchApp(context, pkg) },
                        // EX02 — "N okunmamış bildirim" alt bilgi satırı Bildirim Raporu ekranını açar
                        // (FolderStatsRow.onOpenDashboard ile aynı MainActivity.EXTRA_OPEN_ROUTE deseni).
                        onNotificationSummaryTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(context, MainActivity::class.java).apply {
                                putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.NOTIFICATION_REPORT)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        onDragStart = { index ->
                            dragFromIndex = index
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                            draggingFolders = folders.toMutableList()
                        },
                        onDrag = { dragAmount ->
                            val from = dragFromIndex ?: return@FolderGridPage
                            dragOffsetX += dragAmount.x
                            dragOffsetY += dragAmount.y
                            val colCount = 4
                            val screenWidthPx = with(density) { android.content.res.Resources.getSystem().displayMetrics.widthPixels.toFloat() }
                            val tileWidthPx = screenWidthPx / colCount
                            val tileHeightPx = with(density) { 100.dp.toPx() }
                            val colOffset = (dragOffsetX / tileWidthPx).toInt()
                            val rowOffset = (dragOffsetY / tileHeightPx).toInt()
                            val pageOffset = spec.pageIndex * pageSize
                            val localFrom = from - pageOffset
                            val pageFoldersCnt = spec.folders.size
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
                        editMode = false,
                    )
                },
                modifier = Modifier.fillMaxSize()
            )

            // Sayfa noktaciklari artık HomeShell'in indicator slotunda render ediliyor
            // (bkz. homePagerState/homePagerPageCount yukarıda) — burada tekrar çizilmez.

            // Swipe-up ipucu — ilk 5 acilista goster
            SwipeHint(context = context, visible = !allAppsOpen && swipeHintEnabled)
            } // end inner Column
            } // end BoxWithConstraints

            // P25/F7: eski pager-dışı HomeFavoritesSection kopyası kaldırıldı — tek doğruluk kaynağı SmartDashboardPage/HomePagerHost.

            } // end run (P18: eski "else !focusModeEnabled" bloğu artık koşulsuz)
            // Döngü P03: BOTTOM arama çubuğu, drag pill, PixelDock ve tüm overlay'ler
            // (FullScreenSearchOverlayV2/AllAppsDrawer/SnackbarHost/QuickWheelOverlay) artık
            // HomeShell'in bottomSearch/dock/overlays slotlarında — burada tekrar render edilmez.
            } // end pager slot
        )
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
        onDockAdd = { item ->
            val folderId = DockPrefs.folderId(item)
            if (folderId != null) viewModel.addFolderToDock(context, folderId)
            else viewModel.addToDock(context, item)
        },
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
            AppAnalytics.allAppsOpened()
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
        onEditHomeLayout = {
            onEditHomeLayout()
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


/**
 * Ana ekran "Gradyan" arka plan stiline karşılık gelen Brush'ı döner.
 * Ayarlar > Görünüm > Arka Plan > Gradyan bölümünden seçilir (D260).
 */
private fun homeBackgroundBrush(style: String): Brush = when (style) {
    com.armutlu.apporganizer.utils.AppPrefs.HOME_BG_GECE_MAVISI ->
        Brush.verticalGradient(listOf(Color(0xFF0A1128), Color(0xFF1B2A4A)))
    com.armutlu.apporganizer.utils.AppPrefs.HOME_BG_MINIMAL_GRI ->
        Brush.verticalGradient(listOf(Color(0xFF1C1C1C), Color(0xFF2E2E2E)))
    else -> // HOME_BG_TURKUAZ + bilinmeyen deger fallback
        Brush.verticalGradient(listOf(Color(0xFF00897B), Color(0xFF26C6DA)))
}

/**
 * Ortak kök zemin modifier'ı — HomeScreen ve FolderScreen aynı mantığı paylaşır (Görev 1).
 * Kök neden: HomeScreen kök Box'ı eskiden sabit Color.Black boyuyordu; Ayarlar > Görünüm >
 * Arka Plan'da "Duvar Kağıdı" seçili olsa bile siyah zemin sistem duvar kağıdının önüne
 * geçiyordu (windowShowWallpaper=true zaten aktif — themes.xml). "Duvar Kağıdı" seçiliyken
 * TRANSPARAN kalınır (sistem duvar kağıdı görünür); diğer stillerde (turkuaz/gece mavisi/
 * minimal gri/düz renk) OPAK boyanır — duvar kağıdı hiç sızmaz. FolderScreen'den çıkışta
 * flaş olmaması için klasör ekranı da aynı fonksiyonu kullanır.
 */
internal fun Modifier.homeRootBackground(
    bgType: String,
    bgColorInt: Int,
    bgGradientStyle: String,
): Modifier = when (bgType) {
    "wallpaper" -> this // transparan — sistem duvar kağıdı sızar
    "solid", "wallpaper_color" -> this.background(Color(bgColorInt))
    else -> this.background(homeBackgroundBrush(bgGradientStyle)) // "gradient" + bilinmeyen fallback
}

package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.home.safeRecentNotificationTotal
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.navigation.NotificationReportLaunchContract
import com.armutlu.apporganizer.presentation.navigation.Routes
import com.armutlu.apporganizer.presentation.ui.MainActivity
import com.armutlu.apporganizer.presentation.ui.launcher.AllAppsDrawer
import com.armutlu.apporganizer.presentation.ui.launcher.AppContextMenu
import com.armutlu.apporganizer.presentation.ui.launcher.AppFolder
import com.armutlu.apporganizer.presentation.ui.launcher.CategoryPickerSheet
import com.armutlu.apporganizer.presentation.ui.launcher.DashboardActions
import com.armutlu.apporganizer.presentation.ui.launcher.DashboardUiState
import com.armutlu.apporganizer.presentation.ui.launcher.DockEditSheet
import com.armutlu.apporganizer.presentation.ui.launcher.EditingCenterCard
import com.armutlu.apporganizer.presentation.ui.launcher.FolderScreen
import com.armutlu.apporganizer.presentation.ui.launcher.HomeLongPressSheet
import com.armutlu.apporganizer.presentation.ui.launcher.HomeShell
import com.armutlu.apporganizer.presentation.ui.launcher.LauncherViewModel
import com.armutlu.apporganizer.presentation.ui.launcher.SmartDashboardPage
import com.armutlu.apporganizer.presentation.ui.launcher.WidgetPage
import com.armutlu.apporganizer.presentation.ui.launcher.dashboardContentOrder
import com.armutlu.apporganizer.presentation.ui.launcher.homeRootBackground
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.HomeLayoutPrefs
import timber.log.Timber

/**
 * Home V2 — yeniden tasarlanmış ana ekran kompozisyon kökü.
 *
 * Sözleşme: bu dosya yalnız wiring yapar. Tüm türetme mantığı [HomeV2Assembler]'da
 * (saf, testli), bölümler kendi küçük composable dosyalarında yaşar. HomeShell
 * jest/IME/z-order altyapısı, AllAppsDrawer, FolderScreen ve WidgetPage korunur.
 *
 * Sayfa düzeni (tek yatay pager, iç içe yatay pager YOK):
 *   [Widget sayfası (widget varsa)] → [Klasör sayfaları...]
 */
@Composable
fun HomeV2Screen(
    viewModel: LauncherViewModel,
    onLaunchWidgetPicker: () -> Unit = {},
    onNavigateToFolder: (AppFolder) -> Unit = {},
    onEditHomeLayout: () -> Unit = {},
) {
    val context = LocalContext.current
    val vm = remember(viewModel) { viewModel }

    val initialLoadDone by vm.initialLoadDone.collectAsState()
    val folders by vm.folders.collectAsState()
    val allApps by vm.allApps.collectAsState()
    val dockPackages by vm.dockPackages.collectAsState()
    val allAppsOpen by vm.allAppsOpen.collectAsState()
    val focusSearchOnOpen by vm.focusSearchOnOpen.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val categories by vm.categories.collectAsState()
    val pendingClassificationsCount by vm.pendingClassificationsCount.collectAsState()
    val notificationPermissionMissing by vm.showNotificationBadgePermissionCard.collectAsState()
    val pulseSummary by vm.homePulseSummary.collectAsState()
    val missionSummary by vm.homeMissionSummary.collectAsState()
    val widgetIds by vm.widgetIds.collectAsState()
    val smartAccessState by vm.smartAccessState.collectAsState()
    val recentNotificationCounts by vm.recentNotificationCounts.collectAsState()
    val suggestedApps by vm.suggestedApps.collectAsState()
    val favoriteApps by vm.favoriteApps.collectAsState()

    // Bağlam menüsü + kategori seçici (uygulamaya uzun basma) — tur 6.
    var contextMenuPkg by remember { mutableStateOf<String?>(null) }
    var categoryPickerApp by remember { mutableStateOf<AppInfo?>(null) }
    val contextMenuApp = contextMenuPkg?.let { pkg -> allApps.find { it.packageName == pkg } }
    val favoritePackages = remember(favoriteApps) { favoriteApps.mapTo(mutableSetOf()) { it.packageName } }

    // Düzenleme/Öneri Merkezi (tur 10): klasör birleştirme önerileri, bekleyen
    // sınıflandırmalar, düzeltmeler, eksik izinler ve eski uygulamalar tek kartta.
    // Kart, uyarı yoksa kendini gizler (hasAnyAlert).
    val editingCenterState by vm.editingCenterState.collectAsState()
    val editingCenterEnabled = remember { AppPrefs.isEditingCenterEnabled(context) }

    // Ana ekran boş alanına uzun basma → yönetim menüsü; dock düzenleme sheet'i — tur 7.
    // MainActivity rota açıcı (öneri merkezi ve benzeri yüzeyleşmeler için).
    val openMainRoute: (String) -> Unit = { route ->
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, route)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    var homeLongPressOpen by remember { mutableStateOf(false) }
    var dockEditOpen by remember { mutableStateOf(false) }
    val dockDefaultCategory by vm.dockDefaultCategory.collectAsState()

    // Bağlamsal dock: sabitlenmiş uygulamalar + saat/kullanım bazlı öneriler.
    // Birleştirme SAF buildContextualDockPackages ile yapılır (orijinal dock motorunun
    // ilk gerçek wiring'i); akıllı slot oranı ayarlardan okunur.
    val contextualDockEnabled = remember { AppPrefs.isContextualDockEnabled(context) }
    val dockSmartSlots = remember { AppPrefs.getDockSmartSlots(context) }
    val finalDockPackages = remember(
        dockPackages,
        suggestedApps,
        contextualDockEnabled,
        dockSmartSlots,
    ) {
        com.armutlu.apporganizer.presentation.ui.launcher.buildContextualDockPackages(
            fixed = dockPackages,
            suggested = suggestedApps.map { it.packageName },
            contextualEnabled = contextualDockEnabled,
            maxSize = com.armutlu.apporganizer.presentation.ui.launcher.DOCK_MAX_SIZE,
            smartSlots = dockSmartSlots,
        )
    }

    // Hero sayfasi (sayfa 0): eski ekranin Dashboard sayfasi. Layout siralamasi
    // HomeLayoutPrefs'ten REAKTIF okunur: "Ana Ekrani Duzenle" editorunden yapilan
    // degisiklikler (siralama/gizlilik) aninda Hero'ya yansir — eski ekranla ayni
    // SharedPreferences dinleyici mekanizmasi korunur.
    var heroContentOrder by remember {
        mutableStateOf(dashboardContentOrder(HomeLayoutPrefs.read(context).config))
    }
    DisposableEffect(context) {
        val layoutPrefs = context.getSharedPreferences(
            HomeLayoutPrefs.PREFS_NAME,
            android.content.Context.MODE_PRIVATE,
        )
        val layoutListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in setOf(
                    HomeLayoutPrefs.KEY_HEADER_ORDER,
                    HomeLayoutPrefs.KEY_FOOTER_ORDER,
                    HomeLayoutPrefs.KEY_CONTENT_ORDER,
                    HomeLayoutPrefs.KEY_HIDDEN_SECTIONS,
                    HomeLayoutPrefs.KEY_LAYOUT_VERSION,
                    HomeLayoutPrefs.KEY_CUSTOMIZED,
                )
            ) {
                heroContentOrder = dashboardContentOrder(HomeLayoutPrefs.read(context).config)
            }
        }
        layoutPrefs.registerOnSharedPreferenceChangeListener(layoutListener)
        onDispose { layoutPrefs.unregisterOnSharedPreferenceChangeListener(layoutListener) }
    }
    // Arka plan / görünüm tercihleri (tur 11) — eski ekranla aynı pref anahtarları,
    // Ayarlar > Görünüm değişiklikleri SharedPreferences dinleyicisiyle anlık yansır.
    var bgType by remember { mutableStateOf(AppPrefs.getBgType(context)) }
    var bgColorInt by remember { mutableStateOf(AppPrefs.getBgColor(context)) }
    var bgGradientStyle by remember { mutableStateOf(AppPrefs.getHomeBackgroundStyle(context)) }
    var textAlpha by remember { mutableStateOf(AppPrefs.getTextAlpha(context)) }
    DisposableEffect(context) {
        val appearancePrefs = context.getSharedPreferences(
            AppPrefs.PREFS_NAME,
            android.content.Context.MODE_PRIVATE,
        )
        val appearanceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_BG_TYPE -> bgType = AppPrefs.getBgType(context)
                AppPrefs.KEY_BG_COLOR -> bgColorInt = AppPrefs.getBgColor(context)
                AppPrefs.KEY_HOME_BACKGROUND_STYLE -> bgGradientStyle = AppPrefs.getHomeBackgroundStyle(context)
                AppPrefs.KEY_TEXT_ALPHA -> textAlpha = AppPrefs.getTextAlpha(context)
            }
        }
        appearancePrefs.registerOnSharedPreferenceChangeListener(appearanceListener)
        onDispose { appearancePrefs.unregisterOnSharedPreferenceChangeListener(appearanceListener) }
    }

    val notificationCount24h = safeRecentNotificationTotal(recentNotificationCounts)
    val dashboardActions = remember(context) {
        fun openRoute(route: String) {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_ROUTE, route)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
        DashboardActions(
            onOpenWeeklyReport = { openRoute(Routes.WRAPPED_REPORT) },
            onClockLongPress = { vm.openManager(context) },
            onPulseClick = { openRoute(Routes.WRAPPED_REPORT) },
            onOpenUsageAccessSettings = { openRoute(Routes.SETTINGS_USAGE_DATA) },
            onOpenNotificationAccessSettings = {
                runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            },
            onOpenClassificationReview = { openRoute(Routes.CLASSIFICATION_REVIEW) },
            onOpenMissions = { openRoute(Routes.MISSIONS) },
            onOpenFolderReview = { openRoute(Routes.CLASSIFICATION_REVIEW) },
            onOpenNotificationHistory = { NotificationReportLaunchContract.openHistory(context) },
            onLaunchApp = { pkg -> vm.launchApp(context, pkg) },
            onAppLongClick = { /* HomeV2 v3: baglam menüsü */ },
        )
    }

    val pageSize = remember { AppPrefs.getPageSize(context) }
    val widgetAreaEnabled = remember { AppPrefs.isWidgetAreaEnabled(context) }
    val widgetFreeGridEnabled = remember { AppPrefs.isWidgetFreeGridEnabled(context) }
    var dismissedBanners by remember { mutableStateOf(setOf<String>()) }
    val appsByPackage = remember(allApps) { allApps.associateBy { it.packageName } }

    val state = remember(
        initialLoadDone, folders, dockPackages, pageSize,
        pendingClassificationsCount, notificationPermissionMissing,
        pulseSummary, missionSummary, dismissedBanners,
    ) {
        HomeV2Assembler.assemble(
            initialLoadDone = initialLoadDone,
            folders = folders,
            dockPackages = dockPackages,
            pageSize = pageSize,
            pendingClassificationsCount = pendingClassificationsCount,
            notificationPermissionMissing = notificationPermissionMissing,
            pulseSummary = pulseSummary,
            missionSummary = missionSummary,
            bannerDismissals = dismissedBanners,
        )
    }

    val showWidgetPage = widgetAreaEnabled && widgetIds.isNotEmpty()
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 72.dp.toPx() }

    HomeShell(
        // Kök yüzey: Ayarlar > Görünüm > Arka Plan seçimi. "Duvar kağıdı" seçiliyken
        // transparan kalır (windowShowWallpaper=true ile sistem duvar kağıdı sızar);
        // diğer stillerde opak boyanır. Dock dahil tüm yüzeyi kapsar.
        modifier = Modifier.homeRootBackground(bgType, bgColorInt, bgGradientStyle),
        pager = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Boş alanda yukarı kaydırma → uygulama çekmecesi. Klasör kartları kendi
                    // dikey jestlerini (hızlı başlat) tükettiği için burada çakışma yoktur.
                    .pointerInput(Unit) {
                        var totalUp = 0f
                        detectDragGestures(
                            onDragEnd = {
                                if (totalUp > swipeThresholdPx) vm.openAllApps()
                                totalUp = 0f
                            },
                            onDragCancel = { totalUp = 0f },
                        ) { _, dragAmount ->
                            if (dragAmount.y < 0) totalUp += -dragAmount.y
                        }
                    }
                    // Boş alana uzun basma → ana ekran yönetim menüsü (duvar kağıdı,
                    // ayarlar, dock düzenleme, widget/klasör ekleme, layout editörü).
                    // Hareket slop'u aşarsa drag kazanır ve uzun basma iptal olur.
                    // Boş alana çift tıklama → uygulama çekmecesi (eski ekranın çift tık
                    // jesti HomeV2'de yeniden aktif). Tıklanabilir çocuklar (kart/ikon) kendi
                    // dokunuşlarını üstlenir; çift tık kart araları/başlık/alt boşlukta çalışır.
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { homeLongPressOpen = true },
                            onDoubleTap = { vm.openAllApps() },
                        )
                    },
            ) {
                // TEK saat ilkesi: büyük saat yalnız Hero sayfasında (HeroClockCard).
                // Ana sayfada saat başlığı tekrar render edilmez; nabız/görev çipleri
                // kompakt şerit olarak kalır (ClockHeaderV2'deki saat metni olmadan).
                PulseStripV2(
                    pulse = state.pulse,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                BannerRowV2(
                    banners = state.banners,
                    onAction = { banner ->
                        when (banner.id) {
                            HomeV2Assembler.BANNER_ID_NOTIFICATION_PERMISSION -> {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        },
                                    )
                                }.onFailure { Timber.w(it, "Notification settings açılamadı") }
                            }
                            else -> vm.openAllApps()
                        }
                    },
                    onDismiss = { dismissedBanners = dismissedBanners + it },
                )
                if (editingCenterEnabled) {
                    EditingCenterCard(
                        state = editingCenterState,
                        onNavigateToClassificationReview = { openMainRoute(Routes.CLASSIFICATION_REVIEW) },
                        onNavigateToFolderMerge = { openMainRoute(Routes.FOLDER_MERGE) },
                        onNavigateToAppCorrections = { openMainRoute(Routes.APP_LIST_UNCERTAIN) },
                        onNavigateToPermissions = { openMainRoute(Routes.PERMISSIONS_GUIDE) },
                        onNavigateToStaleApps = { openMainRoute(Routes.APP_LIST) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
                when {
                    // Hero sayfasi her zaman icerik sunar; ilk yukleme tamamlanana kadar
                    // klasör grid'i yerine yükleme göstergesi gösterilir.
                    state.loading -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator() }
                    else -> BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        val columns = (maxWidth / 168.dp).toInt().coerceIn(1, 4)
                        val folderPages = remember(state.folders, state.pageSize, columns) {
                            folderChunks(state.folders, state.pageSize, columns)
                        }
                        // Sayfa düzeni: [0] Hero Dashboard → [1?] Widget → [2+?] Klasörler
                        val heroPageCount = 1
                        val widgetPageCount = if (showWidgetPage) 1 else 0
                        val pageCount = heroPageCount + widgetPageCount + folderPages.size
                        val pagerState = rememberPagerState(pageCount = { pageCount })

                        Column(modifier = Modifier.fillMaxSize()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            ) { page ->
                                when {
                                    page == 0 -> SmartDashboardPage(
                                        state = DashboardUiState(
                                            pulse = pulseSummary,
                                            smartAccess = smartAccessState,
                                            pendingClassificationCount = pendingClassificationsCount,
                                            contentOrder = heroContentOrder,
                                            missionSummary = missionSummary,
                                            notificationCount24h = notificationCount24h,
                                        ),
                                        actions = dashboardActions,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    showWidgetPage && page == 1 -> WidgetPage(
                                        widgetIds = widgetIds,
                                        widgetFreeGridEnabled = widgetFreeGridEnabled,
                                        onRemoveWidget = { vm.removeWidgetId(context, it) },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    else -> {
                                        val pageIndex = page - heroPageCount - widgetPageCount
                                        folderPages.getOrNull(pageIndex)?.let { tiles ->
                                            // Sayfanın global klasör listesindeki başlangıç ofseti
                                            val chunkOffset = folderPages.take(pageIndex).sumOf { it.size }
                                            FolderPageV2(
                                                tiles = tiles,
                                                appsByPackage = appsByPackage,
                                                textAlpha = textAlpha,
                                                onOpenFolder = { tile ->
                                                    folders.firstOrNull { it.category.categoryId == tile.categoryId }
                                                        ?.let(vm::openFolder)
                                                },
                                                onQuickLaunch = { vm.launchApp(context, it) },
                                                onAppClick = { vm.launchApp(context, it) },
                                                onAppLongClick = { contextMenuPkg = it },
                                                onReorder = { from, to ->
                                                    // Sayfa-içi indeksleri global sıraya çevir;
                                                    // kalıcılık LauncherViewModel.reorderFolders'da.
                                                    val reordered = moveItem(
                                                        folders,
                                                        chunkOffset + from,
                                                        chunkOffset + to,
                                                    )
                                                    vm.reorderFolders(context, reordered)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            if (pageCount > 1) {
                                PageDotsV2(pageCount = pageCount, currentPage = pagerState.currentPage)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
                OpenDrawerHint(onClick = vm::openAllApps)
            }
        },
        dock = {
            DockBarV2(
                dockPackages = finalDockPackages,
                appsByPackage = appsByPackage,
                onAppClick = { vm.launchApp(context, it) },
                onAppLongClick = { contextMenuPkg = it },
            )
        },
        folderOverlay = {
            FolderScreen(viewModel = viewModel, onBack = vm::closeFolder)
        },
        overlays = {
            AnimatedVisibility(
                visible = allAppsOpen,
                enter = slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { it },
                exit = slideOutVertically(tween(220, easing = FastOutLinearInEasing)) { it },
            ) {
                AllAppsDrawer(
                    apps = allApps,
                    searchQuery = searchQuery,
                    onSearchQueryChange = vm::setSearchQuery,
                    onClose = vm::closeAllApps,
                    onAppClick = { vm.launchApp(context, it) },
                    onAppLongClick = { app -> contextMenuPkg = app.packageName },
                    focusSearchOnOpen = focusSearchOnOpen,
                    onFocusSearchConsumed = vm::resetFocusSearchOnOpen,
                    categories = categories,
                )
            }
            // Bağlam menüsü + kategori seçici — dock, klasör önizlemeleri ve çekmeceden
            // uzun basma ile açılır; mevcut AppContextMenu/CategoryPickerSheet korunur.
            contextMenuApp?.let { app ->
                AppContextMenu(
                    app = app,
                    isFavorite = app.packageName in favoritePackages,
                    isDocked = app.packageName in finalDockPackages,
                    onDismiss = { contextMenuPkg = null },
                    onLaunch = { vm.launchApp(context, app.packageName) },
                    onAddToDock = { vm.addToDock(context, app.packageName) },
                    onRemoveFromDock = { vm.removeFromDock(context, app.packageName) },
                    onChangeCategory = {
                        categoryPickerApp = app
                        contextMenuPkg = null
                    },
                    onHideApp = { hidden ->
                        vm.setAppHidden(app.packageName, hidden)
                        contextMenuPkg = null
                    },
                    onSaveNote = { note -> vm.saveAppNote(app.packageName, note) },
                    onToggleFavorite = { vm.toggleFavorite(context, app.packageName) },
                    showRemoveFromNotifications = smartAccessState.notificationApps.any { it.app.packageName == app.packageName },
                    showRemoveFromRecents = smartAccessState.recentApps.any { it.packageName == app.packageName },
                    showRemoveFromNow = smartAccessState.nowApps.any { it.packageName == app.packageName },
                    onRemoveFromNotifications = { vm.hideAppFromNotifications(context, app.packageName) },
                    onRemoveFromRecents = { vm.hideAppFromRecents(context, app.packageName) },
                    onRemoveFromNow = { vm.hideAppFromNow(context, app.packageName) },
                )
            }
            categoryPickerApp?.let { app ->
                CategoryPickerSheet(
                    app = app,
                    categories = categories,
                    onDismiss = { categoryPickerApp = null },
                    onCategorySelected = { catId ->
                        vm.updateAppCategory(app.packageName, catId)
                        categoryPickerApp = null
                    },
                )
            }
            if (dockEditOpen) {
                DockEditSheet(
                    allApps = allApps,
                    dockPackages = dockPackages,
                    dockDefaultCategory = dockDefaultCategory,
                    onAdd = { vm.addToDock(context, it) },
                    onRemove = { vm.removeFromDock(context, it) },
                    onDismiss = { dockEditOpen = false },
                )
            }
            if (homeLongPressOpen) {
                HomeLongPressSheet(
                    onDismiss = { homeLongPressOpen = false },
                    onEditHomeLayout = {
                        homeLongPressOpen = false
                        onEditHomeLayout()
                    },
                    onWallpaper = {
                        homeLongPressOpen = false
                        val wallpaperIntent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(wallpaperIntent, "Duvar Kagidi Sec").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        }
                    },
                    onSettings = {
                        homeLongPressOpen = false
                        vm.openManager(context)
                    },
                    onDockEdit = {
                        homeLongPressOpen = false
                        dockEditOpen = true
                    },
                    onAddWidget = {
                        homeLongPressOpen = false
                        onLaunchWidgetPicker()
                    },
                    onAddFolder = {
                        homeLongPressOpen = false
                        vm.createCustomFolder(context)
                    },
                )
            }
        },
    )
}

@Composable
private fun PageDotsV2(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (selected) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun BannerRowV2(
    banners: List<HomeBannerState>,
    onAction: (HomeBannerState) -> Unit,
    onDismiss: (String) -> Unit,
) {
    if (banners.isEmpty()) return
    val banner = banners.first()
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = banner.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (banner.actionLabel != null) {
                Button(onClick = { onAction(banner) }) {
                    Text(banner.actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.padding(start = 4.dp))
            Text(
                text = "✕",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClickLabel = "Kapat") { onDismiss(banner.id) }
                    .padding(4.dp),
            )
        }
    }
}

@Composable
private fun OpenDrawerHint(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        color = Color.Transparent,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "⌃",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Text(
                "Tüm uygulamalar",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
        }
    }
}

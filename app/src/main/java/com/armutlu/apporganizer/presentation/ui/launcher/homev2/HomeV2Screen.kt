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
import com.armutlu.apporganizer.presentation.ui.launcher.AllAppsDrawer
import com.armutlu.apporganizer.presentation.ui.launcher.AppFolder
import com.armutlu.apporganizer.presentation.ui.launcher.FolderScreen
import com.armutlu.apporganizer.presentation.ui.launcher.HomeShell
import com.armutlu.apporganizer.presentation.ui.launcher.LauncherViewModel
import com.armutlu.apporganizer.presentation.ui.launcher.WidgetPage
import com.armutlu.apporganizer.utils.AppPrefs
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
                    },
            ) {
                ClockHeaderV2(pulse = state.pulse)
                BannerRowV2(
                    banners = state.banners,
                    onAction = { banner ->
                        when (banner.id) {
                            HomeV2Assembler.BANNER_ID_NOTIFICATION_PERMISSION -> {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                    )
                                }.onFailure { Timber.w(it, "Notification settings açılamadı") }
                            }
                            else -> vm.openAllApps()
                        }
                    },
                    onDismiss = { dismissedBanners = dismissedBanners + it },
                )
                when {
                    state.loading -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator() }
                    state.folders.isEmpty() && !showWidgetPage -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("📂", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Klasörler hazırlanıyor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        val columns = (maxWidth / 168.dp).toInt().coerceIn(1, 4)
                        val folderPages = remember(state.folders, state.pageSize, columns) {
                            folderChunks(state.folders, state.pageSize, columns)
                        }
                        val widgetPageCount = if (showWidgetPage) 1 else 0
                        val pageCount = widgetPageCount + folderPages.size
                        val pagerState = rememberPagerState(pageCount = { pageCount })

                        Column(modifier = Modifier.fillMaxSize()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            ) { page ->
                                if (showWidgetPage && page == 0) {
                                    WidgetPage(
                                        widgetIds = widgetIds,
                                        widgetFreeGridEnabled = widgetFreeGridEnabled,
                                        onRemoveWidget = { vm.removeWidgetId(context, it) },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    val pageIndex = page - widgetPageCount
                                    folderPages.getOrNull(pageIndex)?.let { tiles ->
                                        FolderPageV2(
                                            tiles = tiles,
                                            appsByPackage = appsByPackage,
                                            onOpenFolder = { tile ->
                                                folders.firstOrNull { it.category.categoryId == tile.categoryId }
                                                    ?.let(vm::openFolder)
                                            },
                                            onQuickLaunch = { vm.launchApp(context, it) },
                                            onAppClick = { vm.launchApp(context, it) },
                                        )
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
                dockPackages = state.dockPackages,
                appsByPackage = appsByPackage,
                onAppClick = { vm.launchApp(context, it) },
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
                    focusSearchOnOpen = focusSearchOnOpen,
                    onFocusSearchConsumed = vm::resetFocusSearchOnOpen,
                    categories = categories,
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
                        if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
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

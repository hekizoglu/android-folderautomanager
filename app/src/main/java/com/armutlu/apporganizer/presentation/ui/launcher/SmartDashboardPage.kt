package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R

/**
 * Döngü P06 — Akıllı Ana Ekran / Dashboard sayfası. Roadmap bölüm 1.2 "Sayfa 0" içerik
 * listesinde yer alan zeka bileşenlerini (saat, görev/dijital yaşam kartları, arama/widget
 * alanı, içgörü kartları, akıllı nabız şeridi, favoriler) TEK bileşende toplar.
 *
 * Bu composable "yeniden yazma" değil, mevcut alt bileşenlerin (PulseClockWidget,
 * HomeIntelligenceCardsRow, GoogleSearchBar, WidgetArea, AssistantInsightRow, HomeTickerRow,
 * FolderStatsRow, HomeFavoritesSection) doğrudan çağrılmasıdır — görsel/mantıksal davranışları
 * HomeScreen.kt'deki eski konumlarıyla birebir aynıdır.
 *
 * ÖNEMLİ (P06 koşullu ikili yerleşim kararı — bkz. görev raporu): Bu composable şu an yalnız
 * `HomePagerHost`'un `dashboardContent` slotundan çağrılır ve `dashboardEnabledForPager` P24'e
 * kadar `false` sabit olduğu için EKRANDA GÖRÜNMEZ. Aynı bileşenler HomeScreen.kt'de eski
 * konumlarında (pager slotunun başında, klasör grid'inin üstünde) render EDİLMEYE DEVAM EDER —
 * tek doğruluk kaynağı aynı alt composable çağrılarıdır, sadece iki farklı çağıran (caller) var.
 * P24 `dashboardEnabledForPager`'ı gerçek tercihe bağladığında HomeScreen'deki eski çağrı
 * bloğu `if (!dashboardEnabled)` koşuluyla kapatılacak (bu döngüde henüz yapılmadı — flag hâlâ
 * sabit false, davranış değişmemeli).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P06 (satır 676-756),
 * Bölüm 1.2 "Sayfa 0" içerik listesi (satır 90-105).
 */
@Composable
internal fun SmartDashboardPage(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    // P06 madde 8: Dashboard boş kalırsa yalnız saat + öneri açıklaması gösterilir.
    val hasAnyContent = state.intelligence.missionsEnabled ||
        (state.intelligence.digitalLifeCardVisible && state.intelligence.pulse != null) ||
        state.recentInstalls.let { it.enabled && it.apps.isNotEmpty() } ||
        state.secondarySections.googleSearchEnabled ||
        (state.secondarySections.widgetAreaEnabled && state.secondarySections.widgetIds.isNotEmpty()) ||
        (state.insights.assistantCardsEnabled && !state.insights.tickerEnabled && state.insights.insightCards.isNotEmpty()) ||
        (state.ticker.tickerEnabled && !state.ticker.tickerMuted) ||
        (!state.ticker.tickerEnabled && state.ticker.folders.isNotEmpty()) ||
        state.favorites.favoritesEnabled ||
        state.favorites.suggestionsEnabled ||
        state.favorites.recentNotificationAppsEnabled ||
        state.favorites.recentAppsEnabled

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        val compactClock = state.clock.compact
        PulseClockWidget(
            compact = compactClock,
            onOpenWeeklyReport = actions.onOpenWeeklyReport,
            onOpenScoreDetails = actions.onOpenScoreDetails,
            onLongPress = actions.onClockLongPress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (compactClock) 16.dp else 32.dp, bottom = 8.dp)
        )

        HomeIntelligenceCardsRow(
            missionsEnabled = state.intelligence.missionsEnabled,
            mission = state.intelligence.mission,
            digitalLifeCardVisible = state.intelligence.digitalLifeCardVisible,
            pulse = state.intelligence.pulse,
            onMissionClick = actions.onMissionClick,
            onPulseClick = actions.onPulseClick,
            onPulseReasonAction = actions.onPulseReasonAction,
        )

        if (state.recentInstalls.enabled && state.recentInstalls.apps.isNotEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clickable { actions.onOpenRecentInstalls() },
                cornerRadius = 18.dp,
                backgroundAlpha = 0.10f,
                borderAlpha = 0.18f,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.material3.Text("📥", fontSize = 15.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.recent_installs_home_chip_title),
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        val count = state.recentInstalls.apps.size
                        val subtitle = if (count == 1) {
                            stringResource(R.string.recent_installs_home_chip_subtitle_one)
                        } else {
                            stringResource(R.string.recent_installs_home_chip_subtitle_other, count)
                        }
                        androidx.compose.material3.Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.52f),
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                    androidx.compose.material3.Text("›", color = Color.White.copy(alpha = 0.45f), fontSize = 18.sp)
                }
            }
        }

        val hideSecondaryRows = state.hideSecondaryRowsForIme
        com.armutlu.apporganizer.presentation.ui.launcher.HomeSectionRenderer(
            items = listOf(
                com.armutlu.apporganizer.domain.models.HomeLayoutItem(
                    com.armutlu.apporganizer.domain.models.HomeSectionId.GOOGLE_SEARCH,
                    com.armutlu.apporganizer.domain.models.HomeSectionId.GOOGLE_SEARCH.defaultZone,
                    0,
                    !hideSecondaryRows && state.secondarySections.googleSearchEnabled,
                ),
                com.armutlu.apporganizer.domain.models.HomeLayoutItem(
                    com.armutlu.apporganizer.domain.models.HomeSectionId.ANDROID_WIDGETS,
                    com.armutlu.apporganizer.domain.models.HomeSectionId.ANDROID_WIDGETS.defaultZone,
                    1,
                    !hideSecondaryRows && state.secondarySections.widgetAreaEnabled && state.secondarySections.widgetIds.isNotEmpty(),
                ),
            ),
        ) { sectionId ->
            when (sectionId) {
                com.armutlu.apporganizer.domain.models.HomeSectionId.GOOGLE_SEARCH -> GoogleSearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
                com.armutlu.apporganizer.domain.models.HomeSectionId.ANDROID_WIDGETS -> WidgetArea(
                    widgetIds = state.secondarySections.widgetIds,
                    onRemoveWidget = actions.onRemoveWidget,
                    onReorderWidgets = actions.onReorderWidgets,
                    autoResize = state.secondarySections.widgetAutoResize,
                    screenHeightDp = state.secondarySections.screenHeightDp,
                    modifier = Modifier.fillMaxWidth()
                )
                else -> Unit
            }
        }

        if (state.insights.assistantCardsEnabled && !state.insights.tickerEnabled) {
            if (state.insights.insightCards.isNotEmpty()) {
                AssistantInsightRow(
                    cards = state.insights.insightCards,
                    onCardClick = actions.onInsightCardClick,
                    onOpenDashboard = actions.onOpenDashboardShortcut,
                )
            }
        }

        if (state.ticker.tickerEnabled && !state.ticker.tickerMuted) {
            HomeTickerRow(
                items = state.ticker.tickerItems,
                visible = state.ticker.homeTickerVisible,
                onMute = actions.onTickerMute,
                onDismissItem = actions.onTickerDismissItem,
                onHideType = actions.onTickerHideType,
                onOpenTickerSettings = actions.onOpenTickerSettings,
                onDisableTicker = actions.onDisableTicker,
                autoAdvanceEnabled = state.ticker.tickerAutoAdvance,
                autoAdvanceIntervalMs = state.ticker.tickerIntervalSeconds * 1000L,
                onItemClick = actions.onTickerItemClick,
            )
        } else if (!state.ticker.tickerEnabled) {
            FolderStatsRow(
                folders = state.ticker.folders,
                onOpenFolderStats = actions.onOpenFolderStats,
                onOpenAppStats = actions.onOpenAppStats,
                onOpenDashboard = actions.onOpenDashboard,
                onOpenUsageReport = actions.onOpenUsageReport,
            )
        }

        if (!state.hideSecondaryRowsForIme) {
            HomeFavoritesSection(
                favoritesEnabled = state.favorites.favoritesEnabled,
                favoriteApps = state.favorites.favoriteApps,
                suggestionsEnabled = state.favorites.suggestionsEnabled,
                suggestedApps = state.favorites.suggestedApps,
                suggestionsIconSizeDp = state.favorites.suggestionsIconSizeDp,
                recentNotificationAppsEnabled = state.favorites.recentNotificationAppsEnabled,
                recentNotificationApps = state.favorites.recentNotificationApps,
                recentNotificationCounts = state.favorites.recentNotificationCounts,
                recentAppsEnabled = state.favorites.recentAppsEnabled,
                recentApps = state.favorites.recentApps,
                dockPackages = state.favorites.dockPackages,
                iconPackPkg = state.favorites.iconPackPkg,
                haptic = androidx.compose.ui.platform.LocalHapticFeedback.current,
                onLaunchApp = actions.onLaunchApp,
                onAppLongClick = actions.onAppLongClick,
                screenHeightDp = state.favorites.screenHeightDp,
                showSecondaryRowsInCompactMode = false,
            )
        }

        if (!hasAnyContent) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.dashboard_empty_hint),
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

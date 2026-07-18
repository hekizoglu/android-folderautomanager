package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.PulseAction
import com.armutlu.apporganizer.domain.home.SmartTickerItem
import com.armutlu.apporganizer.domain.home.SmartTickerType
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.InsightCard
// AppFolder aynı pakette (presentation.ui.launcher) tanımlı — bkz. LauncherViewModel.kt.

/**
 * Döngü P06 — `SmartDashboardPage`'in ihtiyaç duyduğu tüm veriyi tek yerde toplayan saf model.
 * Onlarca ayrı parametre yerine anlamlı alt modeller kullanılır (roadmap P06 madde 1).
 * Compose/Android bağımlılığı yoktur (yalnızca domain tipleri) — saf veri taşıyıcıdır.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P06 (satır 676-756).
 */
data class DashboardClockState(
    val compact: Boolean,
)

data class DashboardIntelligenceState(
    val missionsEnabled: Boolean,
    val mission: HomeMissionSummary?,
    val digitalLifeCardVisible: Boolean,
    val pulse: HomePulseSummary?,
)

data class DashboardRecentInstallsState(
    val enabled: Boolean,
    val apps: List<AppInfo>,
)

data class DashboardSecondarySectionsState(
    val googleSearchEnabled: Boolean,
    val widgetAreaEnabled: Boolean,
    val widgetIds: List<Int>,
    val widgetAutoResize: Boolean,
    val screenHeightDp: Int,
)

data class DashboardInsightsState(
    val assistantCardsEnabled: Boolean,
    val tickerEnabled: Boolean,
    val insightCards: List<InsightCard>,
)

data class DashboardTickerState(
    val tickerEnabled: Boolean,
    val tickerMuted: Boolean,
    val tickerItems: List<SmartTickerItem>,
    val homeTickerVisible: Boolean,
    val tickerAutoAdvance: Boolean,
    val tickerIntervalSeconds: Int,
    val folders: List<AppFolder>,
)

data class DashboardFavoritesState(
    val favoritesEnabled: Boolean,
    val favoriteApps: List<AppInfo>,
    val suggestionsEnabled: Boolean,
    val suggestedApps: List<AppInfo>,
    val suggestionsIconSizeDp: Int,
    val recentNotificationAppsEnabled: Boolean,
    val recentNotificationApps: List<AppInfo>,
    val recentNotificationCounts: Map<String, Int>,
    val recentAppsEnabled: Boolean,
    val recentApps: List<AppInfo>,
    val dockPackages: List<String>,
    val iconPackPkg: String,
    val screenHeightDp: Int,
)

/**
 * `SmartDashboardPage`'e verilen tüm state tek çatı altında — eylemler (callback) ayrı,
 * `DashboardActions` içinde tutulur.
 */
data class DashboardUiState(
    val clock: DashboardClockState,
    val intelligence: DashboardIntelligenceState,
    val recentInstalls: DashboardRecentInstallsState,
    val secondarySections: DashboardSecondarySectionsState,
    val insights: DashboardInsightsState,
    val ticker: DashboardTickerState,
    val favorites: DashboardFavoritesState,
    val hideSecondaryRowsForIme: Boolean,
)

/** `SmartDashboardPage` içindeki tıklama/eylem callback'leri — tek yerde toplanır. */
data class DashboardActions(
    val onOpenWeeklyReport: () -> Unit,
    val onOpenScoreDetails: () -> Unit,
    val onClockLongPress: () -> Unit,
    val onMissionClick: () -> Unit,
    val onPulseClick: () -> Unit,
    val onPulseReasonAction: (PulseAction) -> Unit,
    val onOpenRecentInstalls: () -> Unit,
    val onRemoveWidget: (Int) -> Unit,
    val onReorderWidgets: (List<Int>) -> Unit,
    val onInsightCardClick: (InsightCard) -> Unit,
    val onOpenDashboardShortcut: () -> Unit,
    val onTickerMute: (Long) -> Unit,
    val onTickerDismissItem: (SmartTickerItem) -> Unit,
    val onTickerHideType: (SmartTickerType) -> Unit,
    val onOpenTickerSettings: () -> Unit,
    val onDisableTicker: () -> Unit,
    val onTickerItemClick: (SmartTickerItem) -> Unit,
    val onOpenFolderStats: () -> Unit,
    val onOpenAppStats: () -> Unit,
    val onOpenDashboard: () -> Unit,
    val onOpenUsageReport: () -> Unit,
    val onLaunchApp: (String) -> Unit,
    val onAppLongClick: (String) -> Unit,
)

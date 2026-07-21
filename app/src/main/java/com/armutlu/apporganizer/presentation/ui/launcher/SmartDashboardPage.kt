package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.armutlu.apporganizer.presentation.ui.launcher.hero.HeroDashboardPage

/** Sayfa 0 için tek kompozisyon yolu. Eski section renderer bu sayfada kullanılmaz. */
@Composable
internal fun SmartDashboardPage(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") pagerState: PagerState? = null,
) {
    HeroDashboardPage(
        pulse = state.pulse,
        smartAccess = state.smartAccess,
        onOpenWeeklyReport = actions.onOpenWeeklyReport,
        onClockLongPress = actions.onClockLongPress,
        onOpenPulse = actions.onPulseClick,
        onOpenSearch = actions.onOpenSearch,
        onOpenSearchSettings = actions.onOpenSearchSettings,
        onOpenUsageAccessSettings = actions.onOpenUsageAccessSettings,
        onOpenNotificationAccessSettings = actions.onOpenNotificationAccessSettings,
        onLaunchApp = actions.onLaunchApp,
        onAppLongClick = actions.onAppLongClick,
        modifier = modifier,
    )
}

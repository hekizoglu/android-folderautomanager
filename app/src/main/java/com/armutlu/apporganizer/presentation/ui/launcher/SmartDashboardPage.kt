package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.armutlu.apporganizer.presentation.ui.launcher.hero.HeroDashboardPage

/** Sayfa 0 için tek kompozisyon yolu. Eski section renderer bu sayfada kullanılmaz. */
@Composable
internal fun SmartDashboardPage(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier,
) {
    HeroDashboardPage(
        pulse = state.pulse,
        smartAccess = state.smartAccess,
        pendingClassificationCount = state.pendingClassificationCount,
        contentOrder = state.contentOrder,
        missionSummary = state.missionSummary,
        onOpenWeeklyReport = actions.onOpenWeeklyReport,
        onClockLongPress = actions.onClockLongPress,
        onOpenPulse = actions.onPulseClick,
        onOpenUsageAccessSettings = actions.onOpenUsageAccessSettings,
        onOpenNotificationAccessSettings = actions.onOpenNotificationAccessSettings,
        onOpenClassificationReview = actions.onOpenClassificationReview,
        onOpenMissions = actions.onOpenMissions,
        onLaunchApp = actions.onLaunchApp,
        onAppLongClick = actions.onAppLongClick,
        modifier = modifier,
    )
}

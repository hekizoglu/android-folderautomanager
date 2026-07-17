package com.armutlu.apporganizer.domain.home

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [TickerActionRouter] — her [TickerAction] doğru [TickerActionRouter.RouteTarget] üretir
 * (Döngü T01 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1597-1650).
 * [PulseActionRouterTest] ile aynı desen.
 */
class TickerActionRouterTest {

    @Test
    fun openFolder_resolvesToAppListRouteWithCategoryId() {
        val target = TickerActionRouter.resolve(TickerAction.OpenFolder("social"))
        assertEquals(
            TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_APP_LIST, categoryId = "social"),
            target,
        )
    }

    @Test
    fun openApp_resolvesToAppListRouteWithPackageName() {
        val target = TickerActionRouter.resolve(TickerAction.OpenApp("com.example.app"))
        assertEquals(
            TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_APP_LIST, packageName = "com.example.app"),
            target,
        )
    }

    @Test
    fun openAppList_resolvesToAppListRoute() {
        val target = TickerActionRouter.resolve(TickerAction.OpenAppList)
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_APP_LIST), target)
    }

    @Test
    fun openClassificationReview_resolvesToAppListUncertainRoute() {
        val target = TickerActionRouter.resolve(TickerAction.OpenClassificationReview)
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_APP_LIST_UNCERTAIN), target)
    }

    @Test
    fun openNotificationReport_resolvesToNotificationReportRoute() {
        val target = TickerActionRouter.resolve(TickerAction.OpenNotificationReport)
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_NOTIFICATION_REPORT), target)
    }

    @Test
    fun openDashboard_resolvesToDashboardRoute() {
        val target = TickerActionRouter.resolve(TickerAction.OpenDashboard)
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_DASHBOARD), target)
    }

    @Test
    fun openWeeklyReport_resolvesToWrappedReportRoute() {
        val target = TickerActionRouter.resolve(TickerAction.OpenWeeklyReport)
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_WRAPPED_REPORT), target)
    }

    @Test
    fun openSettings_defaultSection_resolvesToSettingsRoot() {
        val target = TickerActionRouter.resolve(TickerAction.OpenSettings())
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_SETTINGS), target)
    }

    @Test
    fun openSettings_launcherSection_resolvesToSettingsLauncherRoute() {
        val target = TickerActionRouter.resolve(TickerAction.OpenSettings(SettingsSection.LAUNCHER))
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_SETTINGS_LAUNCHER), target)
    }

    @Test
    fun openSearchStats_resolvesToSettingsStatsRoute() {
        val target = TickerActionRouter.resolve(TickerAction.OpenSearchStats)
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_SETTINGS_STATS), target)
    }

    @Test
    fun openReportsCenter_resolvesToReportsCenterRoute() {
        val target = TickerActionRouter.resolve(TickerAction.OpenReportsCenter)
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_REPORTS_CENTER), target)
    }

    @Test
    fun openUsageReport_resolvesToUsageReportRoute() {
        val target = TickerActionRouter.resolve(TickerAction.OpenUsageReport)
        assertEquals(TickerActionRouter.RouteTarget.Screen(TickerActionRouter.ROUTE_USAGE_REPORT), target)
    }

    @Test
    fun none_resolvesToNoTarget() {
        val target = TickerActionRouter.resolve(TickerAction.None)
        assertEquals(TickerActionRouter.RouteTarget.None, target)
    }

    @Test
    fun nullAction_doesNotCrash_resolvesToNoTarget() {
        val target = TickerActionRouter.resolve(null)
        assertEquals(TickerActionRouter.RouteTarget.None, target)
    }
}

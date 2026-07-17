package com.armutlu.apporganizer.domain.home

/**
 * [TickerAction] -> gerçek navigasyon hedefi çözümü (Döngü T01). [PulseActionRouter] ile
 * AYNI desen: route stringi TEK burada üretilir — UI/ViewModel sadece [RouteTarget]'i
 * tüketir. Route sabitleri `presentation.navigation.AppNavigation.Routes` ile birebir
 * aynı değerlerdir (bağımlılık döngüsünü önlemek için domain katmanından navigation
 * modülüne import yapılmaz, string sabitler burada tekrar tanımlanır).
 */
object TickerActionRouter {

    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_NOTIFICATION_REPORT = "notification_report"
    const val ROUTE_APP_LIST = "app_list"
    const val ROUTE_APP_LIST_UNCERTAIN = "app_list?filter=uncertain"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_SETTINGS_LAUNCHER = "settings_launcher"
    const val ROUTE_SETTINGS_NOTIFICATIONS = "settings_notifications"
    const val ROUTE_SETTINGS_APPEARANCE = "settings_appearance"
    const val ROUTE_SETTINGS_STATS = "settings_stats"
    const val ROUTE_SEARCH_SETTINGS = "search_settings"
    const val ROUTE_REPORTS_CENTER = "reports_center"
    const val ROUTE_USAGE_REPORT = "usage_report"
    const val ROUTE_WRAPPED_REPORT = "wrapped_report"

    /** Navigasyon hedefi: uygulama-içi route (opsiyonel klasör/paket bağlamıyla) ya da hedefsiz. */
    sealed interface RouteTarget {
        data class Screen(
            val route: String,
            val categoryId: String? = null,
            val packageName: String? = null,
        ) : RouteTarget
        data object None : RouteTarget
    }

    /** [TickerAction] -> [RouteTarget]. Bilinmeyen/null action güvenle [RouteTarget.None] döner. */
    fun resolve(action: TickerAction?): RouteTarget = when (action) {
        is TickerAction.OpenFolder -> RouteTarget.Screen(ROUTE_APP_LIST, categoryId = action.categoryId)
        is TickerAction.OpenApp -> RouteTarget.Screen(ROUTE_APP_LIST, packageName = action.packageName)
        TickerAction.OpenAppList -> RouteTarget.Screen(ROUTE_APP_LIST)
        TickerAction.OpenClassificationReview -> RouteTarget.Screen(ROUTE_APP_LIST_UNCERTAIN)
        TickerAction.OpenNotificationReport -> RouteTarget.Screen(ROUTE_NOTIFICATION_REPORT)
        TickerAction.OpenDashboard -> RouteTarget.Screen(ROUTE_DASHBOARD)
        TickerAction.OpenWeeklyReport -> RouteTarget.Screen(ROUTE_WRAPPED_REPORT)
        is TickerAction.OpenSettings -> RouteTarget.Screen(routeForSettingsSection(action.section))
        TickerAction.OpenSearchStats -> RouteTarget.Screen(ROUTE_SETTINGS_STATS)
        TickerAction.OpenReportsCenter -> RouteTarget.Screen(ROUTE_REPORTS_CENTER)
        TickerAction.OpenUsageReport -> RouteTarget.Screen(ROUTE_USAGE_REPORT)
        TickerAction.None, null -> RouteTarget.None
    }

    private fun routeForSettingsSection(section: SettingsSection): String = when (section) {
        SettingsSection.ROOT -> ROUTE_SETTINGS
        SettingsSection.LAUNCHER -> ROUTE_SETTINGS_LAUNCHER
        SettingsSection.NOTIFICATIONS -> ROUTE_SETTINGS_NOTIFICATIONS
        SettingsSection.APPEARANCE -> ROUTE_SETTINGS_APPEARANCE
        SettingsSection.STATS -> ROUTE_SETTINGS_STATS
        SettingsSection.SEARCH -> ROUTE_SEARCH_SETTINGS
    }
}

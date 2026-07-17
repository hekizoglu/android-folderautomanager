package com.armutlu.apporganizer.domain.home

/**
 * [PulseAction] -> gerçek navigasyon hedefi çözümü (Döngü D04 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1500-1549).
 * [com.armutlu.apporganizer.domain.usecase.missions.MissionActionRouter] ile aynı desen:
 * route stringi TEK burada üretilir — UI sadece [RouteTarget]'i tüketir.
 *
 * Route sabitleri `presentation.navigation.AppNavigation.Routes` ile birebir aynı
 * değerlerdir (bağımlılık döngüsünü önlemek için domain katmanından navigation modülüne
 * import yapılmaz, string sabitler burada tekrar tanımlanır — Routes.kt değişirse
 * [PulseActionRouterTest] kırılır ve senkron kalması garanti edilir).
 */
object PulseActionRouter {

    /** [presentation.navigation.AppNavigation.Routes.APP_LIST_UNCERTAIN] ile aynı değer. */
    const val ROUTE_APP_LIST_UNCERTAIN = "app_list?filter=uncertain"

    /** [presentation.navigation.AppNavigation.Routes.NOTIFICATION_REPORT] ile aynı değer. */
    const val ROUTE_NOTIFICATION_REPORT = "notification_report"

    /** [presentation.navigation.AppNavigation.Routes.APP_LIST] ile aynı değer. */
    const val ROUTE_APP_LIST = "app_list"

    /** [presentation.navigation.AppNavigation.Routes.WRAPPED_REPORT] ile aynı değer. */
    const val ROUTE_WEEKLY_REPORT = "wrapped_report"

    /** [presentation.navigation.AppNavigation.Routes.MISSIONS] ile aynı değer. */
    const val ROUTE_MISSIONS = "missions"

    /** Navigasyon hedefi: uygulama-içi route ya da hedefsiz. */
    sealed interface RouteTarget {
        data class Screen(val route: String) : RouteTarget
        data object None : RouteTarget
    }

    /** [PulseAction] -> [RouteTarget]. Bilinmeyen/null action güvenle [RouteTarget.None] döner. */
    fun resolve(action: PulseAction?): RouteTarget = when (action) {
        is PulseAction.OpenClassificationReview -> RouteTarget.Screen(ROUTE_APP_LIST_UNCERTAIN)
        is PulseAction.OpenNotificationReport -> RouteTarget.Screen(ROUTE_NOTIFICATION_REPORT)
        is PulseAction.OpenAppList -> RouteTarget.Screen(ROUTE_APP_LIST)
        is PulseAction.OpenWeeklyReport -> RouteTarget.Screen(ROUTE_WEEKLY_REPORT)
        is PulseAction.OpenMissions -> RouteTarget.Screen(ROUTE_MISSIONS)
        PulseAction.None, null -> RouteTarget.None
    }
}

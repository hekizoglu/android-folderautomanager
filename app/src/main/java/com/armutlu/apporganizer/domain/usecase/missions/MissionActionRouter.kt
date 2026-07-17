package com.armutlu.apporganizer.domain.usecase.missions

import android.provider.Settings

/**
 * [MissionAction] -> gercek navigasyon hedefi cozumu (Dongu M05). Route stringleri veya
 * sistem Intent'leri TEK burada uretilir — UI (MissionsScreen) sadece [RouteTarget]'i
 * tuketir, kendi route bilgisi tasimaz.
 *
 * Route sabitleri `presentation.navigation.AppNavigation.Routes` ile birebir ayni
 * degerlerdir (bagimlilik donguyu onlemek icin domain katmanindan navigation modulune
 * import yapilmaz, string sabitler burada tekrar tanimlanir — Routes.kt degisirse
 * [MissionActionRouterTest] kirilir ve senkron kalmasi garanti edilir).
 */
object MissionActionRouter {

    /** [presentation.navigation.AppNavigation.Routes.APP_LIST_UNCERTAIN] ile ayni deger. */
    const val ROUTE_APP_LIST_UNCERTAIN = "app_list?filter=uncertain"

    /** [presentation.navigation.AppNavigation.Routes.NOTIFICATION_REPORT] ile ayni deger. */
    const val ROUTE_NOTIFICATION_REPORT = "notification_report"

    /** [presentation.navigation.AppNavigation.Routes.USAGE_REPORT] ile ayni deger. */
    const val ROUTE_USAGE_REPORT = "usage_report"

    /**
     * Navigasyon hedefi: ya uygulama-ici route, ya sistem Intent action'i, ya da hedefsiz.
     * Intent NESNESI domain katmaninda kurulmaz (JVM unit testte Android stub'lari calismaz);
     * [SystemIntent.intentAction] string'ini UI katmani Intent'e cevirir.
     */
    sealed interface RouteTarget {
        data class Screen(val route: String) : RouteTarget
        data class SystemIntent(val intentAction: String) : RouteTarget
        data object None : RouteTarget
    }

    /** [MissionAction] -> [RouteTarget]. Bilinmeyen/null action guvenle [RouteTarget.None] doner. */
    fun resolve(action: MissionAction?): RouteTarget = when (action) {
        is MissionAction.OpenClassificationReview -> RouteTarget.Screen(ROUTE_APP_LIST_UNCERTAIN)
        is MissionAction.OpenNotificationReport -> RouteTarget.Screen(ROUTE_NOTIFICATION_REPORT)
        is MissionAction.OpenUsageReport -> RouteTarget.Screen(ROUTE_USAGE_REPORT)
        is MissionAction.OpenSettingsUsageAccess -> RouteTarget.SystemIntent(
            Settings.ACTION_USAGE_ACCESS_SETTINGS
        )
        MissionAction.None, null -> RouteTarget.None
    }
}

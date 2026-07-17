package com.armutlu.apporganizer.domain.usecase.missions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MissionActionRouter — her [MissionAction] dogru [MissionActionRouter.RouteTarget] uretir
 * (Dongu M05). Android sinif ciftleri (Intent/Settings) `isReturnDefaultValues = true`
 * (app/build.gradle.kts) sayesinde plain JVM testinde guvenle kullanilabiliyor.
 */
class MissionActionRouterTest {

    @Test
    fun openClassificationReview_resolvesToAppListUncertainRoute() {
        val target = MissionActionRouter.resolve(MissionAction.OpenClassificationReview)
        assertEquals(
            MissionActionRouter.RouteTarget.Screen(MissionActionRouter.ROUTE_APP_LIST_UNCERTAIN),
            target,
        )
    }

    @Test
    fun openNotificationReport_resolvesToNotificationReportRoute() {
        val target = MissionActionRouter.resolve(MissionAction.OpenNotificationReport)
        assertEquals(
            MissionActionRouter.RouteTarget.Screen(MissionActionRouter.ROUTE_NOTIFICATION_REPORT),
            target,
        )
    }

    @Test
    fun openUsageReport_resolvesToUsageReportRoute() {
        val target = MissionActionRouter.resolve(MissionAction.OpenUsageReport)
        assertEquals(
            MissionActionRouter.RouteTarget.Screen(MissionActionRouter.ROUTE_USAGE_REPORT),
            target,
        )
    }

    @Test
    fun openSettingsUsageAccess_resolvesToSystemIntent() {
        val target = MissionActionRouter.resolve(MissionAction.OpenSettingsUsageAccess)
        assertTrue(target is MissionActionRouter.RouteTarget.SystemIntent)
        assertEquals(
            "android.settings.USAGE_ACCESS_SETTINGS",
            (target as MissionActionRouter.RouteTarget.SystemIntent).intentAction,
        )
    }

    @Test
    fun none_resolvesToNoTarget() {
        val target = MissionActionRouter.resolve(MissionAction.None)
        assertEquals(MissionActionRouter.RouteTarget.None, target)
    }

    @Test
    fun nullAction_doesNotCrash_resolvesToNoTarget() {
        val target = MissionActionRouter.resolve(null)
        assertEquals(MissionActionRouter.RouteTarget.None, target)
    }
}

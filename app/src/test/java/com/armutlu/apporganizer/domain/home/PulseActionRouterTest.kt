package com.armutlu.apporganizer.domain.home

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [PulseActionRouter] — her [PulseAction] doğru [PulseActionRouter.RouteTarget] üretir
 * (Döngü D04 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1500-1549).
 * [com.armutlu.apporganizer.domain.usecase.missions.MissionActionRouterTest] ile aynı desen.
 */
class PulseActionRouterTest {

    @Test
    fun openClassificationReview_resolvesToAppListUncertainRoute() {
        val target = PulseActionRouter.resolve(PulseAction.OpenClassificationReview)
        assertEquals(
            PulseActionRouter.RouteTarget.Screen(PulseActionRouter.ROUTE_APP_LIST_UNCERTAIN),
            target,
        )
    }

    @Test
    fun openNotificationReport_resolvesToNotificationReportRoute() {
        val target = PulseActionRouter.resolve(PulseAction.OpenNotificationReport)
        assertEquals(
            PulseActionRouter.RouteTarget.Screen(PulseActionRouter.ROUTE_NOTIFICATION_REPORT),
            target,
        )
    }

    @Test
    fun openAppList_resolvesToAppListRoute() {
        val target = PulseActionRouter.resolve(PulseAction.OpenAppList)
        assertEquals(
            PulseActionRouter.RouteTarget.Screen(PulseActionRouter.ROUTE_APP_LIST),
            target,
        )
    }

    @Test
    fun openWeeklyReport_resolvesToWrappedReportRoute() {
        val target = PulseActionRouter.resolve(PulseAction.OpenWeeklyReport)
        assertEquals(
            PulseActionRouter.RouteTarget.Screen(PulseActionRouter.ROUTE_WEEKLY_REPORT),
            target,
        )
    }

    @Test
    fun openMissions_resolvesToMissionsRoute() {
        val target = PulseActionRouter.resolve(PulseAction.OpenMissions)
        assertEquals(
            PulseActionRouter.RouteTarget.Screen(PulseActionRouter.ROUTE_MISSIONS),
            target,
        )
    }

    @Test
    fun none_resolvesToNoTarget() {
        val target = PulseActionRouter.resolve(PulseAction.None)
        assertEquals(PulseActionRouter.RouteTarget.None, target)
    }

    @Test
    fun nullAction_doesNotCrash_resolvesToNoTarget() {
        val target = PulseActionRouter.resolve(null)
        assertEquals(PulseActionRouter.RouteTarget.None, target)
    }
}

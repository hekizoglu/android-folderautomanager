package com.armutlu.apporganizer.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Dongu M01 — instanceId determinizmi. Ayni (missionId, periodType, periodStartEpoch)
 * her zaman ayni id'yi uretmeli — bu, insertAllIgnore ile "ayni donem+gorev ikinci kez
 * eklenmez" garantisinin temelidir (unique index + deterministik PK).
 */
class MissionInstanceEntityTest {

    @Test
    fun `same mission and period produce identical instanceId`() {
        val first = MissionInstanceEntity.buildInstanceId("daily_screen_under_3h", MissionInstanceEntity.PERIOD_DAILY, 20_650L)
        val second = MissionInstanceEntity.buildInstanceId("daily_screen_under_3h", MissionInstanceEntity.PERIOD_DAILY, 20_650L)
        assertEquals(first, second)
    }

    @Test
    fun `different period produces different instanceId`() {
        val day1 = MissionInstanceEntity.buildInstanceId("daily_screen_under_3h", MissionInstanceEntity.PERIOD_DAILY, 20_650L)
        val day2 = MissionInstanceEntity.buildInstanceId("daily_screen_under_3h", MissionInstanceEntity.PERIOD_DAILY, 20_651L)
        assertNotEquals(day1, day2)
    }

    @Test
    fun `different mission produces different instanceId`() {
        val a = MissionInstanceEntity.buildInstanceId("daily_screen_under_3h", MissionInstanceEntity.PERIOD_DAILY, 20_650L)
        val b = MissionInstanceEntity.buildInstanceId("daily_no_late_night", MissionInstanceEntity.PERIOD_DAILY, 20_650L)
        assertNotEquals(a, b)
    }

    @Test
    fun `daily and weekly period types with same epoch produce different instanceId`() {
        val daily = MissionInstanceEntity.buildInstanceId("weekly_positive_actions", MissionInstanceEntity.PERIOD_DAILY, 2_950L)
        val weekly = MissionInstanceEntity.buildInstanceId("weekly_positive_actions", MissionInstanceEntity.PERIOD_WEEKLY, 2_950L)
        assertNotEquals(daily, weekly)
    }
}

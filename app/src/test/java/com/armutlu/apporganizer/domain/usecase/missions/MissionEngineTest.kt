package com.armutlu.apporganizer.domain.usecase.missions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MissionEngine — deterministik uretim + checkProgress senaryolari (D257). */
class MissionEngineTest {

    @Test
    fun `same epochDay generates identical daily missions`() {
        val day = 20_650L
        val selection = MissionEngine.MissionSelectionInput(
            checkInput = MissionEngine.MissionCheckInput(
                screenTimeMinutesToday = 120L,
                usedAfter23Today = false,
                unlockCountToday = 10,
            )
        )
        val first = MissionEngine.generateDaily(day, selection)
        val second = MissionEngine.generateDaily(day, selection)

        assertEquals(MissionEngine.DAILY_MISSION_COUNT, first.size)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertTrue(first.all { it.type == MissionEngine.MissionType.DAILY })
        assertTrue(first.all { it.starReward == MissionEngine.DAILY_STAR })
        // Ayni id iki kez secilmez
        assertEquals(first.size, first.map { it.id }.distinct().size)
    }

    @Test
    fun `weekly missions carry double star reward`() {
        val weekly = MissionEngine.generateWeekly(
            epochWeek = 2_950L,
            selection = MissionEngine.MissionSelectionInput(
                checkInput = MissionEngine.MissionCheckInput(
                    weeklyScreenTimeMinutes = 500L,
                    previousWeeklyScreenTimeMinutes = 700L,
                )
            )
        )

        assertEquals(2, weekly.size)
        assertTrue(weekly.all { it.type == MissionEngine.MissionType.WEEKLY })
        assertTrue(weekly.all { it.starReward == MissionEngine.WEEKLY_STAR })
    }

    @Test
    fun `daily selection skips missions whose required signal is unavailable`() {
        val missions = MissionEngine.generateDaily(
            epochDay = 20_651L,
            selection = MissionEngine.MissionSelectionInput(
                checkInput = MissionEngine.MissionCheckInput(
                    screenTimeMinutesToday = null,
                    usedAfter23Today = null,
                    unlockCountToday = null,
                )
            )
        )

        assertFalse(missions.any { it.id == MissionEngine.DAILY_SCREEN_UNDER_3H })
        assertFalse(missions.any { it.id == MissionEngine.DAILY_NO_LATE_NIGHT })
        assertFalse(missions.any { it.id == MissionEngine.DAILY_UNLOCK_UNDER_30 })
        assertTrue(missions.any { it.id == MissionEngine.DAILY_CLASSIFICATION_CLEANUP })
        assertTrue(missions.any { it.id == MissionEngine.DAILY_VIEW_NOTIF_REPORT })
    }

    @Test
    fun `daily selection honors cooldown when enough alternatives exist`() {
        val missions = MissionEngine.generateDaily(
            epochDay = 20_652L,
            selection = MissionEngine.MissionSelectionInput(
                checkInput = MissionEngine.MissionCheckInput(
                    screenTimeMinutesToday = 90L,
                    usedAfter23Today = false,
                    unlockCountToday = 8,
                ),
                recentlyCompletedMissionIds = setOf(MissionEngine.DAILY_SCREEN_UNDER_3H)
            )
        )

        assertEquals(MissionEngine.DAILY_MISSION_COUNT, missions.size)
        assertFalse(missions.any { it.id == MissionEngine.DAILY_SCREEN_UNDER_3H })
    }

    @Test
    fun `screen time under 3 hours completes daily mission and over does not`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_SCREEN_UNDER_3H, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        assertTrue(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = 120L)))
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = 200L)))
        // Veri yoksa (izin verilmemis) uydurma basari yok
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(screenTimeMinutesToday = null)))
    }

    @Test
    fun `weekly screen less requires valid previous baseline`() {
        val mission = MissionEngine.Mission(
            MissionEngine.WEEKLY_SCREEN_LESS, MissionEngine.MissionType.WEEKLY,
            MissionEngine.WEEKLY_STAR, autoCheckable = true,
        )
        assertTrue(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(weeklyScreenTimeMinutes = 500L, previousWeeklyScreenTimeMinutes = 700L),
            )
        )
        assertFalse(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(weeklyScreenTimeMinutes = 800L, previousWeeklyScreenTimeMinutes = 700L),
            )
        )
        // Baseline 0 (ilk hafta) — sahte basari verilmez
        assertFalse(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(weeklyScreenTimeMinutes = 100L, previousWeeklyScreenTimeMinutes = 0L),
            )
        )
    }

    @Test
    fun `classification cleanup mission needs a real classification action`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_CLASSIFICATION_CLEANUP, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput()))
        assertTrue(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(
                    taskEvents = MissionEngine.TaskEventInput(classificationActionsToday = 1)
                ),
            )
        )
    }

    @Test
    fun `no late night mission true only when hourly data confirms no usage`() {
        val mission = MissionEngine.Mission(
            MissionEngine.DAILY_NO_LATE_NIGHT, MissionEngine.MissionType.DAILY,
            MissionEngine.DAILY_STAR, autoCheckable = true,
        )
        assertTrue(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(usedAfter23Today = false)))
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(usedAfter23Today = true)))
        assertFalse(MissionEngine.checkProgress(mission, MissionEngine.MissionCheckInput(usedAfter23Today = null)))
    }

    @Test
    fun `weekly positive actions needs three real positive events`() {
        val mission = MissionEngine.Mission(
            MissionEngine.WEEKLY_POSITIVE_ACTIONS, MissionEngine.MissionType.WEEKLY,
            MissionEngine.WEEKLY_STAR, autoCheckable = true,
        )
        assertFalse(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(taskEvents = MissionEngine.TaskEventInput(positiveEventsThisWeek = 2)),
            )
        )
        assertTrue(
            MissionEngine.checkProgress(
                mission,
                MissionEngine.MissionCheckInput(taskEvents = MissionEngine.TaskEventInput(positiveEventsThisWeek = 3)),
            )
        )
    }
}

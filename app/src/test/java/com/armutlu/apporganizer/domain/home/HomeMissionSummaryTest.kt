package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.usecase.missions.MissionAction
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.domain.usecase.missions.MissionSummaryUseCase.MissionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [HomeMissionSummarySelector] testleri (Dongu M07). Saf Kotlin — Android bagimliligi yok.
 */
class HomeMissionSummaryTest {

    private fun outcome(
        id: String,
        status: MissionStatus,
        deadlineText: String? = null,
        progressFraction: Float? = null,
        currentText: String? = null,
        remainingText: String? = null,
        title: String = id,
    ) = MissionOutcome(
        id = id,
        title = title,
        starReward = 1,
        status = status,
        autoCheckable = true,
        currentText = currentText,
        remainingText = remainingText,
        progressText = null,
        progressFraction = progressFraction,
        deadlineText = deadlineText,
        action = MissionAction.None,
        actionLabel = null,
    )

    // --- Oncelik 1: AT_RISK her zaman kazanir ---

    @Test
    fun `AT_RISK mission wins even when a soon-deadline mission also exists`() {
        val atRisk = outcome("at-risk", MissionStatus.AT_RISK, deadlineText = "3 sa. kaldı")
        val soonDeadline = outcome("soon", MissionStatus.IN_PROGRESS, deadlineText = "1 sa. kaldı")
        val summary = HomeMissionSummarySelector.build(listOf(soonDeadline, atRisk))

        assertEquals("at-risk", summary.primaryMissionId)
        assertTrue(summary.urgent)
    }

    // --- Oncelik 2: soonest deadline (deadlineText'i olan ilk gorev) AT_RISK yokken kazanir ---

    @Test
    fun `soonest deadline mission wins when no AT_RISK mission exists`() {
        val withDeadline = outcome("with-deadline", MissionStatus.IN_PROGRESS, deadlineText = "2 sa. kaldı")
        val noDeadline = outcome("no-deadline", MissionStatus.IN_PROGRESS, deadlineText = null, progressFraction = 0.5f)
        val summary = HomeMissionSummarySelector.build(listOf(noDeadline, withDeadline))

        assertEquals("with-deadline", summary.primaryMissionId)
        assertFalse(summary.urgent)
    }

    // --- Oncelik 3: tek eylemle tamamlanabilir (progressFraction >= 0.99) deadline yokken kazanir ---

    @Test
    fun `single-action-away mission wins when no AT_RISK and no deadline missions exist`() {
        val singleAction = outcome("single-action", MissionStatus.IN_PROGRESS, progressFraction = 0.99f)
        val lowProgress = outcome("low-progress", MissionStatus.IN_PROGRESS, progressFraction = 0.2f)
        val summary = HomeMissionSummarySelector.build(listOf(lowProgress, singleAction))

        assertEquals("single-action", summary.primaryMissionId)
    }

    // --- Oncelik 4: en yuksek progressFraction, digerleri yokken kazanir ---

    @Test
    fun `highest progress fraction mission wins when no higher-priority rule matches`() {
        val higher = outcome("higher", MissionStatus.IN_PROGRESS, progressFraction = 0.7f)
        val lower = outcome("lower", MissionStatus.IN_PROGRESS, progressFraction = 0.3f)
        val summary = HomeMissionSummarySelector.build(listOf(lower, higher))

        assertEquals("higher", summary.primaryMissionId)
    }

    // --- Oncelik 5: hicbir sinyal yoksa ilk bekleyen gorev secilir ---

    @Test
    fun `first pending mission wins when no other signal is present`() {
        val first = outcome("first", MissionStatus.NOT_STARTED)
        val second = outcome("second", MissionStatus.NOT_STARTED)
        val summary = HomeMissionSummarySelector.build(listOf(first, second))

        assertEquals("first", summary.primaryMissionId)
    }

    // --- completed/total sayaci ---

    @Test
    fun `summary reflects 0 of 3 completed`() {
        val missions = listOf(
            outcome("a", MissionStatus.NOT_STARTED),
            outcome("b", MissionStatus.IN_PROGRESS),
            outcome("c", MissionStatus.IN_PROGRESS),
        )
        val summary = HomeMissionSummarySelector.build(missions)

        assertEquals(0, summary.completedCount)
        assertEquals(3, summary.totalCount)
    }

    @Test
    fun `summary reflects 1 of 3 completed`() {
        val missions = listOf(
            outcome("a", MissionStatus.COMPLETED),
            outcome("b", MissionStatus.IN_PROGRESS),
            outcome("c", MissionStatus.IN_PROGRESS),
        )
        val summary = HomeMissionSummarySelector.build(missions)

        assertEquals(1, summary.completedCount)
        assertEquals(3, summary.totalCount)
    }

    @Test
    fun `summary reflects 3 of 3 completed`() {
        val missions = listOf(
            outcome("a", MissionStatus.COMPLETED),
            outcome("b", MissionStatus.COMPLETED),
            outcome("c", MissionStatus.COMPLETED),
        )
        val summary = HomeMissionSummarySelector.build(missions)

        assertEquals(3, summary.completedCount)
        assertEquals(3, summary.totalCount)
        // Hepsi tamamlandiginda bile secim robust kalmali (caller "tamamlandi" metnini
        // completedCount==totalCount kontroluyle kendisi gosterir).
        assertEquals("a", summary.primaryMissionId)
    }

    // --- bos liste / veri yok durumu ---

    @Test
    fun `empty mission list yields null primary and zero counts`() {
        val summary = HomeMissionSummarySelector.build(emptyList())

        assertEquals(0, summary.completedCount)
        assertEquals(0, summary.totalCount)
        assertNull(summary.primaryMissionId)
        assertNull(summary.primaryTitle)
        assertNull(summary.primaryCurrentText)
        assertNull(summary.primaryRemainingText)
        assertNull(summary.primaryStatus)
        assertFalse(summary.urgent)
    }

    @Test
    fun `DATA_UNAVAILABLE primary mission is not marked urgent`() {
        val missions = listOf(outcome("data-unavailable", MissionStatus.DATA_UNAVAILABLE))
        val summary = HomeMissionSummarySelector.build(missions)

        assertEquals(MissionStatus.DATA_UNAVAILABLE, summary.primaryStatus)
        assertFalse(summary.urgent)
    }
}

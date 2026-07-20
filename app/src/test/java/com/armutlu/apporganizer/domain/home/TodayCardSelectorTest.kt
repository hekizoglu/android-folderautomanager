package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.PulseReasonId
import com.armutlu.apporganizer.domain.usecase.pulse.PulseScoreReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [TodayCardSelector] testleri (Görev S1 + Görev 3). Saf Kotlin — Android bağımlılığı yok.
 * Öncelik sırası: CRITICAL_PERMISSION > RISKY_MISSION > FOLDER_REVIEW > REPORT_READY >
 * DAILY_MISSIONS > BALANCE_SUMMARY > null (hiçbir girdi/öncelik yok).
 */
class TodayCardSelectorTest {

    private fun mission(
        urgent: Boolean,
        status: MissionStatus? = if (urgent) MissionStatus.AT_RISK else MissionStatus.IN_PROGRESS,
        totalCount: Int = 3,
        completedCount: Int = 1,
        totalStars: Int = 0,
    ) = HomeMissionSummary(
        completedCount = completedCount,
        totalCount = totalCount,
        primaryMissionId = "m1",
        primaryTitle = "Görev",
        primaryCurrentText = "1/3",
        primaryRemainingText = "2 kaldı",
        primaryStatus = status,
        urgent = urgent,
        totalStars = totalStars,
    )

    private fun pulse(
        freshness: DataFreshness = DataFreshness.LIVE,
        topReasonId: PulseReasonId? = null,
        score: Int = 70,
        confidence: DataConfidence = DataConfidence.HIGH,
    ) = HomePulseSummary(
        score = score,
        statusBand = PulseStatusBand.forScore(score),
        delta = 2,
        topReasonId = topReasonId,
        topReason = topReasonId?.let { PulseScoreReason(id = it, value = 5, delta = 3) },
        confidence = confidence,
        freshness = freshness,
    )

    // ── Öncelik 1: CRITICAL_PERMISSION ─────────────────────────────────────

    @Test
    fun `pulse unavailable wins over everything else`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = true),
            pulse = pulse(freshness = DataFreshness.UNAVAILABLE),
            weeklyReportReady = true,
        )
        assertEquals(TodayCardKind.CRITICAL_PERMISSION, spec?.kind)
    }

    // ── Öncelik 2: RISKY_MISSION ────────────────────────────────────────────

    @Test
    fun `urgent mission wins when pulse is available`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = true),
            pulse = pulse(topReasonId = PulseReasonId.ORGANIZATION_UNCATEGORIZED),
            weeklyReportReady = true,
        )
        assertEquals(TodayCardKind.RISKY_MISSION, spec?.kind)
        assertEquals("Görev", spec?.missionTitle)
    }

    // ── Öncelik 3: FOLDER_REVIEW ────────────────────────────────────────────

    @Test
    fun `uncategorized apps reason wins when no urgent mission`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = false),
            pulse = pulse(topReasonId = PulseReasonId.ORGANIZATION_UNCATEGORIZED),
            weeklyReportReady = true,
        )
        assertEquals(TodayCardKind.FOLDER_REVIEW, spec?.kind)
    }

    // ── Öncelik 4: REPORT_READY ─────────────────────────────────────────────

    @Test
    fun `weekly report ready wins when no permission-mission-folder signal`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = false),
            pulse = pulse(topReasonId = PulseReasonId.ATTENTION_CALM),
            weeklyReportReady = true,
        )
        assertEquals(TodayCardKind.REPORT_READY, spec?.kind)
    }

    // ── Öncelik 5: DAILY_MISSIONS (Görev 3) ──────────────────────────────────

    @Test
    fun `daily missions shown on a normal day when balance would otherwise win`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = false, totalCount = 3, completedCount = 1, totalStars = 12),
            pulse = pulse(topReasonId = PulseReasonId.ATTENTION_CALM),
            weeklyReportReady = false,
        )
        assertEquals(TodayCardKind.DAILY_MISSIONS, spec?.kind)
        assertEquals(1, spec?.missionCompletedCount)
        assertEquals(3, spec?.missionTotalCount)
        assertEquals(12, spec?.missionTotalStars)
    }

    @Test
    fun `daily missions stays behind urgent mission priority`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = true, totalCount = 3, completedCount = 1),
            pulse = pulse(topReasonId = PulseReasonId.ATTENTION_CALM),
            weeklyReportReady = false,
        )
        assertEquals(TodayCardKind.RISKY_MISSION, spec?.kind)
    }

    @Test
    fun `daily missions stays behind report ready priority`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = false, totalCount = 3, completedCount = 1),
            pulse = pulse(topReasonId = PulseReasonId.ATTENTION_CALM),
            weeklyReportReady = true,
        )
        assertEquals(TodayCardKind.REPORT_READY, spec?.kind)
    }

    @Test
    fun `daily missions not shown when mission list is empty`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = false, totalCount = 0, completedCount = 0),
            pulse = pulse(topReasonId = PulseReasonId.ATTENTION_CALM),
            weeklyReportReady = false,
        )
        assertEquals(TodayCardKind.BALANCE_SUMMARY, spec?.kind)
    }

    // ── Öncelik 6: BALANCE_SUMMARY ──────────────────────────────────────────

    @Test
    fun `balance summary shown when nothing else applies`() {
        val spec = TodayCardSelector.select(
            mission = null,
            pulse = pulse(topReasonId = PulseReasonId.ATTENTION_CALM),
            weeklyReportReady = false,
        )
        assertEquals(TodayCardKind.BALANCE_SUMMARY, spec?.kind)
        assertEquals(70, spec?.pulseScore)
    }

    @Test
    fun `balance summary hidden when pulse confidence is low`() {
        val spec = TodayCardSelector.select(
            mission = null,
            pulse = pulse(confidence = DataConfidence.LOW),
            weeklyReportReady = false,
        )
        assertNull(spec)
    }

    // ── Hiçbir girdi yok ─────────────────────────────────────────────────────

    @Test
    fun `no inputs returns null`() {
        val spec = TodayCardSelector.select(mission = null, pulse = null, weeklyReportReady = false)
        assertNull(spec)
    }

    @Test
    fun `mission with empty list and no pulse returns null`() {
        val spec = TodayCardSelector.select(mission = mission(urgent = false, totalCount = 0, completedCount = 0), pulse = null, weeklyReportReady = false)
        assertNull(spec)
    }
}

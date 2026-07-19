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
 * [TodayCardSelector] testleri (Görev S1). Saf Kotlin — Android bağımlılığı yok.
 * Öncelik sırası: CRITICAL_PERMISSION > RISKY_MISSION > FOLDER_REVIEW > REPORT_READY >
 * BALANCE_SUMMARY > null (hiçbir girdi/öncelik yok).
 */
class TodayCardSelectorTest {

    private fun mission(
        urgent: Boolean,
        status: MissionStatus? = if (urgent) MissionStatus.AT_RISK else MissionStatus.IN_PROGRESS,
    ) = HomeMissionSummary(
        completedCount = 1,
        totalCount = 3,
        primaryMissionId = "m1",
        primaryTitle = "Görev",
        primaryCurrentText = "1/3",
        primaryRemainingText = "2 kaldı",
        primaryStatus = status,
        urgent = urgent,
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

    // ── Öncelik 5: BALANCE_SUMMARY ──────────────────────────────────────────

    @Test
    fun `balance summary shown when nothing else applies`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = false),
            pulse = pulse(topReasonId = PulseReasonId.ATTENTION_CALM),
            weeklyReportReady = false,
        )
        assertEquals(TodayCardKind.BALANCE_SUMMARY, spec?.kind)
        assertEquals(70, spec?.pulseScore)
    }

    @Test
    fun `balance summary hidden when pulse confidence is low`() {
        val spec = TodayCardSelector.select(
            mission = mission(urgent = false),
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
    fun `mission not urgent and no pulse returns null`() {
        val spec = TodayCardSelector.select(mission = mission(urgent = false), pulse = null, weeklyReportReady = false)
        assertNull(spec)
    }
}

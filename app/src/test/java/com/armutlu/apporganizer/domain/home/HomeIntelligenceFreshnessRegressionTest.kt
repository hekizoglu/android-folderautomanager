package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.HomeErrorCodes
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class HomeIntelligenceFreshnessRegressionTest {

    private val now = 1_800_000_000_000L

    @Test
    fun `eight hour digital life snapshot remains recent`() {
        val report = HomeIntelligenceHealthReport.build(
            inputWithSnapshotAge(TimeUnit.HOURS.toMillis(8)),
        )

        assertTrue(report.digitalLife.lines.contains("Veri tazeligi: RECENT"))
        assertFalse(
            report.digitalLife.warningCodes.contains(HomeErrorCodes.DIGITAL_LIFE_DATA_STALE),
        )
    }

    @Test
    fun `snapshot older than eighteen hours is stale`() {
        val report = HomeIntelligenceHealthReport.build(
            inputWithSnapshotAge(TimeUnit.HOURS.toMillis(19)),
        )

        assertTrue(report.digitalLife.lines.contains("Veri tazeligi: STALE"))
        assertTrue(
            report.digitalLife.warningCodes.contains(HomeErrorCodes.DIGITAL_LIFE_DATA_STALE),
        )
    }

    private fun inputWithSnapshotAge(ageMs: Long): HomeIntelligenceHealthReport.Input {
        val snapshot = DigitalPulseSnapshot(
            score = DigitalPulseScore(
                total = 84,
                baseScore = 84,
                taskContribution = 0,
                organization = 80,
                attention = 80,
                balance = 80,
                cleanup = 80,
                consistency = 80,
                confidence = DataConfidence.HIGH,
                reasons = emptyList(),
            ),
            computedAt = now - ageMs,
            validUntil = now,
        )
        val missionSummary = HomeMissionSummary(
            completedCount = 2,
            totalCount = 7,
            primaryMissionId = "mission",
            primaryTitle = "mission",
            primaryCurrentText = null,
            primaryRemainingText = null,
            primaryStatus = MissionStatus.IN_PROGRESS,
            urgent = false,
        )
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot)),
            mission = HomeDataResult.Ready(MissionSourceState(missionSummary)),
            ticker = HomeDataResult.Ready(TickerSourceState(emptyList())),
        )
        return HomeIntelligenceHealthReport.Input(
            homeIntelligenceState = state,
            settlementLastSucceededAt = now - TimeUnit.HOURS.toMillis(1),
            settlementLastFailedAt = 0L,
            settlementLastFailureCode = "-",
            settlementNextScheduledAt = now + TimeUnit.HOURS.toMillis(4),
            pendingSettlementCount = 0,
            now = now,
        )
    }
}

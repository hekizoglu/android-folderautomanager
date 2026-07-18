package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.HomeErrorCodes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a privacy-safe health report for the home intelligence sources.
 *
 * The report only contains source states, counters, fixed warning codes, and timestamps. It must not
 * expose package names, notification text, contact data, file names, search queries, or mission titles.
 */
object HomeIntelligenceHealthReport {

    val SETTLEMENT_STALE_THRESHOLD_MS: Long = 26L * 60 * 60 * 1000

    data class Input(
        val homeIntelligenceState: HomeIntelligenceState,
        val settlementLastSucceededAt: Long,
        val settlementLastFailedAt: Long,
        val settlementLastFailureCode: String,
        val settlementNextScheduledAt: Long?,
        val pendingSettlementCount: Int,
        val now: Long,
    )

    data class Section(
        val lines: List<String>,
        val warningCodes: Set<String>,
    )

    data class Report(
        val missionSystem: Section,
        val digitalLife: Section,
        val smartPulseTicker: Section,
    ) {
        val allWarningCodes: Set<String> =
            missionSystem.warningCodes + digitalLife.warningCodes + smartPulseTicker.warningCodes
    }

    fun build(input: Input): Report = Report(
        missionSystem = buildMissionSystemSection(input),
        digitalLife = buildDigitalLifeSection(input),
        smartPulseTicker = buildTickerSection(input),
    )

    private fun buildMissionSystemSection(input: Input): Section {
        val missionResult = input.homeIntelligenceState.mission
        val warnings = mutableSetOf<String>()

        val active = missionResult !is HomeDataResult.Missing && missionResult !is HomeDataResult.Failed
        val summary = when (missionResult) {
            is HomeDataResult.Ready -> missionResult.value.summary
            is HomeDataResult.Stale -> missionResult.value.summary
            else -> null
        }

        val sourceState = sourceHealthCode(missionResult)
        if (sourceState != SourceHealth.READY) {
            warnings += HomeErrorCodes.MISSION_PROGRESS_DATA_STALE
        }
        if (isSettlementStale(input)) {
            warnings += HomeErrorCodes.MISSION_SETTLEMENT_STALE
        }

        val lines = listOf(
            "Aktif: ${yesNo(active)}",
            "Kaynak durumu: ${sourceState.name}",
            "Atanmis gunluk: ${summary?.totalCount?.toString() ?: "-"}",
            "Tamamlanan gunluk: ${summary?.completedCount?.toString() ?: "-"}",
            "Riskli: ${riskyCount(summary)}",
            "Settlement bekleyen: ${input.pendingSettlementCount}",
            "Son settlement: ${formatTimestamp(input.settlementLastSucceededAt)}",
            "Son worker durumu: ${workerOutcomeText(input)}",
            "Sonraki planlanan sinir: ${formatTimestamp(input.settlementNextScheduledAt ?: 0L)}",
        )

        return Section(lines, warnings)
    }

    private fun riskyCount(summary: HomeMissionSummary?): String {
        if (summary == null) return "-"
        return if (summary.urgent) "1" else "0"
    }

    private fun workerOutcomeText(input: Input): String = when {
        input.settlementLastFailedAt > input.settlementLastSucceededAt ->
            "FAILED(${input.settlementLastFailureCode})"
        input.settlementLastSucceededAt > 0L -> "SUCCEEDED"
        else -> "YOK"
    }

    private fun isSettlementStale(input: Input): Boolean {
        if (input.settlementLastSucceededAt <= 0L) {
            return input.pendingSettlementCount > 0
        }
        val ageMs = input.now - input.settlementLastSucceededAt
        if (ageMs > SETTLEMENT_STALE_THRESHOLD_MS && input.pendingSettlementCount > 0) return true
        return input.settlementLastFailedAt > input.settlementLastSucceededAt
    }

    private fun buildDigitalLifeSection(input: Input): Section {
        val pulseResult = input.homeIntelligenceState.pulse
        val warnings = mutableSetOf<String>()

        val sourceState = sourceHealthCode(pulseResult)
        if (sourceState != SourceHealth.READY) {
            warnings += HomeErrorCodes.PULSE_SNAPSHOT_STALE
        }

        val snapshot = when (pulseResult) {
            is HomeDataResult.Ready -> pulseResult.value.snapshot
            is HomeDataResult.Stale -> pulseResult.value.snapshot
            else -> null
        }
        val freshness = freshnessOf(snapshot?.computedAt, input.now)

        val scoreOutOfRange = snapshot?.score?.total?.let { it !in 0..100 } ?: false
        if (scoreOutOfRange) {
            warnings += HomeErrorCodes.PULSE_SOURCE_MISMATCH
        }

        val lines = listOf(
            "Kaynak durumu: ${sourceState.name}",
            "Skor: ${snapshot?.score?.total?.toString() ?: "-"}",
            "Confidence: ${snapshot?.score?.confidence?.name ?: "-"}",
            "Hesap zamani: ${formatTimestamp(snapshot?.computedAt ?: 0L)}",
            "Veri tazeligi: ${freshness.name}",
            "Tek skor kaynagi: DigitalPulseEngine",
        )

        return Section(lines, warnings)
    }

    private fun buildTickerSection(input: Input): Section {
        val tickerResult = input.homeIntelligenceState.ticker
        val warnings = mutableSetOf<String>()
        val sourceState = sourceHealthCode(tickerResult)

        val items = when (tickerResult) {
            is HomeDataResult.Ready -> tickerResult.value.items
            is HomeDataResult.Stale -> tickerResult.value.items
            else -> emptyList()
        }

        val missionSummary = when (val result = input.homeIntelligenceState.mission) {
            is HomeDataResult.Ready -> result.value.summary
            is HomeDataResult.Stale -> result.value.summary
            else -> null
        }
        if (items.isEmpty() && missionSummary?.urgent == true) {
            warnings += HomeErrorCodes.TICKER_EMPTY_WITH_ACTIONABLE_ITEMS
        }

        val lines = listOf(
            "Kaynak durumu: ${sourceState.name}",
            "Aday: -",
            "Gosterilen: ${items.size}",
            "Sessize alinan tur: -",
            "Son secim hatasi: ${if (sourceState == SourceHealth.READY) "Yok" else sourceState.name}",
        )

        return Section(lines, warnings)
    }

    private enum class SourceHealth { READY, STALE, MISSING, FAILED }

    private fun sourceHealthCode(result: HomeDataResult<*>): SourceHealth = when (result) {
        is HomeDataResult.Ready -> SourceHealth.READY
        is HomeDataResult.Stale -> SourceHealth.STALE
        is HomeDataResult.Missing -> SourceHealth.MISSING
        is HomeDataResult.Failed -> SourceHealth.FAILED
    }

    private fun freshnessOf(computedAt: Long?, now: Long): DataFreshness {
        if (computedAt == null || computedAt <= 0L) return DataFreshness.UNAVAILABLE
        val ageMs = now - computedAt
        return when {
            ageMs <= 5 * 60 * 1000L -> DataFreshness.LIVE
            ageMs <= 30 * 60 * 1000L -> DataFreshness.RECENT
            else -> DataFreshness.STALE
        }
    }

    private fun yesNo(value: Boolean): String = if (value) "Evet" else "Hayir"

    private fun formatTimestamp(value: Long): String =
        if (value <= 0L) "-" else HealthReportDateFormat.format(value)
}

private object HealthReportDateFormat {
    private val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun format(value: Long): String = synchronized(dateTime) {
        dateTime.format(Date(value))
    }
}

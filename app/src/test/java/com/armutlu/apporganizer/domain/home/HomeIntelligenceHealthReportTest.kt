package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.HomeErrorCodes
import com.armutlu.apporganizer.domain.common.MissingReason
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Döngü U03 — Sağlık raporu koordinatör sağlığı toplayıcısının testleri
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 2057-2112).
 *
 * Fake kaynak state'leriyle: bir kaynağın Failed/Stale/Missing olması doğru
 * [HomeErrorCodes] koduna map olur, settlement zamanı/sonucu doğru raporlanır,
 * rapor içeriği gizlilik kuralına uyar (uygulama/kişi/dosya/bildirim içeriği yok).
 */
class HomeIntelligenceHealthReportTest {

    private val now = 1_800_000_000_000L

    private fun fakeSnapshot(total: Int = 72, computedAt: Long = now): DigitalPulseSnapshot = DigitalPulseSnapshot(
        score = DigitalPulseScore(
            total = total,
            baseScore = total,
            taskContribution = 0,
            organization = 70,
            attention = 70,
            balance = 70,
            cleanup = 70,
            consistency = 70,
            confidence = DataConfidence.HIGH,
            reasons = emptyList(),
        ),
        computedAt = computedAt,
        validUntil = computedAt + 15 * 60 * 1000L,
    )

    private fun fakeMissionSummary(urgent: Boolean = false) = HomeMissionSummary(
        completedCount = 1,
        totalCount = 3,
        primaryMissionId = "mission-1",
        primaryTitle = "Focus Sprint",
        primaryCurrentText = "1/3",
        primaryRemainingText = "2 kaldı",
        primaryStatus = if (urgent) MissionStatus.AT_RISK else MissionStatus.IN_PROGRESS,
        urgent = urgent,
    )

    private fun baseInput(
        homeIntelligenceState: HomeIntelligenceState,
        settlementLastSucceededAt: Long = now - 60_000L,
        settlementLastFailedAt: Long = 0L,
        settlementLastFailureCode: String = "-",
        settlementNextScheduledAt: Long? = now + 3_600_000L,
        pendingSettlementCount: Int = 0,
    ) = HomeIntelligenceHealthReport.Input(
        homeIntelligenceState = homeIntelligenceState,
        settlementLastSucceededAt = settlementLastSucceededAt,
        settlementLastFailedAt = settlementLastFailedAt,
        settlementLastFailureCode = settlementLastFailureCode,
        settlementNextScheduledAt = settlementNextScheduledAt,
        pendingSettlementCount = pendingSettlementCount,
        now = now,
    )

    // 1) Tüm kaynaklar Ready ve settlement yakın zamanda başarılıysa hiç uyarı olmamalı.
    @Test
    fun `all sources ready and settlement recent yields no warnings`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot())),
            mission = HomeDataResult.Ready(MissionSourceState(summary = fakeMissionSummary())),
            ticker = HomeDataResult.Ready(TickerSourceState(items = listOf(fakeTickerItem()))),
        )

        val report = HomeIntelligenceHealthReport.build(baseInput(state))

        assertTrue(report.allWarningCodes.isEmpty())
    }

    // 2) Pulse kaynağı Failed ise PULSE_SNAPSHOT_STALE uyarısı üretilmeli (doğru koda map).
    @Test
    fun `failed pulse source maps to PULSE_SNAPSHOT_STALE`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Failed(HomeErrorCodes.PULSE_COMPUTE_FAILED),
            mission = HomeDataResult.Ready(MissionSourceState(summary = fakeMissionSummary())),
            ticker = HomeDataResult.Ready(TickerSourceState(items = listOf(fakeTickerItem()))),
        )

        val report = HomeIntelligenceHealthReport.build(baseInput(state))

        assertTrue(report.digitalLife.warningCodes.contains(HomeErrorCodes.PULSE_SNAPSHOT_STALE))
        assertTrue(report.allWarningCodes.contains(HomeErrorCodes.PULSE_SNAPSHOT_STALE))
    }

    // 3) Mission kaynağı Stale ise MISSION_PROGRESS_DATA_STALE uyarısı üretilmeli.
    @Test
    fun `stale mission source maps to MISSION_PROGRESS_DATA_STALE`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot())),
            mission = HomeDataResult.Stale(MissionSourceState(summary = fakeMissionSummary()), HomeErrorCodes.MISSION_SETTLEMENT_FAILED),
            ticker = HomeDataResult.Ready(TickerSourceState(items = listOf(fakeTickerItem()))),
        )

        val report = HomeIntelligenceHealthReport.build(baseInput(state))

        assertTrue(report.missionSystem.warningCodes.contains(HomeErrorCodes.MISSION_PROGRESS_DATA_STALE))
    }

    // 4) Mission kaynağı Missing ise de MISSION_PROGRESS_DATA_STALE üretilmeli (Ready dışında her durum).
    @Test
    fun `missing mission source maps to MISSION_PROGRESS_DATA_STALE`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot())),
            mission = HomeDataResult.Missing(MissingReason.NO_DATA_YET),
            ticker = HomeDataResult.Ready(TickerSourceState(items = listOf(fakeTickerItem()))),
        )

        val report = HomeIntelligenceHealthReport.build(baseInput(state))

        assertTrue(report.missionSystem.warningCodes.contains(HomeErrorCodes.MISSION_PROGRESS_DATA_STALE))
        assertTrue(report.missionSystem.lines.any { it.contains("Aktif: Hayır") })
    }

    // 5) Settlement hiç çalışmamış (lastSucceededAt=0) ve bekleyen instance varsa MISSION_SETTLEMENT_STALE.
    @Test
    fun `settlement never ran with pending instances is stale`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot())),
            mission = HomeDataResult.Ready(MissionSourceState(summary = fakeMissionSummary())),
            ticker = HomeDataResult.Ready(TickerSourceState(items = listOf(fakeTickerItem()))),
        )
        val input = baseInput(
            state,
            settlementLastSucceededAt = 0L,
            settlementNextScheduledAt = null,
            pendingSettlementCount = 2,
        )

        val report = HomeIntelligenceHealthReport.build(input)

        assertTrue(report.missionSystem.warningCodes.contains(HomeErrorCodes.MISSION_SETTLEMENT_STALE))
        assertTrue(report.missionSystem.lines.contains("Son worker durumu: YOK"))
        assertTrue(report.missionSystem.lines.contains("Settlement bekleyen: 2"))
    }

    // 6) Settlement zamanı doğru raporlanır — lastSucceededAt formatlanmış tarih olarak satırda yer alır.
    @Test
    fun `settlement time is reported correctly`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot())),
            mission = HomeDataResult.Ready(MissionSourceState(summary = fakeMissionSummary())),
            ticker = HomeDataResult.Ready(TickerSourceState(items = listOf(fakeTickerItem()))),
        )
        val succeededAt = now - 3_600_000L
        val input = baseInput(state, settlementLastSucceededAt = succeededAt)

        val report = HomeIntelligenceHealthReport.build(input)

        val expectedFormatted = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(succeededAt))
        assertTrue(report.missionSystem.lines.contains("Son settlement: $expectedFormatted"))
        assertTrue(report.missionSystem.lines.contains("Son worker durumu: SUCCEEDED"))
    }

    // 7) Settlement son çalışması hata ile bitmiş (failedAt > succeededAt) ise FAILED(kod) raporlanır ve stale sayılır.
    @Test
    fun `settlement last run failed reports failure code and is stale`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot())),
            mission = HomeDataResult.Ready(MissionSourceState(summary = fakeMissionSummary())),
            ticker = HomeDataResult.Ready(TickerSourceState(items = listOf(fakeTickerItem()))),
        )
        val input = baseInput(
            state,
            settlementLastSucceededAt = now - 7_200_000L,
            settlementLastFailedAt = now - 60_000L,
            settlementLastFailureCode = "DATABASE_ERROR",
        )

        val report = HomeIntelligenceHealthReport.build(input)

        assertTrue(report.missionSystem.lines.contains("Son worker durumu: FAILED(DATABASE_ERROR)"))
        assertTrue(report.missionSystem.warningCodes.contains(HomeErrorCodes.MISSION_SETTLEMENT_STALE))
    }

    // 8) Şerit boş ama birincil görev AT_RISK (aksiyon alınabilir) ise TICKER_EMPTY_WITH_ACTIONABLE_ITEMS.
    @Test
    fun `empty ticker with urgent mission item warns TICKER_EMPTY_WITH_ACTIONABLE_ITEMS`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot())),
            mission = HomeDataResult.Ready(MissionSourceState(summary = fakeMissionSummary(urgent = true))),
            ticker = HomeDataResult.Ready(TickerSourceState(items = emptyList())),
        )

        val report = HomeIntelligenceHealthReport.build(baseInput(state))

        assertTrue(report.smartPulseTicker.warningCodes.contains(HomeErrorCodes.TICKER_EMPTY_WITH_ACTIONABLE_ITEMS))
    }

    // 9) Şerit boş ama aksiyon alınabilir bir öğe yoksa uyarı üretilmemeli (yanlış pozitif olmasın).
    @Test
    fun `empty ticker without actionable items does not warn`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot())),
            mission = HomeDataResult.Ready(MissionSourceState(summary = fakeMissionSummary(urgent = false))),
            ticker = HomeDataResult.Ready(TickerSourceState(items = emptyList())),
        )

        val report = HomeIntelligenceHealthReport.build(baseInput(state))

        assertFalse(report.smartPulseTicker.warningCodes.contains(HomeErrorCodes.TICKER_EMPTY_WITH_ACTIONABLE_ITEMS))
    }

    // 10) Skor 0..100 aralığı dışına çıkarsa PULSE_SOURCE_MISMATCH (bariz bozulma tespiti).
    @Test
    fun `out of range pulse score warns PULSE_SOURCE_MISMATCH`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot(total = 150))),
            mission = HomeDataResult.Ready(MissionSourceState(summary = fakeMissionSummary())),
            ticker = HomeDataResult.Ready(TickerSourceState(items = listOf(fakeTickerItem()))),
        )

        val report = HomeIntelligenceHealthReport.build(baseInput(state))

        assertTrue(report.digitalLife.warningCodes.contains(HomeErrorCodes.PULSE_SOURCE_MISMATCH))
    }

    // 11) Gizlilik: rapor satırları uygulama paket adı, bildirim metni veya kişi bilgisi içermemeli
    //     (yalnızca sayaç/kod/zaman damgası taşınmalı).
    @Test
    fun `report lines never leak app package or personal content`() {
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Ready(PulseSourceState(snapshot = fakeSnapshot())),
            mission = HomeDataResult.Ready(MissionSourceState(summary = fakeMissionSummary())),
            ticker = HomeDataResult.Ready(TickerSourceState(items = listOf(fakeTickerItem()))),
        )

        val report = HomeIntelligenceHealthReport.build(baseInput(state))
        val allLines = report.missionSystem.lines + report.digitalLife.lines + report.smartPulseTicker.lines

        allLines.forEach { line ->
            assertFalse(line.contains("com.example"))
            assertFalse(line.contains("Focus Sprint")) // görev başlığı bile rapora sızmamalı
        }
    }

    // 12) Uyarı kodları her zaman HomeErrorCodes sabitlerinden gelir — serbest metin değil.
    @Test
    fun `warning codes are always known HomeErrorCodes constants`() {
        val known = setOf(
            HomeErrorCodes.MISSION_SETTLEMENT_STALE,
            HomeErrorCodes.MISSION_PROGRESS_DATA_STALE,
            HomeErrorCodes.PULSE_SNAPSHOT_STALE,
            HomeErrorCodes.PULSE_SOURCE_MISMATCH,
            HomeErrorCodes.TICKER_EMPTY_WITH_ACTIONABLE_ITEMS,
        )
        val state = HomeIntelligenceState(
            pulse = HomeDataResult.Failed(HomeErrorCodes.PULSE_COMPUTE_FAILED),
            mission = HomeDataResult.Missing(MissingReason.NO_DATA_YET),
            ticker = HomeDataResult.Ready(TickerSourceState(items = emptyList())),
        )
        val input = baseInput(state, settlementLastSucceededAt = 0L, pendingSettlementCount = 1)

        val report = HomeIntelligenceHealthReport.build(input)

        assertTrue(report.allWarningCodes.isNotEmpty())
        assertTrue(known.containsAll(report.allWarningCodes))
    }

    private fun fakeTickerItem(): SmartTickerItem = SmartTickerItem(
        id = "ticker-1",
        type = SmartTickerType.FEATURE_DISCOVERY,
        title = "ticker-1",
        icon = "📰",
        priority = 0,
        createdAt = now,
    )
}

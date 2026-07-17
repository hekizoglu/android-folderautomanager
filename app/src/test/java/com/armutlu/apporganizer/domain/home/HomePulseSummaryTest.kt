package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import com.armutlu.apporganizer.domain.usecase.pulse.PulseReasonId
import com.armutlu.apporganizer.domain.usecase.pulse.PulseScoreReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [toHomePulseSummary] — Dongu D02 (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satir 1360-1450). Skor bandi, confidence ve freshness davranislari.
 */
class HomePulseSummaryTest {

    private fun score(
        total: Int,
        confidence: DataConfidence = DataConfidence.HIGH,
        reasons: List<PulseScoreReason> = emptyList(),
    ): DigitalPulseScore = DigitalPulseScore(
        total = total,
        baseScore = total,
        taskContribution = 0,
        organization = 70,
        attention = 70,
        balance = 70,
        cleanup = 70,
        consistency = 70,
        confidence = confidence,
        reasons = reasons,
    )

    private fun snapshot(
        total: Int,
        confidence: DataConfidence = DataConfidence.HIGH,
        reasons: List<PulseScoreReason> = emptyList(),
        computedAt: Long = 1_000_000L,
        previousScore: Int? = null,
        scoreDelta: Int? = null,
    ): DigitalPulseSnapshot = DigitalPulseSnapshot(
        score = score(total, confidence, reasons),
        computedAt = computedAt,
        validUntil = computedAt + 15 * 60 * 1000L,
        previousScore = previousScore,
        scoreDelta = scoreDelta,
    )

    // ── Skor bandi -> etiket ────────────────────────────────────────────────

    @Test
    fun `score 90 maps to EXCELLENT band`() {
        val summary = toHomePulseSummary(snapshot(90), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.EXCELLENT, summary.statusBand)
    }

    @Test
    fun `score 80 boundary maps to EXCELLENT band`() {
        val summary = toHomePulseSummary(snapshot(80), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.EXCELLENT, summary.statusBand)
    }

    @Test
    fun `score 79 boundary maps to GOOD band`() {
        val summary = toHomePulseSummary(snapshot(79), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.GOOD, summary.statusBand)
    }

    @Test
    fun `score 65 boundary maps to GOOD band`() {
        val summary = toHomePulseSummary(snapshot(65), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.GOOD, summary.statusBand)
    }

    @Test
    fun `score 64 boundary maps to BALANCED band`() {
        val summary = toHomePulseSummary(snapshot(64), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.BALANCED, summary.statusBand)
    }

    @Test
    fun `score 50 boundary maps to BALANCED band`() {
        val summary = toHomePulseSummary(snapshot(50), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.BALANCED, summary.statusBand)
    }

    @Test
    fun `score 49 boundary maps to NEEDS_FOCUS band`() {
        val summary = toHomePulseSummary(snapshot(49), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.NEEDS_FOCUS, summary.statusBand)
    }

    @Test
    fun `score 35 boundary maps to NEEDS_FOCUS band`() {
        val summary = toHomePulseSummary(snapshot(35), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.NEEDS_FOCUS, summary.statusBand)
    }

    @Test
    fun `score 34 boundary maps to IMPROVING band`() {
        val summary = toHomePulseSummary(snapshot(34), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.IMPROVING, summary.statusBand)
    }

    @Test
    fun `score 0 maps to IMPROVING band`() {
        val summary = toHomePulseSummary(snapshot(0), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseStatusBand.IMPROVING, summary.statusBand)
    }

    // ── Delta ────────────────────────────────────────────────────────────────

    @Test
    fun `first week delta is null`() {
        val summary = toHomePulseSummary(
            snapshot(70, scoreDelta = null, previousScore = null),
            DataFreshness.LIVE,
            nowMillis = 1_000_000L,
        )
        assertNull(summary.delta)
    }

    @Test
    fun `positive delta is carried through`() {
        val summary = toHomePulseSummary(
            snapshot(72, scoreDelta = 4, previousScore = 68),
            DataFreshness.LIVE,
            nowMillis = 1_000_000L,
        )
        assertEquals(4, summary.delta)
    }

    @Test
    fun `negative delta is carried through`() {
        val summary = toHomePulseSummary(
            snapshot(60, scoreDelta = -6, previousScore = 66),
            DataFreshness.LIVE,
            nowMillis = 1_000_000L,
        )
        assertEquals(-6, summary.delta)
    }

    // ── Confidence ───────────────────────────────────────────────────────────

    @Test
    fun `HIGH confidence shows score`() {
        val summary = toHomePulseSummary(
            snapshot(72, confidence = DataConfidence.HIGH),
            DataFreshness.LIVE,
            nowMillis = 1_000_000L,
        )
        assertFalse(summary.shouldHideScore)
        assertEquals(72, summary.score)
    }

    @Test
    fun `MEDIUM confidence still shows score (UI adds estimate badge)`() {
        val summary = toHomePulseSummary(
            snapshot(72, confidence = DataConfidence.MEDIUM),
            DataFreshness.LIVE,
            nowMillis = 1_000_000L,
        )
        assertFalse(summary.shouldHideScore)
        assertEquals(DataConfidence.MEDIUM, summary.confidence)
    }

    @Test
    fun `LOW confidence hides the score`() {
        val summary = toHomePulseSummary(
            snapshot(72, confidence = DataConfidence.LOW),
            DataFreshness.LIVE,
            nowMillis = 1_000_000L,
        )
        assertTrue(summary.shouldHideScore)
    }

    // ── topReason ────────────────────────────────────────────────────────────

    @Test
    fun `top reason picks the largest absolute delta`() {
        val reasons = listOf(
            PulseScoreReason(PulseReasonId.ORGANIZATION_HIGH, delta = 5),
            PulseScoreReason(PulseReasonId.ATTENTION_NOISY, delta = -18),
            PulseScoreReason(PulseReasonId.CLEANUP_TIDY, delta = 2),
        )
        val summary = toHomePulseSummary(snapshot(60, reasons = reasons), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertEquals(PulseReasonId.ATTENTION_NOISY, summary.topReasonId)
    }

    @Test
    fun `reasons with zero delta are ignored for top reason`() {
        val reasons = listOf(
            PulseScoreReason(PulseReasonId.BALANCE_STEADY, delta = 0),
            PulseScoreReason(PulseReasonId.CONSISTENCY_NO_DATA, delta = 0),
        )
        val summary = toHomePulseSummary(snapshot(60, reasons = reasons), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertNull(summary.topReasonId)
    }

    @Test
    fun `no reasons yields null top reason`() {
        val summary = toHomePulseSummary(snapshot(60, reasons = emptyList()), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertNull(summary.topReasonId)
    }

    // ── Freshness ────────────────────────────────────────────────────────────

    @Test
    fun `STALE freshness computes elapsed minutes`() {
        val computedAt = 1_000_000L
        val nowMillis = computedAt + 45 * 60 * 1000L
        val summary = toHomePulseSummary(
            snapshot(70, computedAt = computedAt),
            DataFreshness.STALE,
            nowMillis = nowMillis,
        )
        assertEquals(45L, summary.staleMinutes)
    }

    @Test
    fun `non-STALE freshness has null staleMinutes`() {
        val summary = toHomePulseSummary(snapshot(70), DataFreshness.LIVE, nowMillis = 1_000_000L)
        assertNull(summary.staleMinutes)
    }

    @Test
    fun `UNAVAILABLE freshness with null snapshot yields CTA-ready summary`() {
        val summary = toHomePulseSummary(null, DataFreshness.UNAVAILABLE, nowMillis = 1_000_000L)
        assertEquals(DataFreshness.UNAVAILABLE, summary.freshness)
        assertNull(summary.score)
        assertNull(summary.statusBand)
        assertFalse(summary.isActionable)
    }

    @Test
    fun `non-UNAVAILABLE freshness is actionable`() {
        val summary = toHomePulseSummary(snapshot(70), DataFreshness.STALE, nowMillis = 1_000_000L)
        assertTrue(summary.isActionable)
    }
}

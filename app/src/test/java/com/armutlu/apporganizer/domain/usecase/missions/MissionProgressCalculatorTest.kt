package com.armutlu.apporganizer.domain.usecase.missions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MissionProgressCalculator — Dongu M03 (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satir 852-941). UPPER_LIMIT senaryolari roadmap'te tanimli tam sayilarla dogrulanir.
 */
class MissionProgressCalculatorTest {

    private fun upperLimitEvaluation(current: Long, target: Long, status: MissionStatus = MissionStatus.IN_PROGRESS) =
        MissionEvaluation(
            status = status,
            currentValue = current,
            targetValue = target,
            remainingValue = (target - current).coerceAtLeast(0L),
        )

    @Test
    fun `90 of 180 yields remaining 90 and fraction 0_5`() {
        val progress = MissionProgressCalculator.calculate(
            upperLimitEvaluation(90L, 180L),
            MissionProgressKind.UPPER_LIMIT,
        )
        assertEquals(90L, progress.remainingValue)
        assertEquals(0.5f, progress.progressFraction)
        assertNull(progress.exceededValue)
    }

    @Test
    fun `179 of 180 yields remaining 1`() {
        val progress = MissionProgressCalculator.calculate(
            upperLimitEvaluation(179L, 180L),
            MissionProgressKind.UPPER_LIMIT,
        )
        assertEquals(1L, progress.remainingValue)
        assertNull(progress.exceededValue)
    }

    @Test
    fun `180 of 180 yields exceeded 0 and no remaining`() {
        val progress = MissionProgressCalculator.calculate(
            upperLimitEvaluation(180L, 180L, MissionStatus.FAILED),
            MissionProgressKind.UPPER_LIMIT,
        )
        assertEquals(0L, progress.exceededValue)
        assertNull(progress.remainingValue)
        assertEquals(1f, progress.progressFraction)
    }

    @Test
    fun `200 of 180 yields exceeded 20 and no remaining`() {
        val progress = MissionProgressCalculator.calculate(
            upperLimitEvaluation(200L, 180L, MissionStatus.FAILED),
            MissionProgressKind.UPPER_LIMIT,
        )
        assertEquals(20L, progress.exceededValue)
        assertNull(progress.remainingValue)
        assertEquals(1f, progress.progressFraction) // fraction 0..1 clamp
    }

    @Test
    fun `null current or target produces no text specs`() {
        val progress = MissionProgressCalculator.calculate(
            MissionEvaluation(
                status = MissionStatus.DATA_UNAVAILABLE,
                currentValue = null,
                targetValue = 180L,
                remainingValue = null,
            ),
            MissionProgressKind.UPPER_LIMIT,
        )
        assertNull(progress.currentTextRes)
        assertNull(progress.remainingTextRes)
        assertNull(progress.progressTextRes)
        assertNull(progress.progressFraction)
        assertNull(progress.exceededValue)
    }

    @Test
    fun `action count below target has positive remaining and no exceeded`() {
        val progress = MissionProgressCalculator.calculate(
            MissionEvaluation(MissionStatus.IN_PROGRESS, 1L, 2L, 1L),
            MissionProgressKind.ACTION_COUNT,
        )
        assertEquals(1L, progress.remainingValue)
        assertNull(progress.exceededValue)
        assertEquals(0.5f, progress.progressFraction)
    }

    @Test
    fun `action count at target has zero remaining`() {
        val progress = MissionProgressCalculator.calculate(
            MissionEvaluation(MissionStatus.COMPLETED, 2L, 2L, 0L),
            MissionProgressKind.ACTION_COUNT,
        )
        assertEquals(0L, progress.remainingValue)
        assertEquals(1f, progress.progressFraction)
    }

    @Test
    fun `period comparison below baseline has no exceeded`() {
        val progress = MissionProgressCalculator.calculate(
            MissionEvaluation(MissionStatus.IN_PROGRESS, 100L, 150L, 50L),
            MissionProgressKind.PERIOD_COMPARISON,
        )
        assertNull(progress.exceededValue)
        assertEquals(50L, progress.remainingValue)
    }

    @Test
    fun `period comparison over baseline reports exceeded and no remaining`() {
        val progress = MissionProgressCalculator.calculate(
            MissionEvaluation(MissionStatus.FAILED, 200L, 150L, 0L),
            MissionProgressKind.PERIOD_COMPARISON,
        )
        assertEquals(50L, progress.exceededValue)
        assertNull(progress.remainingValue)
    }

    @Test
    fun `progress fraction is always clamped between 0 and 1`() {
        val over = MissionProgressCalculator.calculate(
            upperLimitEvaluation(500L, 180L, MissionStatus.FAILED),
            MissionProgressKind.UPPER_LIMIT,
        )
        assertTrue(over.progressFraction!! <= 1f)
        assertTrue(over.progressFraction!! >= 0f)
    }

    // ── Dongu G3a: AVOID_BEFORE_TIME (sabah pozitifi) ────────────────────────────────

    @Test
    fun `avoid before time safe flag yields zero fraction and no exceeded`() {
        val progress = MissionProgressCalculator.calculate(
            MissionEvaluation(MissionStatus.COMPLETED, 0L, 0L, 0L),
            MissionProgressKind.AVOID_BEFORE_TIME,
        )
        assertEquals(0f, progress.progressFraction)
        assertNull(progress.exceededValue)
        assertEquals(0L, progress.remainingValue)
    }

    @Test
    fun `avoid before time violated flag yields full fraction and exceeded value`() {
        val progress = MissionProgressCalculator.calculate(
            MissionEvaluation(MissionStatus.FAILED, 1L, 0L, 0L, failureReasonCode = "MORNING_SOCIAL_USAGE_DETECTED"),
            MissionProgressKind.AVOID_BEFORE_TIME,
        )
        assertEquals(1f, progress.progressFraction)
        assertEquals(1L, progress.exceededValue)
        assertNull(progress.remainingValue)
    }
}

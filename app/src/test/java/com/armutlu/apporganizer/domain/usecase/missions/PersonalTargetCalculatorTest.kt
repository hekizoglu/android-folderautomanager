package com.armutlu.apporganizer.domain.usecase.missions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PersonalTargetCalculator — Dongu G1 (GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md).
 * Medyan x tempo formulu, clamp sinirlari ve <3 gun "tanisma modu" (null) senaryolarini dogrular.
 */
class PersonalTargetCalculatorTest {

    @Test
    fun `less than 3 days returns null (tanisma modu)`() {
        assertNull(PersonalTargetCalculator.calculateScreenTimeTarget(listOf(120L, 130L), tempo = 0.9))
        assertNull(PersonalTargetCalculator.calculateScreenTimeTarget(emptyList(), tempo = 0.9))
        assertNull(PersonalTargetCalculator.calculateUnlockTarget(listOf(20L), tempo = 0.9))
    }

    @Test
    fun `exactly 3 days is enough`() {
        val result = PersonalTargetCalculator.calculateScreenTimeTarget(listOf(100L, 120L, 140L), tempo = 1.0)
        assertEquals(120L, result)
    }

    @Test
    fun `median of odd count is middle value`() {
        val result = PersonalTargetCalculator.calculate(
            listOf(200L, 100L, 300L, 150L, 250L),
            tempo = 1.0,
            minClamp = 0L,
            maxClamp = 1000L,
        )
        assertEquals(200L, result) // sorted: 100,150,200,250,300 -> middle 200
    }

    @Test
    fun `median of even count averages two middle values`() {
        val result = PersonalTargetCalculator.calculate(
            listOf(100L, 200L, 300L, 400L),
            tempo = 1.0,
            minClamp = 0L,
            maxClamp = 1000L,
        )
        assertEquals(250L, result) // (200+300)/2
    }

    @Test
    fun `tempo coefficient scales the median`() {
        val rahat = PersonalTargetCalculator.calculateScreenTimeTarget(listOf(200L, 200L, 200L), tempo = 1.0)
        val dengeli = PersonalTargetCalculator.calculateScreenTimeTarget(listOf(200L, 200L, 200L), tempo = 0.9)
        val iddiali = PersonalTargetCalculator.calculateScreenTimeTarget(listOf(200L, 200L, 200L), tempo = 0.8)

        assertEquals(200L, rahat)
        assertEquals(180L, dengeli)
        assertEquals(160L, iddiali)
    }

    @Test
    fun `screen time target clamps to 60-360 minutes`() {
        val tooLow = PersonalTargetCalculator.calculateScreenTimeTarget(listOf(10L, 10L, 10L), tempo = 1.0)
        val tooHigh = PersonalTargetCalculator.calculateScreenTimeTarget(listOf(500L, 500L, 500L), tempo = 1.0)

        assertEquals(PersonalTargetCalculator.SCREEN_MIN_MINUTES, tooLow)
        assertEquals(PersonalTargetCalculator.SCREEN_MAX_MINUTES, tooHigh)
    }

    @Test
    fun `unlock target clamps to 15-80`() {
        val tooLow = PersonalTargetCalculator.calculateUnlockTarget(listOf(5L, 5L, 5L), tempo = 1.0)
        val tooHigh = PersonalTargetCalculator.calculateUnlockTarget(listOf(200L, 200L, 200L), tempo = 1.0)

        assertEquals(PersonalTargetCalculator.UNLOCK_MIN_COUNT, tooLow)
        assertEquals(PersonalTargetCalculator.UNLOCK_MAX_COUNT, tooHigh)
    }

    @Test
    fun `mid-range values pass through unclamped after tempo`() {
        val result = PersonalTargetCalculator.calculateScreenTimeTarget(listOf(150L, 160L, 170L), tempo = 0.9)
        // median 160 * 0.9 = 144
        assertEquals(144L, result)
    }
}

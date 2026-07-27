package com.armutlu.apporganizer.domain.usecase.goals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P2 — [AdaptiveCategoryTargetCalculator]. Roadmap §16 "Saf hesaplama" 10 senaryosunun tamamı.
 */
class AdaptiveCategoryTargetCalculatorTest {

    private fun input(
        previousWeekActualMinutes: Long,
        previousTargetMinutes: Long? = null,
        validDataDayCount: Int = 7,
        pace: AdaptiveGoalPace = AdaptiveGoalPace.DENGELI,
    ) = AdaptiveCategoryTargetCalculator.Input(
        previousWeekActualMinutes = previousWeekActualMinutes,
        previousTargetMinutes = previousTargetMinutes,
        validDataDayCount = validDataDayCount,
        pace = pace,
    )

    @Test
    fun `1 - first week 600 minutes DENGELI produces approximately 540`() {
        val result = AdaptiveCategoryTargetCalculator.calculate(input(previousWeekActualMinutes = 600))
        assertTrue(result is AdaptiveCategoryTargetCalculator.Result.Target)
        assertEquals(540L, (result as AdaptiveCategoryTargetCalculator.Result.Target).targetMinutes)
    }

    @Test
    fun `2 - previous target 540 actual usage 450 drops but does not exceed 15 percent`() {
        val result = AdaptiveCategoryTargetCalculator.calculate(
            input(previousWeekActualMinutes = 450, previousTargetMinutes = 540),
        )
        assertTrue(result is AdaptiveCategoryTargetCalculator.Result.Target)
        val target = (result as AdaptiveCategoryTargetCalculator.Result.Target).targetMinutes
        assertTrue("target=$target should be < 540 (dropped)", target < 540L)
        val minAllowed = (540L * 0.85).toLong()
        assertTrue("target=$target should not exceed 15% drop floor=$minAllowed", target >= minAllowed)
    }

    @Test
    fun `3 - previous target 420 actual usage 700 rises but by at most 15 percent`() {
        val result = AdaptiveCategoryTargetCalculator.calculate(
            input(previousWeekActualMinutes = 700, previousTargetMinutes = 420),
        )
        assertTrue(result is AdaptiveCategoryTargetCalculator.Result.Target)
        val target = (result as AdaptiveCategoryTargetCalculator.Result.Target).targetMinutes
        assertTrue("target=$target should be > 420 (risen)", target > 420L)
        val maxAllowed = (420L * 1.15).toLong()
        assertTrue("target=$target should not exceed 15% rise ceiling=$maxAllowed", target <= maxAllowed)
    }

    @Test
    fun `4 - usage 80 minutes below eligibility threshold produces no target`() {
        val result = AdaptiveCategoryTargetCalculator.calculate(input(previousWeekActualMinutes = 80))
        assertEquals(AdaptiveCategoryTargetCalculator.Result.BelowEligibilityThreshold, result)
    }

    @Test
    fun `5 - only 3 valid data days means learning mode`() {
        val result = AdaptiveCategoryTargetCalculator.calculate(
            input(previousWeekActualMinutes = 600, validDataDayCount = 3),
        )
        assertEquals(AdaptiveCategoryTargetCalculator.Result.InsufficientData, result)
    }

    @Test
    fun `6 - below eligibility threshold still respected even with existing target present`() {
        // Mevcut hedef varken kullanım eşiğin altına düşerse de üretim mantığı çalışmaya devam
        // eder (mevcut hedefi korumak çağıran tarafın sorumluluğu — snapshot unavailable ise bu
        // fonksiyon zaten hiç çağrılmaz, roadmap S4.1 notu).
        val result = AdaptiveCategoryTargetCalculator.calculate(
            input(previousWeekActualMinutes = 80, previousTargetMinutes = 300),
        )
        assertTrue(result is AdaptiveCategoryTargetCalculator.Result.Target)
    }

    @Test
    fun `7 - manual previous target is never overwritten by this calculator directly`() {
        // Bu hesaplayıcı MANUAL/AUTO ayrımını bilmez (roadmap: mode kontrolü çağıran tarafın işi,
        // EnsureCurrentWeekAdaptiveGoalsUseCase P4'te MANUAL modda bu fonksiyonu hiç çağırmaz).
        // Burada sadece hesaplayıcının kendi başına "hedef ez" davranışı olmadığını, sadece
        // girdiye göre yeni bir Target ürettiğini doğruluyoruz — orkestrasyon P4'te test edilir.
        val result = AdaptiveCategoryTargetCalculator.calculate(
            input(previousWeekActualMinutes = 600, previousTargetMinutes = 600),
        )
        assertTrue(result is AdaptiveCategoryTargetCalculator.Result.Target)
    }

    @Test
    fun `8 - outlier protected last week combined with 4-week median stays within guard bounds`() {
        val priorWeeks = listOf(300L, 320L, 280L, 310L)
        val outlierLastWeek = 900L // medyan ~305, %75 üstü sınırı asar
        val guarded = OutlierWeekGuard.guard(outlierLastWeek, priorWeeks)
        assertTrue("guarded=$guarded should be clamped below raw outlier", guarded < outlierLastWeek)

        val result = AdaptiveCategoryTargetCalculator.calculate(input(previousWeekActualMinutes = guarded))
        assertTrue(result is AdaptiveCategoryTargetCalculator.Result.Target)
    }

    @Test
    fun `9 - result is rounded to a 5-minute boundary`() {
        val result = AdaptiveCategoryTargetCalculator.calculate(
            input(previousWeekActualMinutes = 613, previousTargetMinutes = null),
        )
        assertTrue(result is AdaptiveCategoryTargetCalculator.Result.Target)
        val target = (result as AdaptiveCategoryTargetCalculator.Result.Target).targetMinutes
        assertEquals(0L, target % 5L)
    }

    @Test
    fun `10 - calculation is deterministic for identical inputs`() {
        val i = input(previousWeekActualMinutes = 517, previousTargetMinutes = 480)
        val first = AdaptiveCategoryTargetCalculator.calculate(i)
        val second = AdaptiveCategoryTargetCalculator.calculate(i)
        assertEquals(first, second)
    }

    @Test
    fun `min and max target minutes are respected`() {
        val veryLow = AdaptiveCategoryTargetCalculator.calculate(
            input(previousWeekActualMinutes = 91, pace = AdaptiveGoalPace.IDDIALI),
        )
        assertTrue(veryLow is AdaptiveCategoryTargetCalculator.Result.Target)
        assertTrue(
            (veryLow as AdaptiveCategoryTargetCalculator.Result.Target).targetMinutes
                >= AdaptiveCategoryTargetCalculator.MIN_TARGET_MINUTES,
        )

        val veryHigh = AdaptiveCategoryTargetCalculator.calculate(
            input(previousWeekActualMinutes = 20_000, pace = AdaptiveGoalPace.RAHAT),
        )
        assertTrue(veryHigh is AdaptiveCategoryTargetCalculator.Result.Target)
        assertTrue(
            (veryHigh as AdaptiveCategoryTargetCalculator.Result.Target).targetMinutes
                <= AdaptiveCategoryTargetCalculator.MAX_TARGET_MINUTES,
        )
    }
}

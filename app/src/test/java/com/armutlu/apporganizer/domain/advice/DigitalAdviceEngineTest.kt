package com.armutlu.apporganizer.domain.advice

import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.common.DataFreshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P7 — [DigitalAdviceEngine]. Roadmap §16 "Tavsiye motoru" test grubu.
 */
class DigitalAdviceEngineTest {

    private val now = 1_753_000_000_000L

    private fun baseInput() = DigitalAdviceInput(permissionFreshness = DataFreshness.LIVE)

    @Test
    fun `permission issue has the highest priority over everything else`() {
        val input = baseInput().copy(
            permissionFreshness = DataFreshness.UNAVAILABLE,
            exceededCategoryGoalCount = 3,
        )
        val advice = DigitalAdviceEngine.evaluate(input, now)
        assertEquals(DigitalAdviceType.PERMISSION_ISSUE, advice?.type)
    }

    @Test
    fun `goal exceeded outranks projected overage risk and usage increase`() {
        val input = baseInput().copy(
            exceededCategoryGoalCount = 1,
            atRiskCategoryGoalCount = 2,
            categoryUsageChanges = listOf(
                CategoryUsageChange(R.string.mission_unknown, previousWeekMinutes = 100, currentWeekMinutes = 200),
            ),
        )
        val advice = DigitalAdviceEngine.evaluate(input, now)
        assertEquals(DigitalAdviceType.GOAL_EXCEEDED, advice?.type)
    }

    @Test
    fun `small change does not produce a significant usage increase advice`() {
        // %10 artis + 15dk -> ikisi de esigin altinda.
        val input = baseInput().copy(
            categoryUsageChanges = listOf(
                CategoryUsageChange(R.string.mission_unknown, previousWeekMinutes = 150, currentWeekMinutes = 165),
            ),
        )
        val advice = DigitalAdviceEngine.evaluate(input, now)
        assertNull(advice)
    }

    @Test
    fun `percent threshold alone without minute threshold does not trigger advice`() {
        // %50 artis ama sadece 10dk fark (mutlak esik 30dk'nin altinda).
        val input = baseInput().copy(
            categoryUsageChanges = listOf(
                CategoryUsageChange(R.string.mission_unknown, previousWeekMinutes = 20, currentWeekMinutes = 30),
            ),
        )
        val advice = DigitalAdviceEngine.evaluate(input, now)
        assertNull(advice)
    }

    @Test
    fun `20 percent and 30 minutes together trigger significant increase advice`() {
        val input = baseInput().copy(
            categoryUsageChanges = listOf(
                CategoryUsageChange(R.string.mission_unknown, previousWeekMinutes = 150, currentWeekMinutes = 200),
            ),
        )
        val advice = DigitalAdviceEngine.evaluate(input, now)
        assertEquals(DigitalAdviceType.SIGNIFICANT_USAGE_INCREASE, advice?.type)
    }

    @Test
    fun `positive advice only fires when there is real category goal data`() {
        val input = baseInput().copy(hasAnyCategoryGoal = true, allCategoryGoalsOnTrack = true)
        val advice = DigitalAdviceEngine.evaluate(input, now)
        assertEquals(DigitalAdviceType.POSITIVE_REINFORCEMENT, advice?.type)
    }

    @Test
    fun `positive advice does not fire when there is no category goal data (no fabricated numbers)`() {
        val input = baseInput().copy(hasAnyCategoryGoal = false, allCategoryGoalsOnTrack = true)
        val advice = DigitalAdviceEngine.evaluate(input, now)
        assertNull("Veri yokken (hasAnyCategoryGoal=false) tavsiye uydurulmamali", advice)
    }

    @Test
    fun `same suggestionKey is stable for the same advice type across calls`() {
        val input = baseInput().copy(exceededCategoryGoalCount = 1)
        val first = DigitalAdviceEngine.evaluate(input, now)
        val second = DigitalAdviceEngine.evaluate(input, now + 1000)
        assertEquals(first?.suggestionKey, second?.suggestionKey)
    }

    @Test
    fun `typed action is correct for each advice type`() {
        val goalExceeded = DigitalAdviceEngine.evaluate(baseInput().copy(exceededCategoryGoalCount = 1), now)
        assertEquals(DigitalAdviceAction.OpenCategoryGoals, goalExceeded?.action)

        val notificationNoise = DigitalAdviceEngine.evaluate(
            baseInput().copy(notificationNoiseTopSourceShare = 0.5f),
            now,
        )
        assertEquals(DigitalAdviceAction.OpenNotificationReport, notificationNoise?.action)

        val uncategorized = DigitalAdviceEngine.evaluate(baseInput().copy(uncategorizedAppCount = 5), now)
        assertEquals(DigitalAdviceAction.OpenClassificationReview, uncategorized?.action)
    }

    @Test
    fun `only one advice is produced at a time even when multiple thresholds are met`() {
        val input = baseInput().copy(
            exceededCategoryGoalCount = 1,
            notificationNoiseTopSourceShare = 0.9f,
            unusedAppCount = 20,
            uncategorizedAppCount = 20,
        )
        val advice = DigitalAdviceEngine.evaluate(input, now)
        assertTrue(advice != null)
        // Sadece en yuksek oncelikli (GOAL_EXCEEDED) donmeli, digerleri BASTIRILIR.
        assertEquals(DigitalAdviceType.GOAL_EXCEEDED, advice?.type)
    }

    @Test
    fun `notification noise below threshold produces nothing`() {
        val input = baseInput().copy(notificationNoiseTopSourceShare = 0.2f)
        assertNull(DigitalAdviceEngine.evaluate(input, now))
    }

    @Test
    fun `unused app count below threshold produces nothing`() {
        val input = baseInput().copy(unusedAppCount = 2)
        assertNull(DigitalAdviceEngine.evaluate(input, now))
    }
}

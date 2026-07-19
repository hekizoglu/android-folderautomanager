package com.armutlu.apporganizer.domain.usecase.missions

import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * AppLimitCandidateSelector — Dongu G3b (GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md G3,
 * "Uygulama-spesifik gorevler"). Aday secimi (esik/kategori filtresi, en yuksek kullanim)
 * ve hedef hesabi (medyan x oran x tempo, min/max clamp) senaryolarini dogrular.
 */
class AppLimitCandidateSelectorTest {

    private fun candidate(
        pkg: String,
        categoryId: String = Category.CAT_SOCIAL,
        daily: List<Long>,
    ) = AppLimitCandidateSelector.PackageUsageCandidate(pkg, categoryId, daily)

    @Test
    fun `no candidates returns null`() {
        val result = AppLimitCandidateSelector.selectCandidate(emptyList(), tempo = 1.0)
        assertNull(result)
    }

    @Test
    fun `below minimum daily average is not eligible`() {
        val candidates = listOf(
            candidate("com.social.app", daily = listOf(20L, 25L, 29L)), // avg ~24.6 < 30
        )
        val result = AppLimitCandidateSelector.selectCandidate(candidates, tempo = 1.0)
        assertNull(result)
    }

    @Test
    fun `at or above minimum daily average is eligible`() {
        val candidates = listOf(
            candidate("com.social.app", daily = listOf(30L, 30L, 30L)), // avg 30 == threshold
        )
        val result = AppLimitCandidateSelector.selectCandidate(candidates, tempo = 1.0)
        assertEquals("com.social.app", result?.packageName)
    }

    @Test
    fun `non-eligible category is excluded even with high usage`() {
        val candidates = listOf(
            candidate("com.productivity.app", categoryId = Category.CAT_PRODUCTIVITY, daily = listOf(200L, 200L, 200L)),
        )
        val result = AppLimitCandidateSelector.selectCandidate(candidates, tempo = 1.0)
        assertNull(result)
    }

    @Test
    fun `games and video categories are eligible same as social`() {
        val gameResult = AppLimitCandidateSelector.selectCandidate(
            listOf(candidate("com.game.app", categoryId = Category.CAT_GAMES, daily = listOf(60L, 60L, 60L))),
            tempo = 1.0,
        )
        assertEquals("com.game.app", gameResult?.packageName)

        val videoResult = AppLimitCandidateSelector.selectCandidate(
            listOf(candidate("com.video.app", categoryId = Category.CAT_VIDEO, daily = listOf(60L, 60L, 60L))),
            tempo = 1.0,
        )
        assertEquals("com.video.app", videoResult?.packageName)
    }

    @Test
    fun `highest average usage among eligible candidates wins`() {
        val candidates = listOf(
            candidate("com.social.low", daily = listOf(35L, 35L, 35L)),
            candidate("com.social.high", daily = listOf(100L, 100L, 100L)),
            candidate("com.game.mid", categoryId = Category.CAT_GAMES, daily = listOf(60L, 60L, 60L)),
        )
        val result = AppLimitCandidateSelector.selectCandidate(candidates, tempo = 1.0)
        assertEquals("com.social.high", result?.packageName)
    }

    @Test
    fun `empty daily usage list is excluded`() {
        val candidates = listOf(candidate("com.social.app", daily = emptyList()))
        val result = AppLimitCandidateSelector.selectCandidate(candidates, tempo = 1.0)
        assertNull(result)
    }

    @Test
    fun `target is median times ratio times tempo clamped to min`() {
        // median 40 * 0.8 ratio * 1.0 tempo = 32
        val target = AppLimitCandidateSelector.calculateTarget(listOf(40L, 40L, 40L), tempo = 1.0)
        assertEquals(32L, target)
    }

    @Test
    fun `target never drops below 15 minutes floor`() {
        // median 30 (threshold) * 0.8 * 0.8 (iddiali tempo) = 19.2 -> 19, still above floor
        // Force a very low result: median 20 * 0.8 * 0.8 = 12.8 -> 12 -> clamped to 15
        val target = AppLimitCandidateSelector.calculateTarget(listOf(20L, 20L, 20L), tempo = 0.8)
        assertEquals(AppLimitCandidateSelector.MIN_TARGET_MINUTES, target)
    }

    @Test
    fun `target clamps to screen time max`() {
        val target = AppLimitCandidateSelector.calculateTarget(listOf(1000L, 1000L, 1000L), tempo = 1.0)
        assertEquals(AppLimitCandidateSelector.MAX_TARGET_MINUTES, target)
    }

    @Test
    fun `tempo coefficient scales target down for iddiali`() {
        val rahat = AppLimitCandidateSelector.calculateTarget(listOf(100L, 100L, 100L), tempo = 1.0)
        val iddiali = AppLimitCandidateSelector.calculateTarget(listOf(100L, 100L, 100L), tempo = 0.8)
        assertEquals(80L, rahat) // 100 * 0.8 * 1.0
        assertEquals(64L, iddiali) // 100 * 0.8 * 0.8
    }

    @Test
    fun `empty daily minutes returns null target`() {
        assertNull(AppLimitCandidateSelector.calculateTarget(emptyList(), tempo = 1.0))
    }

    @Test
    fun `selectCandidate combines filter and target calculation end to end`() {
        val candidates = listOf(
            candidate("com.social.app", daily = listOf(60L, 80L, 100L)), // median 80
        )
        val result = AppLimitCandidateSelector.selectCandidate(candidates, tempo = 0.9)
        // median 80 * 0.8 * 0.9 = 57.6 -> 57
        assertEquals("com.social.app", result?.packageName)
        assertEquals(57L, result?.targetMinutes)
    }
}

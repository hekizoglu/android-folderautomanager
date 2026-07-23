package com.armutlu.apporganizer.domain.usecase.folder

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FolderMergeCandidateScorer birim testleri (R3.1).
 * Amac: deterministik siralama, kilitli uygulama disi tutma, esik/guven kurallarinin dogrulanmasi.
 */
class FolderMergeCandidateScorerTest {

    private fun app(
        packageName: String,
        categoryId: String,
        isCategoryLocked: Boolean = false,
        isHidden: Boolean = false,
        isSystemApp: Boolean = false,
    ): AppInfo = AppInfo(
        packageName = packageName,
        appName = packageName,
        categoryId = categoryId,
        isCategoryLocked = isCategoryLocked,
        isHidden = isHidden,
        isSystemApp = isSystemApp,
    )

    @Test
    fun `known static mapping with small source folder produces a merge plan`() {
        val apps = listOf(
            app("com.video.one", Category.CAT_VIDEO),
            app("com.ent.one", Category.CAT_ENTERTAINMENT),
            app("com.ent.two", Category.CAT_ENTERTAINMENT),
        )

        val plans = FolderMergeCandidateScorer.score(apps)

        assertEquals(1, plans.size)
        val plan = plans.first()
        assertEquals(Category.CAT_VIDEO, plan.sourceCategoryId)
        assertEquals(Category.CAT_ENTERTAINMENT, plan.targetCategoryId)
        assertEquals(listOf("com.video.one"), plan.movablePackageNames)
        assertEquals(2, plan.targetAppCount)
        assertEquals(FolderSuggestionReason.STATIC_MAPPING, plan.reason)
    }

    @Test
    fun `unknown category produces no plan - no guessed target`() {
        val apps = listOf(
            app("com.random.one", "totally_unknown_category"),
            app("com.random.two", "another_unknown_category"),
        )

        val plans = FolderMergeCandidateScorer.score(apps)

        assertTrue(plans.isEmpty())
    }

    @Test
    fun `source folder above threshold produces no plan`() {
        val apps = listOf(
            app("com.video.one", Category.CAT_VIDEO),
            app("com.video.two", Category.CAT_VIDEO),
            app("com.video.three", Category.CAT_VIDEO),
            app("com.ent.one", Category.CAT_ENTERTAINMENT),
        )

        val plans = FolderMergeCandidateScorer.score(apps, smallFolderThreshold = 2)

        assertTrue(plans.isEmpty())
    }

    @Test
    fun `locked apps are excluded from movable list but reported separately`() {
        val apps = listOf(
            app("com.video.locked", Category.CAT_VIDEO, isCategoryLocked = true),
            app("com.video.free", Category.CAT_VIDEO),
            app("com.ent.one", Category.CAT_ENTERTAINMENT),
        )

        val plans = FolderMergeCandidateScorer.score(apps)

        assertEquals(1, plans.size)
        val plan = plans.first()
        assertEquals(listOf("com.video.free"), plan.movablePackageNames)
        assertEquals(listOf("com.video.locked"), plan.lockedPackageNames)
        assertTrue(plan.hasLockedApps)
    }

    @Test
    fun `all apps locked in source folder produces no plan`() {
        val apps = listOf(
            app("com.video.locked", Category.CAT_VIDEO, isCategoryLocked = true),
            app("com.ent.one", Category.CAT_ENTERTAINMENT),
        )

        val plans = FolderMergeCandidateScorer.score(apps)

        assertTrue(plans.isEmpty())
    }

    @Test
    fun `hidden and system apps are excluded from scoring`() {
        val apps = listOf(
            app("com.video.hidden", Category.CAT_VIDEO, isHidden = true),
            app("com.video.system", Category.CAT_VIDEO, isSystemApp = true),
            app("com.ent.one", Category.CAT_ENTERTAINMENT),
        )

        val plans = FolderMergeCandidateScorer.score(apps)

        assertTrue(plans.isEmpty())
    }

    @Test
    fun `target folder must exist for a plan to be produced`() {
        val apps = listOf(
            app("com.video.one", Category.CAT_VIDEO),
            // No CAT_ENTERTAINMENT apps present - target folder does not exist.
        )

        val plans = FolderMergeCandidateScorer.score(apps)

        assertTrue(plans.isEmpty())
    }

    @Test
    fun `results are sorted deterministically by confidence desc then source category asc`() {
        val apps = listOf(
            // CAT_DATING -> CAT_SOCIAL: 1 movable app (slack=1, high confidence)
            app("com.dating.one", Category.CAT_DATING),
            app("com.social.one", Category.CAT_SOCIAL),
            // CAT_MAPS -> CAT_TRAVEL: 2 movable apps (slack=0, lower confidence)
            app("com.maps.one", Category.CAT_MAPS),
            app("com.maps.two", Category.CAT_MAPS),
            app("com.travel.one", Category.CAT_TRAVEL),
        )

        val plans = FolderMergeCandidateScorer.score(apps)

        assertEquals(2, plans.size)
        // Higher confidence (fewer movable apps relative to threshold) should sort first.
        assertEquals(Category.CAT_DATING, plans[0].sourceCategoryId)
        assertEquals(Category.CAT_MAPS, plans[1].sourceCategoryId)
        assertTrue(plans[0].confidence >= plans[1].confidence)
    }

    @Test
    fun `scoring is deterministic across repeated calls`() {
        val apps = listOf(
            app("com.video.b", Category.CAT_VIDEO),
            app("com.ent.one", Category.CAT_ENTERTAINMENT),
            app("com.dating.a", Category.CAT_DATING),
            app("com.social.one", Category.CAT_SOCIAL),
        )

        val first = FolderMergeCandidateScorer.score(apps)
        val second = FolderMergeCandidateScorer.score(apps)

        assertEquals(first, second)
    }

    @Test
    fun `minConfidence filters out low confidence plans`() {
        val apps = listOf(
            app("com.maps.one", Category.CAT_MAPS),
            app("com.maps.two", Category.CAT_MAPS),
            app("com.travel.one", Category.CAT_TRAVEL),
        )

        val plansUnfiltered = FolderMergeCandidateScorer.score(apps, minConfidence = 0)
        val plansFiltered = FolderMergeCandidateScorer.score(apps, minConfidence = 100)

        assertEquals(1, plansUnfiltered.size)
        assertTrue(plansFiltered.isEmpty())
    }

    @Test
    fun `custom mergeTargets map overrides static mapping`() {
        val apps = listOf(
            app("com.utils.one", Category.CAT_UTILITIES),
            app("com.biz.one", Category.CAT_BUSINESS),
        )
        val customTargets = mapOf(Category.CAT_UTILITIES to Category.CAT_BUSINESS)

        val plans = FolderMergeCandidateScorer.score(apps, mergeTargets = customTargets)

        assertEquals(1, plans.size)
        assertEquals(Category.CAT_UTILITIES, plans.first().sourceCategoryId)
        assertEquals(Category.CAT_BUSINESS, plans.first().targetCategoryId)
    }
}

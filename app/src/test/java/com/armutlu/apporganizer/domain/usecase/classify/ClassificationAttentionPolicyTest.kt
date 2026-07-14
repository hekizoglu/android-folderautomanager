package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * ClassificationAttentionPolicy birim testleri (P0.2).
 * Amac: sayac ile liste her zaman ayni kumeyi dondursun - her AttentionReason
 * icin 1 senaryo + genel "sayac == liste boyutu" senaryosu.
 */
class ClassificationAttentionPolicyTest {

    private val now = 1_700_000_000_000L

    private fun baseApp(
        packageName: String = "com.example.app",
        categoryId: String = "productivity",
        isHidden: Boolean = false,
        isSystemApp: Boolean = false,
        isCategoryLocked: Boolean = false,
        classificationConfidence: Int = 95,
        classificationReviewState: String = ClassificationReviewState.NOT_REQUIRED.name,
        classificationSource: String = ClassificationSource.BUNDLED_CATALOG.name,
        classificationReason: String = ClassificationReason.EXACT_PACKAGE_MATCH.name,
        reviewSnoozedUntil: Long = 0L,
    ): AppInfo = AppInfo(
        packageName = packageName,
        appName = "Example",
        categoryId = categoryId,
        isHidden = isHidden,
        isSystemApp = isSystemApp,
        isCategoryLocked = isCategoryLocked,
        classificationConfidence = classificationConfidence,
        classificationReviewState = classificationReviewState,
        classificationSource = classificationSource,
        classificationReason = classificationReason,
        reviewSnoozedUntil = reviewSnoozedUntil,
    )

    @Test
    fun `well classified locked app needs no attention`() {
        val app = baseApp(isCategoryLocked = true, classificationConfidence = 100)
        assertNull(ClassificationAttentionPolicy.evaluate(app, now))
    }

    @Test
    fun `uncategorized app returns UNCATEGORIZED`() {
        val app = baseApp(categoryId = Category.CAT_UNCATEGORIZED)
        assertEquals(AttentionReason.UNCATEGORIZED, ClassificationAttentionPolicy.evaluate(app, now))
    }

    @Test
    fun `other category without lock returns OTHER_WITHOUT_CONFIDENCE`() {
        val app = baseApp(
            categoryId = Category.CAT_OTHER,
            classificationSource = ClassificationSource.FALLBACK_OTHER.name,
            classificationConfidence = ClassificationConfidence.FALLBACK_OTHER,
        )
        assertEquals(
            AttentionReason.OTHER_WITHOUT_CONFIDENCE,
            ClassificationAttentionPolicy.evaluate(app, now)
        )
    }

    @Test
    fun `low confidence below threshold returns LOW_CONFIDENCE`() {
        val app = baseApp(
            categoryId = Category.CAT_SOCIAL,
            classificationConfidence = ClassificationConfidence.REVIEW_THRESHOLD - 1,
            classificationSource = ClassificationSource.PACKAGE_NAME_KEYWORD.name,
        )
        assertEquals(AttentionReason.LOW_CONFIDENCE, ClassificationAttentionPolicy.evaluate(app, now))
    }

    @Test
    fun `pending review state without snooze returns REVIEW_PENDING`() {
        val app = baseApp(
            categoryId = Category.CAT_SOCIAL,
            classificationConfidence = 95,
            classificationReviewState = ClassificationReviewState.PENDING.name,
        )
        assertEquals(AttentionReason.REVIEW_PENDING, ClassificationAttentionPolicy.evaluate(app, now))
    }

    @Test
    fun `snoozed pending review is skipped until snooze expires`() {
        val app = baseApp(
            categoryId = Category.CAT_SOCIAL,
            classificationConfidence = 95,
            classificationReviewState = ClassificationReviewState.PENDING.name,
            reviewSnoozedUntil = now + 100_000L,
        )
        assertNull(ClassificationAttentionPolicy.evaluate(app, now))

        val expired = app.copy(reviewSnoozedUntil = now - 1L)
        assertEquals(AttentionReason.REVIEW_PENDING, ClassificationAttentionPolicy.evaluate(expired, now))
    }

    @Test
    fun `blank category returns MISSING_CATEGORY`() {
        val app = baseApp(categoryId = "")
        assertEquals(AttentionReason.MISSING_CATEGORY, ClassificationAttentionPolicy.evaluate(app, now))
    }

    @Test
    fun `unknown source returns CLASSIFIER_CONFLICT`() {
        val app = baseApp(
            categoryId = Category.CAT_SOCIAL,
            classificationConfidence = 95,
            classificationSource = ClassificationSource.UNKNOWN.name,
        )
        assertEquals(AttentionReason.CLASSIFIER_CONFLICT, ClassificationAttentionPolicy.evaluate(app, now))
    }

    @Test
    fun `conflicting signals reason returns CLASSIFIER_CONFLICT`() {
        val app = baseApp(
            categoryId = Category.CAT_SOCIAL,
            classificationConfidence = 95,
            classificationReason = ClassificationReason.CONFLICTING_SIGNALS.name,
        )
        assertEquals(AttentionReason.CLASSIFIER_CONFLICT, ClassificationAttentionPolicy.evaluate(app, now))
    }

    @Test
    fun `hidden and system apps never require attention`() {
        val hidden = baseApp(categoryId = Category.CAT_UNCATEGORIZED, isHidden = true)
        val system = baseApp(categoryId = Category.CAT_UNCATEGORIZED, isSystemApp = true)
        assertNull(ClassificationAttentionPolicy.evaluate(hidden, now))
        assertNull(ClassificationAttentionPolicy.evaluate(system, now))
    }

    @Test
    fun `attention count always matches attention list size across a mixed batch`() {
        val apps = listOf(
            baseApp(packageName = "a1", categoryId = Category.CAT_UNCATEGORIZED),
            baseApp(
                packageName = "a2",
                categoryId = Category.CAT_OTHER,
                classificationSource = ClassificationSource.FALLBACK_OTHER.name,
                classificationConfidence = ClassificationConfidence.FALLBACK_OTHER,
            ),
            baseApp(
                packageName = "a3",
                categoryId = Category.CAT_SOCIAL,
                classificationConfidence = ClassificationConfidence.REVIEW_THRESHOLD - 5,
                classificationSource = ClassificationSource.PACKAGE_NAME_KEYWORD.name,
            ),
            baseApp(
                packageName = "a4",
                categoryId = Category.CAT_SOCIAL,
                classificationReviewState = ClassificationReviewState.PENDING.name,
            ),
            baseApp(packageName = "a5", categoryId = ""),
            baseApp(
                packageName = "a6",
                categoryId = Category.CAT_SOCIAL,
                classificationSource = ClassificationSource.UNKNOWN.name,
            ),
            // Well-classified, locked -> should NOT need attention.
            baseApp(packageName = "a7", categoryId = Category.CAT_PRODUCTIVITY, isCategoryLocked = true, classificationConfidence = 100),
            // Hidden uncategorized -> should NOT need attention.
            baseApp(packageName = "a8", categoryId = Category.CAT_UNCATEGORIZED, isHidden = true),
            // System app in "other" -> should NOT need attention.
            baseApp(packageName = "a9", categoryId = Category.CAT_OTHER, isSystemApp = true),
        )

        val count = ClassificationAttentionPolicy.attentionCount(apps, now)
        val list = ClassificationAttentionPolicy.attentionList(apps, now)
        val grouped = ClassificationAttentionPolicy.attentionApps(apps, now)

        assertEquals(count, list.size)
        assertEquals(count, grouped.values.sumOf { it.size })
        assertEquals(6, count)
    }
}

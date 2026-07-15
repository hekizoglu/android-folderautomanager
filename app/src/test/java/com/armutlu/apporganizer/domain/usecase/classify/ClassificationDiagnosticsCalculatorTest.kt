package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassificationDiagnosticsCalculatorTest {

    private val now = 1_800_000_000_000L

    @Test
    fun `her kullanici uygulamasi tam bir kovaya duser ve toplam uzlasir`() {
        val apps = listOf(
            app("auto", state = ClassificationReviewState.NOT_REQUIRED, confidence = 95),
            app("pending", state = ClassificationReviewState.PENDING),
            app("snoozed", state = ClassificationReviewState.PENDING, snoozedUntil = now + 60_000L),
            app("confirmed", state = ClassificationReviewState.CONFIRMED),
            app("corrected", state = ClassificationReviewState.CORRECTED),
            app("skipped", state = ClassificationReviewState.SKIPPED),
            app("uncategorized", categoryId = Category.CAT_UNCATEGORIZED),
            app("invalid", stateName = "NOT_A_STATE"),
        )

        val result = ClassificationDiagnosticsCalculator.calculate(apps, now)

        assertEquals(8, result.totalUserApps)
        assertEquals(8, result.reconciledTotal)
        assertTrue(result.isConsistent)
        assertEquals(1, result.automaticAccepted)
        assertEquals(1, result.needsAttention)
        assertEquals(1, result.snoozed)
        assertEquals(1, result.confirmed)
        assertEquals(1, result.corrected)
        assertEquals(1, result.skipped)
        assertEquals(1, result.uncategorized)
        assertEquals(1, result.invalidOrUnknown)
    }

    @Test
    fun `sistem uygulamasi siniflandirma toplaminda sayilmaz`() {
        val result = ClassificationDiagnosticsCalculator.calculate(
            listOf(
                app("user", state = ClassificationReviewState.NOT_REQUIRED, confidence = 95),
                app("system", isSystemApp = true, state = ClassificationReviewState.PENDING),
            ),
            now,
        )

        assertEquals(1, result.totalUserApps)
        assertEquals(1, result.reconciledTotal)
        assertEquals(0, result.needsAttention)
    }

    @Test
    fun `gizli kullanici uygulamasi toplamdan dusmez ve ayrica sayilir`() {
        val result = ClassificationDiagnosticsCalculator.calculate(
            listOf(app("hidden", isHidden = true, state = ClassificationReviewState.NOT_REQUIRED, confidence = 95)),
            now,
        )

        assertEquals(1, result.totalUserApps)
        assertEquals(1, result.hiddenUserApps)
        assertEquals(1, result.reconciledTotal)
        assertTrue(result.isConsistent)
    }

    @Test
    fun `aktif snooze dikkat yerine erteleme kovasina gider`() {
        val result = ClassificationDiagnosticsCalculator.calculate(
            listOf(app("snoozed", state = ClassificationReviewState.PENDING, snoozedUntil = now + 60_000L)),
            now,
        )

        assertEquals(1, result.snoozed)
        assertEquals(0, result.needsAttention)
        assertEquals(0, result.attentionByReason.getValue(AttentionReason.REVIEW_PENDING))
    }

    @Test
    fun `suresi bitmis snooze yeniden dikkat gerektirir`() {
        val result = ClassificationDiagnosticsCalculator.calculate(
            listOf(app("expired", state = ClassificationReviewState.PENDING, snoozedUntil = now - 60_000L)),
            now,
        )

        assertEquals(0, result.snoozed)
        assertEquals(1, result.needsAttention)
        assertEquals(1, result.attentionByReason.getValue(AttentionReason.REVIEW_PENDING))
    }

    @Test
    fun `other ve dusuk guven attention policy ile ayni kirilimi verir`() {
        val result = ClassificationDiagnosticsCalculator.calculate(
            listOf(
                app("other", categoryId = Category.CAT_OTHER, state = ClassificationReviewState.NOT_REQUIRED, confidence = 80),
                app("low", categoryId = Category.CAT_SOCIAL, state = ClassificationReviewState.NOT_REQUIRED, confidence = 40),
            ),
            now,
        )

        assertEquals(2, result.needsAttention)
        assertEquals(1, result.attentionByReason.getValue(AttentionReason.OTHER_WITHOUT_CONFIDENCE))
        assertEquals(1, result.attentionByReason.getValue(AttentionReason.LOW_CONFIDENCE))
    }

    @Test
    fun `bos kategori uncategorized kovasina girer`() {
        val result = ClassificationDiagnosticsCalculator.calculate(
            listOf(app("blank", categoryId = "")),
            now,
        )

        assertEquals(1, result.uncategorized)
        assertEquals(1, result.reconciledTotal)
    }

    @Test
    fun `bilinmeyen review state invalid olur ve tutarlilik korunur`() {
        val result = ClassificationDiagnosticsCalculator.calculate(
            listOf(app("invalid", stateName = "WHAT_IS_THIS")),
            now,
        )

        assertEquals(1, result.invalidOrUnknown)
        assertEquals(1, result.reconciledTotal)
        assertTrue(result.isConsistent)
    }

    @Test
    fun `manuel bozuk snapshot tutarlilik uyarisi uretebilir`() {
        val consistent = ClassificationDiagnosticsCalculator.calculate(
            listOf(app("auto", state = ClassificationReviewState.NOT_REQUIRED, confidence = 95)),
            now,
        )

        assertFalse(consistent.copy(reconciledTotal = 0, isConsistent = false).isConsistent)
    }

    private fun app(
        packageName: String,
        categoryId: String = Category.CAT_SOCIAL,
        state: ClassificationReviewState = ClassificationReviewState.PENDING,
        stateName: String = state.name,
        source: ClassificationSource = ClassificationSource.BUNDLED_CATALOG,
        reason: ClassificationReason = ClassificationReason.PACKAGE_NAME_MATCH,
        confidence: Int = 95,
        isSystemApp: Boolean = false,
        isHidden: Boolean = false,
        snoozedUntil: Long = 0L,
    ) = AppInfo(
        packageName = "pkg.$packageName",
        appName = packageName,
        categoryId = categoryId,
        isSystemApp = isSystemApp,
        isHidden = isHidden,
        classificationSource = source.name,
        classificationReason = reason.name,
        classificationConfidence = confidence,
        classificationReviewState = stateName,
        reviewSnoozedUntil = snoozedUntil,
    )
}

package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category

internal data class ClassificationDiagnostics(
    val totalUserApps: Int,
    val hiddenUserApps: Int,
    val automaticAccepted: Int,
    val needsAttention: Int,
    val snoozed: Int,
    val confirmed: Int,
    val corrected: Int,
    val skipped: Int,
    val uncategorized: Int,
    val invalidOrUnknown: Int,
    val reconciledTotal: Int,
    val isConsistent: Boolean,
    val attentionByReason: Map<AttentionReason, Int>,
)

internal object ClassificationDiagnosticsCalculator {

    fun calculate(apps: List<AppInfo>, now: Long = System.currentTimeMillis()): ClassificationDiagnostics {
        val attentionByReason = linkedMapOf<AttentionReason, Int>().apply {
            AttentionReason.entries.forEach { put(it, 0) }
        }
        var hiddenUserApps = 0
        var automaticAccepted = 0
        var needsAttention = 0
        var snoozed = 0
        var confirmed = 0
        var corrected = 0
        var skipped = 0
        var uncategorized = 0
        var invalidOrUnknown = 0

        val userApps = apps.filterNot { it.isSystemApp }
        userApps.forEach { app ->
            if (app.isHidden) hiddenUserApps += 1

            val reviewState = runCatching {
                ClassificationReviewState.valueOf(app.classificationReviewState)
            }.getOrNull()
            val source = runCatching {
                ClassificationSource.valueOf(app.classificationSource)
            }.getOrNull()
            val reason = runCatching {
                ClassificationReason.valueOf(app.classificationReason)
            }.getOrNull()

            val attentionReason = ClassificationAttentionPolicy.evaluate(app, now)
            if (attentionReason != null) {
                attentionByReason[attentionReason] = attentionByReason.getValue(attentionReason) + 1
            }

            when {
                reviewState == null || source == null || reason == null -> invalidOrUnknown += 1
                app.categoryId.isBlank() || app.categoryId == Category.CAT_UNCATEGORIZED -> uncategorized += 1
                app.reviewSnoozedUntil > now -> snoozed += 1
                reviewState == ClassificationReviewState.CONFIRMED -> confirmed += 1
                reviewState == ClassificationReviewState.CORRECTED -> corrected += 1
                reviewState == ClassificationReviewState.SKIPPED -> skipped += 1
                attentionReason != null -> needsAttention += 1
                else -> automaticAccepted += 1
            }
        }

        val reconciledTotal = automaticAccepted +
            needsAttention +
            snoozed +
            confirmed +
            corrected +
            skipped +
            uncategorized +
            invalidOrUnknown

        return ClassificationDiagnostics(
            totalUserApps = userApps.size,
            hiddenUserApps = hiddenUserApps,
            automaticAccepted = automaticAccepted,
            needsAttention = needsAttention,
            snoozed = snoozed,
            confirmed = confirmed,
            corrected = corrected,
            skipped = skipped,
            uncategorized = uncategorized,
            invalidOrUnknown = invalidOrUnknown,
            reconciledTotal = reconciledTotal,
            isConsistent = reconciledTotal == userApps.size,
            attentionByReason = attentionByReason,
        )
    }
}

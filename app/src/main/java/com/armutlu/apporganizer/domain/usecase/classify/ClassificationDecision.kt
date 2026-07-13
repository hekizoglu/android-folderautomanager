package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.Category

const val CLASSIFICATION_ENGINE_VERSION = 2

enum class ClassificationSource {
    USER_CONFIRMED,
    USER_CORRECTED,
    BUNDLED_CATALOG,
    REMOTE_CATALOG,
    ANDROID_CATEGORY,
    MANUFACTURER_RULE,
    APP_NAME_KEYWORD,
    PACKAGE_NAME_KEYWORD,
    LLM_LEGACY,
    FALLBACK_OTHER,
    UNKNOWN,
}

enum class ClassificationReason {
    USER_SELECTION,
    EXACT_PACKAGE_MATCH,
    UPDATED_CATALOG_MATCH,
    ANDROID_DECLARED_CATEGORY,
    MANUFACTURER_PACKAGE_MATCH,
    APP_NAME_MATCH,
    PACKAGE_NAME_MATCH,
    LEGACY_AI_RESULT,
    NO_RELIABLE_MATCH,
    CONFLICTING_SIGNALS,
}

enum class ClassificationReviewState {
    NOT_REQUIRED,
    PENDING,
    CONFIRMED,
    CORRECTED,
    SKIPPED,
}

data class ClassificationDecision(
    val categoryId: String,
    val confidence: Int,
    val source: ClassificationSource,
    val reasonCode: ClassificationReason,
    val requiresReview: Boolean,
    val reviewState: ClassificationReviewState,
    val engineVersion: Int = CLASSIFICATION_ENGINE_VERSION,
)

object ClassificationConfidence {
    const val USER_DECISION = 100
    const val REMOTE_CATALOG_EXACT = 97
    const val BUNDLED_CATALOG_EXACT = 95
    const val ANDROID_CATEGORY = 85
    const val MANUFACTURER_RULE = 78
    const val APP_NAME_KEYWORD = 72
    const val PACKAGE_NAME_KEYWORD = 65
    const val LLM_LEGACY = 65
    const val FALLBACK_OTHER = 30
    const val REVIEW_THRESHOLD = 70

    fun clampAutomatic(value: Int): Int = value.coerceIn(0, 99)
    fun clampUser(value: Int): Int = value.coerceIn(0, 100)
}

object ClassificationReviewPolicy {
    fun resolve(
        categoryId: String,
        confidence: Int,
        source: ClassificationSource,
        conflictingSignals: Boolean = false,
    ): Pair<Boolean, ClassificationReviewState> {
        val trustedUserDecision = source == ClassificationSource.USER_CONFIRMED ||
            source == ClassificationSource.USER_CORRECTED
        val requiresReview = !trustedUserDecision && (
            confidence < ClassificationConfidence.REVIEW_THRESHOLD ||
                categoryId == Category.CAT_OTHER ||
                categoryId == Category.CAT_UNCATEGORIZED ||
                source == ClassificationSource.UNKNOWN ||
                conflictingSignals
            )
        return requiresReview to if (requiresReview) {
            ClassificationReviewState.PENDING
        } else {
            ClassificationReviewState.NOT_REQUIRED
        }
    }
}

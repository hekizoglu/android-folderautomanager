package com.armutlu.apporganizer.domain.advice

import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.common.DataFreshness

/**
 * P7 — roadmap §11. Saf Kotlin, Android/Room/Context bağımlılığı yok — unit test edilebilir
 * ([AdaptiveCategoryTargetCalculator][com.armutlu.apporganizer.domain.usecase.goals.AdaptiveCategoryTargetCalculator]
 * ile aynı stil). Aynı anda en fazla 1 ana tavsiye üretir — [evaluate] öncelik sırasını (§11.2,
 * 8 seviye) dener, ilk eşleşen döner.
 *
 * Görev ile TAVSİYE farklıdır (roadmap §11): ölçülebilir/tamamlanabilir/yıldız veren şey görev,
 * bu motor yalnız açıklama+öneri üretir — puan vermez.
 */
object DigitalAdviceEngine {

    /** roadmap §11.1: anlamlı artış/azalış için HEM yüzde HEM mutlak eşik aşılmalı. */
    const val SIGNIFICANT_CHANGE_PERCENT_THRESHOLD = 0.20f
    const val SIGNIFICANT_CHANGE_MINUTES_THRESHOLD = 30L

    const val NOTIFICATION_NOISE_SHARE_THRESHOLD = 0.40f
    const val USAGE_PATTERN_DAY_THRESHOLD = 3
    const val UNUSED_APP_COUNT_THRESHOLD = 5
    const val UNCATEGORIZED_APP_COUNT_THRESHOLD = 3

    fun evaluate(input: DigitalAdviceInput, nowMillis: Long): DigitalAdvice? {
        // 1. Kullanım izni/veri sorunu — her şeyin önünde (roadmap §11.2 sıra 1).
        if (input.permissionFreshness == DataFreshness.UNAVAILABLE) {
            return DigitalAdvice(
                id = "permission_issue",
                type = DigitalAdviceType.PERMISSION_ISSUE,
                priority = 1,
                titleRes = R.string.advice_permission_issue_title,
                messageRes = R.string.advice_permission_issue_message,
                action = DigitalAdviceAction.OpenUsageAccessSettings,
                actionLabelRes = R.string.advice_action_grant_access,
                suggestionKey = "advice_permission_issue",
                createdAt = nowMillis,
            )
        }

        // 2. Hedefin fiilen aşılması.
        if (input.exceededCategoryGoalCount > 0) {
            return DigitalAdvice(
                id = "goal_exceeded",
                type = DigitalAdviceType.GOAL_EXCEEDED,
                priority = 2,
                titleRes = R.string.advice_goal_exceeded_title,
                messageRes = R.string.advice_goal_exceeded_message,
                messageArgs = listOf(input.exceededCategoryGoalCount),
                action = DigitalAdviceAction.OpenCategoryGoals,
                actionLabelRes = R.string.advice_action_open_category_goals,
                suggestionKey = "advice_goal_exceeded",
                createdAt = nowMillis,
                expiresAt = nowMillis + MS_PER_DAY,
            )
        }

        // 3. Haftalık projeksiyona göre yüksek aşım riski.
        if (input.atRiskCategoryGoalCount > 0) {
            return DigitalAdvice(
                id = "projected_overage_risk",
                type = DigitalAdviceType.PROJECTED_OVERAGE_RISK,
                priority = 3,
                titleRes = R.string.advice_projected_overage_title,
                messageRes = R.string.advice_projected_overage_message,
                messageArgs = listOf(input.atRiskCategoryGoalCount),
                action = DigitalAdviceAction.OpenCategoryGoals,
                actionLabelRes = R.string.advice_action_open_category_goals,
                suggestionKey = "advice_projected_overage_risk",
                createdAt = nowMillis,
                expiresAt = nowMillis + MS_PER_DAY,
            )
        }

        // 4. Anlamlı kullanım artışı (HEM %20 HEM 30dk eşiği — küçük değişim büyük olay gibi
        // sunulmaz, roadmap §11.1). Birden fazla kategori eşiği geçerse en büyük mutlak fark seçilir.
        val significantIncrease = input.categoryUsageChanges
            .filter { it.percentChange >= SIGNIFICANT_CHANGE_PERCENT_THRESHOLD }
            .filter { it.absoluteChangeMinutes >= SIGNIFICANT_CHANGE_MINUTES_THRESHOLD }
            .maxByOrNull { it.absoluteChangeMinutes }
        if (significantIncrease != null) {
            return DigitalAdvice(
                id = "significant_usage_increase",
                type = DigitalAdviceType.SIGNIFICANT_USAGE_INCREASE,
                priority = 4,
                titleRes = R.string.advice_usage_increase_title,
                messageRes = R.string.advice_usage_increase_message,
                messageArgs = listOf(significantIncrease.categoryNameRes),
                evidenceRes = R.string.advice_usage_increase_evidence,
                evidenceArgs = listOf((significantIncrease.percentChange * 100).toInt()),
                action = DigitalAdviceAction.OpenUsageReport,
                actionLabelRes = R.string.advice_action_open_report,
                suggestionKey = "advice_usage_increase",
                createdAt = nowMillis,
                expiresAt = nowMillis + MS_PER_DAY,
            )
        }

        // 5. Bildirim gürültüsü.
        val noiseShare = input.notificationNoiseTopSourceShare
        if (noiseShare != null && noiseShare >= NOTIFICATION_NOISE_SHARE_THRESHOLD) {
            return DigitalAdvice(
                id = "notification_noise",
                type = DigitalAdviceType.NOTIFICATION_NOISE,
                priority = 5,
                titleRes = R.string.advice_notification_noise_title,
                messageRes = R.string.advice_notification_noise_message,
                messageArgs = listOf((noiseShare * 100).toInt()),
                action = DigitalAdviceAction.OpenNotificationReport,
                actionLabelRes = R.string.advice_action_open_report,
                suggestionKey = "advice_notification_noise",
                createdAt = nowMillis,
                expiresAt = nowMillis + MS_PER_DAY,
                sensitive = true,
            )
        }

        // 6. Sabah/gece kullanım paterni.
        val morningDays = input.morningSocialOpenDaysLast7
        val nightDays = input.lateNightUsageDaysLast7
        if (morningDays != null && morningDays >= USAGE_PATTERN_DAY_THRESHOLD) {
            return DigitalAdvice(
                id = "usage_pattern_morning",
                type = DigitalAdviceType.USAGE_PATTERN,
                priority = 6,
                titleRes = R.string.advice_pattern_morning_title,
                messageRes = R.string.advice_pattern_morning_message,
                messageArgs = listOf(morningDays),
                action = DigitalAdviceAction.OpenFocusSettings,
                actionLabelRes = R.string.advice_action_open_focus_settings,
                suggestionKey = "advice_pattern_morning",
                createdAt = nowMillis,
                expiresAt = nowMillis + MS_PER_DAY,
            )
        }
        if (nightDays != null && nightDays >= USAGE_PATTERN_DAY_THRESHOLD) {
            return DigitalAdvice(
                id = "usage_pattern_night",
                type = DigitalAdviceType.USAGE_PATTERN,
                priority = 6,
                titleRes = R.string.advice_pattern_night_title,
                messageRes = R.string.advice_pattern_night_message,
                messageArgs = listOf(nightDays),
                action = DigitalAdviceAction.OpenFocusSettings,
                actionLabelRes = R.string.advice_action_open_focus_settings,
                suggestionKey = "advice_pattern_night",
                createdAt = nowMillis,
                expiresAt = nowMillis + MS_PER_DAY,
            )
        }

        // 7. Kullanılmayan uygulama veya kategorisiz uygulama.
        if (input.unusedAppCount >= UNUSED_APP_COUNT_THRESHOLD) {
            return DigitalAdvice(
                id = "unused_apps",
                type = DigitalAdviceType.UNUSED_OR_UNCATEGORIZED_APPS,
                priority = 7,
                titleRes = R.string.advice_unused_apps_title,
                messageRes = R.string.advice_unused_apps_message,
                messageArgs = listOf(input.unusedAppCount),
                action = DigitalAdviceAction.OpenUsageReport,
                actionLabelRes = R.string.advice_action_open_report,
                suggestionKey = "advice_unused_apps",
                createdAt = nowMillis,
                expiresAt = nowMillis + MS_PER_WEEK,
            )
        }
        if (input.uncategorizedAppCount >= UNCATEGORIZED_APP_COUNT_THRESHOLD) {
            return DigitalAdvice(
                id = "uncategorized_apps",
                type = DigitalAdviceType.UNUSED_OR_UNCATEGORIZED_APPS,
                priority = 7,
                titleRes = R.string.advice_uncategorized_apps_title,
                messageRes = R.string.advice_uncategorized_apps_message,
                messageArgs = listOf(input.uncategorizedAppCount),
                action = DigitalAdviceAction.OpenClassificationReview,
                actionLabelRes = R.string.advice_action_review,
                suggestionKey = "advice_uncategorized_apps",
                createdAt = nowMillis,
                expiresAt = nowMillis + MS_PER_WEEK,
            )
        }

        // 8. Olumlu gelişme/pekiştirme — sayı uydurulmadan sadece gerçek "her şey planında" sinyaliyle.
        if (input.hasAnyCategoryGoal && input.allCategoryGoalsOnTrack) {
            return DigitalAdvice(
                id = "positive_all_on_track",
                type = DigitalAdviceType.POSITIVE_REINFORCEMENT,
                priority = 8,
                titleRes = R.string.advice_positive_all_on_track_title,
                messageRes = R.string.advice_positive_all_on_track_message,
                action = DigitalAdviceAction.OpenCategoryGoals,
                actionLabelRes = R.string.advice_action_open_category_goals,
                suggestionKey = "advice_positive_all_on_track",
                createdAt = nowMillis,
                expiresAt = nowMillis + MS_PER_DAY,
            )
        }

        return null
    }

    private const val MS_PER_DAY = 24L * 60 * 60 * 1000
    private const val MS_PER_WEEK = 7 * MS_PER_DAY
}

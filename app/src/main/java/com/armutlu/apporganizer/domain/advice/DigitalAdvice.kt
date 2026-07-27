package com.armutlu.apporganizer.domain.advice

/**
 * P7 — [DigitalAdviceEngine] çıktısı. Roadmap §11: tamamlanması zorunlu değil, puan/yıldız
 * vermez, suçlayıcı dil kullanmaz. Metinler burada HARDCODE EDİLMEZ — [titleRes]/[messageRes]
 * string resource ID'leri taşır, argümanlar [messageArgs]'ta ayrı tutulur (MissionTextSpec
 * deseniyle aynı felsefe — UI katmanı `context.getString(messageRes, *messageArgs)` ile çözer).
 */
data class DigitalAdvice(
    val id: String,
    val type: DigitalAdviceType,
    val priority: Int,
    val titleRes: Int,
    val messageRes: Int,
    val messageArgs: List<Any> = emptyList(),
    /** Kanıt satırı (ör. "%18 artış", "12 uygulama") — ayrı string olarak taşınır, mesajla birleştirilmez. */
    val evidenceRes: Int? = null,
    val evidenceArgs: List<Any> = emptyList(),
    val action: DigitalAdviceAction = DigitalAdviceAction.None,
    val actionLabelRes: Int? = null,
    /** [SuggestionCoordinator]/[TickerRanker] suppression'ı için tekil anahtar (roadmap §12). */
    val suggestionKey: String,
    val createdAt: Long,
    val expiresAt: Long? = null,
    val sensitive: Boolean = false,
)

/** Roadmap §11 8-seviye öncelik sırasının tür karşılığı — [DigitalAdviceEngine] bu sırayla dener. */
enum class DigitalAdviceType {
    PERMISSION_ISSUE,
    GOAL_EXCEEDED,
    PROJECTED_OVERAGE_RISK,
    SIGNIFICANT_USAGE_INCREASE,
    NOTIFICATION_NOISE,
    USAGE_PATTERN,
    UNUSED_OR_UNCATEGORIZED_APPS,
    POSITIVE_REINFORCEMENT,
}

/** Route stringlerini domain içine yaymadan (roadmap §11.4) tipli eylem. */
sealed interface DigitalAdviceAction {
    data object OpenCategoryGoals : DigitalAdviceAction
    data object OpenMissions : DigitalAdviceAction
    data object OpenNotificationReport : DigitalAdviceAction
    data object OpenUsageReport : DigitalAdviceAction
    data object OpenClassificationReview : DigitalAdviceAction
    data object OpenFocusSettings : DigitalAdviceAction
    data object OpenUsageAccessSettings : DigitalAdviceAction
    data object None : DigitalAdviceAction
}

package com.armutlu.apporganizer.domain.models

/**
 * Device-local smart notification categories.
 *
 * defaultImportance is only the classifier base score. The final score is calculated
 * from content, package, urgency, security and Android priority.
 */
enum class NotificationCategory(
    val defaultImportance: Int,
    val suppressible: Boolean = false,
) {
    MESSAGING(defaultImportance = 60),
    DELIVERY(defaultImportance = 58),
    FINANCE(defaultImportance = 78),
    PROMOTION(defaultImportance = 15, suppressible = true),
    REMINDER(defaultImportance = 64),
    SOCIAL(defaultImportance = 40),
    SYSTEM(defaultImportance = 50),
    MISSED_CALL(defaultImportance = 82),
    CALENDAR(defaultImportance = 64),
    MARKET(defaultImportance = 68),
    FAMILY(defaultImportance = 76),
    NEWS(defaultImportance = 38, suppressible = true),
    MEDIA(defaultImportance = 32, suppressible = true),
    UPDATE(defaultImportance = 42, suppressible = true),
    OTHER(defaultImportance = 35),
}

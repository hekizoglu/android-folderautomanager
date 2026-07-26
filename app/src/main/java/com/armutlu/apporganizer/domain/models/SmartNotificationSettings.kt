package com.armutlu.apporganizer.domain.models

enum class NotificationBadgeMode {
    CLASSIC_APP,
    CATEGORY,
}

data class SmartNotificationSettings(
    val engineEnabled: Boolean,
    val filterPromotions: Boolean,
    val hideSensitiveContent: Boolean,
    val visibleCategories: Set<NotificationCategory>,
    val badgeMode: NotificationBadgeMode,
) {
    companion object {
        fun defaults(engineEnabled: Boolean): SmartNotificationSettings =
            SmartNotificationSettings(
                engineEnabled = engineEnabled,
                filterPromotions = true,
                hideSensitiveContent = true,
                visibleCategories = NotificationCategory.entries.toSet(),
                badgeMode = NotificationBadgeMode.CLASSIC_APP,
            )
    }
}

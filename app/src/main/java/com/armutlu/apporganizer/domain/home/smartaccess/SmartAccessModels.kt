package com.armutlu.apporganizer.domain.home.smartaccess

import com.armutlu.apporganizer.domain.models.AppInfo

enum class SmartAccessTab { NOW, RECENT, NOTIFICATIONS }

data class NotificationAccessItem(
    val app: AppInfo,
    val count: Int,
    val lastPostedAt: Long,
)

data class SmartAccessUiState(
    val selectedTab: SmartAccessTab = SmartAccessTab.NOW,
    val nowApps: List<AppInfo> = emptyList(),
    val recentApps: List<AppInfo> = emptyList(),
    val notificationApps: List<NotificationAccessItem> = emptyList(),
    val notificationTotal: Int = 0,
    val usagePermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val loading: Boolean = true,
)

data class SmartAccessCandidate(
    val app: AppInfo,
    val sameTimeSlotScore: Float,
    val recencyScore: Float,
    val frequencyScore: Float,
    val weekdayContextScore: Float,
)

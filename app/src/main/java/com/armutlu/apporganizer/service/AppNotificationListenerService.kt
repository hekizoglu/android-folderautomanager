package com.armutlu.apporganizer.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aktif bildirimleri sayar ve package -> count map'i yayınlar.
 * LauncherViewModel bu flow'u dinleyerek AppInfo.notificationCount'u günceller.
 */
class AppNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        rebuildCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        rebuildCounts()
    }

    override fun onListenerConnected() {
        rebuildCounts()
    }

    private fun rebuildCounts() {
        val counts = mutableMapOf<String, Int>()
        runCatching {
            activeNotifications?.forEach { sbn ->
                if (!sbn.isOngoing) {
                    counts[sbn.packageName] = (counts[sbn.packageName] ?: 0) + 1
                }
            }
        }
        _badgeCounts.value = counts
    }

    companion object {
        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()
    }
}

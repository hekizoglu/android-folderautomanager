package com.armutlu.apporganizer.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Aktif bildirimleri sayar ve son bildirim metnini (package -> text) yayinlar.
 * LauncherViewModel bu flow'u dinleyerek AppInfo.notificationCount ve notificationText'i gunceller.
 */
class AppNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        runCatching {
            if (!sbn.isOngoing) {
                val extras = sbn.notification?.extras
                val title = extras?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() ?: ""
                val text = extras?.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() ?: ""
                val combined = when {
                    title.isNotBlank() && text.isNotBlank() -> "$title: $text"
                    title.isNotBlank() -> title
                    else -> text
                }
                // atomic update — race condition'u önler
                _badgeCounts.update { counts ->
                    counts + (sbn.packageName to ((counts[sbn.packageName] ?: 0) + 1))
                }
                if (combined.isNotBlank()) {
                    _latestTexts.update { current -> current + (sbn.packageName to combined) }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        runCatching {
            sbn?.packageName?.let { pkg ->
                // Rebuild badge counts atomically from system source of truth
                rebuildCounts()
                val hasActive = activeNotifications?.any { it.packageName == pkg && !it.isOngoing } == true
                if (!hasActive) {
                    _latestTexts.update { current ->
                        if (current.containsKey(pkg)) current - pkg else current
                    }
                }
            }
        }
    }

    override fun onListenerConnected() {
        rebuildCounts()
    }

    // Servis bağlantısı kesilince (sistem yeniden başlatma, izin iptali vs.)
    // stale badge ve metin verilerini temizle.
    override fun onListenerDisconnected() {
        _badgeCounts.value = emptyMap()
        _latestTexts.value = emptyMap()
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

        private val _latestTexts = MutableStateFlow<Map<String, String>>(emptyMap())
        val latestTexts: StateFlow<Map<String, String>> = _latestTexts.asStateFlow()
    }
}

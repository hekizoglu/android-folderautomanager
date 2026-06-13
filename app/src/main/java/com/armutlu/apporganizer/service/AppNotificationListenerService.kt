package com.armutlu.apporganizer.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aktif bildirimleri sayar ve son bildirim metnini (package -> text) yayinlar.
 * LauncherViewModel bu flow'u dinleyerek AppInfo.notificationCount ve notificationText'i gunceller.
 */
class AppNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        runCatching {
            rebuildCounts()
            if (!sbn.isOngoing) {
                val extras = sbn.notification?.extras
                val title = extras?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() ?: ""
                val text = extras?.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() ?: ""
                val combined = when {
                    title.isNotBlank() && text.isNotBlank() -> "$title: $text"
                    title.isNotBlank() -> title
                    else -> text
                }
                if (combined.isNotBlank()) {
                    val current = _latestTexts.value.toMutableMap()
                    current[sbn.packageName] = combined
                    _latestTexts.value = current
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        runCatching {
            rebuildCounts()
            sbn?.packageName?.let { pkg ->
                val hasActive = activeNotifications?.any { it.packageName == pkg && !it.isOngoing } == true
                if (!hasActive) {
                    val current = _latestTexts.value.toMutableMap()
                    if (current.remove(pkg) != null) {
                        _latestTexts.value = current
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

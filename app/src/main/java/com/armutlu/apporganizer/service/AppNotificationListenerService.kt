package com.armutlu.apporganizer.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.domain.models.NotificationEvent
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Aktif bildirimleri sayar (badge) ve — yalnızca Ayarlar'dan "bildirim metni göster"
 * açıkken — son bildirim metnini (package -> text) yayınlar; bu metin LauncherViewModel
 * tarafından `apps.notificationText` sütununa kalıcı yazılır (varsayılan: KAPALI).
 *
 * Ayrıca her bildirim `notification_events` tablosuna loglanır (Bildirim Analiz Raporu):
 * yalnızca paket adı + zaman damgası — bu tabloya içerik saklanmaz, veri cihazda kalır.
 */
@AndroidEntryPoint
class AppNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var notificationEventDao: NotificationEventDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val knownNotificationKeys = LinkedHashSet<String>()

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
                knownNotificationKeys += sbn.key
                rebuildCounts()
                // Gizlilik: bildirim metni yalnızca ayar açıkken yayınlanır/DB'ye yazılır (varsayılan kapalı)
                if (combined.isNotBlank() && AppPrefs.isNotificationTextEnabled(this)) {
                    _latestTexts.update { current -> current + (sbn.packageName to combined) }
                }
                // Bildirim analizi — yalnızca paket + zaman kaydı (Ayarlar'dan kapatılabilir)
                if (AppPrefs.isNotifAnalyticsEnabled(this)) {
                    serviceScope.launch {
                        runCatching {
                            notificationEventDao.insert(
                                NotificationEvent(packageName = sbn.packageName, postedAt = System.currentTimeMillis())
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        runCatching {
            sbn?.packageName?.let { pkg ->
                sbn.key?.let { knownNotificationKeys.remove(it) }
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
        // 30 günden eski analiz kayıtlarını temizle — bağlantı başına bir kez yeterli
        serviceScope.launch {
            runCatching {
                notificationEventDao.deleteOlderThan(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
            }
        }
    }

    // Servis bağlantısı kesilince (sistem yeniden başlatma, izin iptali vs.)
    // stale badge ve metin verilerini temizle.
    override fun onListenerDisconnected() {
        knownNotificationKeys.clear()
        _badgeCounts.value = emptyMap()
        _latestTexts.value = emptyMap()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun rebuildCounts() {
        knownNotificationKeys.clear()
        val counts = mutableMapOf<String, Int>()
        runCatching {
            activeNotifications?.forEach { sbn ->
                if (!sbn.isOngoing) {
                    knownNotificationKeys += sbn.key
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

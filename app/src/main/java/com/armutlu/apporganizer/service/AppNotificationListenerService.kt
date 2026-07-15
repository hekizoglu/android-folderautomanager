package com.armutlu.apporganizer.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.domain.models.NotificationEvent
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var notificationEventDao: NotificationEventDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val knownNotificationKeys = LinkedHashSet<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        runCatching {
            if (!sbn.isOngoing) {
                knownNotificationKeys += sbn.key
                rebuildCounts()
                _lastPostedAt.update { current -> current + (sbn.packageName to System.currentTimeMillis()) }
                updatePreviewState()
                if (AppPrefs.isNotifAnalyticsEnabled(this)) {
                    serviceScope.launch {
                        runCatching {
                            notificationEventDao.insert(
                                NotificationEvent(
                                    packageName = sbn.packageName,
                                    postedAt = System.currentTimeMillis(),
                                )
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
                _previewItems.update { current -> NotificationPreviewStore.removePreview(current, pkg, sbn.key) }
                updatePreviewState()
            }
        }
    }

    override fun onListenerConnected() {
        rebuildCounts()
        updatePreviewState()
        serviceScope.launch {
            runCatching {
                notificationEventDao.deleteOlderThan(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
            }
        }
    }

    override fun onListenerDisconnected() {
        knownNotificationKeys.clear()
        _badgeCounts.value = emptyMap()
        _latestTexts.value = emptyMap()
        _previewItems.value = emptyMap()
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

    private fun updatePreviewState() {
        val counts = _badgeCounts.value
        val showContent = AppPrefs.isNotificationTextEnabled(this)
        val blockedPackages = AppPrefs.getNotificationPreviewBlockedPackages(this)
        val rebuilt = linkedMapOf<String, List<NotificationPreview>>()
        runCatching {
            activeNotifications?.forEach { sbn ->
                if (!sbn.isOngoing) {
                    val packageName = sbn.packageName
                    if ((counts[packageName] ?: 0) <= 0) return@forEach
                    val preview = if (showContent && packageName !in blockedPackages) {
                        NotificationPreviewStore.extractPreview(sbn)
                    } else {
                        null
                    }
                    if (preview != null) {
                        val current = rebuilt[packageName].orEmpty()
                        rebuilt[packageName] = (current + preview)
                            .sortedByDescending { it.postedAt }
                            .take(2)
                    } else if (!rebuilt.containsKey(packageName)) {
                        rebuilt[packageName] = emptyList()
                    }
                }
            }
        }
        _previewItems.value = rebuilt
        _latestTexts.value = counts.mapValues { (pkg, count) ->
            NotificationPreviewStore.summarize(
                previews = rebuilt[pkg].orEmpty(),
                count = count,
                showContent = showContent && pkg !in blockedPackages,
            )
        }.filterValues { it.isNotBlank() }
    }

    companion object {
        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

        private val _latestTexts = MutableStateFlow<Map<String, String>>(emptyMap())
        val latestTexts: StateFlow<Map<String, String>> = _latestTexts.asStateFlow()

        private val _previewItems = MutableStateFlow<Map<String, List<NotificationPreview>>>(emptyMap())
        val previewItems: StateFlow<Map<String, List<NotificationPreview>>> = _previewItems.asStateFlow()

        private val _lastPostedAt = MutableStateFlow<Map<String, Long>>(emptyMap())
        val lastPostedAt: StateFlow<Map<String, Long>> = _lastPostedAt.asStateFlow()
    }
}

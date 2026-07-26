package com.armutlu.apporganizer.service

import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.NotificationEvent
import com.armutlu.apporganizer.domain.models.SmartNotification
import com.armutlu.apporganizer.domain.usecase.notification.NotificationClassifierUseCase
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
    @Inject lateinit var appDao: com.armutlu.apporganizer.data.local.AppDao
    @Inject lateinit var notificationClassifier: NotificationClassifierUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val knownNotificationKeys = LinkedHashSet<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        runCatching {
            if (!sbn.isOngoing) {
                knownNotificationKeys += sbn.key
                rebuildCounts()
                _lastPostedAt.update { current ->
                    current + (sbn.packageName to System.currentTimeMillis())
                }
                updatePreviewState()
                if (AppPrefs.isNotifAnalyticsEnabled(this)) {
                    serviceScope.launch {
                        runCatching {
                            val importance = notificationPriority(sbn)
                            val timestamp = System.currentTimeMillis()
                            appDao.updateNotificationImportance(sbn.packageName, importance)
                            appDao.updateLastNotificationPostedAt(sbn.packageName, timestamp)
                            notificationEventDao.insert(
                                NotificationEvent(
                                    packageName = sbn.packageName,
                                    postedAt = timestamp,
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
                _previewItems.update { current ->
                    NotificationPreviewStore.removePreview(current, pkg, sbn.key)
                }
                updatePreviewState()
            }
        }
    }

    override fun onListenerConnected() {
        rebuildCounts()
        updatePreviewState()
        serviceScope.launch {
            runCatching {
                notificationEventDao.deleteOlderThan(
                    System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                )
            }
        }
    }

    override fun onListenerDisconnected() {
        knownNotificationKeys.clear()
        _badgeCounts.value = emptyMap()
        _latestTexts.value = emptyMap()
        _previewItems.value = emptyMap()
        _smartNotifications.value = emptyList()
        _smartBadgeCounts.value = emptyMap()
        _categoryCounts.value = emptyMap()
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

    /**
     * Tek aktif bildirim taramasından hem eski önizleme akışını hem yeni akıllı modeli üretir.
     * Eski [badgeCounts] davranışı korunur; promosyonları dışlayan sayı [smartBadgeCounts] ile
     * ayrı yayınlanır. UI geçişi tamamlanana kadar geriye dönük uyumluluk bozulmaz.
     */
    private fun updatePreviewState() {
        val counts = _badgeCounts.value
        val showContent = AppPrefs.isNotificationTextEnabled(this)
        val blockedPackages = AppPrefs.getNotificationPreviewBlockedPackages(this)
        val rebuilt = linkedMapOf<String, List<NotificationPreview>>()
        val smartRebuilt = linkedMapOf<String, SmartNotification>()

        runCatching {
            activeNotifications?.forEach { sbn ->
                if (!sbn.isOngoing) {
                    val packageName = sbn.packageName
                    if ((counts[packageName] ?: 0) <= 0) return@forEach

                    // İçerik sınıflandırma için cihaz içinde okunur; yalnız gösterim tercihi açıksa
                    // preview StateFlow'una eklenir. Metin DB'ye yazılmaz.
                    val extractedPreview = NotificationPreviewStore.extractPreview(sbn)
                    val visiblePreview = extractedPreview?.takeIf {
                        showContent && packageName !in blockedPackages
                    }

                    if (visiblePreview != null) {
                        val current = rebuilt[packageName].orEmpty()
                        rebuilt[packageName] = (current + visiblePreview)
                            .sortedByDescending { it.postedAt }
                            .take(2)
                    } else if (!rebuilt.containsKey(packageName)) {
                        rebuilt[packageName] = emptyList()
                    }

                    val timestamp = extractedPreview?.postedAt
                        ?: sbn.postTime.takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                    smartRebuilt[sbn.key] = notificationClassifier.classify(
                        key = sbn.key,
                        packageName = packageName,
                        title = "",
                        text = extractedPreview?.text.orEmpty(),
                        timestamp = timestamp,
                        systemPriority = notificationPriority(sbn),
                    )
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

        val ranked = smartRebuilt.values.sortedWith(
            compareByDescending<SmartNotification> { it.importanceScore }
                .thenByDescending { it.timestamp }
        )
        _smartNotifications.value = ranked
        _smartBadgeCounts.value = ranked
            .asSequence()
            .filterNot { it.shouldSuppress }
            .groupingBy { it.packageName }
            .eachCount()
        _categoryCounts.value = ranked
            .asSequence()
            .filterNot { it.shouldSuppress }
            .groupingBy { it.category }
            .eachCount()
    }

    private fun notificationPriority(sbn: StatusBarNotification): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sbn.notification?.priority ?: 0
        } else {
            0
        }
    }

    companion object {
        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

        private val _latestTexts = MutableStateFlow<Map<String, String>>(emptyMap())
        val latestTexts: StateFlow<Map<String, String>> = _latestTexts.asStateFlow()

        private val _previewItems =
            MutableStateFlow<Map<String, List<NotificationPreview>>>(emptyMap())
        val previewItems: StateFlow<Map<String, List<NotificationPreview>>> =
            _previewItems.asStateFlow()

        private val _lastPostedAt = MutableStateFlow<Map<String, Long>>(emptyMap())
        val lastPostedAt: StateFlow<Map<String, Long>> = _lastPostedAt.asStateFlow()

        private val _smartNotifications = MutableStateFlow<List<SmartNotification>>(emptyList())
        val smartNotifications: StateFlow<List<SmartNotification>> =
            _smartNotifications.asStateFlow()

        private val _smartBadgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val smartBadgeCounts: StateFlow<Map<String, Int>> = _smartBadgeCounts.asStateFlow()

        private val _categoryCounts =
            MutableStateFlow<Map<NotificationCategory, Int>>(emptyMap())
        val categoryCounts: StateFlow<Map<NotificationCategory, Int>> =
            _categoryCounts.asStateFlow()
    }
}

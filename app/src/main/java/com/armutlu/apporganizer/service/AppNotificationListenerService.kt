package com.armutlu.apporganizer.service

import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.local.NotificationHistoryDao
import com.armutlu.apporganizer.data.repository.SmartNotificationLegacyBadgeBridge
import com.armutlu.apporganizer.data.repository.SmartNotificationRepository
import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.NotificationEvent
import com.armutlu.apporganizer.domain.models.NotificationHistoryEntity
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
open class AppNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var notificationEventDao: NotificationEventDao
    @Inject lateinit var notificationHistoryDao: NotificationHistoryDao
    @Inject lateinit var appDao: com.armutlu.apporganizer.data.local.AppDao
    @Inject lateinit var notificationClassifier: NotificationClassifierUseCase
    @Inject lateinit var smartNotificationRepository: SmartNotificationRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        runCatching {
            if (!sbn.isOngoing) {
                val classified = classifyNotification(sbn)
                val timestamp = classified.smart.timestamp
                _lastPostedAt.update { current -> current + (sbn.packageName to timestamp) }
                rebuildActiveSnapshot(preclassified = classified)
                if (AppPrefs.isNotifAnalyticsEnabled(this)) {
                    serviceScope.launch {
                        runCatching {
                            appDao.updateNotificationImportance(
                                sbn.packageName,
                                classified.systemPriority,
                            )
                            appDao.updateLastNotificationPostedAt(sbn.packageName, timestamp)
                            notificationEventDao.insert(
                                NotificationEvent(
                                    packageName = sbn.packageName,
                                    postedAt = timestamp,
                                    category = classified.smart.category.name,
                                    importanceScore = classified.smart.importanceScore,
                                    wasSuppressed = classified.smart.shouldSuppress,
                                    systemPriority = classified.systemPriority,
                                )
                            )
                        }
                    }
                }
                // D242c — Bildirim Geçmişi: yalnızca kullanıcı "Bildirim metnini göster" ayarını
                // açtıysa gerçek başlık/metin kaydedilir (varsayılan kapalı, AppPrefs.kt:200).
                // Kapalıyken hiçbir içerik notification_history'ye yazılmaz.
                if (AppPrefs.isNotificationTextEnabled(this)) {
                    val preview = classified.preview
                    val title = preview?.title?.takeIf { it.isNotBlank() }
                    if (preview != null && title != null) {
                        serviceScope.launch {
                            runCatching {
                                notificationHistoryDao.insert(
                                    NotificationHistoryEntity(
                                        packageName = sbn.packageName,
                                        title = title,
                                        text = preview.body.takeIf { it.isNotBlank() } ?: preview.text,
                                        postedAt = timestamp,
                                    )
                                )
                                notificationHistoryDao.deleteOlderThan(
                                    System.currentTimeMillis() - NOTIFICATION_HISTORY_RETENTION_MS
                                )
                                notificationHistoryDao.trimToLatest(NOTIFICATION_HISTORY_MAX_ROWS)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        runCatching { rebuildActiveSnapshot() }
    }

    override fun onListenerConnected() {
        rebuildActiveSnapshot()
        serviceScope.launch {
            runCatching {
                // P0 gizlilik migration'ı: eski sürümlerin apps.notificationText alanına yazdığı
                // içerikleri bağlantı anında temizle. Yeni DAO bariyeri dolu metin yazımını da
                // engeller; canlı özet yalnız servis belleğindeki latestTexts akışında yaşar.
                appDao.clearAllNotificationTexts()
                notificationEventDao.deleteOlderThan(
                    System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                )
            }
        }
    }

    override fun onListenerDisconnected() {
        _latestTexts.value = emptyMap()
        _previewItems.value = emptyMap()
        _smartNotifications.value = emptyList()
        _smartBadgeCounts.value = emptyMap()
        _categoryCounts.value = emptyMap()
        SmartNotificationLegacyBadgeBridge.clear()
        if (::smartNotificationRepository.isInitialized) {
            serviceScope.launch { smartNotificationRepository.clearActive() }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /** Tek callback için Android aktif bildirim kaynağını yalnız bir kez okur. */
    protected open fun currentActiveNotifications(): List<StatusBarNotification> =
        runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())

    /**
     * Eski badge/preview akışlarıyla yeni akıllı akışları aynı aktif bildirim snapshot'ından üretir.
     * Posted bildirimin sınıflandırması [preclassified] ile tekrar kullanılabilir.
     */
    private fun rebuildActiveSnapshot(preclassified: ClassifiedNotification? = null) {
        val active = currentActiveNotifications().filterNot { it.isOngoing }
        val showContent = AppPrefs.isNotificationTextEnabled(this)
        val blockedPackages = AppPrefs.getNotificationPreviewBlockedPackages(this)
        val counts = linkedMapOf<String, Int>()
        val previewBuckets = linkedMapOf<String, MutableList<NotificationPreview>>()
        val smartItems = ArrayList<SmartNotification>(active.size)

        active.forEach { sbn ->
            val packageName = sbn.packageName
            counts[packageName] = (counts[packageName] ?: 0) + 1

            val classified = preclassified
                ?.takeIf { it.smart.key == sbn.key }
                ?: classifyNotification(sbn)
            val extractedPreview = classified.preview

            if (showContent && packageName !in blockedPackages && extractedPreview != null) {
                previewBuckets.getOrPut(packageName) { mutableListOf() } += extractedPreview
            } else {
                previewBuckets.putIfAbsent(packageName, mutableListOf())
            }
            smartItems += classified.smart
        }

        val previews = previewBuckets.mapValues { (_, items) ->
            items.sortedByDescending { it.postedAt }.take(2)
        }
        val ranked = smartItems.sortedWith(
            compareByDescending<SmartNotification> { it.importanceScore }
                .thenByDescending { it.timestamp }
        )

        _previewItems.value = previews
        _latestTexts.value = counts.mapValues { (pkg, count) ->
            NotificationPreviewStore.summarize(
                previews = previews[pkg].orEmpty(),
                count = count,
                showContent = showContent && pkg !in blockedPackages,
            )
        }.filterValues { it.isNotBlank() }
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

        if (::smartNotificationRepository.isInitialized) {
            serviceScope.launch { smartNotificationRepository.replaceActive(ranked) }
        }
    }

    /** İçerik yalnız sınıflandırma ve aktif önizleme için bellekte tutulur. */
    private fun classifyNotification(sbn: StatusBarNotification): ClassifiedNotification {
        val preview = NotificationPreviewStore.extractPreview(sbn)
        val timestamp = preview?.postedAt
            ?: sbn.postTime.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        val smartText = preview?.body
            ?.takeIf { it.isNotBlank() }
            ?: preview?.text.orEmpty()
        val systemPriority = notificationPriority(sbn)
        return ClassifiedNotification(
            preview = preview,
            smart = notificationClassifier.classify(
                key = sbn.key,
                packageName = sbn.packageName,
                title = preview?.title.orEmpty(),
                text = smartText,
                timestamp = timestamp,
                systemPriority = systemPriority,
            ),
            systemPriority = systemPriority,
        )
    }

    private fun notificationPriority(sbn: StatusBarNotification): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sbn.notification?.priority ?: 0
        } else {
            0
        }
    }

    private data class ClassifiedNotification(
        val preview: NotificationPreview?,
        val smart: SmartNotification,
        val systemPriority: Int,
    )

    companion object {
        /** Bildirim Geçmişi retention — D242c: 7 gün veya 500 kayıt, hangisi önce dolarsa. */
        private const val NOTIFICATION_HISTORY_RETENTION_MS = 7L * 24 * 60 * 60 * 1000
        private const val NOTIFICATION_HISTORY_MAX_ROWS = 500

        /**
         * Geçiş API'si: tüketiciler aynı servis alanını kullanır ancak verinin gerçek sahibi
         * artık [SmartNotificationRepository]'dir. Doğrudan ViewModel enjeksiyonu sonrası kaldırılacak.
         */
        val badgeCounts: StateFlow<Map<String, Int>> =
            SmartNotificationLegacyBadgeBridge.badgeCounts

        /** Yalnız process belleğinde yaşayan UI özeti; Room, backup veya telemetriye yazılmaz. */
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
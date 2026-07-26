package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import com.armutlu.apporganizer.domain.models.SmartNotificationSettings
import com.armutlu.apporganizer.domain.usecase.notification.UnreadNotificationModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemorySmartNotificationRepository @Inject constructor(
    private val readStateSource: NotificationReadStateSource,
    private val settingsSource: SmartNotificationSettingsSource,
) : SmartNotificationRepository {

    private val mutationMutex = Mutex()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _activeNotifications = MutableStateFlow<List<SmartNotification>>(emptyList())
    override val activeNotifications: StateFlow<List<SmartNotification>> =
        _activeNotifications.asStateFlow()

    private val _actionablePackageCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val actionablePackageCounts: StateFlow<Map<String, Int>> =
        _actionablePackageCounts.asStateFlow()

    private val _categoryCounts =
        MutableStateFlow<Map<NotificationCategory, Int>>(emptyMap())
    override val categoryCounts: StateFlow<Map<NotificationCategory, Int>> =
        _categoryCounts.asStateFlow()

    private val _suppressedCount = MutableStateFlow(0)
    override val suppressedCount: StateFlow<Int> = _suppressedCount.asStateFlow()

    init {
        combine(
            readStateSource.lastReadAt,
            settingsSource.settings,
        ) { readState, settings -> readState to settings }
            .onEach { (readState, settings) ->
                mutationMutex.withLock {
                    publish(_activeNotifications.value, readState, settings)
                }
            }
            .launchIn(repositoryScope)
    }

    override suspend fun replaceActive(items: List<SmartNotification>) {
        mutationMutex.withLock {
            publish(
                items = items.distinctBy { it.key },
                lastReadAt = readStateSource.lastReadAt.value,
                settings = settingsSource.settings.value,
            )
        }
    }

    override suspend fun remove(notificationKey: String) {
        mutationMutex.withLock {
            publish(
                items = _activeNotifications.value.filterNot { it.key == notificationKey },
                lastReadAt = readStateSource.lastReadAt.value,
                settings = settingsSource.settings.value,
            )
        }
    }

    override suspend fun clearActive() {
        mutationMutex.withLock {
            publish(
                items = emptyList(),
                lastReadAt = readStateSource.lastReadAt.value,
                settings = settingsSource.settings.value,
            )
        }
    }

    private fun publish(
        items: List<SmartNotification>,
        lastReadAt: Map<String, Long>,
        settings: SmartNotificationSettings,
    ) {
        val ranked = items.sortedWith(
            compareByDescending<SmartNotification> { it.importanceScore }
                .thenByDescending { it.timestamp }
        )
        val unread = ranked
            .asSequence()
            .filter { item ->
                UnreadNotificationModel.isUnread(
                    postedAt = item.timestamp,
                    lastReadAt = lastReadAt[item.packageName],
                )
            }
        val actionableUnread = if (!settings.engineEnabled) {
            // Mevcut kullanıcı güvenliği: motor kapalıyken eski klasik rozet davranışı korunur.
            unread.toList()
        } else {
            unread
                .filter { it.category in settings.visibleCategories }
                .filter { item -> !settings.filterPromotions || !item.shouldSuppress }
                .toList()
        }
        val actionableCounts = actionableUnread
            .groupingBy { it.packageName }
            .eachCount()
        val actionableCategoryCounts = actionableUnread
            .groupingBy { it.category }
            .eachCount()

        _activeNotifications.value = ranked
        _actionablePackageCounts.value = actionableCounts
        _categoryCounts.value = actionableCategoryCounts
        _suppressedCount.value = ranked.count { it.shouldSuppress }

        SmartNotificationLegacyBadgeBridge.publish(actionableCounts)
    }
}

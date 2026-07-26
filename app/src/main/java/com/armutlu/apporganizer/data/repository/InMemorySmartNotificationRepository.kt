package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import com.armutlu.apporganizer.domain.usecase.notification.UnreadNotificationModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemorySmartNotificationRepository @Inject constructor(
    private val readStateSource: NotificationReadStateSource,
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
        readStateSource.lastReadAt
            .onEach { readState ->
                mutationMutex.withLock {
                    publish(_activeNotifications.value, readState)
                }
            }
            .launchIn(repositoryScope)
    }

    override suspend fun replaceActive(items: List<SmartNotification>) {
        mutationMutex.withLock {
            publish(items.distinctBy { it.key }, readStateSource.lastReadAt.value)
        }
    }

    override suspend fun remove(notificationKey: String) {
        mutationMutex.withLock {
            publish(
                _activeNotifications.value.filterNot { it.key == notificationKey },
                readStateSource.lastReadAt.value,
            )
        }
    }

    override suspend fun clearActive() {
        mutationMutex.withLock {
            publish(emptyList(), readStateSource.lastReadAt.value)
        }
    }

    private fun publish(items: List<SmartNotification>, lastReadAt: Map<String, Long>) {
        val ranked = items.sortedWith(
            compareByDescending<SmartNotification> { it.importanceScore }
                .thenByDescending { it.timestamp }
        )
        val actionableUnread = ranked
            .asSequence()
            .filterNot { it.shouldSuppress }
            .filter { item ->
                UnreadNotificationModel.isUnread(
                    postedAt = item.timestamp,
                    lastReadAt = lastReadAt[item.packageName],
                )
            }
            .toList()

        _activeNotifications.value = ranked
        _actionablePackageCounts.value = actionableUnread
            .groupingBy { it.packageName }
            .eachCount()
        _categoryCounts.value = actionableUnread
            .groupingBy { it.category }
            .eachCount()
        _suppressedCount.value = ranked.count { it.shouldSuppress }
    }
}

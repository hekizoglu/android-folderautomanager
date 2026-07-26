package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemorySmartNotificationRepository @Inject constructor() : SmartNotificationRepository {

    private val mutationMutex = Mutex()

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

    override suspend fun replaceActive(items: List<SmartNotification>) {
        mutationMutex.withLock {
            publish(items.distinctBy { it.key })
        }
    }

    override suspend fun remove(notificationKey: String) {
        mutationMutex.withLock {
            publish(_activeNotifications.value.filterNot { it.key == notificationKey })
        }
    }

    override suspend fun clearActive() {
        mutationMutex.withLock {
            publish(emptyList())
        }
    }

    private fun publish(items: List<SmartNotification>) {
        val ranked = items.sortedWith(
            compareByDescending<SmartNotification> { it.importanceScore }
                .thenByDescending { it.timestamp }
        )
        val actionable = ranked.filterNot { it.shouldSuppress }

        _activeNotifications.value = ranked
        _actionablePackageCounts.value = actionable
            .groupingBy { it.packageName }
            .eachCount()
        _categoryCounts.value = actionable
            .groupingBy { it.category }
            .eachCount()
        _suppressedCount.value = ranked.count { it.shouldSuppress }
    }
}

package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import kotlinx.coroutines.flow.StateFlow

/**
 * Aktif akıllı bildirimlerin process içi tek durum sahibi.
 * Başlık ve metin kalıcı depolamaya yazılmaz; repository yalnız uygulama belleğinde yaşar.
 */
interface SmartNotificationRepository {
    val activeNotifications: StateFlow<List<SmartNotification>>
    val actionablePackageCounts: StateFlow<Map<String, Int>>
    val categoryCounts: StateFlow<Map<NotificationCategory, Int>>
    val suppressedCount: StateFlow<Int>

    suspend fun replaceActive(items: List<SmartNotification>)
    suspend fun remove(notificationKey: String)
    suspend fun clearActive()
}

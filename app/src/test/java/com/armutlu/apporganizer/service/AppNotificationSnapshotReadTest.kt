package com.armutlu.apporganizer.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.domain.usecase.notification.NotificationClassifierUseCase
import com.armutlu.apporganizer.utils.AppPrefs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppNotificationSnapshotReadTest {

    @Before
    fun setup() {
        mockkObject(AppPrefs)
        every { AppPrefs.isNotifAnalyticsEnabled(any()) } returns false
        every { AppPrefs.isNotificationTextEnabled(any()) } returns false
        every { AppPrefs.getNotificationPreviewBlockedPackages(any()) } returns emptySet()
    }

    @After
    fun tearDown() {
        AppNotificationListenerService().onListenerDisconnected()
        unmockkObject(AppPrefs)
    }

    @Test
    fun `posted callback reads active notifications exactly once`() {
        val posted = sbn(packageName = "com.test.app", key = "posted-1")
        val service = serviceWithSnapshot(listOf(posted))

        service.onNotificationPosted(posted)

        assertEquals(1, service.readCount)
        assertEquals(1, AppNotificationListenerService.badgeCounts.value["com.test.app"])
    }

    @Test
    fun `removed callback reads active notifications exactly once`() {
        val removed = sbn(packageName = "com.test.app", key = "removed-1")
        val service = serviceWithSnapshot(emptyList())

        service.onNotificationRemoved(removed)

        assertEquals(1, service.readCount)
        assertEquals(emptyMap<String, Int>(), AppNotificationListenerService.badgeCounts.value)
    }

    @Test
    fun `listener connection reads active notifications exactly once`() {
        val service = serviceWithSnapshot(
            listOf(
                sbn(packageName = "com.test.one", key = "one"),
                sbn(packageName = "com.test.two", key = "two"),
            )
        )

        service.onListenerConnected()

        assertEquals(1, service.readCount)
        assertEquals(1, AppNotificationListenerService.badgeCounts.value["com.test.one"])
        assertEquals(1, AppNotificationListenerService.badgeCounts.value["com.test.two"])
    }

    @Test
    fun `ongoing posted notification does not rebuild snapshot`() {
        val ongoing = sbn(packageName = "com.test.player", key = "ongoing", ongoing = true)
        val service = serviceWithSnapshot(listOf(ongoing))

        service.onNotificationPosted(ongoing)

        assertEquals(0, service.readCount)
    }

    private fun serviceWithSnapshot(snapshot: List<StatusBarNotification>): CountingService {
        return CountingService(snapshot).apply {
            notificationEventDao = mockk<NotificationEventDao>(relaxed = true)
            appDao = mockk<AppDao>(relaxed = true)
            notificationClassifier = NotificationClassifierUseCase()
        }
    }

    private fun sbn(
        packageName: String,
        key: String,
        ongoing: Boolean = false,
    ): StatusBarNotification {
        val notification = mockk<Notification>(relaxed = true)
        notification.extras = mockk<Bundle>(relaxed = true)
        every { notification.priority } returns 0

        return mockk<StatusBarNotification>(relaxed = true) {
            every { this@mockk.packageName } returns packageName
            every { this@mockk.key } returns key
            every { isOngoing } returns ongoing
            every { postTime } returns 1L
            every { this@mockk.notification } returns notification
        }
    }

    private class CountingService(
        private val snapshot: List<StatusBarNotification>,
    ) : AppNotificationListenerService() {
        var readCount: Int = 0
            private set

        override fun currentActiveNotifications(): List<StatusBarNotification> {
            readCount += 1
            return snapshot
        }
    }
}

package com.armutlu.apporganizer.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.InMemorySmartNotificationRepository
import com.armutlu.apporganizer.data.repository.NotificationReadStateSource
import com.armutlu.apporganizer.data.repository.SmartNotificationSettingsSource
import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotificationSettings
import com.armutlu.apporganizer.domain.usecase.notification.NotificationClassifierUseCase
import com.armutlu.apporganizer.utils.AppPrefs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartNotificationServiceIntegrationTest {

    private lateinit var notificationEventDao: NotificationEventDao
    private lateinit var appDao: AppDao
    private lateinit var readState: FakeReadStateSource
    private lateinit var settings: FakeSettingsSource
    private lateinit var repository: InMemorySmartNotificationRepository
    private lateinit var service: MutableSnapshotService

    @Before
    fun setup() {
        mockkObject(AppPrefs)
        every { AppPrefs.isNotifAnalyticsEnabled(any()) } returns false
        every { AppPrefs.isNotificationTextEnabled(any()) } returns true
        every { AppPrefs.getNotificationPreviewBlockedPackages(any()) } returns emptySet()

        notificationEventDao = mockk(relaxed = true)
        appDao = mockk(relaxed = true)
        readState = FakeReadStateSource()
        settings = FakeSettingsSource(
            SmartNotificationSettings.defaults(engineEnabled = true)
        )
        repository = InMemorySmartNotificationRepository(readState, settings)
        service = MutableSnapshotService().apply {
            this.notificationEventDao = this@SmartNotificationServiceIntegrationTest.notificationEventDao
            this.appDao = this@SmartNotificationServiceIntegrationTest.appDao
            notificationClassifier = NotificationClassifierUseCase()
            smartNotificationRepository = repository
        }
    }

    @After
    fun tearDown() {
        service.onListenerDisconnected()
        service.onDestroy()
        unmockkObject(AppPrefs)
    }

    @Test
    fun `promotion is active but excluded from actionable badge when filter is enabled`() {
        val promotion = notification(
            packageName = "com.shop.app",
            key = "promo-1",
            title = "Kampanya",
            text = "Yüzde 50 indirim kuponu seni bekliyor",
            postedAt = 1_000L,
        )
        service.snapshot = listOf(promotion)

        service.onNotificationPosted(promotion)

        awaitCondition { repository.activeNotifications.value.size == 1 }
        assertEquals(NotificationCategory.PROMOTION, repository.activeNotifications.value.single().category)
        assertTrue(repository.activeNotifications.value.single().shouldSuppress)
        assertTrue(repository.actionablePackageCounts.value.isEmpty())
        assertEquals(1, repository.suppressedCount.value)
        assertTrue(AppNotificationListenerService.latestTexts.value["com.shop.app"].orEmpty().isNotBlank())
        coVerify(exactly = 0) { notificationEventDao.insert(any()) }
    }

    @Test
    fun `analytics disabled still updates active smart UI without persistent event`() {
        val message = notification(
            packageName = "com.whatsapp",
            key = "message-1",
            title = "Ayşe",
            text = "Toplantı tamamlandı",
            postedAt = 2_000L,
        )
        service.snapshot = listOf(message)

        service.onNotificationPosted(message)

        awaitCondition { repository.actionablePackageCounts.value["com.whatsapp"] == 1 }
        assertEquals(NotificationCategory.MESSAGING, repository.activeNotifications.value.single().category)
        coVerify(exactly = 0) { notificationEventDao.insert(any()) }
    }

    @Test
    fun `removed callback clears repository snapshot and badge`() {
        val message = notification(
            packageName = "com.whatsapp",
            key = "message-remove",
            title = "Mesaj",
            text = "Merhaba",
            postedAt = 3_000L,
        )
        service.snapshot = listOf(message)
        service.onNotificationPosted(message)
        awaitCondition { repository.activeNotifications.value.size == 1 }

        service.snapshot = emptyList()
        service.onNotificationRemoved(message)

        awaitCondition { repository.activeNotifications.value.isEmpty() }
        assertTrue(repository.actionablePackageCounts.value.isEmpty())
    }

    @Test
    fun `listener reconnect rebuilds repository from current active notifications`() {
        service.snapshot = listOf(
            notification("com.whatsapp", "reconnect-message", "Mesaj", "Merhaba", 4_000L),
            notification("com.bank", "reconnect-bank", "Güvenlik", "Giriş kodu 123456", 5_000L),
        )

        service.onListenerConnected()

        awaitCondition { repository.activeNotifications.value.size == 2 }
        assertEquals(
            setOf("com.whatsapp", "com.bank"),
            repository.activeNotifications.value.map { it.packageName }.toSet(),
        )
        coVerify(timeout = 2_000) { appDao.clearAllNotificationTexts() }
    }

    @Test
    fun `marking app read resets badge without deleting active snapshot`() {
        val message = notification(
            packageName = "com.whatsapp",
            key = "read-message",
            title = "Mesaj",
            text = "Yeni mesaj",
            postedAt = 6_000L,
        )
        service.snapshot = listOf(message)
        service.onNotificationPosted(message)
        awaitCondition { repository.actionablePackageCounts.value["com.whatsapp"] == 1 }

        readState.state.value = mapOf("com.whatsapp" to 7_000L)

        awaitCondition { repository.actionablePackageCounts.value.isEmpty() }
        assertEquals(1, repository.activeNotifications.value.size)
    }

    @Test
    fun `ongoing notification does not enter repository or history`() {
        val player = notification(
            packageName = "com.music.player",
            key = "ongoing-player",
            title = "Çalıyor",
            text = "Şarkı",
            postedAt = 8_000L,
            ongoing = true,
        )
        service.snapshot = listOf(player)

        service.onNotificationPosted(player)

        Thread.sleep(200)
        assertTrue(repository.activeNotifications.value.isEmpty())
        coVerify(exactly = 0) { notificationEventDao.insert(any()) }
    }

    private fun notification(
        packageName: String,
        key: String,
        title: String,
        text: String,
        postedAt: Long,
        ongoing: Boolean = false,
    ): StatusBarNotification {
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(NotificationCompat.EXTRA_TITLE) } returns title
        every { extras.getCharSequence(NotificationCompat.EXTRA_TEXT) } returns text
        every { extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) } returns null

        val androidNotification = mockk<Notification>(relaxed = true)
        androidNotification.extras = extras
        androidNotification.priority = 0
        androidNotification.`when` = 0L

        return mockk<StatusBarNotification>(relaxed = true).also { sbn ->
            every { sbn.packageName } returns packageName
            every { sbn.key } returns key
            every { sbn.isOngoing } returns ongoing
            every { sbn.postTime } returns postedAt
            every { sbn.notification } returns androidNotification
        }
    }

    private fun awaitCondition(condition: () -> Boolean) {
        repeat(200) {
            if (condition()) return
            Thread.sleep(10)
        }
        error("Condition was not met")
    }

    private class MutableSnapshotService : AppNotificationListenerService() {
        var snapshot: List<StatusBarNotification> = emptyList()

        override fun currentActiveNotifications(): List<StatusBarNotification> = snapshot
    }

    private class FakeReadStateSource : NotificationReadStateSource {
        val state = MutableStateFlow<Map<String, Long>>(emptyMap())
        override val lastReadAt: StateFlow<Map<String, Long>> = state
    }

    private class FakeSettingsSource(
        initial: SmartNotificationSettings,
    ) : SmartNotificationSettingsSource {
        val state = MutableStateFlow(initial)
        override val settings: StateFlow<SmartNotificationSettings> = state
    }
}

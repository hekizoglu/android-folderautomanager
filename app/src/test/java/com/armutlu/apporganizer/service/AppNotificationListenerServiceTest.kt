package com.armutlu.apporganizer.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.SmartNotificationRepository
import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.NotificationEvent
import com.armutlu.apporganizer.domain.usecase.notification.NotificationClassifierUseCase
import com.armutlu.apporganizer.utils.AppPrefs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test

class AppNotificationListenerServiceTest {

    private lateinit var mockDao: NotificationEventDao
    private lateinit var mockAppDao: com.armutlu.apporganizer.data.local.AppDao
    private lateinit var mockSmartRepository: SmartNotificationRepository
    private lateinit var service: AppNotificationListenerService

    @Before
    fun setup() {
        mockDao = mockk(relaxed = true)
        mockAppDao = mockk(relaxed = true)
        mockSmartRepository = mockk(relaxed = true)
        service = AppNotificationListenerService().apply {
            notificationEventDao = mockDao
            appDao = mockAppDao
            notificationClassifier = NotificationClassifierUseCase()
            smartNotificationRepository = mockSmartRepository
        }
        mockkObject(AppPrefs)
        every { AppPrefs.isNotificationTextEnabled(any()) } returns false
        every { AppPrefs.getNotificationPreviewBlockedPackages(any()) } returns emptySet()
    }

    @After
    fun tearDown() {
        service.onListenerDisconnected()
        unmockkObject(AppPrefs)
    }

    private fun sbn(
        pkg: String,
        ongoing: Boolean = false,
        title: String = "",
        text: String = "",
    ): StatusBarNotification {
        val bundle = mockk<Bundle>(relaxed = true)
        every { bundle.getCharSequence(NotificationCompat.EXTRA_TITLE) } returns title
        every { bundle.getCharSequence(NotificationCompat.EXTRA_TEXT) } returns text
        every { bundle.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT) } returns null
        every { bundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) } returns null

        val notification = mockk<Notification>(relaxed = true)
        notification.extras = bundle
        notification.priority = 0
        notification.`when` = 0L

        val result = mockk<StatusBarNotification>(relaxed = true)
        every { result.packageName } returns pkg
        every { result.key } returns "$pkg-key"
        every { result.isOngoing } returns ongoing
        every { result.postTime } returns 1_000L
        every { result.notification } returns notification
        return result
    }

    @Test
    fun `onNotificationPosted inserts content-free metadata when analytics is enabled`() {
        every { AppPrefs.isNotifAnalyticsEnabled(any()) } returns true

        service.onNotificationPosted(
            sbn(
                pkg = "com.akbank.android.apps.akbank_direkt",
                title = "Kampanya",
                text = "Kartınıza özel yüzde 50 indirim fırsatı",
            )
        )

        coVerify(timeout = 2_000) {
            mockDao.insert(
                match<NotificationEvent> { event ->
                    event.packageName == "com.akbank.android.apps.akbank_direkt" &&
                        event.postedAt == 1_000L &&
                        event.category == NotificationCategory.PROMOTION.name &&
                        event.importanceScore < 40 &&
                        event.wasSuppressed
                }
            )
        }
    }

    @Test
    fun `onNotificationPosted does NOT insert event when analytics toggle is disabled`() {
        every { AppPrefs.isNotifAnalyticsEnabled(any()) } returns false

        service.onNotificationPosted(sbn("com.test.app"))

        Thread.sleep(400)
        coVerify(exactly = 0) { mockDao.insert(any()) }
    }

    @Test
    fun `onNotificationPosted ignores ongoing notifications regardless of toggle`() {
        every { AppPrefs.isNotifAnalyticsEnabled(any()) } returns true

        service.onNotificationPosted(sbn("com.test.app", ongoing = true))

        Thread.sleep(400)
        coVerify(exactly = 0) { mockDao.insert(any()) }
    }

    @Test
    fun `onListenerConnected purges legacy notification text`() {
        service.onListenerConnected()

        coVerify(timeout = 2_000) {
            mockAppDao.clearAllNotificationTexts()
        }
    }

    @Test
    fun `onListenerConnected triggers deleteOlderThan with correct 30-day cutoff`() {
        val before = System.currentTimeMillis()

        service.onListenerConnected()

        val toleranceMs = 5_000L
        val expectedMin = before - 30L * 24 * 60 * 60 * 1000 - toleranceMs
        val expectedMax = System.currentTimeMillis() + toleranceMs - 30L * 24 * 60 * 60 * 1000 + toleranceMs

        coVerify(timeout = 2_000) {
            mockDao.deleteOlderThan(match { it in expectedMin..expectedMax })
        }
    }
}

package com.armutlu.apporganizer.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.domain.models.NotificationEvent
import com.armutlu.apporganizer.utils.AppPrefs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * ROADMAP "Akilli Bildirim Analiz Sistemi":
 * madde 1 — analiz master toggle kapaliyken notification_events'e yazilmadigini kanitlar.
 * madde 5 — onListenerConnected() 30 gunden eski kayitlari dogru esik degeriyle temizledigini kanitlar.
 */
class AppNotificationListenerServiceTest {

    private lateinit var mockDao: NotificationEventDao
    private lateinit var service: AppNotificationListenerService

    @Before
    fun setup() {
        mockDao = mockk(relaxed = true)
        service = AppNotificationListenerService()
        service.notificationEventDao = mockDao
        mockkObject(AppPrefs)
        // D239 gizlilik guard'i: stub'lanmazsa MockK exception firlatir, runCatching yutar
        // ve analiz insert'ine hic ulasilamaz — tum testler icin varsayilan kapali.
        every { AppPrefs.isNotificationTextEnabled(any()) } returns false
        every { AppPrefs.getNotificationPreviewBlockedPackages(any()) } returns emptySet()
    }

    @After
    fun tearDown() {
        unmockkObject(AppPrefs)
    }

    private fun sbn(pkg: String, ongoing: Boolean = false): StatusBarNotification {
        val bundle = mockk<Bundle>(relaxed = true)
        val notification = mockk<Notification>(relaxed = true)
        // Notification.extras bir Java field'idir (getter degil) — mockk every{} ile
        // intercept edilemez, dogrudan alan atamasi yapiyoruz.
        notification.extras = bundle
        val sbn = mockk<StatusBarNotification>(relaxed = true)
        every { sbn.packageName } returns pkg
        every { sbn.isOngoing } returns ongoing
        every { sbn.notification } returns notification
        return sbn
    }

    // ── madde 1: master toggle gating ───────────────────────────────────────

    @Test
    fun `onNotificationPosted inserts event when analytics toggle is enabled`() {
        every { AppPrefs.isNotifAnalyticsEnabled(any()) } returns true

        service.onNotificationPosted(sbn("com.test.app"))

        coVerify(timeout = 2000) {
            mockDao.insert(match<NotificationEvent> { it.packageName == "com.test.app" })
        }
    }

    @Test
    fun `onNotificationPosted does NOT insert event when analytics toggle is disabled`() {
        every { AppPrefs.isNotifAnalyticsEnabled(any()) } returns false

        service.onNotificationPosted(sbn("com.test.app"))

        // Kısa bekleme sonrası hiçbir insert çağrısı yapılmamış olmalı.
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

    // ── madde 5: 30 gün temizlik tetikleme ──────────────────────────────────

    @Test
    fun `onListenerConnected triggers deleteOlderThan with correct 30-day cutoff`() {
        val before = System.currentTimeMillis()

        service.onListenerConnected()

        // Cevrimici coroutine gec calisabilecegi icin toleransli bir pencere kullanilir.
        val toleranceMs = 5_000L
        val expectedMin = before - 30L * 24 * 60 * 60 * 1000 - toleranceMs
        val expectedMax = System.currentTimeMillis() + toleranceMs - 30L * 24 * 60 * 60 * 1000 + toleranceMs

        coVerify(timeout = 2000) {
            mockDao.deleteOlderThan(match { it in expectedMin..expectedMax })
        }
    }
}

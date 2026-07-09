package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * NotificationAnalyzer.analyze() senaryo testleri — ROADMAP "Akilli Bildirim Analiz Sistemi" madde 2.
 * Kapsam: cok konusan (mostTalkative), gece/burst rahatsiz eden (disturbing),
 * dikkat dagitan (distracting) ve trend (dailyCounts) senaryolari.
 */
class NotificationAnalyzerTest {

    private fun eventAtHour(pkg: String, hourOfDay: Int, minuteOffset: Int = 0): NotificationEvent {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minuteOffset)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return NotificationEvent(packageName = pkg, postedAt = cal.timeInMillis)
    }

    private fun eventsAtHour(pkg: String, hourOfDay: Int, count: Int): List<NotificationEvent> =
        (0 until count).map { eventAtHour(pkg, hourOfDay, minuteOffset = it) }

    // ── boş liste ────────────────────────────────────────────────────────────

    @Test
    fun `empty events returns empty report`() {
        val report = NotificationAnalyzer.analyze(emptyList(), emptyMap(), emptyMap())

        assertEquals(0, report.totalNotifications)
        assertTrue(report.appStats.isEmpty())
        assertTrue(report.mostTalkative.isEmpty())
        assertTrue(report.disturbing.isEmpty())
        assertTrue(report.distracting.isEmpty())
    }

    // ── çok konuşan ──────────────────────────────────────────────────────────

    @Test
    fun `mostTalkative sorts apps by total count descending`() {
        val events = eventsAtHour("com.talkative", hourOfDay = 12, count = 20) +
            eventsAtHour("com.quiet", hourOfDay = 12, count = 3)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertEquals(23, report.totalNotifications)
        assertEquals("com.talkative", report.mostTalkative.first().packageName)
        assertEquals(20, report.mostTalkative.first().total)
        assertEquals("com.quiet", report.mostTalkative[1].packageName)
    }

    @Test
    fun `mostTalkative caps at top 10 apps`() {
        val events = (1..15).flatMap { i -> eventsAtHour("com.app$i", hourOfDay = 12, count = i) }

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertEquals(10, report.mostTalkative.size)
        // en çok bildirim gönderen com.app15 (15 adet) ilk sırada olmalı
        assertEquals("com.app15", report.mostTalkative.first().packageName)
    }

    // ── gece rahatsız eden ───────────────────────────────────────────────────

    @Test
    fun `disturbing includes app with high night ratio`() {
        // 12 gece (23:xx) bildirimi -> total>=10 && nightRatio>0.3
        val events = eventsAtHour("com.nightowl", hourOfDay = 23, count = 12)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertTrue(report.disturbing.any { it.packageName == "com.nightowl" })
        assertTrue(report.disturbing.first().nightRatio > 0.3f)
    }

    @Test
    fun `disturbing excludes daytime app with low night ratio`() {
        // 12 gündüz bildirimi, hiç gece yok, burst da yok (farklı dakikalarda aynı saat -> burst sayılır aslında)
        // burst'ten kaçınmak için farklı saatlere dağıtıyoruz
        val events = (0 until 12).map { i -> eventAtHour("com.daytime", hourOfDay = 10 + (i % 8)) }

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertTrue(report.disturbing.none { it.packageName == "com.daytime" })
    }

    // ── kısa aralıkta tekrar eden (burst) ────────────────────────────────────

    @Test
    fun `disturbing includes app with hourly burst even without night activity`() {
        // aynı saat diliminde 5+ bildirim -> maxBurstPerHour >= 5, gece değil
        val events = eventsAtHour("com.burst", hourOfDay = 14, count = 6)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        val stats = report.appStats.first { it.packageName == "com.burst" }
        assertTrue(stats.maxBurstPerHour >= 5)
        assertEquals(0, stats.nightCount)
        assertTrue(report.disturbing.any { it.packageName == "com.burst" })
    }

    // ── dikkat dağıtan ───────────────────────────────────────────────────────

    @Test
    fun `distracting includes high-notification low-usage app`() {
        val events = eventsAtHour("com.distracting", hourOfDay = 12, count = 20)
        val usage = mapOf("com.distracting" to 5 * 60_000L) // 5 dakika kullanım

        val report = NotificationAnalyzer.analyze(events, emptyMap(), usage)

        val stats = report.appStats.first { it.packageName == "com.distracting" }
        assertTrue(stats.distractionScore > 1f)
        assertTrue(report.distracting.any { it.packageName == "com.distracting" })
    }

    @Test
    fun `distracting excludes app with sufficient usage time`() {
        val events = eventsAtHour("com.wellused", hourOfDay = 12, count = 20)
        val usage = mapOf("com.wellused" to 120 * 60_000L) // 2 saat kullanım -> düşük skor

        val report = NotificationAnalyzer.analyze(events, emptyMap(), usage)

        assertTrue(report.distracting.none { it.packageName == "com.wellused" })
    }

    // ── trend (dailyCounts) ──────────────────────────────────────────────────

    @Test
    fun `dailyCounts places today's events in the last index`() {
        val events = eventsAtHour("com.today", hourOfDay = 9, count = 4)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        val stats = report.appStats.first { it.packageName == "com.today" }
        assertEquals(7, stats.dailyCounts.size)
        assertEquals(4, stats.dailyCounts.last())
        assertEquals(4, stats.dailyCounts.sum())
    }

    // ── görünen isim fallback ────────────────────────────────────────────────

    @Test
    fun `appName falls back to package suffix when name unknown`() {
        val events = eventsAtHour("com.example.unknownapp", hourOfDay = 12, count = 1)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertEquals("unknownapp", report.appStats.first().appName)
    }

    @Test
    fun `appName uses provided display name when available`() {
        val events = eventsAtHour("com.instagram.android", hourOfDay = 12, count = 1)
        val names = mapOf("com.instagram.android" to "Instagram")

        val report = NotificationAnalyzer.analyze(events, names, emptyMap())

        assertEquals("Instagram", report.appStats.first().appName)
    }
}

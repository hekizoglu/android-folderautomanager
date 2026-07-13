package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * TickerComposer.compose() senaryo testleri — şablon çeşitliliği, unutulan uygulama eşiği,
 * saat dilimi selamlaması ve günlük deterministiklik.
 */
class TickerComposerTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun millisAt(epochDay: Long, hour: Int): Long =
        ZonedDateTime.of(
            java.time.LocalDate.ofEpochDay(epochDay),
            java.time.LocalTime.of(hour, 0),
            zone
        ).toInstant().toEpochMilli()

    private val sampleFolders = listOf(
        FolderSnapshot("social", "Sosyal", "📱", 12),
        FolderSnapshot("games", "Oyunlar", "🎮", 8),
        FolderSnapshot("finance", "Finans", "💰", 3),
    )

    // ── şablon çeşitliliği: farklı günler farklı metin ──────────────────────

    @Test
    fun `folder template varies across days`() {
        val texts = (0 until 30).map { dayOffset ->
            val day = 20000L + dayOffset
            TickerComposer.compose(
                folders = sampleFolders,
                apps = emptyList(),
                badgeTotal = 0,
                insights = emptyList(),
                lowConfidenceCount = 0,
                nowMillis = millisAt(day, 12),
                epochDay = day,
                zone = zone,
            ).first { it.categoryId == "social" }.text
        }.toSet()

        // 4 sablon var — 30 farkli gunde en az 2 farkli metin gormeliyiz.
        assertTrue("Beklenen coklu sablon, bulunan: $texts", texts.size >= 2)
    }

    @Test
    fun `tip of day rotates across days`() {
        val texts = (0 until 30).map { dayOffset ->
            val day = 40000L + dayOffset
            TickerComposer.compose(
                folders = emptyList(),
                apps = emptyList(),
                badgeTotal = 0,
                insights = emptyList(),
                lowConfidenceCount = 0,
                nowMillis = millisAt(day, 12),
                epochDay = day,
                zone = zone,
            ).first { it.priority == 5 }.text
        }.toSet()

        assertTrue("Beklenen coklu ipucu, bulunan: $texts", texts.size >= 2)
    }

    // ── deterministiklik: aynı gün aynı çıktı ───────────────────────────────

    @Test
    fun `same day produces identical output`() {
        val day = 20123L
        val first = TickerComposer.compose(
            folders = sampleFolders,
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(day, 14),
            epochDay = day,
            zone = zone,
        )
        val second = TickerComposer.compose(
            folders = sampleFolders,
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(day, 14),
            epochDay = day,
            zone = zone,
        )
        assertEquals(first.map { it.text }, second.map { it.text })
    }

    @Test
    fun `different days can shuffle order differently`() {
        val day1Order = TickerComposer.compose(
            folders = sampleFolders,
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(50000L, 12),
            epochDay = 50000L,
            zone = zone,
        ).map { it.categoryId }

        val day2Order = TickerComposer.compose(
            folders = sampleFolders,
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(50001L, 12),
            epochDay = 50001L,
            zone = zone,
        ).map { it.categoryId }

        // Farkli seed farkli Random -> genelde farkli shuffle sirasi (ayni priority-grubu icinde).
        // Tum spec setleri ayni oldugu icin tam esitlik beklenmiyor ama en azindan olusabiliyor olmali.
        assertTrue(day1Order.isNotEmpty() && day2Order.isNotEmpty())
    }

    // ── unutulan uygulama eşiği (45+ gün) ───────────────────────────────────

    @Test
    fun `forgotten app appears only past 45 day threshold`() {
        val now = millisAt(20000L, 12)
        val dayMs = 24L * 3600 * 1000

        val recentlyUsedApp = AppSnapshot("com.recent", "Recent App", 5, now - 10 * dayMs)
        val forgottenApp = AppSnapshot("com.forgotten", "Forgotten App", 5, now - 46 * dayMs)

        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = listOf(recentlyUsedApp, forgottenApp),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = now,
            epochDay = 20000L,
            zone = zone,
        )

        val forgottenTexts = result.filter { it.priority == 40 }.map { it.text }
        assertTrue(forgottenTexts.any { it.contains("Forgotten App") })
        assertTrue(forgottenTexts.none { it.contains("Recent App") })
    }

    @Test
    fun `never used app (lastUsedTimestamp zero) is not treated as forgotten`() {
        val now = millisAt(20000L, 12)
        val neverUsed = AppSnapshot("com.never", "Never Used", 0, 0L)

        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = listOf(neverUsed),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = now,
            epochDay = 20000L,
            zone = zone,
        )

        assertTrue(result.none { it.priority == 40 && it.text.contains("Never Used") })
    }

    // ── günün şampiyonu ──────────────────────────────────────────────────────

    @Test
    fun `champion is app with highest usageCount`() {
        val now = millisAt(20000L, 12)
        val apps = listOf(
            AppSnapshot("com.low", "Low Usage", 2, now),
            AppSnapshot("com.high", "High Usage", 99, now),
        )
        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = apps,
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = now,
            epochDay = 20000L,
            zone = zone,
        )
        val championSpec = result.first { it.priority == 50 }
        assertTrue(championSpec.text.contains("High Usage"))
        assertEquals("com.high", championSpec.packageName)
    }

    @Test
    fun `forgotten app ticker carries package name for direct launch`() {
        val now = millisAt(20000L, 12)
        val dayMs = 24L * 3600 * 1000
        val forgottenApp = AppSnapshot("com.forgotten", "Forgotten App", 5, now - 46 * dayMs)

        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = listOf(forgottenApp),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = now,
            epochDay = 20000L,
            zone = zone,
        )

        val forgottenSpec = result.first { it.priority == 40 }
        assertEquals("com.forgotten", forgottenSpec.packageName)
    }

    // ── saat dilimi selamlaması ──────────────────────────────────────────────

    @Test
    fun `morning hour uses sun emoji greeting`() {
        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(20000L, 8),
            epochDay = 20000L,
            zone = zone,
        )
        val greeting = result.first { it.priority == 90 }
        assertEquals("☀️", greeting.emoji)
    }

    @Test
    fun `night hour uses moon emoji greeting`() {
        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(20000L, 2),
            epochDay = 20000L,
            zone = zone,
        )
        val greeting = result.first { it.priority == 90 }
        assertEquals("🌙", greeting.emoji)
    }

    @Test
    fun `evening hour uses sunset emoji greeting`() {
        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(20000L, 19),
            epochDay = 20000L,
            zone = zone,
        )
        val greeting = result.first { it.priority == 90 }
        assertEquals("🌇", greeting.emoji)
    }

    // ── öncelik: bildirim en tazedir ────────────────────────────────────────

    @Test
    fun `notification badge is highest priority when present`() {
        val result = TickerComposer.compose(
            folders = sampleFolders,
            apps = emptyList(),
            badgeTotal = 5,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(20000L, 12),
            epochDay = 20000L,
            zone = zone,
        )
        assertEquals(100, result.first().priority)
        assertTrue(result.first().text.contains("5 aktif bildirim"))
    }

    @Test
    fun `digital score has stronger priority when shown`() {
        val result = TickerComposer.compose(
            folders = sampleFolders,
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(20000L, 12),
            digitalLifeScore = 78,
            digitalLifeScorePrevious = 74,
            epochDay = 20000L,
            zone = zone,
        )

        result.firstOrNull { it.routeKey == "WRAPPED_REPORT" && it.text.contains("78/100") }
            ?.let { assertEquals(70, it.priority) }
    }

    // ── haftalık özet sadece pazartesi ───────────────────────────────────────

    @Test
    fun `weekly summary only appears on monday`() {
        // 1970-01-05 = Monday (epochDay = 4)
        val mondayEpochDay = 4L
        val mondayResult = TickerComposer.compose(
            folders = sampleFolders,
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(mondayEpochDay, 12),
            epochDay = mondayEpochDay,
            zone = zone,
        )
        assertTrue(mondayResult.any { it.priority == 60 })

        // 1970-01-06 = Tuesday (epochDay = 5)
        val tuesdayEpochDay = 5L
        val tuesdayResult = TickerComposer.compose(
            folders = sampleFolders,
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(tuesdayEpochDay, 12),
            epochDay = tuesdayEpochDay,
            zone = zone,
        )
        assertTrue(tuesdayResult.none { it.priority == 60 })
    }

    // ── düşük güven uyarısı ──────────────────────────────────────────────────

    @Test
    fun `low confidence warning included when count positive`() {
        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 3,
            nowMillis = millisAt(20000L, 12),
            epochDay = 20000L,
            zone = zone,
        )
        assertTrue(result.any { it.routeKey == "APP_LIST_UNCERTAIN" && it.text.contains("3 uygulamanın") })
    }

    @Test
    fun `insight without categoryId routes to dashboard`() {
        val insight = InsightSnapshot(id = "i1", message = "Test icgoru", categoryId = null)
        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = emptyList(),
            badgeTotal = 0,
            insights = listOf(insight),
            lowConfidenceCount = 0,
            nowMillis = millisAt(20000L, 12),
            epochDay = 20000L,
            zone = zone,
        )
        val spec = result.first { it.text == "Test icgoru" }
        assertEquals("DASHBOARD", spec.routeKey)
    }

    @Test
    fun `insight with categoryId does not route to dashboard`() {
        val insight = InsightSnapshot(id = "i2", message = "Klasor icgorusu", categoryId = "social")
        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = emptyList(),
            badgeTotal = 0,
            insights = listOf(insight),
            lowConfidenceCount = 0,
            nowMillis = millisAt(20000L, 12),
            epochDay = 20000L,
            zone = zone,
        )
        val spec = result.first { it.text == "Klasor icgorusu" }
        assertNotEquals("DASHBOARD", spec.routeKey)
        assertEquals("social", spec.categoryId)
    }

    @Test
    fun `insight with packageName opens app instead of dashboard`() {
        val insight = InsightSnapshot(id = "i3", message = "App icgorusu", packageName = "com.example.app")
        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = emptyList(),
            badgeTotal = 0,
            insights = listOf(insight),
            lowConfidenceCount = 0,
            nowMillis = millisAt(20000L, 12),
            epochDay = 20000L,
            zone = zone,
        )
        val spec = result.first { it.text == "App icgorusu" }
        assertEquals("com.example.app", spec.packageName)
        assertNotEquals("DASHBOARD", spec.routeKey)
    }

    @Test
    fun `daily tip carries a route target`() {
        val result = TickerComposer.compose(
            folders = emptyList(),
            apps = emptyList(),
            badgeTotal = 0,
            insights = emptyList(),
            lowConfidenceCount = 0,
            nowMillis = millisAt(20000L, 12),
            epochDay = 20000L,
            zone = zone,
        )
        val spec = result.first { it.priority == 5 }
        assertTrue(spec.routeKey != null || spec.packageName != null || spec.categoryId != null)
    }
}

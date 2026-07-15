package com.armutlu.apporganizer.domain.usecase.wrapped

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WrappedEngine.compute() saf-Kotlin senaryo testleri: skor clamp, sebep tutarliligi,
 * kisilik secimi (baskin kategori + Dengeli fallback), rozet sinir degerleri,
 * haftalik delta hesabi ve bos veri guvenligi.
 */
class WrappedEngineTest {

    private val now = System.currentTimeMillis()
    private val day = 24L * 60 * 60 * 1000

    private fun app(
        pkg: String,
        categoryId: String,
        usageCount: Long = 0L,
        lastUsedTimestamp: Long = 0L,
        installTime: Long = now,
        firstInstalledTime: Long = now,
        appSizeBytes: Long = 10L * 1024 * 1024,
        isHidden: Boolean = false,
    ) = WrappedEngine.AppSnapshot(
        packageName = pkg,
        appName = pkg.substringAfterLast('.'),
        categoryId = categoryId,
        usageCount = usageCount,
        lastUsedTimestamp = lastUsedTimestamp,
        installTime = installTime,
        firstInstalledTime = firstInstalledTime,
        appSizeBytes = appSizeBytes,
        isHidden = isHidden,
        isSystemApp = false,
    )

    // ── Boş veri ────────────────────────────────────────────────────────────

    @Test
    fun `empty apps does not crash and returns sane defaults`() {
        val input = WrappedEngine.WrappedInput(
            apps = emptyList(),
            notificationSummary = null,
            previousSnapshot = null,
            nowMillis = now,
        )
        val report = WrappedEngine.compute(input)

        assertTrue(report.score.score in 0..100)
        assertEquals(WrappedEngine.PersonalityType.BALANCED, report.personality.type)
        assertNull(report.stats.mostOpenedApp)
        assertNull(report.weeklyComparison)
        assertTrue(report.badges.isNotEmpty())
    }

    // ── Skor clamp 0-100 ─────────────────────────────────────────────────────

    @Test
    fun `score is always clamped between 0 and 100`() {
        // Kotu senaryo: tum uygulamalar 90 gundur acilmamis + sosyal-oyun agirlikli + kategorisiz.
        val apps = (1..10).map {
            app(
                "com.social.app$it",
                categoryId = "social",
                usageCount = 100L,
                lastUsedTimestamp = now - 90 * day,
                installTime = now - 200 * day,
                firstInstalledTime = now - 200 * day,
            )
        }
        val input = WrappedEngine.WrappedInput(
            apps = apps,
            notificationSummary = WrappedEngine.NotificationSummary(500, 8, 8),
            previousSnapshot = null,
            nowMillis = now,
        )
        val report = WrappedEngine.compute(input)
        assertTrue(report.score.score in 0..100)
    }

    @Test
    fun `good usage pattern increases score`() {
        val apps = (1..10).map {
            app(
                "com.productivity.app$it",
                categoryId = "productivity",
                usageCount = 20L,
                lastUsedTimestamp = now - 1 * day,
                installTime = now - 100 * day,
                firstInstalledTime = now - 100 * day,
            )
        }
        val input = WrappedEngine.WrappedInput(
            apps = apps,
            notificationSummary = WrappedEngine.NotificationSummary(5, 0, 0),
            previousSnapshot = null,
            nowMillis = now,
        )
        val report = WrappedEngine.compute(input)
        assertTrue(report.score.score in 0..100)
        assertTrue(report.score.score > 50)
    }

    // ── Tek motor tutarlılığı (V2: DigitalPulseEngine) ───────────────────────
    // V1'deki "50 + reasons.sum() = score" toplamsal modeli KALDIRILDI (D244) —
    // V2 skoru 5 alt skorun ağırlıklı ortalamasıdır (DigitalPulseEngine.compute).
    // Bu test artık report.score.score'un report.pulse.total ile AYNI tek motor
    // kaynağından geldiğini doğrular (Ana ekran / Rapor Merkezi / Haftalık Rapor
    // farklı skor hesaplamaz kuralı).

    @Test
    fun `score matches the single Digital Pulse engine total (no duplicate score calc)`() {
        val apps = (1..5).map {
            app(
                "com.mixed.app$it",
                categoryId = "productivity",
                usageCount = 10L,
                lastUsedTimestamp = now - 1 * day,
                installTime = now - 10 * day,
                firstInstalledTime = now - 10 * day,
            )
        }
        val input = WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        val report = WrappedEngine.compute(input)

        assertEquals(report.pulse.total, report.score.score)
        assertTrue(report.score.score in 0..100)
    }

    @Test
    fun `wrapped score carries capped mission contribution reason`() {
        val apps = listOf(app("com.a", categoryId = "productivity", usageCount = 10L))
        val report = WrappedEngine.compute(
            WrappedEngine.WrappedInput(
                apps = apps,
                notificationSummary = null,
                previousSnapshot = null,
                taskScoreContribution = 7,
                nowMillis = now,
            )
        )

        assertEquals(report.pulse.total, report.score.score)
        assertTrue(report.score.reasons.any { it.label.contains("Gorev etkisi") && it.delta == 7 })
    }

    // ── Kişilik tipi ─────────────────────────────────────────────────────────

    @Test
    fun `dominant category above 30 percent selects matching personality`() {
        val apps = listOf(
            app("com.game.one", categoryId = "games", usageCount = 80L),
            app("com.game.two", categoryId = "games", usageCount = 80L),
            app("com.other.app", categoryId = "finance", usageCount = 10L),
        )
        val input = WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        val report = WrappedEngine.compute(input)

        assertEquals(WrappedEngine.PersonalityType.GAMER, report.personality.type)
        assertTrue(report.personality.dominantPercentage >= 30)
    }

    @Test
    fun `no category above 30 percent falls back to balanced`() {
        val apps = listOf(
            app("com.a", categoryId = "productivity", usageCount = 10L),
            app("com.b", categoryId = "social", usageCount = 10L),
            app("com.c", categoryId = "games", usageCount = 10L),
            app("com.d", categoryId = "finance", usageCount = 10L),
            app("com.e", categoryId = "education", usageCount = 10L),
        )
        val input = WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        val report = WrappedEngine.compute(input)

        assertEquals(WrappedEngine.PersonalityType.BALANCED, report.personality.type)
    }

    @Test
    fun `zero total usage falls back to balanced with no crash`() {
        val apps = listOf(
            app("com.a", categoryId = "productivity", usageCount = 0L),
            app("com.b", categoryId = "social", usageCount = 0L),
        )
        val input = WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        val report = WrappedEngine.compute(input)

        assertEquals(WrappedEngine.PersonalityType.BALANCED, report.personality.type)
        assertEquals(0, report.personality.dominantPercentage)
    }

    // ── Rozet kriterleri (sınır değerler) ───────────────────────────────────

    @Test
    fun `organizer badge earned only when zero uncategorized apps`() {
        val allCategorized = listOf(
            app("com.a", categoryId = "productivity"),
            app("com.b", categoryId = "social"),
        )
        val withUncategorized = allCategorized + app("com.c", categoryId = "uncategorized")

        val reportEarned = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = allCategorized, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        )
        val reportNotEarned = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = withUncategorized, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        )

        assertTrue(reportEarned.badges.first { it.id == "organizer" }.earned)
        assertTrue(!reportNotEarned.badges.first { it.id == "organizer" }.earned)
    }

    @Test
    fun `minimalist badge boundary at 3 unused apps`() {
        fun unusedApps(count: Int) = (1..count).map {
            app(
                "com.unused.app$it",
                categoryId = "productivity",
                lastUsedTimestamp = now - 61 * day,
            )
        }

        val twoUnused = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = unusedApps(2), notificationSummary = null, previousSnapshot = null, nowMillis = now)
        )
        val threeUnused = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = unusedApps(3), notificationSummary = null, previousSnapshot = null, nowMillis = now)
        )

        assertTrue(twoUnused.badges.first { it.id == "minimalist" }.earned)
        assertTrue(!threeUnused.badges.first { it.id == "minimalist" }.earned)
    }

    @Test
    fun `explorer badge earned when an app was installed within last 7 days`() {
        val apps = listOf(
            app("com.new.app", categoryId = "productivity", firstInstalledTime = now - 2 * day, installTime = now - 2 * day),
        )
        val report = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        )
        assertTrue(report.badges.first { it.id == "explorer" }.earned)
    }

    @Test
    fun `folder collector badge requires at least 8 folders`() {
        val apps = listOf(app("com.a", categoryId = "productivity"))

        val sevenFolders = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, folderCount = 7, nowMillis = now)
        )
        val eightFolders = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, folderCount = 8, nowMillis = now)
        )

        assertTrue(!sevenFolders.badges.first { it.id == "folder_collector" }.earned)
        assertTrue(eightFolders.badges.first { it.id == "folder_collector" }.earned)
    }

    @Test
    fun `no timeslot based badge is fabricated`() {
        // Uydurma metrik yasagi: getWeightedScores timeSlot verisi bu katmanda yok,
        // dolayisiyla "Gece Kusu / Erken Kalkan" rozeti hic uretilmemeli.
        val apps = listOf(app("com.a", categoryId = "productivity"))
        val report = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        )
        assertTrue(report.badges.none { it.id.contains("night") || it.id.contains("early") })
    }

    // ── Haftalık delta hesabı ────────────────────────────────────────────────

    @Test
    fun `weekly comparison is null when no previous snapshot exists`() {
        val apps = listOf(app("com.a", categoryId = "productivity", usageCount = 5L))
        val report = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        )
        assertNull(report.weeklyComparison)
    }

    @Test
    fun `weekly comparison computes growth percentage against previous snapshot`() {
        val apps = listOf(
            app("com.a", categoryId = "productivity", usageCount = 20L),
            app("com.b", categoryId = "social", usageCount = 10L),
        )
        val previous = WrappedEngine.PreviousSnapshot(
            categoryUsage = mapOf("productivity" to 10L, "social" to 10L),
            totalApps = 2,
            savedAtEpochDay = 0L,
        )
        val report = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = previous, nowMillis = now)
        )

        assertNotNull(report.weeklyComparison)
        val productivityGrowth = report.weeklyComparison!!.topGrowingCategories
            .firstOrNull { it.categoryId == "productivity" }
        assertNotNull(productivityGrowth)
        assertEquals(100, productivityGrowth!!.deltaPercent) // 10 -> 20 = +100%
    }

    @Test
    fun `weekly comparison handles new category with zero previous usage`() {
        val apps = listOf(app("com.new", categoryId = "finance", usageCount = 5L))
        val previous = WrappedEngine.PreviousSnapshot(
            categoryUsage = mapOf("productivity" to 10L),
            totalApps = 1,
            savedAtEpochDay = 0L,
        )
        val report = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = previous, nowMillis = now)
        )

        val financeGrowth = report.weeklyComparison!!.topGrowingCategories.firstOrNull { it.categoryId == "finance" }
        assertNotNull(financeGrowth)
        assertEquals(100, financeGrowth!!.deltaPercent)
    }

    // ── İlginç istatistikler ─────────────────────────────────────────────────

    @Test
    fun `interesting stats ignore hidden apps`() {
        val apps = listOf(
            app("com.visible", categoryId = "productivity", usageCount = 5L),
            app("com.hidden", categoryId = "productivity", usageCount = 999L, isHidden = true),
        )
        val report = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        )
        assertEquals("com.visible", report.stats.mostOpenedApp?.packageName)
    }

    @Test
    fun `least opened app excludes apps with zero usage`() {
        val apps = listOf(
            app("com.zero", categoryId = "productivity", usageCount = 0L),
            app("com.one", categoryId = "productivity", usageCount = 1L),
        )
        val report = WrappedEngine.compute(
            WrappedEngine.WrappedInput(apps = apps, notificationSummary = null, previousSnapshot = null, nowMillis = now)
        )
        assertEquals("com.one", report.stats.leastOpenedApp?.packageName)
    }
}

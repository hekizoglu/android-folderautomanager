package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.PulseStatusBand
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.presentation.ui.launcher.AppFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeV2AssemblerTest {

    private fun category(id: String, name: String = id, emoji: String = "📁") =
        Category(categoryId = id, categoryName = name, iconEmoji = emoji, isSystemCategory = false)

    private fun app(
        pkg: String,
        name: String = pkg,
        usage: Long = 0L,
        lastUsed: Long = 0L,
        launchCount: Long = 0L,
        notifCount: Int = 0,
        notifImportance: Int = 0,
        hidden: Boolean = false,
    ) = AppInfo(
        packageName = pkg,
        appName = name,
        usageCount = usage,
        lastUsedTimestamp = lastUsed,
        launchCount = launchCount,
        notificationCount = notifCount,
        notificationImportance = notifImportance,
        isHidden = hidden,
    )

    private fun assemble(
        folders: List<AppFolder> = emptyList(),
        pending: Int = 0,
        notifPermMissing: Boolean = false,
        pulse: HomePulseSummary? = null,
        mission: HomeMissionSummary? = null,
        dismissals: Set<String> = emptySet(),
    ) = HomeV2Assembler.assemble(
        initialLoadDone = true,
        folders = folders,
        dockPackages = listOf("com.a"),
        pageSize = 8,
        pendingClassificationsCount = pending,
        notificationPermissionMissing = notifPermMissing,
        pulseSummary = pulse,
        missionSummary = mission,
        bannerDismissals = dismissals,
    )

    @Test
    fun `folder tile preview is ordered by usage then recency and capped`() {
        val folder = AppFolder(
            category = category("games", "Oyunlar", "🎮"),
            apps = listOf(
                app("com.low", usage = 1L),
                app("com.top", usage = 100L),
                app("com.mid", usage = 50L),
                app("com.recent", usage = 50L, lastUsed = 999L),
                app("com.extra1", usage = 0L),
                app("com.extra2", usage = 0L),
            ),
        )

        val tile = assemble(folders = listOf(folder)).folders.single()

        assertEquals(listOf("com.top", "com.recent", "com.mid", "com.low"), tile.previewPackages)
        assertEquals("Oyunlar", tile.title)
        assertEquals("🎮", tile.emoji)
        assertEquals(6, tile.appCount)
    }

    @Test
    fun `hidden apps are excluded from preview count and quick launch`() {
        val folder = AppFolder(
            category = category("social"),
            apps = listOf(
                app("com.hidden", usage = 1_000L, launchCount = 50L, hidden = true),
                app("com.visible", usage = 10L, launchCount = 5L),
            ),
        )

        val tile = assemble(folders = listOf(folder)).folders.single()

        assertEquals(listOf("com.visible"), tile.previewPackages)
        assertEquals(1, tile.appCount)
        assertEquals("com.visible", tile.quickLaunchPackage)
    }

    @Test
    fun `urgent notification flag requires importance at or above threshold`() {
        val calm = AppFolder(
            category = category("news"),
            apps = listOf(app("com.a", notifCount = 3, notifImportance = 1)),
        )
        val urgent = AppFolder(
            category = category("finance"),
            apps = listOf(app("com.b", notifCount = 1, notifImportance = 2)),
        )

        val tiles = assemble(folders = listOf(calm, urgent)).folders

        assertFalse(tiles[0].hasUrgentNotification)
        assertTrue(tiles[1].hasUrgentNotification)
        assertEquals(3, tiles[0].notificationTotal)
        assertEquals(1, tiles[1].notificationTotal)
    }

    @Test
    fun `banner priority is permission first and dismissals stick`() {
        val both = assemble(pending = 4, notifPermMissing = true)
        assertEquals(1, both.banners.size)
        assertEquals(HomeV2Assembler.BANNER_ID_NOTIFICATION_PERMISSION, both.banners.single().id)

        val dismissed = assemble(
            pending = 4,
            notifPermMissing = true,
            dismissals = setOf(HomeV2Assembler.BANNER_ID_NOTIFICATION_PERMISSION),
        )
        assertEquals(HomeV2Assembler.BANNER_ID_PENDING_CLASSIFICATIONS, dismissed.banners.single().id)

        val none = assemble(pending = 0, notifPermMissing = false)
        assertTrue(none.banners.isEmpty())
    }

    @Test
    fun `pulse strip appears only with actionable data`() {
        val noData = assemble()
        assertNull(noData.pulse)

        val lowConfidence = assemble(
            pulse = HomePulseSummary(
                score = 72,
                statusBand = PulseStatusBand.GOOD,
                delta = null,
                topReasonId = null,
                confidence = DataConfidence.LOW,
                freshness = DataFreshness.LIVE,
            ),
        )
        // LOW confidence: skor gösterilmez ama gorev yoksa strip hic uretilmez
        assertNull(lowConfidence.pulse)

        val withMission = assemble(
            pulse = HomePulseSummary(
                score = 72,
                statusBand = PulseStatusBand.GOOD,
                delta = 3,
                topReasonId = null,
                confidence = DataConfidence.HIGH,
                freshness = DataFreshness.LIVE,
            ),
            mission = HomeMissionSummary(
                completedCount = 1,
                totalCount = 3,
                primaryMissionId = "m1",
                primaryTitle = "Ekran süreni koru",
                primaryCurrentText = null,
                primaryRemainingText = null,
                primaryStatus = null,
                urgent = false,
                primaryProgressFraction = 0.4f,
                currentStreak = 3,
            ),
        )
        assertEquals("72", withMission.pulse?.pulseScoreText)
        assertEquals("Ekran süreni koru", withMission.pulse?.missionTitle)
        assertEquals(0.4f, withMission.pulse?.missionProgressFraction ?: 0f)
        assertEquals(3, withMission.pulse?.missionStreak)
    }

    @Test
    fun `page size is never zero`() {
        val state = HomeV2Assembler.assemble(
            initialLoadDone = false,
            folders = emptyList(),
            dockPackages = emptyList(),
            pageSize = 0,
            pendingClassificationsCount = 0,
            notificationPermissionMissing = false,
            pulseSummary = null,
            missionSummary = null,
            bannerDismissals = emptySet(),
        )
        assertTrue(state.loading)
        assertEquals(1, state.pageSize)
    }
}

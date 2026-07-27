package com.armutlu.apporganizer.domain.usecase.goals

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.common.DataFreshnessResolver
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.missions.MissionUsageStatsSource
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P1 — [CategoryUsageSnapshotProvider]. Sahte [MissionUsageStatsSource] + [AppDao] ile
 * izin-yok/kısmi-veri/gizli-sistem-filtresi/ISO-hafta senaryolarını doğrular.
 */
class CategoryUsageSnapshotProviderTest {

    // Sabit "şimdi": Cuma 2026-07-17 12:00:00 UTC — ISO hafta Pazartesi 2026-07-13 başlar,
    // önceki ISO hafta 2026-07-06 - 2026-07-13 arası.
    private val fixedInstant: Instant = Instant.parse("2026-07-17T12:00:00Z")
    private val zoneId = ZoneOffset.UTC

    private fun clockAt(instant: Instant = fixedInstant): Clock = Clock.fixed(instant, zoneId)

    private class FakeUsageStatsSource(
        private val sessions: List<DailyPackageUsage>?,
    ) : MissionUsageStatsSource {
        override fun getDailySessionUsage(context: Context, days: Int, nowMillis: Long): List<DailyPackageUsage>? = sessions
        override fun getUnlockCount(context: Context, days: Int, nowMillis: Long): Int? = null
        override fun getUnlockCountPerDay(context: Context, days: Int, nowMillis: Long): Map<Long, Int>? = null
        override fun getScreenOnEventsInWindow(
            context: Context,
            startHour: Int,
            endHour: Int,
            date: java.time.LocalDate,
        ): Boolean? = null
    }

    private class FakeAppDao(private val apps: List<AppInfo>) : AppDao by mockk(relaxed = true) {
        override suspend fun getAllApps(): List<AppInfo> = apps
    }

    private fun app(
        packageName: String,
        categoryId: String,
        isHidden: Boolean = false,
        isSystemApp: Boolean = false,
    ) = AppInfo(
        packageName = packageName,
        appName = packageName,
        categoryId = categoryId,
        isHidden = isHidden,
        isSystemApp = isSystemApp,
    )

    private fun dailyUsage(epochDay: Long, foregroundDurationMs: Long, packageName: String) = DailyPackageUsage(
        localDate = java.time.LocalDate.ofEpochDay(epochDay).toString(),
        epochDay = epochDay,
        packageName = packageName,
        launchCount = 1,
        foregroundDurationMs = foregroundDurationMs,
        hourlyForegroundMs = LongArray(24).toList(),
        globalForegroundMs = foregroundDurationMs,
        isPartial = false,
    )

    private fun buildProvider(
        sessions: List<DailyPackageUsage>?,
        apps: List<AppInfo>,
        instant: Instant = fixedInstant,
    ): CategoryUsageSnapshotProvider {
        val clock = clockAt(instant)
        return CategoryUsageSnapshotProvider(
            context = mockk<Context>(relaxed = true),
            periodBoundaryResolver = PeriodBoundaryResolver(clock, zoneId),
            dataFreshnessResolver = DataFreshnessResolver(clock),
            clock = clock,
            zoneId = zoneId,
            usageStatsSource = FakeUsageStatsSource(sessions),
            appDao = FakeAppDao(apps),
        )
    }

    @Test
    fun `no permission means empty maps and UNAVAILABLE freshness, not fake zero`() = runBlocking {
        val provider = buildProvider(sessions = null, apps = listOf(app("com.social.app", Category.CAT_SOCIAL)))
        val snapshot = provider.capture()

        assertEquals(DataFreshness.UNAVAILABLE, snapshot.freshness)
        assertTrue(snapshot.previousWeekMinutesByCategory.isEmpty())
        assertTrue(snapshot.currentWeekMinutesByCategory.isEmpty())
        assertEquals(0, snapshot.validDataDayCount)
        assertEquals(null, snapshot.previousWeekMinutes(Category.CAT_SOCIAL))
    }

    @Test
    fun `previous ISO week usage is summed per category from foreground minutes`() = runBlocking {
        // Onceki ISO hafta: Pazartesi 2026-07-06 - Pazar 2026-07-12 (dahil).
        val mondayEpochDay = Instant.parse("2026-07-06T00:00:00Z").atZone(zoneId).toLocalDate().toEpochDay()
        val sessions = listOf(
            dailyUsage(mondayEpochDay, foregroundDurationMs = 30 * 60_000L, packageName = "com.social.app"),
            dailyUsage(mondayEpochDay + 1, foregroundDurationMs = 20 * 60_000L, packageName = "com.social.app"),
            dailyUsage(mondayEpochDay + 2, foregroundDurationMs = 15 * 60_000L, packageName = "com.game.app"),
        )
        val apps = listOf(
            app("com.social.app", Category.CAT_SOCIAL),
            app("com.game.app", Category.CAT_GAMES),
        )
        val provider = buildProvider(sessions = sessions, apps = apps)
        val snapshot = provider.capture()

        assertEquals(DataFreshness.LIVE, snapshot.freshness)
        assertEquals(50L, snapshot.previousWeekMinutes(Category.CAT_SOCIAL))
        assertEquals(15L, snapshot.previousWeekMinutes(Category.CAT_GAMES))
        assertEquals(3, snapshot.validDataDayCount)
    }

    @Test
    fun `hidden and system apps are excluded from category aggregation`() = runBlocking {
        val mondayEpochDay = Instant.parse("2026-07-06T00:00:00Z").atZone(zoneId).toLocalDate().toEpochDay()
        val sessions = listOf(
            dailyUsage(mondayEpochDay, foregroundDurationMs = 30 * 60_000L, packageName = "com.hidden.app"),
            dailyUsage(mondayEpochDay, foregroundDurationMs = 10 * 60_000L, packageName = "com.system.app"),
            dailyUsage(mondayEpochDay, foregroundDurationMs = 5 * 60_000L, packageName = "com.visible.app"),
        )
        val apps = listOf(
            app("com.hidden.app", Category.CAT_SOCIAL, isHidden = true),
            app("com.system.app", Category.CAT_SOCIAL, isSystemApp = true),
            app("com.visible.app", Category.CAT_SOCIAL),
        )
        val provider = buildProvider(sessions = sessions, apps = apps)
        val snapshot = provider.capture()

        assertEquals(5L, snapshot.previousWeekMinutes(Category.CAT_SOCIAL))
    }

    @Test
    fun `unmapped package category is dropped instead of crashing`() = runBlocking {
        val mondayEpochDay = Instant.parse("2026-07-06T00:00:00Z").atZone(zoneId).toLocalDate().toEpochDay()
        val sessions = listOf(
            dailyUsage(mondayEpochDay, foregroundDurationMs = 30 * 60_000L, packageName = "com.unknown.app"),
        )
        val provider = buildProvider(sessions = sessions, apps = emptyList())
        val snapshot = provider.capture()

        assertTrue(snapshot.previousWeekMinutesByCategory.isEmpty())
    }

    @Test
    fun `current ISO week and previous ISO week are aggregated independently`() = runBlocking {
        val previousMondayEpochDay = Instant.parse("2026-07-06T00:00:00Z").atZone(zoneId).toLocalDate().toEpochDay()
        val currentMondayEpochDay = Instant.parse("2026-07-13T00:00:00Z").atZone(zoneId).toLocalDate().toEpochDay()
        val sessions = listOf(
            dailyUsage(previousMondayEpochDay, foregroundDurationMs = 40 * 60_000L, packageName = "com.social.app"),
            dailyUsage(currentMondayEpochDay, foregroundDurationMs = 10 * 60_000L, packageName = "com.social.app"),
        )
        val apps = listOf(app("com.social.app", Category.CAT_SOCIAL))
        val provider = buildProvider(sessions = sessions, apps = apps)
        val snapshot = provider.capture()

        assertEquals(40L, snapshot.previousWeekMinutes(Category.CAT_SOCIAL))
        assertEquals(10L, snapshot.currentWeekMinutes(Category.CAT_SOCIAL))
    }
}

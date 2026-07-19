package com.armutlu.apporganizer.domain.usecase.missions

import android.content.Context
import com.armutlu.apporganizer.data.local.TaskScoreEventDao
import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.common.DataFreshnessResolver
import com.armutlu.apporganizer.domain.models.TaskScoreEventEntry
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import com.armutlu.apporganizer.utils.TaskScoreManager
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MissionMetricSnapshotProvider — Dongu M02 (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satir 790-848). Sahte [MissionUsageStatsSource] + in-memory [TaskScoreEventDao] ile
 * kullanim izni/veri-yok/haftalik-ISO/gece-kullanimi senaryolarini dogrular.
 */
class MissionMetricSnapshotProviderTest {

    // Sabit "simdi": Cuma 2026-07-17 12:00:00 UTC — ISO hafta Pazartesi 2026-07-13 baslar.
    private val fixedInstant: Instant = Instant.parse("2026-07-17T12:00:00Z")
    private val zoneId = ZoneOffset.UTC

    private fun clockAt(instant: Instant = fixedInstant): Clock = Clock.fixed(instant, zoneId)

    private class FakeTaskScoreEventDao(
        private val events: MutableList<TaskScoreEventEntry> = mutableListOf(),
    ) : TaskScoreEventDao {
        fun seed(eventKey: String, delta: Int, createdAt: Long) {
            events += TaskScoreEventEntry(
                eventKey = eventKey,
                label = eventKey,
                delta = delta,
                createdAt = createdAt,
            )
        }

        override suspend fun insert(entry: TaskScoreEventEntry): Long {
            events += entry
            return events.size.toLong()
        }

        override suspend fun getTotalScore(): Int = events.sumOf { it.delta }

        override suspend fun getScoreBetween(fromInclusive: Long, toInclusive: Long): Int =
            events.filter { it.createdAt in fromInclusive..toInclusive }.sumOf { it.delta }

        override suspend fun getLatestEvent(): TaskScoreEventEntry? =
            events.maxByOrNull { it.createdAt }

        override suspend fun countEventsBetween(
            fromInclusive: Long,
            toInclusive: Long,
            positiveOnly: Boolean,
        ): Int = events.count {
            it.createdAt in fromInclusive..toInclusive && (!positiveOnly || it.delta > 0)
        }

        override suspend fun countEventsBetweenByKeys(
            fromInclusive: Long,
            toInclusive: Long,
            eventKeys: List<String>,
        ): Int = events.count {
            it.createdAt in fromInclusive..toInclusive && it.eventKey in eventKeys
        }

        override suspend fun insertOnceBetween(
            entry: TaskScoreEventEntry,
            fromInclusive: Long,
            toInclusive: Long,
        ): Boolean {
            if (countEventsBetweenByKeys(fromInclusive, toInclusive, listOf(entry.eventKey)) > 0) return false
            insert(entry)
            return true
        }

        override suspend fun getPositiveScore(): Int = events.filter { it.delta > 0 }.sumOf { it.delta }

        override suspend fun getNegativeScore(): Int = events.filter { it.delta < 0 }.sumOf { it.delta }

        override suspend fun clearAll() {
            events.clear()
        }
    }

    private class FakeUsageStatsSource(
        private val sessions: List<DailyPackageUsage>?,
        private val unlockCount: Int?,
        private val unlockCountPerDay: Map<Long, Int>? = null,
    ) : MissionUsageStatsSource {
        override fun getDailySessionUsage(context: Context, days: Int, nowMillis: Long): List<DailyPackageUsage>? = sessions

        override fun getUnlockCount(context: Context, days: Int, nowMillis: Long): Int? = unlockCount

        override fun getUnlockCountPerDay(context: Context, days: Int, nowMillis: Long): Map<Long, Int>? = unlockCountPerDay
    }

    /** Dongu G3a — DAILY_MORNING_CALM icin sahte kategori->paket eslesmesi. */
    private class FakeAppDao(private val socialPackages: List<String> = emptyList()) : com.armutlu.apporganizer.data.local.AppDao by mockk(relaxed = true) {
        override suspend fun getPackageNamesByCategory(categoryId: String): List<String> =
            if (categoryId == com.armutlu.apporganizer.domain.models.Category.CAT_SOCIAL) socialPackages else emptyList()
    }

    private fun buildProvider(
        instant: Instant = fixedInstant,
        sessions: List<DailyPackageUsage>?,
        unlockCount: Int?,
        dao: FakeTaskScoreEventDao = FakeTaskScoreEventDao(),
        appDao: com.armutlu.apporganizer.data.local.AppDao = FakeAppDao(),
    ): MissionMetricSnapshotProvider {
        val clock = clockAt(instant)
        return MissionMetricSnapshotProvider(
            context = mockk<Context>(relaxed = true),
            periodBoundaryResolver = PeriodBoundaryResolver(clock, zoneId),
            dataFreshnessResolver = DataFreshnessResolver(clock),
            taskScoreEventDao = dao,
            clock = clock,
            zoneId = zoneId,
            usageStatsSource = FakeUsageStatsSource(sessions, unlockCount),
            appDao = appDao,
        )
    }

    private fun dailyUsage(
        epochDay: Long,
        globalForegroundMs: Long,
        hourly: LongArray = LongArray(24),
        packageName: String = "com.example.app",
    ) = DailyPackageUsage(
        localDate = java.time.LocalDate.ofEpochDay(epochDay).toString(),
        epochDay = epochDay,
        packageName = packageName,
        launchCount = 1,
        foregroundDurationMs = globalForegroundMs,
        hourlyForegroundMs = hourly.toList(),
        globalForegroundMs = globalForegroundMs,
        isPartial = false,
    )

    @Test
    fun `no permission means usage metrics are null but action counters stay populated`() = runBlocking {
        val dao = FakeTaskScoreEventDao()
        val todayStartMs = fixedInstant.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        dao.seed(TaskScoreManager.EventType.ClassificationApproved.eventKey, 5, todayStartMs + 1000)
        dao.seed(TaskScoreManager.EventType.NotificationReportViewed.eventKey, 1, todayStartMs + 2000)

        val provider = buildProvider(sessions = null, unlockCount = null, dao = dao)
        val snapshot = provider.capture()

        assertNull(snapshot.screenTimeMinutesToday)
        assertNull(snapshot.unlockCountToday)
        assertNull(snapshot.usedAfter23Today)
        assertNull(snapshot.firstUseAfter23At)
        assertNull(snapshot.screenTimeMinutesThisWeek)
        assertNull(snapshot.screenTimeMinutesPreviousWeek)
        assertEquals(1, snapshot.classificationActionsToday)
        assertTrue(snapshot.notificationReportViewedToday)
    }

    @Test
    fun `zero usage is real zero, no data is null`() = runBlocking {
        val epochDay = fixedInstant.atZone(zoneId).toLocalDate().toEpochDay()
        // Sadece dunun verisi var — bugun icin sessions listesinde girdi yok ama liste non-null
        // (izin var, veri var demek) — bu yuzden bugun gercek 0 olmali.
        val yesterday = dailyUsage(epochDay - 1, globalForegroundMs = 60_000L)
        val provider = buildProvider(sessions = listOf(yesterday), unlockCount = 0)

        val snapshot = provider.capture()

        assertEquals(0L, snapshot.screenTimeMinutesToday)
        assertEquals(0, snapshot.unlockCountToday)
    }

    @Test
    fun `weekly boundaries follow ISO week (Monday start)`() = runBlocking {
        // fixedInstant = Cuma 2026-07-17. ISO hafta Pazartesi 2026-07-13 basliyor.
        val monday = java.time.LocalDate.of(2026, 7, 13).toEpochDay()
        val tuesday = monday + 1
        val prevWeekThursday = monday - 3 // onceki haftanin icinde bir gun

        val sessions = listOf(
            dailyUsage(monday, globalForegroundMs = 30 * 60_000L), // 30 dk
            dailyUsage(tuesday, globalForegroundMs = 45 * 60_000L), // 45 dk
            dailyUsage(prevWeekThursday, globalForegroundMs = 20 * 60_000L), // 20 dk (onceki hafta)
        )
        val provider = buildProvider(sessions = sessions, unlockCount = 5)

        val snapshot = provider.capture()

        assertEquals(75L, snapshot.screenTimeMinutesThisWeek) // 30+45, bugun (Cuma) veri yok -> eklenmiyor
        assertEquals(20L, snapshot.screenTimeMinutesPreviousWeek)
    }

    @Test
    fun `first night usage after 23h is detected correctly`() = runBlocking {
        val epochDay = fixedInstant.atZone(zoneId).toLocalDate().toEpochDay()
        val hourly = LongArray(24).also { it[23] = 5 * 60_000L } // 23:00 sonrasi 5 dk kullanim
        val today = dailyUsage(epochDay, globalForegroundMs = 10 * 60_000L, hourly = hourly)
        val provider = buildProvider(sessions = listOf(today), unlockCount = 2)

        val snapshot = provider.capture()

        assertTrue(snapshot.usedAfter23Today == true)
        val expectedNightStart = fixedInstant.atZone(zoneId).toLocalDate()
            .atTime(23, 0).atZone(zoneId).toInstant().toEpochMilli()
        assertEquals(expectedNightStart, snapshot.firstUseAfter23At)
    }

    @Test
    fun `no night usage means null firstUseAfter23At`() = runBlocking {
        val epochDay = fixedInstant.atZone(zoneId).toLocalDate().toEpochDay()
        val today = dailyUsage(epochDay, globalForegroundMs = 10 * 60_000L)
        val provider = buildProvider(sessions = listOf(today), unlockCount = 2)

        val snapshot = provider.capture()

        assertFalse(snapshot.usedAfter23Today == true)
        assertNull(snapshot.firstUseAfter23At)
    }

    @Test
    fun `capturedAt matches injected clock and freshness is LIVE`() = runBlocking {
        val provider = buildProvider(sessions = null, unlockCount = null)
        val snapshot = provider.capture()

        assertEquals(fixedInstant.toEpochMilli(), snapshot.capturedAt)
        assertEquals(DataFreshness.LIVE, snapshot.freshness)
    }

    // ── Dongu G3a: yeni gorev sinyalleri ─────────────────────────────────────────────

    @Test
    fun `no usage today means social morning flag is null (window not open yet)`() = runBlocking {
        val provider = buildProvider(sessions = emptyList(), unlockCount = 0)
        val snapshot = provider.capture()

        assertNull(snapshot.socialAppOpenedInFirst30MinToday)
    }

    @Test
    fun `social app active in first used hour marks morning flag true`() = runBlocking {
        val epochDay = fixedInstant.atZone(zoneId).toLocalDate().toEpochDay()
        val hourly = LongArray(24).also { it[7] = 5 * 60_000L } // saat 07 ilk kullanim
        val today = dailyUsage(epochDay, globalForegroundMs = 5 * 60_000L, hourly = hourly, packageName = "com.social.app")
        val provider = buildProvider(
            sessions = listOf(today),
            unlockCount = 1,
            appDao = FakeAppDao(socialPackages = listOf("com.social.app")),
        )

        val snapshot = provider.capture()

        assertTrue(snapshot.socialAppOpenedInFirst30MinToday == true)
    }

    @Test
    fun `non-social app active in first used hour marks morning flag false`() = runBlocking {
        val epochDay = fixedInstant.atZone(zoneId).toLocalDate().toEpochDay()
        val hourly = LongArray(24).also { it[7] = 5 * 60_000L }
        val today = dailyUsage(epochDay, globalForegroundMs = 5 * 60_000L, hourly = hourly, packageName = "com.news.app")
        val provider = buildProvider(
            sessions = listOf(today),
            unlockCount = 1,
            appDao = FakeAppDao(socialPackages = listOf("com.social.app")),
        )

        val snapshot = provider.capture()

        assertFalse(snapshot.socialAppOpenedInFirst30MinToday == true)
    }

    @Test
    fun `folder customized and wrapped report view counters read from TaskScore events`() = runBlocking {
        val dao = FakeTaskScoreEventDao()
        val todayStartMs = fixedInstant.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        dao.seed(TaskScoreManager.EventType.FolderCustomized.eventKey, 2, todayStartMs + 1000)
        dao.seed(TaskScoreManager.EventType.WrappedReportViewed.eventKey, 1, todayStartMs + 2000)

        val provider = buildProvider(sessions = null, unlockCount = null, dao = dao)
        val snapshot = provider.capture()

        assertTrue(snapshot.folderCustomizedToday)
        assertTrue(snapshot.wrappedReportViewedThisWeek)
    }
}

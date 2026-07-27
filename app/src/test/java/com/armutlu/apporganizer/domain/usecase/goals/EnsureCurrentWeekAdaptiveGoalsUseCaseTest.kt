package com.armutlu.apporganizer.domain.usecase.goals

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.WeeklyGoalDao
import com.armutlu.apporganizer.domain.common.DataFreshnessResolver
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import com.armutlu.apporganizer.domain.models.WeeklyGoalMode
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.missions.MissionUsageStatsSource
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import com.armutlu.apporganizer.utils.AppPrefs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * P4 — [EnsureCurrentWeekAdaptiveGoalsUseCase]. Yeni hafta hedef üretimi: pinleme (aynı hafta
 * içinde ikinci çağrı değiştirmez), MANUAL hedeflere dokunmama, eligible kategori filtresi,
 * maksimum aktif hedef sayısı sınırı.
 */
class EnsureCurrentWeekAdaptiveGoalsUseCaseTest {

    private val fixedInstant: Instant = Instant.parse("2026-07-17T12:00:00Z")
    private val zoneId = ZoneOffset.UTC
    private val currentWeekStart = Instant.parse("2026-07-13T00:00:00Z").atZone(zoneId).toLocalDate().toEpochDay()
    private val previousWeekStart = Instant.parse("2026-07-06T00:00:00Z").atZone(zoneId).toLocalDate().toEpochDay()

    private class FakeWeeklyGoalDao : WeeklyGoalDao {
        val goals = mutableListOf<WeeklyGoal>()

        override fun observeGoals(weekStartEpochDay: Long): Flow<List<WeeklyGoal>> =
            flowOf(goals.filter { it.weekStartEpochDay == weekStartEpochDay })

        override suspend fun getGoalsForWeek(weekStartEpochDay: Long): List<WeeklyGoal> =
            goals.filter { it.weekStartEpochDay == weekStartEpochDay }

        override suspend fun upsert(goal: WeeklyGoal) {
            goals.removeAll { it.categoryId == goal.categoryId && it.weekStartEpochDay == goal.weekStartEpochDay }
            goals += goal
        }

        override suspend fun delete(categoryId: String, weekStartEpochDay: Long) {
            goals.removeAll { it.categoryId == categoryId && it.weekStartEpochDay == weekStartEpochDay }
        }

        override suspend fun markAchieved(categoryId: String, weekStartEpochDay: Long, achievedAt: Long) = Unit

        override suspend fun getAutoGoalsForWeek(weekStartEpochDay: Long): List<WeeklyGoal> =
            goals.filter { it.weekStartEpochDay == weekStartEpochDay && it.mode == WeeklyGoalMode.AUTO }

        override suspend fun getGoal(categoryId: String, weekStartEpochDay: Long): WeeklyGoal? =
            goals.firstOrNull { it.categoryId == categoryId && it.weekStartEpochDay == weekStartEpochDay }

        override suspend fun getUnsettledGoals(weekStartEpochDay: Long): List<WeeklyGoal> =
            goals.filter { it.weekStartEpochDay == weekStartEpochDay && it.settledAt == null }

        override suspend fun settle(categoryId: String, weekStartEpochDay: Long, status: String, settledAt: Long) = Unit

        override suspend fun setMode(categoryId: String, weekStartEpochDay: Long, mode: String) {
            val idx = goals.indexOfFirst { it.categoryId == categoryId && it.weekStartEpochDay == weekStartEpochDay }
            if (idx >= 0) goals[idx] = goals[idx].copy(mode = WeeklyGoalMode.valueOf(mode))
        }
    }

    private class FakeUsageStatsSource(private val sessions: List<DailyPackageUsage>?) : MissionUsageStatsSource {
        override fun getDailySessionUsage(context: Context, days: Int, nowMillis: Long) = sessions
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

    @Before
    fun setUp() {
        mockkObject(AppPrefs)
        every { AppPrefs.isAdaptiveGoalsEnabled(any()) } returns true
        every { AppPrefs.getAdaptiveGoalsMaxActive(any()) } returns 3
        every { AppPrefs.getAdaptiveGoalPaceTempo(any()) } returns AppPrefs.MissionTempo.DENGELI
        every { AppPrefs.getAdaptiveGoalsExcludedCategories(any()) } returns emptySet()
        every { AppPrefs.getAdaptiveGoalsIncludedCategories(any()) } returns emptySet()
    }

    @After
    fun tearDown() {
        unmockkObject(AppPrefs)
    }

    private fun buildUseCase(
        goalDao: FakeWeeklyGoalDao,
        sessions: List<DailyPackageUsage>?,
        apps: List<AppInfo>,
    ): EnsureCurrentWeekAdaptiveGoalsUseCase {
        val clock = Clock.fixed(fixedInstant, zoneId)
        val snapshotProvider = CategoryUsageSnapshotProvider(
            context = mockk<Context>(relaxed = true),
            periodBoundaryResolver = PeriodBoundaryResolver(clock, zoneId),
            dataFreshnessResolver = DataFreshnessResolver(clock),
            clock = clock,
            zoneId = zoneId,
            usageStatsSource = FakeUsageStatsSource(sessions),
            appDao = FakeAppDao(apps),
        )
        return EnsureCurrentWeekAdaptiveGoalsUseCase(
            context = mockk(relaxed = true),
            weeklyGoalDao = goalDao,
            categoryUsageSnapshotProvider = snapshotProvider,
            periodBoundaryResolver = PeriodBoundaryResolver(clock, zoneId),
            clock = clock,
        )
    }

    private fun socialSessions() = listOf(
        dailyUsage(previousWeekStart, 100 * 60_000L, "com.social.app"),
        dailyUsage(previousWeekStart + 1, 100 * 60_000L, "com.social.app"),
        dailyUsage(previousWeekStart + 2, 100 * 60_000L, "com.social.app"),
        dailyUsage(previousWeekStart + 3, 100 * 60_000L, "com.social.app"),
        dailyUsage(previousWeekStart + 4, 100 * 60_000L, "com.social.app"),
    )

    private fun socialApps() = listOf(AppInfo(packageName = "com.social.app", appName = "Social", categoryId = Category.CAT_SOCIAL))

    @Test
    fun `generates a new AUTO goal for an eligible category with sufficient data`() = runBlocking {
        val goalDao = FakeWeeklyGoalDao()
        buildUseCase(goalDao, socialSessions(), socialApps()).execute()

        val goal = goalDao.goals.singleOrNull { it.categoryId == Category.CAT_SOCIAL }
        assertTrue("expected a generated AUTO goal", goal != null)
        assertEquals(WeeklyGoalMode.AUTO, goal!!.mode)
        assertEquals(currentWeekStart, goal.weekStartEpochDay)
    }

    @Test
    fun `does not overwrite an existing MANUAL goal in the same category`() = runBlocking {
        val goalDao = FakeWeeklyGoalDao()
        goalDao.goals += WeeklyGoal(
            categoryId = Category.CAT_SOCIAL,
            targetMinutes = 999,
            weekStartEpochDay = currentWeekStart,
            mode = WeeklyGoalMode.MANUAL,
        )
        buildUseCase(goalDao, socialSessions(), socialApps()).execute()

        val goal = goalDao.goals.single { it.categoryId == Category.CAT_SOCIAL }
        assertEquals(WeeklyGoalMode.MANUAL, goal.mode)
        assertEquals(999, goal.targetMinutes)
    }

    @Test
    fun `pinned AUTO goal is not regenerated mid-week`() = runBlocking {
        val goalDao = FakeWeeklyGoalDao()
        goalDao.goals += WeeklyGoal(
            categoryId = Category.CAT_SOCIAL,
            targetMinutes = 123,
            weekStartEpochDay = currentWeekStart,
            mode = WeeklyGoalMode.AUTO,
        )
        buildUseCase(goalDao, socialSessions(), socialApps()).execute()

        val goal = goalDao.goals.single { it.categoryId == Category.CAT_SOCIAL }
        assertEquals(123, goal.targetMinutes)
    }

    @Test
    fun `disabled adaptive goals setting produces nothing`() = runBlocking {
        every { AppPrefs.isAdaptiveGoalsEnabled(any()) } returns false
        val goalDao = FakeWeeklyGoalDao()
        buildUseCase(goalDao, socialSessions(), socialApps()).execute()

        assertTrue(goalDao.goals.isEmpty())
    }

    @Test
    fun `ineligible category never receives an automatic goal`() = runBlocking {
        val goalDao = FakeWeeklyGoalDao()
        val financeApps = listOf(AppInfo(packageName = "com.finance.app", appName = "Finance", categoryId = Category.CAT_FINANCE))
        val financeSessions = listOf(
            dailyUsage(previousWeekStart, 100 * 60_000L, "com.finance.app"),
            dailyUsage(previousWeekStart + 1, 100 * 60_000L, "com.finance.app"),
            dailyUsage(previousWeekStart + 2, 100 * 60_000L, "com.finance.app"),
            dailyUsage(previousWeekStart + 3, 100 * 60_000L, "com.finance.app"),
            dailyUsage(previousWeekStart + 4, 100 * 60_000L, "com.finance.app"),
        )
        buildUseCase(goalDao, financeSessions, financeApps).execute()

        assertTrue(goalDao.goals.none { it.categoryId == Category.CAT_FINANCE })
    }

    @Test
    fun `max active auto goals limit is respected`() = runBlocking {
        every { AppPrefs.getAdaptiveGoalsMaxActive(any()) } returns 1
        val goalDao = FakeWeeklyGoalDao()
        val sessions = listOf(
            dailyUsage(previousWeekStart, 100 * 60_000L, "com.social.app"),
            dailyUsage(previousWeekStart + 1, 100 * 60_000L, "com.social.app"),
            dailyUsage(previousWeekStart + 2, 100 * 60_000L, "com.social.app"),
            dailyUsage(previousWeekStart + 3, 100 * 60_000L, "com.social.app"),
            dailyUsage(previousWeekStart + 4, 100 * 60_000L, "com.social.app"),
            dailyUsage(previousWeekStart, 100 * 60_000L, "com.game.app"),
            dailyUsage(previousWeekStart + 1, 100 * 60_000L, "com.game.app"),
            dailyUsage(previousWeekStart + 2, 100 * 60_000L, "com.game.app"),
            dailyUsage(previousWeekStart + 3, 100 * 60_000L, "com.game.app"),
            dailyUsage(previousWeekStart + 4, 100 * 60_000L, "com.game.app"),
        )
        val apps = listOf(
            AppInfo(packageName = "com.social.app", appName = "Social", categoryId = Category.CAT_SOCIAL),
            AppInfo(packageName = "com.game.app", appName = "Game", categoryId = Category.CAT_GAMES),
        )
        buildUseCase(goalDao, sessions, apps).execute()

        assertEquals(1, goalDao.goals.count { it.weekStartEpochDay == currentWeekStart })
    }
}

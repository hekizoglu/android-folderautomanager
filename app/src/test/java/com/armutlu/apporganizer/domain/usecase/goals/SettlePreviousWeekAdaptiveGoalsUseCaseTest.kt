package com.armutlu.apporganizer.domain.usecase.goals

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.WeeklyGoalDao
import com.armutlu.apporganizer.domain.common.DataFreshnessResolver
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import com.armutlu.apporganizer.domain.models.WeeklyGoalMode
import com.armutlu.apporganizer.domain.models.WeeklyGoalStatus
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.missions.MissionUsageStatsSource
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * P4 — [SettlePreviousWeekAdaptiveGoalsUseCase]. Roadmap §16 "Hedef durumu" senaryolarının
 * settlement kısmı: hafta içinde erken ödül yok, çift settlement idempotent, MANUAL hedefler de
 * dahil (roadmap S1 fix mode'dan bağımsız).
 */
class SettlePreviousWeekAdaptiveGoalsUseCaseTest {

    // Sabit "şimdi": Cuma 2026-07-17 — önceki ISO hafta 2026-07-06 (Pazartesi) başlar.
    private val fixedInstant: Instant = Instant.parse("2026-07-17T12:00:00Z")
    private val zoneId = ZoneOffset.UTC
    private val previousWeekStart = Instant.parse("2026-07-06T00:00:00Z").atZone(zoneId).toLocalDate().toEpochDay()

    private class FakeWeeklyGoalDao : WeeklyGoalDao {
        val goals = mutableListOf<WeeklyGoal>()
        val settleCalls = mutableListOf<Triple<String, String, Long>>()
        val achievedCalls = mutableListOf<String>()

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

        override suspend fun markAchieved(categoryId: String, weekStartEpochDay: Long, achievedAt: Long) {
            achievedCalls += categoryId
            val idx = goals.indexOfFirst { it.categoryId == categoryId && it.weekStartEpochDay == weekStartEpochDay }
            if (idx >= 0) goals[idx] = goals[idx].copy(achievedAt = achievedAt)
        }

        override suspend fun getAutoGoalsForWeek(weekStartEpochDay: Long): List<WeeklyGoal> =
            goals.filter { it.weekStartEpochDay == weekStartEpochDay && it.mode == WeeklyGoalMode.AUTO }

        override suspend fun getGoal(categoryId: String, weekStartEpochDay: Long): WeeklyGoal? =
            goals.firstOrNull { it.categoryId == categoryId && it.weekStartEpochDay == weekStartEpochDay }

        override suspend fun getUnsettledGoals(weekStartEpochDay: Long): List<WeeklyGoal> =
            goals.filter { it.weekStartEpochDay == weekStartEpochDay && it.settledAt == null }

        override suspend fun settle(categoryId: String, weekStartEpochDay: Long, status: String, settledAt: Long) {
            settleCalls += Triple(categoryId, status, settledAt)
            val idx = goals.indexOfFirst { it.categoryId == categoryId && it.weekStartEpochDay == weekStartEpochDay }
            if (idx >= 0) {
                goals[idx] = goals[idx].copy(
                    status = WeeklyGoalStatus.valueOf(status),
                    settledAt = settledAt,
                )
            }
        }

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

    private fun buildUseCase(
        goalDao: FakeWeeklyGoalDao,
        sessions: List<DailyPackageUsage>?,
        apps: List<AppInfo>,
    ): SettlePreviousWeekAdaptiveGoalsUseCase {
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
        return SettlePreviousWeekAdaptiveGoalsUseCase(
            weeklyGoalDao = goalDao,
            categoryUsageSnapshotProvider = snapshotProvider,
            periodBoundaryResolver = PeriodBoundaryResolver(clock, zoneId),
            clock = clock,
        )
    }

    @Test
    fun `usage under target completes goal and marks achieved`() = runBlocking {
        val goalDao = FakeWeeklyGoalDao()
        goalDao.goals += WeeklyGoal(
            categoryId = Category.CAT_SOCIAL,
            targetMinutes = 300,
            weekStartEpochDay = previousWeekStart,
            mode = WeeklyGoalMode.AUTO,
        )
        val sessions = listOf(dailyUsage(previousWeekStart, 100 * 60_000L, "com.social.app"))
        val apps = listOf(AppInfo(packageName = "com.social.app", appName = "Social", categoryId = Category.CAT_SOCIAL))

        val completed = buildUseCase(goalDao, sessions, apps).execute()

        assertEquals(1, completed)
        assertEquals(WeeklyGoalStatus.COMPLETED, goalDao.goals.single().status)
        assertEquals(listOf(Category.CAT_SOCIAL), goalDao.achievedCalls)
    }

    @Test
    fun `usage exceeding target does not award success`() = runBlocking {
        val goalDao = FakeWeeklyGoalDao()
        goalDao.goals += WeeklyGoal(
            categoryId = Category.CAT_SOCIAL,
            targetMinutes = 100,
            weekStartEpochDay = previousWeekStart,
            mode = WeeklyGoalMode.MANUAL,
        )
        val sessions = listOf(dailyUsage(previousWeekStart, 300 * 60_000L, "com.social.app"))
        val apps = listOf(AppInfo(packageName = "com.social.app", appName = "Social", categoryId = Category.CAT_SOCIAL))

        val completed = buildUseCase(goalDao, sessions, apps).execute()

        assertEquals(0, completed)
        assertEquals(WeeklyGoalStatus.EXCEEDED, goalDao.goals.single().status)
        assertEquals(emptyList<String>(), goalDao.achievedCalls)
    }

    @Test
    fun `no usage permission means DATA_UNAVAILABLE not fake success`() = runBlocking {
        val goalDao = FakeWeeklyGoalDao()
        goalDao.goals += WeeklyGoal(
            categoryId = Category.CAT_SOCIAL,
            targetMinutes = 100,
            weekStartEpochDay = previousWeekStart,
            mode = WeeklyGoalMode.AUTO,
        )

        val completed = buildUseCase(goalDao, sessions = null, apps = emptyList()).execute()

        assertEquals(0, completed)
        assertEquals(WeeklyGoalStatus.DATA_UNAVAILABLE, goalDao.goals.single().status)
    }

    @Test
    fun `settlement called twice does not award reward again`() = runBlocking {
        val goalDao = FakeWeeklyGoalDao()
        goalDao.goals += WeeklyGoal(
            categoryId = Category.CAT_SOCIAL,
            targetMinutes = 300,
            weekStartEpochDay = previousWeekStart,
            mode = WeeklyGoalMode.AUTO,
        )
        val sessions = listOf(dailyUsage(previousWeekStart, 100 * 60_000L, "com.social.app"))
        val apps = listOf(AppInfo(packageName = "com.social.app", appName = "Social", categoryId = Category.CAT_SOCIAL))
        val useCase = buildUseCase(goalDao, sessions, apps)

        val firstRun = useCase.execute()
        val secondRun = useCase.execute()

        assertEquals(1, firstRun)
        assertEquals(0, secondRun)
        assertEquals(1, goalDao.achievedCalls.size)
    }

    @Test
    fun `mid-week no goals in current week are settled, only previous completed week`() = runBlocking {
        val goalDao = FakeWeeklyGoalDao()
        val currentWeekStart = Instant.parse("2026-07-13T00:00:00Z").atZone(zoneId).toLocalDate().toEpochDay()
        goalDao.goals += WeeklyGoal(
            categoryId = Category.CAT_SOCIAL,
            targetMinutes = 300,
            weekStartEpochDay = currentWeekStart,
            mode = WeeklyGoalMode.AUTO,
        )
        val completed = buildUseCase(goalDao, sessions = null, apps = emptyList()).execute()

        assertEquals(0, completed)
        assertNull(goalDao.goals.single().settledAt)
    }
}

package com.armutlu.apporganizer.domain.advice

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.local.PackageNotifCount
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import com.armutlu.apporganizer.domain.usecase.goals.CategoryUsageSnapshot
import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.usecase.missions.MissionUsageStatsSource
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * P9-takip — [computeDigitalAdvice]'ın bildirim gürültüsü (notificationNoiseTopSourceShare)
 * ve sabah/gece kullanım paterni (morningSocialOpenDaysLast7/lateNightUsageDaysLast7)
 * sinyallerini gerçek veriden beslediğini doğrular. Bu sinyaller öncesinde her zaman null
 * besleniyordu (bilinçli kapsam daraltması) — bu tur o boşluğu kapatıyor.
 */
class CategoryGoalAdviceComputerTest {

    private val fixedInstant: Instant = Instant.parse("2026-07-17T12:00:00Z")
    private val zoneId = ZoneOffset.UTC

    private fun emptySnapshot() = CategoryUsageSnapshot(
        capturedAt = fixedInstant.toEpochMilli(),
        previousWeekMinutesByCategory = emptyMap(),
        currentWeekMinutesByCategory = emptyMap(),
        validDataDayCount = 7,
        freshness = DataFreshness.LIVE,
    )

    private class FakeAppDao(
        private val apps: List<AppInfo> = emptyList(),
        private val socialPackages: List<String> = emptyList(),
    ) : AppDao by mockk(relaxed = true) {
        override suspend fun getAllApps(): List<AppInfo> = apps
        override suspend fun getPackageNamesByCategory(categoryId: String): List<String> =
            if (categoryId == Category.CAT_SOCIAL) socialPackages else emptyList()
    }

    private fun dailyUsage(epochDay: Long, hourly: LongArray, packageName: String) = DailyPackageUsage(
        localDate = java.time.LocalDate.ofEpochDay(epochDay).toString(),
        epochDay = epochDay,
        packageName = packageName,
        launchCount = 1,
        foregroundDurationMs = hourly.sum(),
        hourlyForegroundMs = hourly.toList(),
        globalForegroundMs = hourly.sum(),
        isPartial = false,
    )

    @Test
    fun `no notification dao means noise share stays null`() = runBlocking {
        val advice = computeDigitalAdvice(
            snapshot = emptySnapshot(),
            goalsUi = emptyList(),
            appDao = FakeAppDao(),
            clock = Clock.fixed(fixedInstant, zoneId),
        )
        // Hicbir esik gecilmedigi icin advice zaten null donebilir - burada crash olmadigini
        // ve notificationEventDao/usageStatsSource verilmeden guvenle calistigini dogruluyoruz.
        assertNull(advice)
    }

    @Test
    fun `dominant notification source above threshold produces NOTIFICATION_NOISE advice`() = runBlocking {
        val notifDao = mockk<NotificationEventDao>()
        coEvery { notifDao.countsSince(any()) } returns listOf(
            PackageNotifCount("com.chat.app", 80),
            PackageNotifCount("com.other.app", 20),
        )
        val advice = computeDigitalAdvice(
            snapshot = emptySnapshot(),
            goalsUi = emptyList(),
            appDao = FakeAppDao(),
            clock = Clock.fixed(fixedInstant, zoneId),
            notificationEventDao = notifDao,
        )
        assertEquals(DigitalAdviceType.NOTIFICATION_NOISE, advice?.type)
    }

    @Test
    fun `notification share below threshold produces no advice`() = runBlocking {
        val notifDao = mockk<NotificationEventDao>()
        // En baskin kaynak (com.a) toplamin %30'u -> %40 esiginin ALTINDA.
        coEvery { notifDao.countsSince(any()) } returns listOf(
            PackageNotifCount("com.a.app", 30),
            PackageNotifCount("com.b.app", 25),
            PackageNotifCount("com.c.app", 25),
            PackageNotifCount("com.d.app", 20),
        )
        val advice = computeDigitalAdvice(
            snapshot = emptySnapshot(),
            goalsUi = emptyList(),
            appDao = FakeAppDao(),
            clock = Clock.fixed(fixedInstant, zoneId),
            notificationEventDao = notifDao,
        )
        assertNull(advice)
    }

    @Test
    fun `no usage permission means pattern signals stay null instead of fake zero`() = runBlocking {
        val statsSource = mockk<MissionUsageStatsSource>()
        coEvery { statsSource.getDailySessionUsage(any(), any(), any()) } returns null
        val advice = computeDigitalAdvice(
            snapshot = emptySnapshot(),
            goalsUi = emptyList(),
            appDao = FakeAppDao(),
            clock = Clock.fixed(fixedInstant, zoneId),
            context = mockk<Context>(relaxed = true),
            usageStatsSource = statsSource,
        )
        assertNull(advice)
    }

    @Test
    fun `late night usage on enough completed days produces USAGE_PATTERN advice`() = runBlocking {
        val today = fixedInstant.atZone(zoneId).toLocalDate().toEpochDay()
        val nightHours = LongArray(24).also { it[23] = 60_000L }
        val sessions = listOf(
            dailyUsage(today - 1, nightHours, "com.any.app"),
            dailyUsage(today - 2, nightHours, "com.any.app"),
            dailyUsage(today - 3, nightHours, "com.any.app"),
        )
        val statsSource = mockk<MissionUsageStatsSource>()
        coEvery { statsSource.getDailySessionUsage(any(), any(), any()) } returns sessions

        val advice = computeDigitalAdvice(
            snapshot = emptySnapshot(),
            goalsUi = emptyList(),
            appDao = FakeAppDao(),
            clock = Clock.fixed(fixedInstant, zoneId),
            context = mockk<Context>(relaxed = true),
            usageStatsSource = statsSource,
            zoneId = zoneId,
        )
        assertEquals(DigitalAdviceType.USAGE_PATTERN, advice?.type)
    }

    @Test
    fun `today's own session data is excluded from the completed-day window`() = runBlocking {
        val today = fixedInstant.atZone(zoneId).toLocalDate().toEpochDay()
        val nightHours = LongArray(24).also { it[23] = 60_000L }
        // Sadece BUGUNUN verisi var (henuz tamamlanmamis gun) -> pattern hesaplanmamali.
        val sessions = listOf(dailyUsage(today, nightHours, "com.any.app"))
        val statsSource = mockk<MissionUsageStatsSource>()
        coEvery { statsSource.getDailySessionUsage(any(), any(), any()) } returns sessions

        val advice = computeDigitalAdvice(
            snapshot = emptySnapshot(),
            goalsUi = emptyList(),
            appDao = FakeAppDao(),
            clock = Clock.fixed(fixedInstant, zoneId),
            context = mockk<Context>(relaxed = true),
            usageStatsSource = statsSource,
            zoneId = zoneId,
        )
        assertNull(advice)
    }
}

package com.armutlu.apporganizer.domain.usecase.missions

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.data.local.WeeklyGoalDao
import com.armutlu.apporganizer.data.repository.MissionsRepository
import com.armutlu.apporganizer.domain.models.MissionInstanceEntity
import com.armutlu.apporganizer.domain.usecase.goals.CategoryUsageSnapshotProvider
import com.armutlu.apporganizer.utils.AppPrefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tur 23: MissionSummaryUseCase.compute sözleşme testleri.
 *
 * Amaç: yıldız-ödül yan etkilerini taşıyan compute gövdesi refactor edilmeden ÖNCE
 * davranışını kilitlemek — sessiz refresh yan etkisizliği (M07), pin/settlement
 * akışları (award modu), kişisel hedef pin önceliği ve uygulama-limiti pin sözleşmesi.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MissionSummaryUseCaseTest {

    private lateinit var context: Context
    private lateinit var missionsRepository: MissionsRepository
    private lateinit var snapshotProvider: MissionMetricSnapshotProvider
    private lateinit var settleUseCase: SettleMissionInstancesUseCase
    private lateinit var weeklyGoalDao: WeeklyGoalDao
    private lateinit var categoryUsageSnapshotProvider: CategoryUsageSnapshotProvider
    private lateinit var useCase: MissionSummaryUseCase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Tercihleri her test öncesi sıfırla (pin testleri birbirine bulaşmasın)
        context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()

        missionsRepository = mockk(relaxed = true)
        snapshotProvider = mockk()
        settleUseCase = mockk(relaxed = true)
        weeklyGoalDao = mockk()
        categoryUsageSnapshotProvider = mockk(relaxed = true)

        coEvery { missionsRepository.getInstancesForPeriod(any(), any()) } returns emptyList()
        coEvery { missionsRepository.getCompletedDailyIds(any()) } returns emptySet()
        coEvery { missionsRepository.getCompletedWeeklyIds(any()) } returns emptySet()
        coEvery { missionsRepository.getRecentlyCompletedDailyIds(any(), any()) } returns emptySet()
        coEvery { missionsRepository.getRecentlyCompletedWeeklyIds(any(), any()) } returns emptySet()
        coEvery { weeklyGoalDao.getAutoGoalsForWeek(any()) } returns emptyList()
        coEvery { snapshotProvider.capture(any()) } returns baseSnapshot()

        useCase = MissionSummaryUseCase(
            context = context,
            missionsRepository = missionsRepository,
            missionMetricSnapshotProvider = snapshotProvider,
            settleMissionInstancesUseCase = settleUseCase,
            weeklyGoalDao = weeklyGoalDao,
            categoryUsageSnapshotProvider = categoryUsageSnapshotProvider,
        )
    }

    private fun baseSnapshot() = MissionMetricSnapshot(
        capturedAt = System.currentTimeMillis(),
        screenTimeMinutesToday = 90L,
        unlockCountToday = 12,
        usedAfter23Today = false,
        firstUseAfter23At = null,
        screenTimeMinutesThisWeek = 400L,
        screenTimeMinutesPreviousWeek = 500L,
        classificationActionsToday = 0,
        notificationReportViewedToday = false,
        positiveActionsThisWeek = 0,
        freshness = com.armutlu.apporganizer.domain.common.DataFreshness.LIVE,
    )

    // ── Sessiz refresh sözleşmesi (M07) ──────────────────────────────────────

    @Test
    fun `silent refresh performs no write side effects`() = runTest {
        val result = useCase.compute(awardStars = false)

        coVerify(exactly = 0) { missionsRepository.pinInstances(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { missionsRepository.markDailyCompleted(any(), any(), any()) }
        coVerify(exactly = 0) { settleUseCase.settleOverdue(any()) }
        // Sonuç yine de dolu döner (ana ekran kartı görüntüleme yapar)
        assertEquals(0, result.newlyAwardedStars)
    }

    // ── Award modu: settlement + pin akışı ───────────────────────────────────

    @Test
    fun `award mode settles overdue and pins daily and weekly instances`() = runTest {
        useCase.compute(awardStars = true)

        coVerify(exactly = 1) { settleUseCase.settleOverdue(any()) }
        // pinInstances çağrıları günlük ve haftalık dönemleri kapsamalı
        // (imza: missions, periodType, boundary, assignedAt, targetValues, baselineValues)
        coVerify(atLeast = 1) {
            missionsRepository.pinInstances(any(), MissionInstanceEntity.PERIOD_DAILY, any(), any(), any(), any())
        }
        coVerify(atLeast = 1) {
            missionsRepository.pinInstances(any(), MissionInstanceEntity.PERIOD_WEEKLY, any(), any(), any(), any())
        }
    }

    // ── Kişisel hedef pin önceliği (G1) ─────────────────────────────────────

    @Test
    fun `existing pinned personal screen target wins over recompute`() = runTest {
        val epochDay = LocalDate.now().toEpochDay()
        val pinnedInstance = MissionInstanceEntity(
            instanceId = MissionInstanceEntity.buildInstanceId(
                MissionEngine.DAILY_SCREEN_UNDER_3H,
                MissionInstanceEntity.PERIOD_DAILY,
                epochDay,
            ),
            missionId = MissionEngine.DAILY_SCREEN_UNDER_3H,
            periodType = MissionInstanceEntity.PERIOD_DAILY,
            periodStartEpoch = epochDay,
            periodStartAt = 0L,
            periodEndAt = 0L,
            targetValue = 999L,
            baselineValue = null,
            starReward = 0,
            status = "ACTIVE",
            assignedAt = 0L,
            settledAt = null,
            definitionVersion = 1,
        )
        coEvery {
            missionsRepository.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, any())
        } returns listOf(pinnedInstance)

        useCase.compute(awardStars = true)

        val targetValuesSlot = slot<Map<String, Long?>>()
        coVerify(atLeast = 1) {
            missionsRepository.pinInstances(
                any(),
                MissionInstanceEntity.PERIOD_DAILY,
                any(),
                any(),
                capture(targetValuesSlot),
                any(),
            )
        }
        // Pin'li değer korunur; yeniden hesaplama onun üzerine yazamaz.
        assertEquals(999L, targetValuesSlot.captured[MissionEngine.DAILY_SCREEN_UNDER_3H])
    }

    // ── Uygulama-limiti pin sözleşmesi (G3b/M07) ─────────────────────────────

    private fun snapshotWithAppLimitCandidate() = baseSnapshot().copy(
        appLimitCandidates = listOf(
            AppLimitCandidateSelector.PackageUsageCandidate(
                packageName = "com.social.app",
                categoryId = com.armutlu.apporganizer.domain.models.Category.CAT_SOCIAL,
                dailyMinutesLast7Days = listOf(120, 130, 110, 125, 115, 120, 128),
            ),
        ),
    )

    @Test
    fun `app limit candidate is NOT pinned during silent refresh`() = runTest {
        coEvery { snapshotProvider.capture(any()) } returns snapshotWithAppLimitCandidate()
        val epochDay = LocalDate.now().toEpochDay()

        useCase.compute(awardStars = false)

        assertNull(AppPrefs.getAppLimitTargetPackage(context, epochDay))
    }

    @Test
    fun `app limit candidate IS pinned in award mode`() = runTest {
        coEvery { snapshotProvider.capture(any()) } returns snapshotWithAppLimitCandidate()
        val epochDay = LocalDate.now().toEpochDay()

        val result = useCase.compute(awardStars = true)

        assertEquals("com.social.app", AppPrefs.getAppLimitTargetPackage(context, epochDay))
        // Sonuç pin'li paketi raporlar
        assertEquals(0, result.newlyAwardedStars)
    }
}

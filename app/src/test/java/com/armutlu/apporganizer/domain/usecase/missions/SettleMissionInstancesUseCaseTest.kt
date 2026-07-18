package com.armutlu.apporganizer.domain.usecase.missions

import android.content.Context
import com.armutlu.apporganizer.data.local.MissionHistoryDao
import com.armutlu.apporganizer.data.local.MissionInstanceDao
import com.armutlu.apporganizer.domain.models.MissionHistoryEntry
import com.armutlu.apporganizer.domain.models.MissionInstanceEntity
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import io.mockk.mockk
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SettleMissionInstancesUseCase — Dongu M04 (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satir 945-1001). In-memory fake DAO/kaynaklarla settlement kurallarini dogrular:
 * - Ayni instance iki kez settle edilmez.
 * - Gun sonu 179/180 -> COMPLETED+yildiz, 180/180 -> FAILED+yildiz yok.
 * - Hafta sonu mevcut < onceki -> COMPLETED.
 * - Veri alinamiyorsa (48s'den eski) DATA_UNAVAILABLE -> settled, yildiz yok.
 * - Catch-up: birden fazla gecikmis instance tek cagrida sonuclanir.
 * - Yildiz, use case seviyesinde (ekran/ViewModel olmadan) yazilir.
 */
class SettleMissionInstancesUseCaseTest {

    private val zoneId = ZoneOffset.UTC

    // Pass-through transaction runner — gercek Room gerektirmez, sadece blogu calistirir.
    private val passthroughTransactionRunner = object : MissionSettlementTransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
    }

    private class FakeMissionInstanceDao(
        seed: List<MissionInstanceEntity> = emptyList(),
    ) : MissionInstanceDao {
        val store = linkedMapOf<String, MissionInstanceEntity>().apply {
            seed.forEach { put(it.instanceId, it) }
        }
        val settleCalls = mutableListOf<Triple<String, String, Long>>()

        override suspend fun getInstancesForPeriod(periodType: String, periodStartEpoch: Long): List<MissionInstanceEntity> =
            store.values.filter { it.periodType == periodType && it.periodStartEpoch == periodStartEpoch }

        override fun observeActiveInstances(): Flow<List<MissionInstanceEntity>> =
            MutableStateFlow(store.values.filter { it.status == MissionInstanceEntity.STATUS_ASSIGNED })

        override suspend fun insertAllIgnore(instances: List<MissionInstanceEntity>): List<Long> {
            var inserted = 0L
            return instances.map { instance ->
                if (store.containsKey(instance.instanceId)) {
                    -1L
                } else {
                    store[instance.instanceId] = instance
                    inserted++
                }
            }
        }

        override suspend fun updateStatus(instanceId: String, status: String) {
            store[instanceId]?.let { store[instanceId] = it.copy(status = status) }
        }

        override suspend fun settleInstance(instanceId: String, status: String, settledAt: Long) {
            settleCalls += Triple(instanceId, status, settledAt)
            store[instanceId]?.let { store[instanceId] = it.copy(status = status, settledAt = settledAt) }
        }

        override suspend fun getUnsettledBefore(beforeEpochMillis: Long): List<MissionInstanceEntity> =
            store.values.filter {
                it.status == MissionInstanceEntity.STATUS_ASSIGNED && it.periodEndAt < beforeEpochMillis
            }

        override suspend fun countUnsettledBefore(beforeEpochMillis: Long): Int =
            getUnsettledBefore(beforeEpochMillis).size

        override suspend fun clearAll() {
            store.clear()
        }
    }

    private class FakeMissionHistoryDao : MissionHistoryDao {
        val entries = mutableListOf<MissionHistoryEntry>()

        override suspend fun insert(entry: MissionHistoryEntry): Long {
            val duplicate = entries.any {
                it.missionId == entry.missionId &&
                    it.periodType == entry.periodType &&
                    it.periodStartEpoch == entry.periodStartEpoch
            }
            if (duplicate) return -1L
            entries += entry
            return entries.size.toLong()
        }

        override suspend fun getCompletedMissionIds(periodType: String, periodStartEpoch: Long): List<String> =
            entries.filter { it.periodType == periodType && it.periodStartEpoch == periodStartEpoch }.map { it.missionId }

        override suspend fun getCompletedMissionIdsBetween(
            periodType: String,
            fromPeriodStartEpoch: Long,
            toPeriodStartEpoch: Long,
        ): List<String> = entries.filter {
            it.periodType == periodType && it.periodStartEpoch in fromPeriodStartEpoch..toPeriodStartEpoch
        }.map { it.missionId }.distinct()

        override suspend fun getTotalStars(): Int = entries.sumOf { it.starReward }

        override suspend fun getCompletionCount(periodType: String): Int =
            entries.count { it.periodType == periodType }

        override suspend fun getCompletionCountByMissionIds(missionIds: List<String>): Int =
            entries.count { it.missionId in missionIds }

        override suspend fun clearAll() {
            entries.clear()
        }
    }

    /** [dailyMinutesByEpochDay] gunluk global-foreground DAKIKA degeri; null -> o gun icin veri yok. */
    private class FakeUsageStatsSource(
        private val dailyMinutesByEpochDay: Map<Long, Long> = emptyMap(),
        private val unlockCountByEpochDay: Map<Long, Int> = emptyMap(),
        private val usedAfter23ByEpochDay: Map<Long, Boolean> = emptyMap(),
        private val dataAvailable: Boolean = true,
    ) : MissionUsageStatsSource {
        override fun getDailySessionUsage(context: Context, days: Int, nowMillis: Long): List<DailyPackageUsage>? {
            if (!dataAvailable) return null
            if (dailyMinutesByEpochDay.isEmpty() && usedAfter23ByEpochDay.isEmpty()) return null
            val days1 = dailyMinutesByEpochDay.map { (epochDay, minutes) ->
                DailyPackageUsage(
                    localDate = epochDay.toString(),
                    epochDay = epochDay,
                    packageName = "com.example.app",
                    launchCount = 1,
                    foregroundDurationMs = minutes * 60_000L,
                    hourlyForegroundMs = List(24) { hour ->
                        if (usedAfter23ByEpochDay[epochDay] == true && hour == 23) 60_000L else 0L
                    },
                    globalForegroundMs = minutes * 60_000L,
                    isPartial = false,
                )
            }
            val days2 = usedAfter23ByEpochDay.keys.minus(dailyMinutesByEpochDay.keys).map { epochDay ->
                DailyPackageUsage(
                    localDate = epochDay.toString(),
                    epochDay = epochDay,
                    packageName = "com.example.app",
                    launchCount = 1,
                    foregroundDurationMs = 0L,
                    hourlyForegroundMs = List(24) { hour ->
                        if (usedAfter23ByEpochDay[epochDay] == true && hour == 23) 60_000L else 0L
                    },
                    globalForegroundMs = 0L,
                    isPartial = false,
                )
            }
            return (days1 + days2).ifEmpty { null }
        }

        override fun getUnlockCount(context: Context, days: Int, nowMillis: Long): Int? {
            if (!dataAvailable) return null
            // Testte 1-gunluk pencere kullaniliyor; anchorMillis'in epochDay'i ile eslesir varsayilir.
            return unlockCountByEpochDay.values.firstOrNull()
        }
    }

    private fun buildUseCase(
        instanceDao: FakeMissionInstanceDao,
        historyDao: FakeMissionHistoryDao = FakeMissionHistoryDao(),
        usageStatsSource: MissionUsageStatsSource = FakeUsageStatsSource(),
    ): SettleMissionInstancesUseCase = SettleMissionInstancesUseCase(
        context = mockk<Context>(relaxed = true),
        transactionRunner = passthroughTransactionRunner,
        missionInstanceDao = instanceDao,
        missionHistoryDao = historyDao,
        usageStatsSource = usageStatsSource,
        zoneId = zoneId,
    )

    private fun dailyInstance(
        missionId: String,
        epochDay: Long,
        starReward: Int = 1,
        targetValue: Long? = null,
        baselineValue: Long? = null,
    ): MissionInstanceEntity {
        val startInclusive = epochDay * 86_400_000L
        val endExclusive = startInclusive + 86_400_000L
        return MissionInstanceEntity(
            instanceId = MissionInstanceEntity.buildInstanceId(missionId, MissionInstanceEntity.PERIOD_DAILY, epochDay),
            missionId = missionId,
            periodType = MissionInstanceEntity.PERIOD_DAILY,
            periodStartEpoch = epochDay,
            periodStartAt = startInclusive,
            periodEndAt = endExclusive,
            targetValue = targetValue,
            baselineValue = baselineValue,
            starReward = starReward,
            status = MissionInstanceEntity.STATUS_ASSIGNED,
            assignedAt = startInclusive,
            settledAt = null,
            definitionVersion = MissionInstanceEntity.CURRENT_DEFINITION_VERSION,
        )
    }

    private fun weeklyInstance(
        missionId: String,
        weekStartEpochDay: Long,
        starReward: Int = 2,
        baselineValue: Long? = null,
    ): MissionInstanceEntity {
        val startInclusive = weekStartEpochDay * 86_400_000L
        val endExclusive = startInclusive + 7 * 86_400_000L
        return MissionInstanceEntity(
            instanceId = MissionInstanceEntity.buildInstanceId(missionId, MissionInstanceEntity.PERIOD_WEEKLY, weekStartEpochDay),
            missionId = missionId,
            periodType = MissionInstanceEntity.PERIOD_WEEKLY,
            periodStartEpoch = weekStartEpochDay,
            periodStartAt = startInclusive,
            periodEndAt = endExclusive,
            targetValue = null,
            baselineValue = baselineValue,
            starReward = starReward,
            status = MissionInstanceEntity.STATUS_ASSIGNED,
            assignedAt = startInclusive,
            settledAt = null,
            definitionVersion = MissionInstanceEntity.CURRENT_DEFINITION_VERSION,
        )
    }

    @Test
    fun `same instance is not settled twice`() = runBlocking {
        val epochDay = 100L
        val instance = dailyInstance(MissionEngine.DAILY_SCREEN_UNDER_3H, epochDay, targetValue = 180L)
        val dao = FakeMissionInstanceDao(listOf(instance))
        val usage = FakeUsageStatsSource(dailyMinutesByEpochDay = mapOf(epochDay to 90L))
        val useCase = buildUseCase(dao, usageStatsSource = usage)

        val now = instance.periodEndAt + 1_000L
        val first = useCase.settleOverdue(now)
        assertEquals(1, first.settledCount)

        // Ikinci cagri: instance artik "assigned" degil -> getUnsettledBefore bos doner -> no-op.
        val second = useCase.settleOverdue(now + 10_000L)
        assertEquals(0, second.settledCount)
        assertEquals(0, second.starsAwarded)
        assertEquals(1, dao.settleCalls.count { it.first == instance.instanceId })
    }

    @Test
    fun `day end 179 of 180 completes and awards star`() = runBlocking {
        val epochDay = 100L
        val instance = dailyInstance(MissionEngine.DAILY_SCREEN_UNDER_3H, epochDay, starReward = 1, targetValue = 180L)
        val dao = FakeMissionInstanceDao(listOf(instance))
        val historyDao = FakeMissionHistoryDao()
        val usage = FakeUsageStatsSource(dailyMinutesByEpochDay = mapOf(epochDay to 179L))
        val useCase = buildUseCase(dao, historyDao, usage)

        val result = useCase.settleOverdue(instance.periodEndAt + 1_000L)

        assertEquals(1, result.settledCount)
        assertEquals(1, result.starsAwarded)
        assertEquals(MissionInstanceEntity.STATUS_COMPLETED, dao.store[instance.instanceId]?.status)
        assertEquals(1, historyDao.entries.size)
        assertEquals(1, historyDao.entries.first().starReward)
    }

    @Test
    fun `day end 180 of 180 fails without star`() = runBlocking {
        val epochDay = 100L
        val instance = dailyInstance(MissionEngine.DAILY_SCREEN_UNDER_3H, epochDay, starReward = 1, targetValue = 180L)
        val dao = FakeMissionInstanceDao(listOf(instance))
        val historyDao = FakeMissionHistoryDao()
        val usage = FakeUsageStatsSource(dailyMinutesByEpochDay = mapOf(epochDay to 180L))
        val useCase = buildUseCase(dao, historyDao, usage)

        val result = useCase.settleOverdue(instance.periodEndAt + 1_000L)

        assertEquals(1, result.settledCount)
        assertEquals(0, result.starsAwarded)
        assertEquals(MissionInstanceEntity.STATUS_FAILED, dao.store[instance.instanceId]?.status)
        assertTrue(historyDao.entries.isEmpty())
    }

    @Test
    fun `week end current less than previous completes`() = runBlocking {
        val weekStartEpochDay = 700L // Monday-aligned in test, exact day doesn't matter for logic.
        val instance = weeklyInstance(
            MissionEngine.WEEKLY_SCREEN_LESS,
            weekStartEpochDay,
            starReward = 2,
            baselineValue = 1000L,
        )
        val dao = FakeMissionInstanceDao(listOf(instance))
        val historyDao = FakeMissionHistoryDao()
        // Haftanin 7 gunune toplamda 900 dakika dagit (< 1000 baseline).
        val minutesPerDay = 900L / 7
        val dailyMinutes = (weekStartEpochDay until weekStartEpochDay + 7).associateWith { minutesPerDay }
        val usage = FakeUsageStatsSource(dailyMinutesByEpochDay = dailyMinutes)
        val useCase = buildUseCase(dao, historyDao, usage)

        val result = useCase.settleOverdue(instance.periodEndAt + 1_000L)

        assertEquals(1, result.settledCount)
        assertEquals(2, result.starsAwarded)
        assertEquals(MissionInstanceEntity.STATUS_COMPLETED, dao.store[instance.instanceId]?.status)
    }

    @Test
    fun `data unavailable older than grace period settles as failed without star`() = runBlocking {
        val epochDay = 100L
        val instance = dailyInstance(MissionEngine.DAILY_SCREEN_UNDER_3H, epochDay, targetValue = 180L)
        val dao = FakeMissionInstanceDao(listOf(instance))
        val historyDao = FakeMissionHistoryDao()
        val usage = FakeUsageStatsSource(dataAvailable = false)
        val useCase = buildUseCase(dao, historyDao, usage)

        // periodEndAt'tan 49 saat sonra (grace period 48s asildi).
        val now = instance.periodEndAt + TimeUnit.HOURS.toMillis(49)
        val result = useCase.settleOverdue(now)

        assertEquals(1, result.settledCount)
        assertEquals(0, result.starsAwarded)
        assertEquals(1, result.dataUnavailable)
        assertEquals(MissionInstanceEntity.STATUS_FAILED, dao.store[instance.instanceId]?.status)
        assertTrue(historyDao.entries.isEmpty())
    }

    @Test
    fun `data unavailable within grace period is retried later, not settled`() = runBlocking {
        val epochDay = 100L
        val instance = dailyInstance(MissionEngine.DAILY_SCREEN_UNDER_3H, epochDay, targetValue = 180L)
        val dao = FakeMissionInstanceDao(listOf(instance))
        val usage = FakeUsageStatsSource(dataAvailable = false)
        val useCase = buildUseCase(dao, usageStatsSource = usage)

        // periodEndAt'tan sadece 1 saat sonra -> grace period icinde, settle EDILMEMELI.
        val now = instance.periodEndAt + TimeUnit.HOURS.toMillis(1)
        val result = useCase.settleOverdue(now)

        assertEquals(0, result.settledCount)
        assertEquals(1, result.skippedRetryLater)
        assertEquals(MissionInstanceEntity.STATUS_ASSIGNED, dao.store[instance.instanceId]?.status)
        assertNull(dao.store[instance.instanceId]?.settledAt)
    }

    @Test
    fun `catch-up settles three overdue instances from three days in one call`() = runBlocking {
        val instances = (1L..3L).map { offset ->
            val epochDay = 100L + offset
            dailyInstance(MissionEngine.DAILY_SCREEN_UNDER_3H, epochDay, targetValue = 180L)
        }
        val dao = FakeMissionInstanceDao(instances)
        val historyDao = FakeMissionHistoryDao()
        val dailyMinutes = instances.associate { it.periodStartEpoch to 90L }
        val usage = FakeUsageStatsSource(dailyMinutesByEpochDay = dailyMinutes)
        val useCase = buildUseCase(dao, historyDao, usage)

        val now = instances.maxOf { it.periodEndAt } + 1_000L
        val result = useCase.settleOverdue(now)

        assertEquals(3, result.settledCount)
        assertEquals(3, result.starsAwarded)
        assertEquals(3, historyDao.entries.size)
        instances.forEach { instance ->
            assertEquals(MissionInstanceEntity.STATUS_COMPLETED, dao.store[instance.instanceId]?.status)
        }
    }

    @Test
    fun `star is written at use-case level without any screen or ViewModel involved`() = runBlocking {
        // Bu test dosyasi hicbir Compose/ViewModel/Activity sinifi import etmez — settlement
        // tamamen use case + DAO katmaninda calisir, "gorev ekrani hic acilmadan da yildiz
        // yazilir" iddiasinin kaniti budur.
        val epochDay = 100L
        val instance = dailyInstance(MissionEngine.DAILY_UNLOCK_UNDER_30, epochDay, starReward = 1, targetValue = 30L)
        val dao = FakeMissionInstanceDao(listOf(instance))
        val historyDao = FakeMissionHistoryDao()
        val usage = FakeUsageStatsSource(unlockCountByEpochDay = mapOf(epochDay to 20))
        val useCase = buildUseCase(dao, historyDao, usage)

        assertEquals(0, historyDao.getTotalStars())
        val result = useCase.settleOverdue(instance.periodEndAt + 1_000L)
        assertEquals(1, result.starsAwarded)
        assertEquals(1, historyDao.getTotalStars())
    }

    @Test
    fun `completeActionMission marks instance completed without writing a second reward`() = runBlocking {
        val epochDay = 100L
        val instance = dailyInstance(MissionEngine.DAILY_CLASSIFICATION_CLEANUP, epochDay, starReward = 1)
            .copy(status = MissionInstanceEntity.STATUS_ASSIGNED)
        val dao = FakeMissionInstanceDao(listOf(instance))
        val historyDao = FakeMissionHistoryDao()
        val useCase = buildUseCase(dao, historyDao)

        val result = useCase.completeActionMission(instance.instanceId, settledAt = 123L)

        assertEquals(1, result.settledCount)
        assertEquals(0, result.starsAwarded)
        assertEquals(MissionInstanceEntity.STATUS_COMPLETED, dao.store[instance.instanceId]?.status)
        assertEquals(123L, dao.store[instance.instanceId]?.settledAt)
        assertTrue(historyDao.entries.isEmpty()) // mission_history'ye dokunmadi — caller zaten yazdi.
    }
}

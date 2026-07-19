package com.armutlu.apporganizer.data.repository

import android.content.Context
import com.armutlu.apporganizer.data.local.MissionHistoryDao
import com.armutlu.apporganizer.data.local.MissionInstanceDao
import com.armutlu.apporganizer.data.local.TaskScoreEventDao
import com.armutlu.apporganizer.domain.models.MissionHistoryEntry
import com.armutlu.apporganizer.domain.models.MissionInstanceEntity
import com.armutlu.apporganizer.domain.models.TaskScoreEventEntry
import com.armutlu.apporganizer.domain.time.PeriodBoundary
import com.armutlu.apporganizer.domain.usecase.missions.MissionEngine
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * MissionsRepository.pinInstances — Dongu G1. Kisisel hedefin donem boyunca SABIT kalmasi,
 * mission_instances tablosunun `insertAllIgnore` (unique index) garantisine dayanir: ayni
 * donem+gorev icin ikinci pin denemesi (farkli hedefle bile) sessizce yok sayilmali.
 */
class MissionsRepositoryTest {

    /** Gercek Room semantigini (insert IGNORE + unique(missionId,periodType,periodStartEpoch)) taklit eder. */
    private class FakeMissionInstanceDao : MissionInstanceDao {
        val store = linkedMapOf<String, MissionInstanceEntity>()

        override suspend fun getInstancesForPeriod(periodType: String, periodStartEpoch: Long): List<MissionInstanceEntity> =
            store.values.filter { it.periodType == periodType && it.periodStartEpoch == periodStartEpoch }

        override fun observeActiveInstances() = MutableStateFlow(store.values.toList()).asStateFlow()

        override suspend fun insertAllIgnore(instances: List<MissionInstanceEntity>): List<Long> =
            instances.map { instance ->
                val key = "${instance.missionId}_${instance.periodType}_${instance.periodStartEpoch}"
                val alreadyExists = store.containsKey(key)
                if (!alreadyExists) {
                    store[key] = instance
                    1L
                } else {
                    -1L
                }
            }

        override suspend fun updateStatus(instanceId: String, status: String) = Unit

        override suspend fun settleInstance(instanceId: String, status: String, settledAt: Long) = Unit

        override suspend fun getUnsettledBefore(beforeEpochMillis: Long): List<MissionInstanceEntity> = emptyList()

        override suspend fun countUnsettledBefore(beforeEpochMillis: Long): Int = 0

        override suspend fun countSettledForDay(epochDay: Long): Int =
            store.values.count { it.periodType == "daily" && it.periodStartEpoch == epochDay && it.status != "assigned" }

        override suspend fun countCompletedForDay(epochDay: Long): Int =
            store.values.count { it.periodType == "daily" && it.periodStartEpoch == epochDay && it.status == "completed" }

        override suspend fun clearAll() {
            store.clear()
        }
    }

    private class FakeMissionHistoryDao : MissionHistoryDao {
        override suspend fun insert(entry: MissionHistoryEntry): Long = 1L
        override suspend fun getCompletedMissionIds(periodType: String, periodStartEpoch: Long): List<String> = emptyList()
        override suspend fun getCompletedMissionIdsBetween(
            periodType: String,
            fromPeriodStartEpoch: Long,
            toPeriodStartEpoch: Long,
        ): List<String> = emptyList()
        override suspend fun getTotalStars(): Int = 0
        override suspend fun getCompletionCount(periodType: String): Int = 0
        override suspend fun getCompletionCountByMissionIds(missionIds: List<String>): Int = 0
        override suspend fun clearAll() = Unit
    }

    private class FakeTaskScoreEventDao : TaskScoreEventDao {
        override suspend fun insert(entry: TaskScoreEventEntry): Long = 1L
        override suspend fun getTotalScore(): Int = 0
        override suspend fun getScoreBetween(fromInclusive: Long, toInclusive: Long): Int = 0
        override suspend fun getLatestEvent(): TaskScoreEventEntry? = null
        override suspend fun countEventsBetween(fromInclusive: Long, toInclusive: Long, positiveOnly: Boolean): Int = 0
        override suspend fun countEventsBetweenByKeys(fromInclusive: Long, toInclusive: Long, eventKeys: List<String>): Int = 0
        override suspend fun insertOnceBetween(entry: TaskScoreEventEntry, fromInclusive: Long, toInclusive: Long): Boolean = true
        override suspend fun getPositiveScore(): Int = 0
        override suspend fun getNegativeScore(): Int = 0
        override suspend fun clearAll() = Unit
    }

    private fun buildRepository(dao: FakeMissionInstanceDao): MissionsRepository = MissionsRepository(
        context = mockk<Context>(relaxed = true),
        missionHistoryDao = FakeMissionHistoryDao(),
        taskScoreEventDao = FakeTaskScoreEventDao(),
        missionInstanceDao = dao,
    )

    private fun mission(id: String) = MissionEngine.Mission(id, MissionEngine.MissionType.DAILY, MissionEngine.DAILY_STAR, autoCheckable = true)

    private val boundary = PeriodBoundary(startInclusive = 0L, endExclusive = 86_400_000L, epochDay = 20_650L, weekStartEpochDay = null)

    @Test
    fun `first pin writes the personal target`() = runBlocking {
        val dao = FakeMissionInstanceDao()
        val repository = buildRepository(dao)

        repository.pinInstances(
            missions = listOf(mission(MissionEngine.DAILY_SCREEN_UNDER_3H)),
            periodType = MissionInstanceEntity.PERIOD_DAILY,
            boundary = boundary,
            targetValues = mapOf(MissionEngine.DAILY_SCREEN_UNDER_3H to 160L),
        )

        val pinned = repository.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, boundary.epochDay)
        assertEquals(1, pinned.size)
        assertEquals(160L, pinned.first().targetValue)
    }

    @Test
    fun `second pin in same period does not overwrite the personal target`() = runBlocking {
        val dao = FakeMissionInstanceDao()
        val repository = buildRepository(dao)

        repository.pinInstances(
            missions = listOf(mission(MissionEngine.DAILY_SCREEN_UNDER_3H)),
            periodType = MissionInstanceEntity.PERIOD_DAILY,
            boundary = boundary,
            targetValues = mapOf(MissionEngine.DAILY_SCREEN_UNDER_3H to 160L),
        )
        // Ikinci cagri farkli bir hedef getiriyor (orn. tempo degisti veya yeni gecmis verisi) —
        // ama AYNI donem oldugu icin ilk deger korunmali (M01 sabitlik ilkesi).
        repository.pinInstances(
            missions = listOf(mission(MissionEngine.DAILY_SCREEN_UNDER_3H)),
            periodType = MissionInstanceEntity.PERIOD_DAILY,
            boundary = boundary,
            targetValues = mapOf(MissionEngine.DAILY_SCREEN_UNDER_3H to 220L),
        )

        val pinned = repository.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, boundary.epochDay)
        assertEquals(1, pinned.size)
        assertEquals(160L, pinned.first().targetValue)
    }

    @Test
    fun `different period gets its own independent target`() = runBlocking {
        val dao = FakeMissionInstanceDao()
        val repository = buildRepository(dao)
        val nextDayBoundary = boundary.copy(epochDay = boundary.epochDay + 1)

        repository.pinInstances(
            missions = listOf(mission(MissionEngine.DAILY_SCREEN_UNDER_3H)),
            periodType = MissionInstanceEntity.PERIOD_DAILY,
            boundary = boundary,
            targetValues = mapOf(MissionEngine.DAILY_SCREEN_UNDER_3H to 160L),
        )
        repository.pinInstances(
            missions = listOf(mission(MissionEngine.DAILY_SCREEN_UNDER_3H)),
            periodType = MissionInstanceEntity.PERIOD_DAILY,
            boundary = nextDayBoundary,
            targetValues = mapOf(MissionEngine.DAILY_SCREEN_UNDER_3H to 200L),
        )

        val day1 = repository.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, boundary.epochDay)
        val day2 = repository.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, nextDayBoundary.epochDay)
        assertEquals(160L, day1.first().targetValue)
        assertEquals(200L, day2.first().targetValue)
    }

    @Test
    fun `missing target value pins null (tanisma modu)`() = runBlocking {
        val dao = FakeMissionInstanceDao()
        val repository = buildRepository(dao)

        repository.pinInstances(
            missions = listOf(mission(MissionEngine.DAILY_SCREEN_UNDER_3H)),
            periodType = MissionInstanceEntity.PERIOD_DAILY,
            boundary = boundary,
            targetValues = emptyMap(),
        )

        val pinned = repository.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, boundary.epochDay)
        assertNull(pinned.first().targetValue)
    }
}

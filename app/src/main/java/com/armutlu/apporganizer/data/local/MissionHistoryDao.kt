package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.armutlu.apporganizer.domain.models.MissionHistoryEntry

@Dao
interface MissionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: MissionHistoryEntry): Long

    @Query(
        """
        SELECT missionId FROM mission_history
        WHERE periodType = :periodType AND periodStartEpoch = :periodStartEpoch
        """
    )
    suspend fun getCompletedMissionIds(periodType: String, periodStartEpoch: Long): List<String>

    @Query(
        """
        SELECT DISTINCT missionId FROM mission_history
        WHERE periodType = :periodType
        AND periodStartEpoch BETWEEN :fromPeriodStartEpoch AND :toPeriodStartEpoch
        """
    )
    suspend fun getCompletedMissionIdsBetween(
        periodType: String,
        fromPeriodStartEpoch: Long,
        toPeriodStartEpoch: Long,
    ): List<String>

    @Query("SELECT COALESCE(SUM(starReward), 0) FROM mission_history")
    suspend fun getTotalStars(): Int

    @Query("DELETE FROM mission_history")
    suspend fun clearAll()
}

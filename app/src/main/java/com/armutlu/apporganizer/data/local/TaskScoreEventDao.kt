package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.armutlu.apporganizer.domain.models.TaskScoreEventEntry

@Dao
interface TaskScoreEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: TaskScoreEventEntry): Long

    @Query("SELECT COALESCE(SUM(delta), 0) FROM task_score_events")
    suspend fun getTotalScore(): Int

    @Query(
        """
        SELECT COALESCE(SUM(delta), 0) FROM task_score_events
        WHERE createdAt BETWEEN :fromInclusive AND :toInclusive
        """
    )
    suspend fun getScoreBetween(
        fromInclusive: Long,
        toInclusive: Long,
    ): Int

    @Query(
        """
        SELECT * FROM task_score_events
        ORDER BY createdAt DESC, id DESC
        LIMIT 1
        """
    )
    suspend fun getLatestEvent(): TaskScoreEventEntry?

    @Query(
        """
        SELECT COUNT(*) FROM task_score_events
        WHERE createdAt BETWEEN :fromInclusive AND :toInclusive
        AND (:positiveOnly = 0 OR delta > 0)
        """
    )
    suspend fun countEventsBetween(
        fromInclusive: Long,
        toInclusive: Long,
        positiveOnly: Boolean = false,
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM task_score_events
        WHERE createdAt BETWEEN :fromInclusive AND :toInclusive
        AND eventKey IN (:eventKeys)
        """
    )
    suspend fun countEventsBetweenByKeys(
        fromInclusive: Long,
        toInclusive: Long,
        eventKeys: List<String>,
    ): Int

    @Query("DELETE FROM task_score_events")
    suspend fun clearAll()
}

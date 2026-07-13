package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyGoalDao {
    @Query("SELECT * FROM weekly_goals WHERE weekStartEpochDay = :weekStartEpochDay ORDER BY categoryId ASC")
    fun observeGoals(weekStartEpochDay: Long): Flow<List<WeeklyGoal>>

    @Query("SELECT * FROM weekly_goals WHERE weekStartEpochDay = :weekStartEpochDay")
    suspend fun getGoalsForWeek(weekStartEpochDay: Long): List<WeeklyGoal>

    @Upsert
    suspend fun upsert(goal: WeeklyGoal)

    @Query("DELETE FROM weekly_goals WHERE categoryId = :categoryId AND weekStartEpochDay = :weekStartEpochDay")
    suspend fun delete(categoryId: String, weekStartEpochDay: Long)

    @Query("UPDATE weekly_goals SET achievedAt = :achievedAt WHERE categoryId = :categoryId AND weekStartEpochDay = :weekStartEpochDay")
    suspend fun markAchieved(categoryId: String, weekStartEpochDay: Long, achievedAt: Long)
}

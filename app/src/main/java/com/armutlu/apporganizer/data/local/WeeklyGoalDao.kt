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

    // P3/P4 — adaptif hedef yaşam döngüsü (EnsureCurrentWeekAdaptiveGoalsUseCase/
    // SettlePreviousWeekAdaptiveGoalsUseCase) için sorgular.

    /** Belirli bir haftadaki AUTO modundaki hedefler — settlement/yeni hafta üretimi bunları filtreler. */
    @Query("SELECT * FROM weekly_goals WHERE weekStartEpochDay = :weekStartEpochDay AND mode = 'AUTO' ORDER BY categoryId ASC")
    suspend fun getAutoGoalsForWeek(weekStartEpochDay: Long): List<WeeklyGoal>

    /** Belirli bir kategori+hafta için tek hedef — pinleme kontrolü ("zaten var mı") için. */
    @Query("SELECT * FROM weekly_goals WHERE categoryId = :categoryId AND weekStartEpochDay = :weekStartEpochDay LIMIT 1")
    suspend fun getGoal(categoryId: String, weekStartEpochDay: Long): WeeklyGoal?

    /**
     * settledAt henüz set edilmemiş (idempotency guard) hedefler — [SettlePreviousWeekAdaptiveGoalsUseCase]
     * bunu okur. Mode'dan BAĞIMSIZ: üst-sınır hedefi sözleşmesi (roadmap S1 fix) MANUAL hedefler
     * için de geçerli — sadece hedef ÜRETİMİ (EnsureCurrentWeekAdaptiveGoalsUseCase) AUTO'ya özeldir.
     */
    @Query("SELECT * FROM weekly_goals WHERE weekStartEpochDay = :weekStartEpochDay AND settledAt IS NULL")
    suspend fun getUnsettledGoals(weekStartEpochDay: Long): List<WeeklyGoal>

    @Query(
        "UPDATE weekly_goals SET status = :status, settledAt = :settledAt " +
            "WHERE categoryId = :categoryId AND weekStartEpochDay = :weekStartEpochDay",
    )
    suspend fun settle(categoryId: String, weekStartEpochDay: Long, status: String, settledAt: Long)

    @Query("UPDATE weekly_goals SET mode = :mode WHERE categoryId = :categoryId AND weekStartEpochDay = :weekStartEpochDay")
    suspend fun setMode(categoryId: String, weekStartEpochDay: Long, mode: String)
}

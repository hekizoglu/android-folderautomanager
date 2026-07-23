package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.armutlu.apporganizer.domain.models.Operation

@Dao
interface OperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: Operation)

    @Query("SELECT * FROM operations WHERE id = :id")
    suspend fun getById(id: String): Operation?

    @Query("SELECT * FROM operations WHERE type = :type ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestByType(type: String): Operation?

    @Query("SELECT * FROM operations ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): Operation?

    @Update
    suspend fun update(operation: Operation)

    @Query("DELETE FROM operations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE operations SET rolledBack = 1, rolledBackAt = :rolledBackAt WHERE id = :id")
    suspend fun markRolledBack(id: String, rolledBackAt: Long)
}

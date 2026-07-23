package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for UndoMergeEntity
 * Handles all database operations related to undo merge history
 */
@Dao
interface UndoMergeDao {

    /**
     * Insert an undo merge record
     */
    @Insert
    suspend fun insertUndoMerge(undoMerge: UndoMergeEntity): Long

    /**
     * Get the latest undo merge record
     */
    @Query("SELECT * FROM undo_merges ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestUndoMerge(): UndoMergeEntity?

    /**
     * Get all undo merge records (newest first)
     */
    @Query("SELECT * FROM undo_merges ORDER BY timestamp DESC")
    fun getAllUndoMerges(): Flow<List<UndoMergeEntity>>

    /**
     * Get undo merge records by source category
     */
    @Query("SELECT * FROM undo_merges WHERE sourceCategoryId = :categoryId ORDER BY timestamp DESC")
    suspend fun getUndoMergesBySourceCategory(categoryId: String): List<UndoMergeEntity>

    /**
     * Delete an undo merge record
     */
    @Delete
    suspend fun deleteUndoMerge(undoMerge: UndoMergeEntity)

    /**
     * Delete all undo merge records
     */
    @Query("DELETE FROM undo_merges")
    suspend fun deleteAllUndoMerges()

    /**
     * Delete old undo merge records (older than specified timestamp)
     */
    @Query("DELETE FROM undo_merges WHERE timestamp < :cutoffTime")
    suspend fun deleteOldUndoMerges(cutoffTime: Long)

    /**
     * Count total undo merge records
     */
    @Query("SELECT COUNT(*) FROM undo_merges")
    suspend fun countUndoMerges(): Int
}

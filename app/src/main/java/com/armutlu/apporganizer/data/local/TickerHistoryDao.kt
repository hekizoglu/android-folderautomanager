package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Ticker arşiv ekranı ("Tüm haberler") için Room DAO. [insertAll] IGNORE stratejisiyle
 * çalışır — aynı [TickerHistoryEntity.id] (dedupeKey) tekrar üretildiğinde mevcut kayıt
 * (okunma durumu dahil) korunur, yeni satır asla açılmaz.
 */
@Dao
interface TickerHistoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<TickerHistoryEntity>)

    @Query("SELECT * FROM ticker_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TickerHistoryEntity>>

    @Query("UPDATE ticker_history SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE ticker_history SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    @Query("DELETE FROM ticker_history WHERE createdAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COUNT(*) FROM ticker_history WHERE isRead = 0")
    fun countUnread(): Flow<Int>
}

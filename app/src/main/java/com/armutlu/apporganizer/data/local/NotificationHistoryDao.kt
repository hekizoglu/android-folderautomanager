package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.armutlu.apporganizer.domain.models.NotificationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {

    @Insert
    suspend fun insert(entry: NotificationHistoryEntity)

    @Query("SELECT * FROM notification_history ORDER BY postedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<NotificationHistoryEntity>>

    @Query("SELECT * FROM notification_history ORDER BY postedAt DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 5000): List<NotificationHistoryEntity>

    @Query("UPDATE notification_history SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notification_history SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    @Query("SELECT COUNT(*) FROM notification_history WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    /** Kullanıcının uzun basarak seçtiği tek geçmiş kaydını kalıcı olarak siler. */
    @Query("DELETE FROM notification_history WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** Zaman bazlı retention — [olderThan]'dan eski kayıtlar silinir (varsayılan 7 gün). */
    @Query("DELETE FROM notification_history WHERE postedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    /**
     * Sayı bazlı retention — [keepLatest] (varsayılan 500) üstündeki en eski kayıtlar silinir.
     * Zaman bazlı temizlik (deleteOlderThan) tek başına yetmez: kullanıcı çok bildirim alan
     * bir cihazda 7 gün içinde binlerce satır birikebilir — bu, tabloyu sabit bir üst sınıra sıkıştırır.
     */
    @Query(
        """
        DELETE FROM notification_history WHERE id IN (
            SELECT id FROM notification_history ORDER BY postedAt DESC
            LIMIT -1 OFFSET :keepLatest
        )
        """,
    )
    suspend fun trimToLatest(keepLatest: Int = 500)

    @Query("DELETE FROM notification_history")
    suspend fun clearAll()
}

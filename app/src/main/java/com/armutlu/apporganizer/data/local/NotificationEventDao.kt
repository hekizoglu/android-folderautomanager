package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.armutlu.apporganizer.domain.models.NotificationEvent
import kotlinx.coroutines.flow.Flow

/** Paket başına bildirim sayısı — rapor sorgu sonucu. */
data class PackageNotifCount(
    val packageName: String,
    val count: Int,
)

/** Hero Bildirimler sekmesi için içeriksiz, paket bazlı son bildirim özeti. */
data class PackageNotificationSummary(
    val packageName: String,
    val count: Int,
    val lastPostedAt: Long,
)

@Dao
interface NotificationEventDao {

    @Insert
    suspend fun insert(event: NotificationEvent)

    @Query("""
        SELECT packageName, COUNT(*) AS count FROM notification_events
        WHERE postedAt >= :since GROUP BY packageName ORDER BY count DESC
    """)
    suspend fun countsSince(since: Long): List<PackageNotifCount>

    @Query("""
        SELECT packageName, COUNT(*) AS count FROM notification_events
        WHERE postedAt >= :since GROUP BY packageName ORDER BY count DESC
    """)
    fun observeCountsSince(since: Long): Flow<List<PackageNotifCount>>

    @Query("""
        SELECT packageName, COUNT(*) AS count, MAX(postedAt) AS lastPostedAt
        FROM notification_events
        WHERE postedAt >= :since
        GROUP BY packageName
        ORDER BY lastPostedAt DESC
        LIMIT :limit
    """)
    fun observeLatestSummaries(
        since: Long,
        limit: Int = 5,
    ): Flow<List<PackageNotificationSummary>>

    @Query("SELECT * FROM notification_events WHERE postedAt >= :since")
    suspend fun eventsSince(since: Long): List<NotificationEvent>

    @Query("SELECT COUNT(*) FROM notification_events WHERE postedAt >= :since")
    suspend fun totalSince(since: Long): Int

    @Query("SELECT MAX(postedAt) FROM notification_events")
    suspend fun latestPostedAt(): Long?

    /** 30 günden eski kayıtları temizler — tablo sınırsız büyümez. */
    @Query("DELETE FROM notification_events WHERE postedAt < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM notification_events")
    suspend fun clearAll()
}

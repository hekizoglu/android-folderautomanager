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

/** Kategori başına içeriksiz bildirim sayısı. */
data class CategoryNotifCount(
    val category: String,
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
        SELECT category, COUNT(*) AS count FROM notification_events
        WHERE postedAt >= :since GROUP BY category ORDER BY count DESC
    """)
    suspend fun categoryCountsSince(since: Long): List<CategoryNotifCount>

    @Query("""
        SELECT COUNT(*) FROM notification_events
        WHERE postedAt >= :since AND wasSuppressed = 1
    """)
    suspend fun suppressedCountSince(since: Long): Int

    @Query("""
        SELECT COUNT(*) FROM notification_events
        WHERE postedAt >= :since AND importanceScore BETWEEN :minScore AND :maxScore
    """)
    suspend fun importanceCountSince(
        since: Long,
        minScore: Int,
        maxScore: Int,
    ): Int

    @Query("""
        SELECT COUNT(*) FROM notification_events
        WHERE postedAt >= :since AND (
            CAST(strftime('%H', postedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) >= :nightStartHour
            OR CAST(strftime('%H', postedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) < :nightEndHour
        )
    """)
    suspend fun nightCountSince(
        since: Long,
        nightStartHour: Int = 23,
        nightEndHour: Int = 7,
    ): Int

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

    @Query("SELECT * FROM notification_events WHERE postedAt >= :since AND postedAt < :until ORDER BY postedAt DESC")
    suspend fun eventsBetween(since: Long, until: Long): List<NotificationEvent>

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

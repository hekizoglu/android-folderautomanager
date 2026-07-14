package com.armutlu.apporganizer.data.local

import androidx.room.*
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for AppInfo entity
 * Handles all database operations related to apps
 */
@Dao
interface AppDao {
    
    /**
     * Insert a single app
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApp(app: AppInfo)

    /**
     * Insert multiple apps — IGNORE: mevcut satır korunur, installTime bozulmaz
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApps(apps: List<AppInfo>)
    
    /**
     * Update an app
     */
    @Update
    suspend fun updateApp(app: AppInfo)
    
    /**
     * Update app category
     */
    @Query("UPDATE apps SET categoryId = :categoryId, lastUpdated = :timestamp WHERE packageName = :packageName")
    suspend fun updateAppCategory(
        packageName: String,
        categoryId: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE apps
        SET categoryId = :categoryId,
            classificationSource = :source,
            classificationConfidence = :confidence,
            classificationReason = :reason,
            classificationReviewState = :reviewState,
            isCategoryLocked = :locked,
            classificationVersion = :version,
            lastClassifiedAt = :classifiedAt,
            lastReviewedAt = :reviewedAt,
            reviewSnoozedUntil = :snoozedUntil,
            lastUpdated = :timestamp
        WHERE packageName = :packageName
    """)
    suspend fun updateAppCategoryWithClassification(
        packageName: String,
        categoryId: String,
        source: String,
        confidence: Int,
        reason: String,
        reviewState: String,
        locked: Boolean,
        version: Int,
        classifiedAt: Long,
        reviewedAt: Long,
        snoozedUntil: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Delete a single app
     */
    @Delete
    suspend fun deleteApp(app: AppInfo)
    
    /**
     * Delete app by package name
     */
    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteAppByPackageName(packageName: String)
    
    /**
     * Delete all apps
     */
    @Query("DELETE FROM apps")
    suspend fun deleteAllApps()
    
    /**
     * Get app by package name
     */
    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): AppInfo?

    /**
     * Get all apps (one-time)
     * Performans: idx_apps_appName index'i ile karsilanir (Migration 10->11) - LIMIT kullanilmaz,
     * cunku BackupManager/SmartInsightWorker gibi tuketiciler tam listeye ihtiyac duyar (D196'da
     * LIMIT 1000 eklenmisti, 1000+ app'li cihazlarda yedekte veri kaybina yol acmasi nedeniyle geri alindi).
     */
    @Query("SELECT * FROM apps ORDER BY appName ASC")
    suspend fun getAllApps(): List<AppInfo>

    /**
     * Get apps page for large datasets (UI pagination icin - opsiyonel kullanim).
     */
    @Query("SELECT * FROM apps ORDER BY appName ASC LIMIT :limit OFFSET :offset")
    suspend fun getAppsPage(limit: Int, offset: Int = 0): List<AppInfo>

    /**
     * Get all apps as Flow (real-time updates)
     * Performans: idx_apps_appName index'i ile karsilanir - LIMIT yok (bkz. getAllApps() notu).
     */
    @Query("SELECT * FROM apps ORDER BY appName ASC")
    fun getAllAppsFlow(): Flow<List<AppInfo>>
    
    /**
     * Get apps by category
     */
    @Query("SELECT * FROM apps WHERE categoryId = :categoryId ORDER BY appName ASC")
    fun getAppsByCategory(categoryId: String): Flow<List<AppInfo>>
    
    /**
     * Get uncategorized apps
     */
    @Query("SELECT * FROM apps WHERE categoryId = 'uncategorized' ORDER BY appName ASC")
    fun getUncategorizedApps(): Flow<List<AppInfo>>

    @Query("""
        SELECT * FROM apps
        WHERE classificationReviewState = 'PENDING'
            AND isSystemApp = 0
            AND (reviewSnoozedUntil = 0 OR reviewSnoozedUntil <= :now)
        ORDER BY classificationConfidence ASC, appName ASC
    """)
    fun getPendingClassificationApps(now: Long = System.currentTimeMillis()): Flow<List<AppInfo>>

    @Query("""
        UPDATE apps
        SET classificationSource = 'USER_CONFIRMED',
            classificationConfidence = 100,
            classificationReason = 'USER_SELECTION',
            classificationReviewState = 'CONFIRMED',
            isCategoryLocked = 1,
            classificationVersion = :version,
            lastReviewedAt = :timestamp,
            reviewSnoozedUntil = 0,
            lastUpdated = :timestamp
        WHERE packageName = :packageName
    """)
    suspend fun confirmClassification(
        packageName: String,
        version: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE apps
        SET classificationReviewState = 'SKIPPED',
            reviewSnoozedUntil = :snoozedUntil,
            lastUpdated = :timestamp
        WHERE packageName = :packageName
    """)
    suspend fun skipClassificationReview(
        packageName: String,
        snoozedUntil: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Count total apps
     */
    @Query("SELECT COUNT(*) FROM apps")
    suspend fun countApps(): Int
    
    /**
     * Count apps in category
     */
    @Query("SELECT COUNT(*) FROM apps WHERE categoryId = :categoryId")
    suspend fun countAppsByCategory(categoryId: String): Int
    
    /**
     * Search apps by name
     */
    @Deprecated("Use searchAppsByNameLimited to avoid unbounded UI reads.")
    @Query("SELECT * FROM apps WHERE appName LIKE '%' || :query || '%' ORDER BY appName ASC")
    fun searchAppsByName(query: String): Flow<List<AppInfo>>

    /**
     * Search apps with a bounded result set for UI paths.
     */
    @Query("SELECT * FROM apps WHERE appName LIKE '%' || :query || '%' ORDER BY appName ASC LIMIT :limit")
    fun searchAppsByNameLimited(query: String, limit: Int = 50): Flow<List<AppInfo>>
    
    /**
     * Get system apps
     */
    @Query("SELECT * FROM apps WHERE isSystemApp = 1 ORDER BY appName ASC")
    fun getSystemApps(): Flow<List<AppInfo>>
    
    /**
     * Get user-installed apps (non-system)
     */
    @Query("SELECT * FROM apps WHERE isSystemApp = 0 ORDER BY appName ASC")
    fun getUserApps(): Flow<List<AppInfo>>
    
    /**
     * Get recently installed apps
     */
    @Query("SELECT * FROM apps ORDER BY installTime DESC LIMIT :limit")
    suspend fun getRecentlyInstalledApps(limit: Int = 10): List<AppInfo>
    
    /**
     * Get apps modified within time range
     */
    @Query("SELECT * FROM apps WHERE lastUpdated >= :fromTime AND lastUpdated <= :toTime ORDER BY lastUpdated DESC")
    suspend fun getAppsModifiedInRange(fromTime: Long, toTime: Long): List<AppInfo>
    
    /**
     * Batch update app categories
     */
    @Query("""
        UPDATE apps 
        SET categoryId = :categoryId, lastUpdated = :timestamp
        WHERE packageName IN (:packageNames)
    """)
    suspend fun updateAppsCategory(
        packageNames: List<String>,
        categoryId: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE apps
        SET categoryId = :categoryId,
            classificationSource = :source,
            classificationConfidence = :confidence,
            classificationReason = :reason,
            classificationReviewState = :reviewState,
            isCategoryLocked = :locked,
            classificationVersion = :version,
            lastClassifiedAt = :classifiedAt,
            lastReviewedAt = :reviewedAt,
            reviewSnoozedUntil = :snoozedUntil,
            lastUpdated = :timestamp
        WHERE packageName IN (:packageNames)
    """)
    suspend fun updateAppsCategoryWithClassification(
        packageNames: List<String>,
        categoryId: String,
        source: String,
        confidence: Int,
        reason: String,
        reviewState: String,
        locked: Boolean,
        version: Int,
        classifiedAt: Long,
        reviewedAt: Long,
        snoozedUntil: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Check if app exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM apps WHERE packageName = :packageName)")
    suspend fun appExists(packageName: String): Boolean
    
    /**
     * Get all package names
     */
    @Query("SELECT packageName FROM apps")
    suspend fun getAllPackageNames(): List<String>

    /**
     * Reset all app categories back to uncategorized
     */
    @Query("""
        UPDATE apps
        SET categoryId = 'uncategorized',
            classificationSource = 'UNKNOWN',
            classificationConfidence = 0,
            classificationReason = 'NO_RELIABLE_MATCH',
            classificationReviewState = 'PENDING',
            isCategoryLocked = 0,
            classificationVersion = 1,
            lastClassifiedAt = 0,
            lastReviewedAt = 0,
            reviewSnoozedUntil = 0
        WHERE isCategoryLocked = 0
    """)
    suspend fun resetAllAppCategories()

    // Adet: launcher'dan her başlatmada +1 (kez açıldı)
    @Query("UPDATE apps SET launchCount = launchCount + 1 WHERE packageName = :packageName")
    suspend fun incrementLaunchCount(packageName: String)

    // Süre: UsageStats ön plan süresi (ms) — syncUsageStats yazar. Alan adı tarihsel: usageCount.
    @Query("UPDATE apps SET usageCount = :timeMs WHERE packageName = :packageName")
    suspend fun updateUsageTimeMs(packageName: String, timeMs: Long)

    // Adet: yedekten geri yükleme için doğrudan set (increment değil)
    @Query("UPDATE apps SET launchCount = :count WHERE packageName = :packageName")
    suspend fun updateLaunchCount(packageName: String, count: Long)

    // Süre (ms) bazlı sıralama — gerçek kullanım büyüklüğü (usageCount alanı ms tutar)
    @Query("SELECT * FROM apps ORDER BY usageCount DESC, appName ASC")
    fun getAllAppsSortedByUsage(): Flow<List<AppInfo>>

    @Query("UPDATE apps SET appSizeBytes = :sizeBytes WHERE packageName = :packageName")
    suspend fun updateAppSize(packageName: String, sizeBytes: Long)

    @Query("UPDATE apps SET isHidden = :hidden WHERE packageName = :packageName")
    suspend fun updateAppHidden(packageName: String, hidden: Boolean)

    @Query("UPDATE apps SET notificationCount = :count WHERE packageName = :packageName")
    suspend fun updateNotificationCount(packageName: String, count: Int)

    @Transaction
    suspend fun updateNotificationCounts(counts: Map<String, Int>) {
        counts.forEach { (packageName, count) ->
            updateNotificationCount(packageName, count)
        }
    }

    @Query("UPDATE apps SET notificationText = :text WHERE packageName = :packageName")
    suspend fun updateNotificationText(packageName: String, text: String)

    @Transaction
    suspend fun updateNotificationTexts(texts: Map<String, String>) {
        texts.forEach { (packageName, text) ->
            updateNotificationText(packageName, text)
        }
    }

    // Gizlilik: bildirim metni ayarı kapatılınca tüm kayıtlı metinleri temizler
    @Query("UPDATE apps SET notificationText = ''")
    suspend fun clearAllNotificationTexts()

    // P0.4: İstatistik sıfırlama sihirbazı — kapsam bazlı toplu sıfırlama (tek UPDATE, per-app döngü yok)
    @Query("UPDATE apps SET usageCount = 0, launchCount = 0")
    suspend fun resetAllUsageCounters()

    @Query("UPDATE apps SET lastUsedTimestamp = 0")
    suspend fun resetAllLastUsedTimestamps()

    @Query("SELECT * FROM apps WHERE isHidden = 1 ORDER BY appName ASC")
    fun getHiddenApps(): Flow<List<AppInfo>>

    @Query("UPDATE apps SET lastUsedTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastUsedTimestamp(packageName: String, timestamp: Long)

    // Sadece mevcut değerden büyükse güncelle — launchApp'ın anlık timestamp'ini ezmez
    @Query("UPDATE apps SET lastUsedTimestamp = :timestamp WHERE packageName = :packageName AND lastUsedTimestamp < :timestamp")
    suspend fun updateLastUsedTimestampIfNewer(packageName: String, timestamp: Long)

    @Query("UPDATE apps SET customNotes = :note WHERE packageName = :packageName")
    suspend fun updateCustomNotes(packageName: String, note: String)
}

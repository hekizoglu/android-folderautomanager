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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfo)
    
    /**
     * Insert multiple apps
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
     */
    @Query("SELECT * FROM apps ORDER BY appName ASC")
    suspend fun getAllApps(): List<AppInfo>
    
    /**
     * Get all apps as Flow (real-time updates)
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
    @Query("SELECT * FROM apps WHERE appName LIKE '%' || :query || '%' ORDER BY appName ASC")
    fun searchAppsByName(query: String): Flow<List<AppInfo>>
    
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
    @Query("UPDATE apps SET categoryId = 'uncategorized'")
    suspend fun resetAllAppCategories()

    @Query("UPDATE apps SET usageCount = usageCount + 1 WHERE packageName = :packageName")
    suspend fun incrementUsageCount(packageName: String)

    @Query("UPDATE apps SET usageCount = :count WHERE packageName = :packageName")
    suspend fun updateUsageCount(packageName: String, count: Long)

    @Query("SELECT * FROM apps ORDER BY usageCount DESC, appName ASC")
    fun getAllAppsSortedByUsage(): Flow<List<AppInfo>>
}

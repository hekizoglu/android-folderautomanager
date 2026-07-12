package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for app-related data operations
 * Acts as a single source of truth for app data
 */
@Singleton
class AppRepository @Inject constructor(
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val classifier: AppClassifier
) {
    fun getAllCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getAllCategoriesFlow()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    suspend fun ensureDefaultCategories() {
        try {
            Category.getDefaultCategories().forEach { category ->
                if (!categoryDao.categoryExists(category.categoryId)) {
                    categoryDao.insertCategory(category)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error ensuring default categories")
        }
    }

    suspend fun addCategory(category: Category) {
        try {
            categoryDao.insertCategory(category)
        } catch (e: Exception) {
            Timber.e(e, "Error adding category")
            throw e
        }
    }

    suspend fun updateCategory(category: Category) {
        try {
            categoryDao.updateCategory(category)
        } catch (e: Exception) {
            Timber.e(e, "Error updating category")
            throw e
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        try {
            categoryDao.deleteCategoryById(categoryId)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting category")
            throw e
        }
    }

    suspend fun findCategoryByName(name: String): Category? {
        return try {
            categoryDao.findByCategoryName(name)
        } catch (e: Exception) {
            Timber.e(e, "Error finding category by name")
            null
        }
    }

    suspend fun getCategoryById(categoryId: String): Category? {
        return try {
            categoryDao.getCategoryById(categoryId)
        } catch (e: Exception) {
            Timber.e(e, "Error getting category by id")
            null
        }
    }

    suspend fun getNextCategoryDisplayOrder(): Int {
        return (categoryDao.getMaxDisplayOrder() ?: 0) + 1
    }

    
    /**
     * Get all apps as a Flow
     */
    fun getAllAppsFlow(): Flow<List<AppInfo>> {
        return appDao.getAllAppsFlow()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * Get all apps one-time
     */
    suspend fun getAllApps(): List<AppInfo> {
        return try {
            appDao.getAllApps()
        } catch (e: Exception) {
            Timber.e(e, "Error getting all apps")
            emptyList()
        }
    }
    
    /**
     * Get apps by category
     */
    fun getAppsByCategory(categoryId: String): Flow<List<AppInfo>> {
        return appDao.getAppsByCategory(categoryId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Get uncategorized apps
     */
    fun getUncategorizedApps(): Flow<List<AppInfo>> {
        return appDao.getUncategorizedApps()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * Get app by package name
     */
    suspend fun getAppByPackageName(packageName: String): AppInfo? {
        return appDao.getAppByPackageName(packageName)
    }
    
    /**
     * Search apps by name
     */
    fun searchApps(query: String): Flow<List<AppInfo>> {
        return appDao.searchAppsByName(query)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * Insert apps with auto-classification
     */
    suspend fun insertApps(apps: List<AppInfo>) {
        try {
            // classifyApp CPU-bound — Dispatchers.Default kullan, IO thread'ini bloke etme
            val classifiedApps = withContext(Dispatchers.Default) {
                apps.map { app ->
                    val category = classifier.classifyApp(app)
                    app.copy(categoryId = category)
                }
            }

            appDao.insertApps(classifiedApps)
            Timber.d("Inserted ${classifiedApps.size} apps")
        } catch (e: Exception) {
            Timber.e(e, "Error inserting apps")
        }
    }
    
    /**
     * Update app category
     */
    suspend fun updateAppCategory(packageName: String, categoryId: String) {
        try {
            appDao.updateAppCategory(packageName, categoryId)
            Timber.d("Updated category for $packageName to $categoryId")
        } catch (e: Exception) {
            Timber.e(e, "Error updating app category")
        }
    }

    suspend fun updateApp(app: AppInfo) {
        try {
            appDao.updateApp(app)
            Timber.d("Updated app metadata for ${app.packageName}")
        } catch (e: Exception) {
            Timber.e(e, "Error updating app")
        }
    }
    
    /**
     * Update multiple apps' category
     */
    suspend fun updateAppsCategory(packageNames: List<String>, categoryId: String) {
        try {
            appDao.updateAppsCategory(packageNames, categoryId)
            Timber.d("Updated ${packageNames.size} apps to category $categoryId")
        } catch (e: Exception) {
            Timber.e(e, "Error updating multiple apps")
        }
    }
    
    /**
     * Delete app
     */
    suspend fun deleteApp(packageName: String) {
        try {
            appDao.deleteAppByPackageName(packageName)
            Timber.d("Deleted app: $packageName")
        } catch (e: Exception) {
            Timber.e(e, "Error deleting app")
        }
    }
    
    /**
     * Clear all apps
     */
    suspend fun clearAllApps() {
        try {
            appDao.deleteAllApps()
            Timber.d("Cleared all apps")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing apps")
        }
    }
    
    /**
     * Count total apps
     */
    suspend fun countApps(): Int {
        return try {
            appDao.countApps()
        } catch (e: Exception) {
            Timber.e(e, "Error counting apps")
            0
        }
    }
    
    /**
     * Count apps in category
     */
    suspend fun countAppsByCategory(categoryId: String): Int {
        return try {
            appDao.countAppsByCategory(categoryId)
        } catch (e: Exception) {
            Timber.e(e, "Error counting apps by category")
            0
        }
    }
    
    /**
     * Get apps with category info
     */
    fun getAppsWithCategories(): Flow<List<AppInfo>> {
        return appDao.getAllAppsFlow()
            .map { apps -> apps.sortedBy { it.appName } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * Get recently installed apps
     */
    suspend fun getRecentlyInstalledApps(limit: Int = 10): List<AppInfo> {
        return try {
            appDao.getRecentlyInstalledApps(limit)
        } catch (e: Exception) {
            Timber.e(e, "Error getting recently installed apps")
            emptyList()
        }
    }
    
    /**
     * Get system apps
     */
    fun getSystemApps(): Flow<List<AppInfo>> {
        return appDao.getSystemApps()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Get user-installed apps
     */
    fun getUserApps(): Flow<List<AppInfo>> {
        return appDao.getUserApps()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * Sync installed apps (main operation for scanning device)
     */
    suspend fun syncInstalledApps(installedApps: List<AppInfo>) {
        try {
            val existingPackages = appDao.getAllPackageNames()
            
            // Insert new or update existing apps
            val appsToInsert = installedApps.filter { app ->
                !existingPackages.contains(app.packageName)
            }
            
            if (appsToInsert.isNotEmpty()) {
                insertApps(appsToInsert)
            }
            
            // Remove uninstalled apps
            val installedPackages = installedApps.map { it.packageName }.toSet()
            existingPackages.forEach { existing ->
                if (!installedPackages.contains(existing)) {
                    deleteApp(existing)
                }
            }
            
            Timber.d("Synced installed apps - Added: ${appsToInsert.size}, Removed: ${existingPackages.size - installedPackages.size}")
        } catch (e: Exception) {
            Timber.e(e, "Error syncing installed apps")
        }
    }
    
    /**
     * Check if app exists
     */
    suspend fun appExists(packageName: String): Boolean {
        return appDao.appExists(packageName)
    }

    suspend fun incrementLaunchCount(packageName: String) {
        try { appDao.incrementLaunchCount(packageName) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun updateUsageTimeMs(packageName: String, timeMs: Long) {
        try { appDao.updateUsageTimeMs(packageName, timeMs) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun updateLaunchCount(packageName: String, count: Long) {
        try { appDao.updateLaunchCount(packageName, count) } catch (e: Exception) { Timber.e(e) }
    }

    fun getAllAppsSortedByUsage(): Flow<List<AppInfo>> =
        appDao.getAllAppsSortedByUsage().distinctUntilChanged().flowOn(Dispatchers.IO)

    suspend fun updateAppSize(packageName: String, sizeBytes: Long) {
        try { appDao.updateAppSize(packageName, sizeBytes) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun resetAllCategories() {
        try {
            appDao.resetAllAppCategories()
            Timber.d("Reset all app categories to uncategorized")
        } catch (e: Exception) {
            Timber.e(e, "Error resetting categories")
        }
    }

    suspend fun updateAppHidden(packageName: String, hidden: Boolean) {
        try { appDao.updateAppHidden(packageName, hidden) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun updateNotificationCount(packageName: String, count: Int) {
        try { appDao.updateNotificationCount(packageName, count) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun updateNotificationText(packageName: String, text: String) {
        try { appDao.updateNotificationText(packageName, text) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun clearAllNotificationTexts() {
        try { appDao.clearAllNotificationTexts() } catch (e: Exception) { Timber.e(e) }
    }

    fun getHiddenApps(): Flow<List<AppInfo>> =
        appDao.getHiddenApps().distinctUntilChanged().flowOn(Dispatchers.IO)

    suspend fun updateLastUsedTimestamp(packageName: String, timestamp: Long) {
        try { appDao.updateLastUsedTimestamp(packageName, timestamp) } catch (e: Exception) { Timber.e(e) }
    }

    // UsageStats sync için — mevcut değerden büyükse yazar, launchApp'ın anlık değerini ezmez
    suspend fun updateLastUsedTimestampIfNewer(packageName: String, timestamp: Long) {
        try { appDao.updateLastUsedTimestampIfNewer(packageName, timestamp) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun updateCustomNotes(packageName: String, note: String) {
        try { appDao.updateCustomNotes(packageName, note) } catch (e: Exception) { Timber.e(e) }
    }
}

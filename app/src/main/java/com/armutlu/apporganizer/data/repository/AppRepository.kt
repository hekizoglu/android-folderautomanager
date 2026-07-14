package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.domain.usecase.classify.CLASSIFICATION_ENGINE_VERSION
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationDecision
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationReason
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationReviewState
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationSource
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
    private val notificationEventDao: NotificationEventDao,
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

    fun getPendingClassificationApps(): Flow<List<AppInfo>> {
        return appDao.getPendingClassificationApps()
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
        return appDao.searchAppsByNameLimited(query.trim(), limit = 50)
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
                    val decision = classifier.classifyAppDecision(app)
                    app.withClassification(decision)
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
            appDao.updateAppCategoryWithClassification(
                packageName = packageName,
                categoryId = categoryId,
                source = ClassificationSource.USER_CORRECTED.name,
                confidence = 100,
                reason = ClassificationReason.USER_SELECTION.name,
                reviewState = ClassificationReviewState.CORRECTED.name,
                locked = true,
                version = CLASSIFICATION_ENGINE_VERSION,
                classifiedAt = System.currentTimeMillis(),
                reviewedAt = System.currentTimeMillis(),
                snoozedUntil = 0L,
            )
            Timber.d("Updated category for $packageName to $categoryId")
        } catch (e: Exception) {
            Timber.e(e, "Error updating app category")
        }
    }

    suspend fun updateAppCategoryAutomatically(packageName: String, decision: ClassificationDecision) {
        try {
            appDao.updateAppCategoryWithClassification(
                packageName = packageName,
                categoryId = decision.categoryId,
                source = decision.source.name,
                confidence = decision.confidence,
                reason = decision.reasonCode.name,
                reviewState = decision.reviewState.name,
                locked = false,
                version = decision.engineVersion,
                classifiedAt = System.currentTimeMillis(),
                reviewedAt = 0L,
                snoozedUntil = 0L,
            )
            Timber.d("Auto-classified $packageName to ${decision.categoryId}")
        } catch (e: Exception) {
            Timber.e(e, "Error auto-classifying app")
        }
    }

    suspend fun confirmClassification(packageName: String) {
        try {
            appDao.confirmClassification(
                packageName = packageName,
                version = CLASSIFICATION_ENGINE_VERSION,
            )
            Timber.d("Confirmed classification for $packageName")
        } catch (e: Exception) {
            Timber.e(e, "Error confirming classification")
        }
    }

    suspend fun skipClassificationReview(packageName: String, days: Int = 7) {
        try {
            val snoozedUntil = System.currentTimeMillis() + days.coerceAtLeast(1) * 24L * 60L * 60L * 1000L
            appDao.skipClassificationReview(packageName, snoozedUntil)
            Timber.d("Skipped classification review for $packageName")
        } catch (e: Exception) {
            Timber.e(e, "Error skipping classification review")
        }
    }

    suspend fun restoreClassificationFromBackup(app: AppInfo, categoryId: String) {
        try {
            appDao.updateAppCategoryWithClassification(
                packageName = app.packageName,
                categoryId = categoryId,
                source = app.classificationSource.ifBlank { ClassificationSource.USER_CORRECTED.name },
                confidence = app.classificationConfidence.coerceIn(0, 100),
                reason = app.classificationReason.ifBlank { ClassificationReason.USER_SELECTION.name },
                reviewState = app.classificationReviewState.ifBlank { ClassificationReviewState.CORRECTED.name },
                locked = app.isCategoryLocked,
                version = app.classificationVersion.coerceAtLeast(1),
                classifiedAt = app.lastClassifiedAt,
                reviewedAt = app.lastReviewedAt,
                snoozedUntil = app.reviewSnoozedUntil,
            )
        } catch (e: Exception) {
            Timber.e(e, "Error restoring classification metadata")
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
            appDao.updateAppsCategoryWithClassification(
                packageNames = packageNames,
                categoryId = categoryId,
                source = ClassificationSource.USER_CORRECTED.name,
                confidence = 100,
                reason = ClassificationReason.USER_SELECTION.name,
                reviewState = ClassificationReviewState.CORRECTED.name,
                locked = true,
                version = CLASSIFICATION_ENGINE_VERSION,
                classifiedAt = System.currentTimeMillis(),
                reviewedAt = System.currentTimeMillis(),
                snoozedUntil = 0L,
            )
            Timber.d("Updated ${packageNames.size} apps to category $categoryId")
        } catch (e: Exception) {
            Timber.e(e, "Error updating multiple apps")
        }
    }

    suspend fun updateAppsCategoryAutomatically(packageNames: List<String>, categoryId: String) {
        try {
            appDao.updateAppsCategoryWithClassification(
                packageNames = packageNames,
                categoryId = categoryId,
                source = ClassificationSource.FALLBACK_OTHER.name,
                confidence = 25,
                reason = ClassificationReason.NO_RELIABLE_MATCH.name,
                reviewState = ClassificationReviewState.PENDING.name,
                locked = false,
                version = CLASSIFICATION_ENGINE_VERSION,
                classifiedAt = System.currentTimeMillis(),
                reviewedAt = 0L,
                snoozedUntil = 0L,
            )
            Timber.d("Auto-updated ${packageNames.size} apps to category $categoryId")
        } catch (e: Exception) {
            Timber.e(e, "Error auto-updating multiple apps")
        }
    }

    suspend fun migrateManualOverrides(overrides: Map<String, String>): Int {
        if (overrides.isEmpty()) return 0
        var migrated = 0
        overrides.forEach { (packageName, categoryId) ->
            val app = appDao.getAppByPackageName(packageName) ?: return@forEach
            if (!app.isCategoryLocked || app.categoryId != categoryId) {
                updateAppCategory(packageName, categoryId)
                migrated++
            }
        }
        return migrated
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
            val existingApps = appDao.getAllApps()
            val existingByPackage = existingApps.associateBy { it.packageName }
            val existingPackages = existingByPackage.keys
            val installedPackages = installedApps.map { it.packageName }.toSet()

            val appsToInsert = installedApps.filter { app ->
                app.packageName !in existingPackages
            }

            if (appsToInsert.isNotEmpty()) {
                insertApps(appsToInsert)
            }

            var updatedCount = 0
            installedApps.forEach { scannedApp ->
                val existing = existingByPackage[scannedApp.packageName] ?: return@forEach
                val merged = scannedApp.copy(
                    categoryId = existing.categoryId,
                    iconUrl = existing.iconUrl,
                    customNotes = existing.customNotes,
                    usageCount = existing.usageCount,
                    launchCount = existing.launchCount,
                    lastUsedTimestamp = existing.lastUsedTimestamp,
                    notificationCount = existing.notificationCount,
                    notificationImportance = existing.notificationImportance,
                    notificationText = existing.notificationText,
                    isHidden = existing.isHidden,
                    installTime = existing.installTime,
                    classificationSource = existing.classificationSource,
                    classificationConfidence = existing.classificationConfidence,
                    classificationReason = existing.classificationReason,
                    classificationReviewState = existing.classificationReviewState,
                    isCategoryLocked = existing.isCategoryLocked,
                    classificationVersion = existing.classificationVersion,
                    lastClassifiedAt = existing.lastClassifiedAt,
                    lastReviewedAt = existing.lastReviewedAt,
                    reviewSnoozedUntil = existing.reviewSnoozedUntil,
                    firstInstalledTime = if (existing.firstInstalledTime > 0L) {
                        existing.firstInstalledTime
                    } else {
                        scannedApp.firstInstalledTime
                    },
                    lastUpdated = System.currentTimeMillis(),
                )
                if (merged != existing) {
                    updateApp(merged)
                    updatedCount++
                }
            }

            var removedCount = 0
            existingPackages.forEach { existing ->
                if (existing !in installedPackages) {
                    deleteApp(existing)
                    removedCount++
                }
            }

            Timber.d(
                "Synced installed apps - Added: ${appsToInsert.size}, Updated: $updatedCount, Removed: $removedCount",
            )
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

    suspend fun updateNotificationCounts(counts: Map<String, Int>) {
        if (counts.isEmpty()) return
        try { appDao.updateNotificationCounts(counts) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun updateNotificationText(packageName: String, text: String) {
        try { appDao.updateNotificationText(packageName, text) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun updateNotificationTexts(texts: Map<String, String>) {
        if (texts.isEmpty()) return
        try { appDao.updateNotificationTexts(texts) } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun clearAllNotificationTexts() {
        try { appDao.clearAllNotificationTexts() } catch (e: Exception) { Timber.e(e) }
    }

    suspend fun clearAllNotificationEvents() {
        try { notificationEventDao.clearAll() } catch (e: Exception) { Timber.e(e) }
    }

    // P0.4: İstatistik sıfırlama sihirbazı — kapsam bazlı toplu sıfırlama
    suspend fun resetAllUsageCounters() {
        appDao.resetAllUsageCounters()
    }

    suspend fun resetAllLastUsedTimestamps() {
        appDao.resetAllLastUsedTimestamps()
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

    private fun AppInfo.withClassification(decision: ClassificationDecision): AppInfo {
        val now = System.currentTimeMillis()
        val locked = decision.source == ClassificationSource.USER_CONFIRMED ||
            decision.source == ClassificationSource.USER_CORRECTED
        return copy(
            categoryId = decision.categoryId,
            classificationSource = decision.source.name,
            classificationConfidence = decision.confidence,
            classificationReason = decision.reasonCode.name,
            classificationReviewState = decision.reviewState.name,
            isCategoryLocked = locked,
            classificationVersion = decision.engineVersion,
            lastClassifiedAt = now,
            lastReviewedAt = if (locked) now else 0L,
            reviewSnoozedUntil = 0L,
        )
    }
}

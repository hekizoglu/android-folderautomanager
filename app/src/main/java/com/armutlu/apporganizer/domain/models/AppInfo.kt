package com.armutlu.apporganizer.domain.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Data model representing an installed application on the device.
 * This model is saved in Room database for persistence.
 */
@Entity(
    tableName = "apps",
    indices = [
        Index(value = ["appName"]),
        Index(value = ["categoryId"]),
        Index(value = ["appName", "categoryId"])
    ]
)
data class AppInfo(
    @PrimaryKey
    val packageName: String,
    
    val appName: String,
    
    val categoryId: String = "uncategorized",
    
    val iconUrl: String = "",
    
    val isSystemApp: Boolean = false,
    
    val isInstalled: Boolean = true,
    
    val installTime: Long = System.currentTimeMillis(),
    
    val lastUpdated: Long = System.currentTimeMillis(),
    
    val customNotes: String = "",

    val usageCount: Long = 0L,

    val lastUsedTimestamp: Long = 0L,

    val notificationCount: Int = 0,

    val notificationImportance: Int = 0,

    val notificationText: String = "",

    val appSizeBytes: Long = 0L,

    val isHidden: Boolean = false,

    val firstInstalledTime: Long = 0L,

    val lastUpdatedTime: Long = 0L,

    val targetSdkVersion: Int = 0,

    val versionName: String = ""
) : Serializable {
    
    companion object {
        /**
         * Create a sample AppInfo for testing
         */
        fun createSample(): AppInfo {
            return AppInfo(
                packageName = "com.example.app",
                appName = "Example App",
                categoryId = "uncategorized",
                isSystemApp = false
            )
        }
        
        /**
         * Create multiple sample apps for testing
         */
        fun createSamples(count: Int = 5): List<AppInfo> {
            return (1..count).map { i ->
                AppInfo(
                    packageName = "com.example.app$i",
                    appName = "App $i",
                    categoryId = when {
                        i % 3 == 0 -> "social"
                        i % 3 == 1 -> "games"
                        else -> "productivity"
                    },
                    isSystemApp = i % 5 == 0
                )
            }
        }
    }
    
    /**
     * Check if this app belongs to a specific category
     */
    fun belongsToCategory(categoryId: String): Boolean {
        return this.categoryId == categoryId
    }
    
    /**
     * Update the category of this app
     */
    fun updateCategory(newCategoryId: String): AppInfo {
        return this.copy(
            categoryId = newCategoryId,
            lastUpdated = System.currentTimeMillis()
        )
    }
}

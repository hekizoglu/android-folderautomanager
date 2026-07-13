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

    // Sure (ms): sistem UsageStats'ten gelen toplam on plan suresi. syncUsageStats yazar.
    // TARIHSEL NOT: Alan adi "usageCount" ama gercek kullanim BUYUKLUGUNU (ms) tutar —
    // siralama/skor/kullanilmama tespiti icin dogru sinyal budur. Adet (kez acildi) icin
    // ayri launchCount alani var. "Milyon adet" bug'i bu ms degerinin "kez acildi" metninde
    // adet sanilmasindan cikiyordu; artik o metinler launchCount kullanir.
    val usageCount: Long = 0L,

    // Adet: kullanicinin bu launcher'dan uygulamayi kac kez baslattigi (+1 increment).
    val launchCount: Long = 0L,

    val lastUsedTimestamp: Long = 0L,

    val notificationCount: Int = 0,

    val notificationImportance: Int = 0,

    val notificationText: String = "",

    val appSizeBytes: Long = 0L,

    val isHidden: Boolean = false,

    val firstInstalledTime: Long = 0L,

    val lastUpdatedTime: Long = 0L,

    val targetSdkVersion: Int = 0,

    val versionName: String = "",

    val classificationSource: String = "UNKNOWN",

    val classificationConfidence: Int = 0,

    val classificationReason: String = "NO_RELIABLE_MATCH",

    val classificationReviewState: String = "PENDING",

    val isCategoryLocked: Boolean = false,

    val classificationVersion: Int = 1,

    val lastClassifiedAt: Long = 0L,

    val lastReviewedAt: Long = 0L,

    val reviewSnoozedUntil: Long = 0L
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

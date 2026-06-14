package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Service for creating folders and organizing apps
 * Uses ShortcutManager API (works with most launchers)
 */
class FolderCreationService(private val context: Context) {
    
    private val shortcutManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        context.getSystemService(ShortcutManager::class.java)
    } else {
        null
    }
    
    /**
     * Organize all apps into category folders
     */
    suspend fun autoOrganizeApps(
        allApps: List<AppInfo>,
        categories: List<Category>
    ): OrganizationResult {
        return withContext(Dispatchers.Default) {
            try {
                Timber.d("Starting auto-organization of ${allApps.size} apps...")
                
                val results = mutableMapOf<String, Int>()
                var successCount = 0
                var failureCount = 0
                
                // Group apps by category
                categories.forEach { category ->
                    val appsInCategory = allApps.filter { it.categoryId == category.categoryId }
                    
                    if (appsInCategory.isNotEmpty()) {
                        val result = createFolderForCategory(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            categoryEmoji = category.iconEmoji,
                            apps = appsInCategory
                        )
                        
                        results[category.categoryName] = appsInCategory.size
                        
                        if (result.success) {
                            successCount += appsInCategory.size
                        } else {
                            failureCount += appsInCategory.size
                        }
                    }
                }
                
                Timber.d("Auto-organization complete: $successCount success, $failureCount failed")
                
                OrganizationResult(
                    success = failureCount == 0,
                    totalAppsOrganized = successCount,
                    failureCount = failureCount,
                    categoryCounts = results
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during auto-organization")
                OrganizationResult(
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * Create a folder (shortcut) for a category with all its apps
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private suspend fun createFolderForCategory(
        categoryId: String,
        categoryName: String,
        categoryEmoji: String,
        apps: List<AppInfo>
    ): FolderCreationResult {
        return withContext(Dispatchers.Default) {
            try {
                if (shortcutManager == null) {
                    return@withContext FolderCreationResult(
                        success = false,
                        error = "ShortcutManager not available"
                    )
                }
                
                val shortcuts = mutableListOf<ShortcutInfo>()
                apps.forEachIndexed { _, app ->
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        
                        if (launchIntent != null) {
                            val appIcon = Icon.createWithResource(context, android.R.drawable.sym_def_app_icon)
                            
                            val shortcut = ShortcutInfo.Builder(context, "$categoryId-${app.packageName}")
                                .setShortLabel(app.appName)
                                .setLongLabel("${app.appName} (${categoryName})")
                                .setIntent(launchIntent)
                                .setIcon(appIcon)
                                .build()
                            
                            shortcuts.add(shortcut)
                            Timber.d("Added shortcut for ${app.appName}")
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error creating shortcut for ${app.packageName}")
                    }
                }
                
                // Push shortcuts to launcher
                if (shortcuts.isNotEmpty()) {
                    try {
                        shortcutManager.dynamicShortcuts = shortcuts.take(8) // Limit to 8 per app
                        Timber.d("Created folder shortcuts for $categoryName (${shortcuts.size} apps)")
                    } catch (e: Exception) {
                        Timber.w(e, "Error pushing shortcuts to launcher")
                    }
                }
                
                FolderCreationResult(
                    success = true,
                    categoryName = categoryName,
                    appCount = apps.size
                )
            } catch (e: Exception) {
                Timber.e(e, "Error creating folder for $categoryName")
                FolderCreationResult(
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * Clear all organized shortcuts
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    suspend fun clearAllShortcuts(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                shortcutManager?.removeAllDynamicShortcuts()
                Timber.d("Cleared all shortcuts")
                true
            } catch (e: Exception) {
                Timber.e(e, "Error clearing shortcuts")
                false
            }
        }
    }
    
    /**
     * Get current shortcuts count
     */
    fun getShortcutsCount(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                shortcutManager?.dynamicShortcuts?.size ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}

/**
 * Result of organization operation
 */
data class OrganizationResult(
    val success: Boolean,
    val totalAppsOrganized: Int = 0,
    val failureCount: Int = 0,
    val categoryCounts: Map<String, Int> = emptyMap(),
    val error: String? = null
) {
    fun toMessage(): String {
        return if (success) {
            "✅ $totalAppsOrganized uygulama kategorilere başarıyla taşındı!\n" +
                    categoryCounts.entries.joinToString("\n") { (category, count) ->
                        "  • $category: $count uygulama"
                    }
        } else {
            "❌ Hata: ${error ?: "Bilinmeyen hata"}"
        }
    }
}

/**
 * Result of folder creation
 */
data class FolderCreationResult(
    val success: Boolean,
    val categoryName: String = "",
    val appCount: Int = 0,
    val error: String? = null
)

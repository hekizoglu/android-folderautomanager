package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import com.armutlu.apporganizer.domain.models.Category

object DockPrefs {
    const val MAX_SLOTS = 5

    private const val PREFS_NAME = "dock_prefs"
    private const val KEY_DOCK_PACKAGES = "dock_packages"
    private const val KEY_PRE_HERO_DOCK_BACKUP = "pre_hero_dock_backup"
    private const val KEY_HERO_DOCK_MIGRATED = "hero_dock_migrated_v1"
    private const val FOLDER_PREFIX = "folder:"

    private val DEFAULT_SLOTS = listOf(
        listOf("com.google.android.dialer", "com.android.dialer"),
        listOf("com.google.android.apps.messaging", "com.android.mms"),
        listOf("com.google.android.GoogleCamera", "com.android.camera2", "com.android.camera"),
        listOf("com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx")
    )

    fun getDockPackages(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_DOCK_PACKAGES, null)
        return if (saved != null) {
            saved.split(",").filter { it.isNotBlank() }.take(MAX_SLOTS)
        } else {
            resolveDefaults(context)
        }
    }

    fun saveDockPackages(context: Context, packages: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_DOCK_PACKAGES, packages.take(MAX_SLOTS).joinToString(","))
        }
    }

    fun migrateToHeroDock(context: Context, fallbackPackages: List<String>): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getDockPackages(context)
        if (!prefs.getBoolean(KEY_HERO_DOCK_MIGRATED, false)) {
            prefs.edit {
                putString(KEY_PRE_HERO_DOCK_BACKUP, current.joinToString(","))
                putBoolean(KEY_HERO_DOCK_MIGRATED, true)
            }
        }
        val candidates = buildHeroDockItems(current, fallbackPackages)
        val installed = candidates.filter { context.packageManager.getLaunchIntentForPackage(it) != null }
        saveDockPackages(context, installed)
        return installed
    }

    internal fun buildHeroDockItems(
        current: List<String>,
        fallbackPackages: List<String>,
    ): List<String> = (current.filterNot(::isFolderItem) + fallbackPackages)
        .filter(String::isNotBlank)
        .distinct()
        .take(MAX_SLOTS)

    fun addToDock(context: Context, packageName: String): Boolean {
        val current = getDockPackages(context).toMutableList()
        if (current.contains(packageName) || current.size >= MAX_SLOTS) return false
        current.add(packageName)
        saveDockPackages(context, current)
        return true
    }

    fun removeFromDock(context: Context, packageName: String): Boolean {
        val current = getDockPackages(context).toMutableList()
        val removed = current.remove(packageName)
        if (!removed) return false
        saveDockPackages(context, current)
        return true
    }

    fun isInDock(context: Context, packageName: String): Boolean =
        getDockPackages(context).contains(packageName)

    fun folderItem(categoryId: String): String = "$FOLDER_PREFIX$categoryId"

    fun isFolderItem(item: String): Boolean = item.startsWith(FOLDER_PREFIX)

    fun folderId(item: String): String? =
        item.takeIf { isFolderItem(it) }?.removePrefix(FOLDER_PREFIX)?.takeIf { it.isNotBlank() }

    fun addSocialFolderIfRoom(context: Context): Boolean {
        val item = folderItem(Category.CAT_SOCIAL)
        val current = getDockPackages(context).toMutableList()
        if (item in current || current.size >= MAX_SLOTS) return false
        current.add(item)
        saveDockPackages(context, current)
        return true
    }

    private fun resolveDefaults(context: Context): List<String> {
        val pm = context.packageManager
        val dialer = resolveDefaultApp(
            context,
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))
        )
        val sms = resolveDefaultApp(
            context,
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
        )
        val camera = DEFAULT_SLOTS[2].firstOrNull { pkg -> pm.getLaunchIntentForPackage(pkg) != null }
        val browser = resolveDefaultBrowser(context)

        return listOfNotNull(dialer, sms, camera, browser)
            .distinct()
            .take(MAX_SLOTS)
            .ifEmpty {
                DEFAULT_SLOTS.mapNotNull { candidates ->
                    candidates.firstOrNull { pkg -> pm.getLaunchIntentForPackage(pkg) != null }
                }
            }
    }

    private fun resolveDefaultBrowser(context: Context): String? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        return resolveDefaultApp(context, intent)
    }

    private fun resolveDefaultApp(context: Context, intent: Intent): String? {
        return context.packageManager
            .resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
            ?.takeIf { context.packageManager.getLaunchIntentForPackage(it) != null }
    }
}

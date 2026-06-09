package com.armutlu.apporganizer.utils

import android.content.Context
import androidx.core.content.edit

object DockPrefs {

    private const val PREFS_NAME = "dock_prefs"
    private const val KEY_DOCK_PACKAGES = "dock_packages"
    private const val MAX_SLOTS = 4

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

    fun addToDock(context: Context, packageName: String): Boolean {
        val current = getDockPackages(context).toMutableList()
        if (current.contains(packageName) || current.size >= MAX_SLOTS) return false
        current.add(packageName)
        saveDockPackages(context, current)
        return true
    }

    fun removeFromDock(context: Context, packageName: String) {
        val current = getDockPackages(context).toMutableList()
        current.remove(packageName)
        saveDockPackages(context, current)
    }

    fun isInDock(context: Context, packageName: String): Boolean =
        getDockPackages(context).contains(packageName)

    private fun resolveDefaults(context: Context): List<String> {
        val pm = context.packageManager
        return DEFAULT_SLOTS.mapNotNull { candidates ->
            candidates.firstOrNull { pkg ->
                pm.getLaunchIntentForPackage(pkg) != null
            }
        }
    }
}

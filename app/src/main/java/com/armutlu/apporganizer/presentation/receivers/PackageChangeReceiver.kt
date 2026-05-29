package com.armutlu.apporganizer.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.armutlu.apporganizer.utils.PackageManagerHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Broadcast receiver for package install/uninstall events
 * Updates app list when apps are added or removed
 */
class PackageChangeReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart ?: return
        
        Timber.d("Package event: $action for $packageName")
        
        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                Timber.d("App installed: $packageName")
                refreshAppList(context)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                Timber.d("App uninstalled: $packageName")
                refreshAppList(context)
            }
            Intent.ACTION_PACKAGE_CHANGED -> {
                Timber.d("App updated: $packageName")
                // Optionally refresh for app updates
            }
        }
    }
    
    /**
     * Refresh app list when package changes
     */
    private fun refreshAppList(context: Context) {
        GlobalScope.launch {
            try {
                val packageManager = PackageManagerHelper(context)
                val apps = packageManager.getInstalledApps(includeSystem = true)
                Timber.d("Refreshed app list: ${apps.size} apps")
                // Would typically sync with ViewModel/Database here
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing app list")
            }
        }
    }
}

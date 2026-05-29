package com.armutlu.apporganizer.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.armutlu.apporganizer.utils.PackageManagerHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart ?: return

        Timber.d("Package event: $action for $packageName")

        when (action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED -> refreshAppList(context)
            Intent.ACTION_PACKAGE_CHANGED -> Timber.d("App updated: $packageName")
        }
    }

    private fun refreshAppList(context: Context) {
        GlobalScope.launch {
            try {
                val apps = PackageManagerHelper(context).getInstalledApps()
                Timber.d("Refreshed app list: ${apps.size} apps")
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing app list")
            }
        }
    }
}

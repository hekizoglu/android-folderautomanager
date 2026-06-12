package com.armutlu.apporganizer.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.utils.PackageManagerHelper
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PackageChangeReceiver : BroadcastReceiver() {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun appRepository(): AppRepository
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        Timber.d("Package event: ${intent.action} for $packageName")

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> onPackageAdded(context, packageName)
            Intent.ACTION_PACKAGE_REMOVED -> onPackageRemoved(context, packageName)
            Intent.ACTION_PACKAGE_CHANGED -> onPackageChanged(context, packageName)
        }
    }

    private fun onPackageAdded(context: Context, packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = getRepository(context)
                val helper = PackageManagerHelper(context)
                val appInfo = helper.getAppInfo(packageName) ?: return@launch
                repo.insertApps(listOf(appInfo))
                Timber.d("Added new app to DB: $packageName")
            } catch (e: Exception) {
                Timber.e(e, "Error adding app: $packageName")
            }
        }
    }

    private fun onPackageRemoved(context: Context, packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = getRepository(context)
                repo.deleteApp(packageName)
                Timber.d("Removed app from DB: $packageName")
            } catch (e: Exception) {
                Timber.e(e, "Error removing app: $packageName")
            }
        }
    }

    private fun onPackageChanged(context: Context, packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = getRepository(context)
                if (!repo.appExists(packageName)) return@launch
                // Mevcut kaydı koru (kategori, gizlilik), sadece PM'den gelen taze veriyi güncelle
                val existing = repo.getAppByPackageName(packageName) ?: return@launch
                val helper = PackageManagerHelper(context)
                val fresh = helper.getAppInfo(packageName) ?: return@launch
                val merged = fresh.copy(
                    categoryId   = existing.categoryId,
                    isHidden     = existing.isHidden,
                    usageCount   = existing.usageCount,
                    lastUsedTimestamp = existing.lastUsedTimestamp,
                    notificationCount = existing.notificationCount
                )
                repo.insertApps(listOf(merged))
                Timber.d("App updated (category preserved): $packageName")
            } catch (e: Exception) {
                Timber.e(e, "Error handling app change: $packageName")
            }
        }
    }

    private fun getRepository(context: Context): AppRepository {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        )
        return entryPoint.appRepository()
    }
}

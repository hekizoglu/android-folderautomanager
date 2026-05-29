package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Helper for interacting with PackageManager to get installed apps
 */
class PackageManagerHelper(private val context: Context) {
    
    private val packageManager = context.packageManager
    
    /**
     * Get all installed apps asynchronously
     */
    suspend fun getInstalledApps(includeSystem: Boolean = true): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Scanning installed apps...")
                
                val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

                val apps = packages
                    .filter { pkgInfo ->
                        if (!includeSystem) !isSystemApp(pkgInfo.applicationInfo)
                        else true
                    }
                    .mapNotNull { pkgInfo ->
                        try {
                            val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
                            val appName = appInfo.loadLabel(packageManager).toString()
                            AppInfo(
                                packageName = pkgInfo.packageName,
                                appName = appName,
                                categoryId = "uncategorized",
                                isSystemApp = isSystemApp(appInfo),
                                isInstalled = true,
                                installTime = pkgInfo.firstInstallTime,
                                lastUpdated = pkgInfo.lastUpdateTime
                            )
                        } catch (e: Exception) {
                            Timber.w(e, "Error loading package: ${pkgInfo.packageName}")
                            null
                        }
                    }
                
                Timber.d("Found ${apps.size} installed apps")
                apps
            } catch (e: Exception) {
                Timber.e(e, "Error getting installed apps")
                emptyList()
            }
        }
    }
    
    /**
     * Get only user-installed apps (excluding system apps)
     */
    suspend fun getUserInstalledApps(): List<AppInfo> {
        return getInstalledApps(includeSystem = false)
    }
    
    /**
     * Get only system apps
     */
    suspend fun getSystemApps(): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            getInstalledApps(includeSystem = true)
                .filter { it.isSystemApp }
        }
    }
    
    /**
     * Check if app is installed
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Get app info by package name
     */
    suspend fun getAppInfo(packageName: String): AppInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val pkgInfo = packageManager.getPackageInfo(packageName, 0)
                val appInfo = pkgInfo.applicationInfo ?: return@withContext null
                val appName = appInfo.loadLabel(packageManager).toString()
                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    isSystemApp = isSystemApp(appInfo),
                    isInstalled = true,
                    installTime = pkgInfo.firstInstallTime,
                    lastUpdated = pkgInfo.lastUpdateTime
                )
            } catch (e: Exception) {
                Timber.e(e, "Error getting app info for $packageName")
                null
            }
        }
    }
    
    /**
     * Launch app
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                Timber.w("No launch intent for $packageName")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error launching app: $packageName")
            false
        }
    }
    
    /**
     * Uninstall app (opens system uninstall dialog)
     */
    fun uninstallApp(packageName: String): Boolean {
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.e(e, "Error uninstalling app: $packageName")
            false
        }
    }
    
    /**
     * Get app size
     */
    fun getAppSize(packageName: String): Long {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appDir = appInfo.sourceDir
            java.io.File(appDir).length()
        } catch (e: Exception) {
            Timber.e(e, "Error getting app size for $packageName")
            0L
        }
    }
    
    /**
     * Get app version
     */
    fun getAppVersion(packageName: String): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            Timber.e(e, "Error getting app version for $packageName")
            "Unknown"
        }
    }
    
    /**
     * Check if app is system app
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }
    
    /**
     * Get installed app count
     */
    suspend fun getInstalledAppCount(includeSystem: Boolean = true): Int {
        return getInstalledApps(includeSystem).size
    }
}

package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.armutlu.apporganizer.domain.models.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for interacting with PackageManager to get installed apps
 */
@Singleton
class PackageManagerHelper @Inject constructor(@ApplicationContext private val context: Context) {
    
    private val packageManager = context.packageManager
    
    companion object {
        // Kullanıcıya gösterilmemesi gereken sistem paketi önekleri
        private val HIDDEN_PREFIXES = listOf(
            "com.android.providers.", "com.android.server.", "com.android.systemui",
            "com.android.inputmethod", "com.android.keychain", "com.android.cts.",
            "com.android.statementservice", "com.android.calllogbackup",
            "com.google.android.syncadapters", "com.google.android.backuptransport",
            "com.google.android.gms.policy", "com.android.shell",
            "com.android.printspooler", "com.android.bips",
            "com.qualcomm.", "com.qti.", "com.qcom.", "org.codeaurora.",
            "com.mediatek.", "com.samsung.android.providers.",
            "android.autoinstalls.", "com.android.hotwordenrollment",
            "com.android.overlay", ".overlay", "android.ext.",
        )

        fun shouldHide(packageName: String): Boolean =
            HIDDEN_PREFIXES.any { packageName.startsWith(it) } ||
            packageName == "android" ||
            packageName.contains(".overlay.")
    }

    /**
     * Get all installed apps asynchronously.
     * @param includeSystem Include system apps in results
     * @param onlyLaunchable Only include apps that have a launcher icon (visible to user)
     */
    suspend fun getInstalledApps(
        includeSystem: Boolean = true,
        onlyLaunchable: Boolean = true
    ): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Scanning installed apps...")

                val apps: List<AppInfo> = if (onlyLaunchable) {
                    // queryIntentActivities: tek sorguda sadece launcher-visible uygulamalari doner.
                    // getInstalledPackages(GET_META_DATA) + her paket icin getLaunchIntentForPackage
                    // yerine ~5x daha hizli — 100 uygulamada ~200ms tasarruf.
                    val launchIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                        .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    packageManager.queryIntentActivities(launchIntent, 0)
                        .distinctBy { it.activityInfo.packageName }
                        .filter { ri ->
                            if (shouldHide(ri.activityInfo.packageName)) return@filter false
                            if (!includeSystem) {
                                val flags = ri.activityInfo.applicationInfo?.flags ?: 0
                                if (flags and ApplicationInfo.FLAG_SYSTEM != 0) return@filter false
                            }
                            true
                        }
                        .mapNotNull { ri ->
                            runCatching {
                                val appInfo = ri.activityInfo.applicationInfo ?: return@mapNotNull null
                                val pkgInfo = packageManager.getPackageInfo(ri.activityInfo.packageName, 0)
                                AppInfo(
                                    packageName = ri.activityInfo.packageName,
                                    appName = appInfo.loadLabel(packageManager).toString(),
                                    appFileName = appInfo.packageFileName(),
                                    categoryId = "uncategorized",
                                    isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                                    isInstalled = true,
                                    installTime = pkgInfo.firstInstallTime,
                                    lastUpdated = pkgInfo.lastUpdateTime,
                                    appSizeBytes = runCatching {
                                        java.io.File(appInfo.sourceDir).length()
                                    }.getOrDefault(0L),
                                    firstInstalledTime = pkgInfo.firstInstallTime,
                                    lastUpdatedTime = pkgInfo.lastUpdateTime,
                                    targetSdkVersion = appInfo.targetSdkVersion,
                                    versionName = pkgInfo.versionName ?: ""
                                )
                            }.onFailure {
                                Timber.w(it, "Error loading package: ${ri.activityInfo.packageName}")
                            }.getOrNull()
                        }
                } else {
                    packageManager.getInstalledPackages(0)
                        .filter { pkgInfo ->
                            if (shouldHide(pkgInfo.packageName)) return@filter false
                            if (!includeSystem && (pkgInfo.applicationInfo?.let { isSystemApp(it) } == true)) return@filter false
                            true
                        }
                        .mapNotNull { pkgInfo ->
                            runCatching {
                                val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
                                AppInfo(
                                    packageName = pkgInfo.packageName,
                                    appName = appInfo.loadLabel(packageManager).toString(),
                                    appFileName = appInfo.packageFileName(),
                                    categoryId = "uncategorized",
                                    isSystemApp = isSystemApp(appInfo),
                                    isInstalled = true,
                                    installTime = pkgInfo.firstInstallTime,
                                    lastUpdated = pkgInfo.lastUpdateTime,
                                    appSizeBytes = runCatching {
                                        java.io.File(appInfo.sourceDir).length()
                                    }.getOrDefault(0L),
                                    firstInstalledTime = pkgInfo.firstInstallTime,
                                    lastUpdatedTime = pkgInfo.lastUpdateTime,
                                    targetSdkVersion = appInfo.targetSdkVersion,
                                    versionName = pkgInfo.versionName ?: ""
                                )
                            }.onFailure {
                                Timber.w(it, "Error loading package: ${pkgInfo.packageName}")
                            }.getOrNull()
                        }
                }

                Timber.d("Found ${apps.size} installed apps (onlyLaunchable=$onlyLaunchable)")
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
                    appFileName = appInfo.packageFileName(),
                    isSystemApp = isSystemApp(appInfo),
                    isInstalled = true,
                    installTime = pkgInfo.firstInstallTime,
                    lastUpdated = pkgInfo.lastUpdateTime,
                    firstInstalledTime = pkgInfo.firstInstallTime,
                    lastUpdatedTime = pkgInfo.lastUpdateTime,
                    targetSdkVersion = appInfo.targetSdkVersion,
                    versionName = pkgInfo.versionName ?: ""
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
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
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
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
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

    private fun ApplicationInfo.packageFileName(): String {
        val source = publicSourceDir ?: sourceDir ?: return ""
        return java.io.File(source).nameWithoutExtension
    }
    
    /**
     * Get installed app count
     */
    suspend fun getInstalledAppCount(includeSystem: Boolean = true): Int {
        return getInstalledApps(includeSystem).size
    }
}

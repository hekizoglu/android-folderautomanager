package com.armutlu.apporganizer.presentation.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.armutlu.apporganizer.presentation.navigation.AppNavigation
import com.armutlu.apporganizer.presentation.ui.screens.OnboardingScreen
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.CrashReporter
import com.armutlu.apporganizer.utils.PackageManagerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

private const val PREFS_NAME = "app_organizer_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AppListViewModel by viewModels()

    // Android 10+ RoleManager launcher seçim sonucu
    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Seçim yapıldı ya da iptal — devam et */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        CrashReporter.install(this)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

        // Onboarding bittiyse launcher seçimini kontrol et
        if (onboardingDone && !isDefaultLauncher()) {
            requestDefaultLauncher()
        }

        setContent {
            AppOrganizerTheme {
                var showOnboarding by remember { mutableStateOf(!onboardingDone) }

                if (showOnboarding) {
                    OnboardingScreen(onFinish = {
                        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                        showOnboarding = false
                        scanApps()
                        // Onboarding bitti, launcher seçimini tetikle
                        if (!isDefaultLauncher()) requestDefaultLauncher()
                    })
                } else {
                    AppNavigation(
                        viewModel = viewModel,
                        onSendBugReport = { openBugReport() }
                    )
                }
            }
        }

        if (onboardingDone) scanApps()

        applyOpenCategoryIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyOpenCategoryIntent(intent)
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == packageName
    }

    private fun requestDefaultLauncher() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — RoleManager ile doğrudan "Ev ekranı uygulaması" dialog'u
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    roleRequestLauncher.launch(intent)
                }
            } else {
                // Android 9 ve altı — HOME intent gönder, sistem seçtirsin
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        } catch (e: Exception) {
            Timber.e(e, "requestDefaultLauncher failed")
        }
    }

    private fun applyOpenCategoryIntent(intent: Intent?) {
        val categoryId = intent?.getStringExtra("open_category") ?: return
        lifecycleScope.launch {
            viewModel.screenState.first { !it.isLoading && !it.isInitializing }
            viewModel.setSelectedCategory(categoryId)
        }
    }

    private fun scanApps() {
        lifecycleScope.launch {
            try {
                Timber.d("Scanning device apps...")
                val apps = PackageManagerHelper(applicationContext).getInstalledApps(
                    includeSystem = true,
                    onlyLaunchable = true
                )
                Timber.d("Found ${apps.size} apps, syncing...")
                viewModel.syncInstalledApps(apps)
            } catch (e: Exception) {
                Timber.e(e, "Error scanning apps")
            }
        }
    }

    private fun openBugReport() {
        val debugLogs = viewModel.getDebugLogs()
        val crashLog = CrashReporter.getLastCrashLog(this)?.take(1500) ?: "Crash logu yok"

        val title = Uri.encode("[Bug] Uygulama Hatasi")
        val body = Uri.encode(
            "**Cihaz:** ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
            "**Android:** ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n\n" +
            "**Son Crash:**\n```\n$crashLog\n```\n\n" +
            "**Debug Loglari:**\n```\n$debugLogs\n```\n\n" +
            "**Nasil Olustu:**\n(Adimlar)"
        )
        val url = "https://github.com/hekizoglu/android-folderautomanager/issues/new?title=$title&body=$body"
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addCategory(Intent.CATEGORY_BROWSABLE) },
                "Tarayici sec"
            )
        )
    }
}

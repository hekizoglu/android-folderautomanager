package com.armutlu.apporganizer.presentation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.armutlu.apporganizer.presentation.navigation.AppNavigation
import com.armutlu.apporganizer.presentation.ui.screens.OnboardingScreen
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.PackageManagerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

private const val PREFS_NAME = "app_organizer_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AppListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

        setContent {
            AppOrganizerTheme {
                var showOnboarding by remember { mutableStateOf(!onboardingDone) }

                if (showOnboarding) {
                    OnboardingScreen(onFinish = {
                        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                        showOnboarding = false
                        scanApps()
                    })
                } else {
                    AppNavigation(
                        viewModel      = viewModel,
                        onSendBugReport = { openBugReport() }
                    )
                }
            }
        }

        // Onboarding bittiyse direkt tara
        if (onboardingDone) scanApps()
    }

    private fun scanApps() {
        lifecycleScope.launch {
            try {
                Timber.d("Scanning device apps...")
                val apps = PackageManagerHelper(applicationContext).getInstalledApps(
                    includeSystem = true,
                    onlyLaunchable = true  // Launcher ikonu olmayanları (sistem servisleri) hariç tut
                )
                Timber.d("Found ${apps.size} apps, syncing...")
                viewModel.syncInstalledApps(apps)
            } catch (e: Exception) {
                Timber.e(e, "Error scanning apps")
            }
        }
    }

    private fun openBugReport() {
        val logs = viewModel.getDebugLogs()
        val title = Uri.encode("[Bug] Uygulama Hatası")
        val body  = Uri.encode(
            "**Cihaz:** ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
            "**Android:** ${android.os.Build.VERSION.RELEASE}\n\n" +
            "**Hata Detayları:**\n$logs\n\n" +
            "**Nasıl Oluştu:**\n(Adımları buraya yazın)"
        )
        val url = "https://github.com/hekizoglu/android-folderautomanager/issues/new?title=$title&body=$body"
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addCategory(Intent.CATEGORY_BROWSABLE) },
            "Tarayıcı seç"
        ))
    }
}

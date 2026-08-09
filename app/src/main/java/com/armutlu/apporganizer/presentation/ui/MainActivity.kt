package com.armutlu.apporganizer.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.armutlu.apporganizer.presentation.navigation.AppNavigation
import com.armutlu.apporganizer.presentation.ui.screens.OnboardingScreen
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.CrashReporter
import com.armutlu.apporganizer.utils.PackageManagerHelper
import com.armutlu.apporganizer.AppOrganizerApp
import com.armutlu.apporganizer.utils.StartupHealthPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var packageManagerHelper: PackageManagerHelper

    private val viewModel: AppListViewModel by viewModels()
    private val pendingRoute = mutableStateOf<String?>(null)

    companion object {
        const val EXTRA_OPEN_ROUTE = "open_route"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val activityStartedAt = SystemClock.elapsedRealtime()
        val coldStart = AppOrganizerApp.consumeColdStart()
        // installSplashScreen super.onCreate'ten ONCE cagrilmali (AndroidX resmi kilavuz) —
        // gec cagrilinca postSplashScreenTheme uygulanmiyor ve DeviceDefault gri baslik
        // cubugu ("App Organizer") tum MainActivity ekranlarinda kaliyordu (D234 fix).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        CrashReporter.install(this)

        val onboardingDone = AppPrefs.isOnboardingDone(this)

        setContent {
            AppOrganizerTheme {
                var showOnboarding by remember { mutableStateOf(!onboardingDone) }

                if (showOnboarding) {
                    OnboardingScreen(onFinish = {
                        AppPrefs.markOnboardingDone(this@MainActivity)
                        showOnboarding = false
                    })
                } else {
                    AppNavigation(
                        viewModel = viewModel,
                        externalRoute = pendingRoute.value,
                        onExternalRouteConsumed = { pendingRoute.value = null }
                    )
                }
            }
        }
        window.decorView.doOnPreDraw {
            StartupHealthPrefs.markReady(this, if (coldStart) AppOrganizerApp.processStartedAtElapsed else activityStartedAt, coldStart, home = false)
            reportFullyDrawn()
            // D234: scanApps cold start'ta ilk frame'i bloke etmesin diye pre-draw sonrasına ötelendi
            lifecycleScope.launch {
                delay(800) // UI'ın stabil hale gelmesi için tampon
                scanApps()
            }
        }

        pendingRoute.value = intent?.getStringExtra(EXTRA_OPEN_ROUTE)
        applyOpenCategoryIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_OPEN_ROUTE)
        applyOpenCategoryIntent(intent)
    }

    private fun applyOpenCategoryIntent(intent: Intent?) {
        val categoryId = intent?.getStringExtra("open_category") ?: return
        // Güvenlik: exported Activity'ye dışarıdan gelen extra — boş veya asırı uzun
        // (gerçek kategori id'leri kısa sabit string'lerdir) değerleri yok say.
        if (categoryId.isBlank() || categoryId.length > 64) {
            Timber.w("Gecersiz open_category extra yok sayildi (uzunluk=${categoryId.length})")
            return
        }
        lifecycleScope.launch {
            viewModel.screenState.first { !it.isLoading && !it.isInitializing }
            viewModel.setSelectedCategory(categoryId)
        }
    }

    private fun scanApps() {
        lifecycleScope.launch {
            try {
                Timber.d("Scanning device apps...")
                val apps = packageManagerHelper.getInstalledApps(
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

}

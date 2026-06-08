package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.armutlu.apporganizer.presentation.ui.MainActivity
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import dagger.hilt.android.AndroidEntryPoint

private const val PREFS_NAME = "app_organizer_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

/**
 * Launcher Activity — Android HOME ekranı olarak çalışır.
 *
 * android:windowShowWallpaper="true" (tema üzerinden) ile sistem duvar kağıdı arka planda görünür.
 * Compose içeriği şeffaf Surface üzerinde çizilir.
 *
 * NASIL ETKİNLEŞTİRİLİR:
 *   Ayarlar > Uygulamalar > Varsayılan uygulamalar > Başlatıcı (Launcher) > AppOrganizer
 *   VEYA: Ana sayfa tuşuna bas → "Her zaman" ile AppOrganizer'ı seç
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Onboarding tamamlanmamışsa → setup akışına yönlendir, bu activity'yi sonlandır
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
            finish()
            return
        }

        // Durum ve navigasyon çubuklarını şeffaf yap
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        viewModel.syncUsageStats(this)

        setContent {
            AppOrganizerTheme(darkTheme = true) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == packageName
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}

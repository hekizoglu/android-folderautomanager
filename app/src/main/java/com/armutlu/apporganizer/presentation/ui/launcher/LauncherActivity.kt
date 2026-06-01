package com.armutlu.apporganizer.presentation.ui.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import dagger.hilt.android.AndroidEntryPoint

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

        // Durum ve navigasyon çubuklarını şeffaf yap
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Arka planı tamamen şeffaf tut — sistem duvar kağıdı görünsün
            AppOrganizerTheme(darkTheme = true) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    // Launcher'da geri tuşu normalde hiçbir şey yapmaz;
    // BackHandler Compose içinde (HomeScreen) yönetir.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // İleride BackHandler yakalamadıysa burada da handle et; şimdi intentional no-op
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}

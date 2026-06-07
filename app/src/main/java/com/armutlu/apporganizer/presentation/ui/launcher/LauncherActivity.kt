package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

        viewModel.syncUsageStats(this)

        setContent {
            // Arka planı tamamen şeffaf tut — sistem duvar kağıdı görünsün
            AppOrganizerTheme(darkTheme = true) {
                var showLauncherDialog by remember {
                    mutableStateOf(!isDefaultLauncher(this@LauncherActivity))
                }

                if (showLauncherDialog) {
                    AlertDialog(
                        onDismissRequest = { showLauncherDialog = false },
                        title = { Text("Ana Ekran Uygulaması") },
                        text = { Text("App Organizer'ı ana ekran (launcher) olarak ayarlamak ister misiniz?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showLauncherDialog = false
                                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }) {
                                Text("Evet, Ayarla")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLauncherDialog = false }) {
                                Text("Hayır, Daha Sonra")
                            }
                        }
                    )
                }

                HomeScreen(viewModel = viewModel)
            }
        }
    }

    private fun isDefaultLauncher(context: android.content.Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
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

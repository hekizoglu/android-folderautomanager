package com.armutlu.apporganizer.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.armutlu.apporganizer.presentation.navigation.AppNavigation
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.PackageManagerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AppListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // İlk açılışta cihaz uygulamalarını tara ve DB'ye yükle
        lifecycleScope.launch {
            try {
                Timber.d("Scanning device apps on startup...")
                val helper = PackageManagerHelper(applicationContext)
                val installedApps = helper.getInstalledApps(includeSystem = true)
                Timber.d("Found ${installedApps.size} apps, syncing...")
                viewModel.syncInstalledApps(installedApps)
            } catch (e: Exception) {
                Timber.e(e, "Error scanning apps on startup")
            }
        }

        setContent {
            AppOrganizerTheme {
                AppOrganizerApp(
                    viewModel = viewModel,
                    onSendBugReport = { openBugReport() }
                )
            }
        }
    }

    private fun openBugReport() {
        val logs = viewModel.getDebugLogs()
        val issueTitle = Uri.encode("[Bug] Uygulama Hatası")
        val issueBody = Uri.encode("""
**Cihaz:** ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
**Android:** ${android.os.Build.VERSION.RELEASE}

**Hata Detayları:**
$logs

**Nasıl Oluştu:**
(Adımları buraya yazın)
        """.trimIndent())
        val url = "https://github.com/hekizoglu/android-folderautomanager/issues/new?title=$issueTitle&body=$issueBody"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
fun AppOrganizerApp(
    viewModel: AppListViewModel,
    onSendBugReport: () -> Unit = {}
) {
    Surface(color = Color.Transparent) {
        AppNavigation(viewModel = viewModel, onSendBugReport = onSendBugReport)
    }
}

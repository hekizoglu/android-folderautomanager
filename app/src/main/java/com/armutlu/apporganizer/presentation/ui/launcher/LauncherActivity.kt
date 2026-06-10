package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private var lastHomePressMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel.loadAppsIfEmpty(this)
        viewModel.syncUsageStats(this)
        setContent {
            AppOrganizerTheme(darkTheme = true) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    // Home tuşuna iki kez hızlıca basılınca (≤500ms) AllApps açılır.
    // Launcher zaten ön planda iken HOME → onNewIntent tetiklenir.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val now = System.currentTimeMillis()
        if (now - lastHomePressMs <= 500L) {
            viewModel.openAllApps()
            lastHomePressMs = 0L
        } else {
            lastHomePressMs = now
        }
    }

    private val packageRemovedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart ?: return
            viewModel.onPackageRemoved(pkg)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(packageRemovedReceiver, IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        })
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(packageRemovedReceiver) }
    }

    private fun isDefaultLauncher(context: android.content.Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }

    // Launcher'da back tuşu uygulamayı kapatmamalı
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // intentionally no-op: HomeScreen'deki BackHandler halleder
    }
}

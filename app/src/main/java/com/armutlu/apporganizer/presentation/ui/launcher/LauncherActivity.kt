package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private var lastHomePressMs = 0L
    private var receiverRegistered = false

    // config_navBarInteractionMode cihaz yeniden başlamadan değişmez — bir kere okumak yeterli
    private val gestureNavEnabled: Boolean by lazy {
        val resId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        resId > 0 && resources.getInteger(resId) == 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Onboarding bitmemişse MainActivity'ye yönlendir
        val prefs = getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean(AppPrefs.KEY_ONBOARDING_DONE, false)) {
            startActivity(
                android.content.Intent(this, com.armutlu.apporganizer.presentation.ui.MainActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            return
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel.loadAppsIfEmpty(this)
        viewModel.syncUsageStats(this)
        AppPrefs.markUsageStatsSynced(this)  // onResume'da tekrar tetiklenmesin
        viewModel.syncAppSizes(this)
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

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart ?: return
            when (intent.action) {
                Intent.ACTION_PACKAGE_REMOVED -> viewModel.onPackageRemoved(pkg)
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REPLACED -> viewModel.onPackageAdded(context, pkg)
            }
        }
    }

    private fun applyNavBarVisibility() {
        if (AppPrefs.isNavButtonsHidden(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let {
                    it.hide(WindowInsets.Type.navigationBars())
                    // Gesture nav aktifse SHOW_TRANSIENT_BARS home gesture ile çakışır
                    it.systemBarsBehavior = if (gestureNavEnabled) {
                        WindowInsetsController.BEHAVIOR_DEFAULT
                    } else {
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.show(WindowInsets.Type.navigationBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyNavBarVisibility()
        // Reconcile 5 dakikada bir — geri tuşunda her seferinde PM sorgusu yapmasın
        if (AppPrefs.shouldReconcile(this)) {
            viewModel.reconcileIfNeeded(this)
            AppPrefs.markReconciled(this)
        }
        // Usage stats pahalı bir sorgu — 30 dakikada bir senkronize et
        if (AppPrefs.shouldSyncUsageStats(this)) {
            viewModel.syncUsageStats(this)
            AppPrefs.markUsageStatsSynced(this)
        }
        viewModel.loadDockPackages(this)
        if (!receiverRegistered) {
            registerReceiver(packageReceiver, PACKAGE_FILTER)
            receiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(packageReceiver) }
            receiverRegistered = false
        }
    }

    // Launcher'da back tuşu uygulamayı kapatmamalı
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // intentionally no-op: HomeScreen'deki BackHandler halleder
    }

    companion object {
        private val PACKAGE_FILTER = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
    }
}

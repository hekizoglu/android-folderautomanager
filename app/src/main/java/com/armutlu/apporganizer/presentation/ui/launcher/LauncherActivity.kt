package com.armutlu.apporganizer.presentation.ui.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.WidgetHostManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private var lastHomePressMs = 0L

    // config_navBarInteractionMode cihaz yeniden başlamadan değişmez — bir kere okumak yeterli
    private val gestureNavEnabled: Boolean by lazy {
        val resId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        resId > 0 && resources.getInteger(resId) == 2
    }

    // Widget picker/configuration iki ayrı activity sonucu döndürür. Bu ID, yapılandırma
    // tamamlanana veya kullanıcı iptal edene kadar korunmalıdır; erken sıfırlanırsa bazı OEM
    // widget'ları result Intent içinde ID döndürmediği için ana ekrana hiç eklenmez.
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    val widgetPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val returnedWidgetId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val widgetId = returnedWidgetId.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            ?: pendingWidgetId

        if (result.resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Widget bind başarılı: konfigürasyon aktivitesi varsa başlat.
            pendingWidgetId = widgetId
            val manager = AppWidgetManager.getInstance(this)
            val info = manager.getAppWidgetInfo(widgetId)
            if (info?.configure != null) {
                val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                // Bazı widget'ların configure aktivitesi dışarıya export edilmemiş olabilir
                // (ör. Google arama araçları) — SecurityException tüm launcher'ı çökertmesin.
                val launched = runCatching { widgetConfigureLauncher.launch(configIntent) }.isSuccess
                if (!launched) {
                    viewModel.addWidgetId(this, widgetId)
                    pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                }
                // Başarıyla açıldıysa pendingWidgetId yapılandırma sonucu gelene kadar korunur.
            } else {
                viewModel.addWidgetId(this, widgetId)
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            }
        } else {
            // İptal — picker'ın döndürdüğü veya daha önce tahsis edilen ID'yi serbest bırak.
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                WidgetHostManager.deleteId(this, widgetId)
            }
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    // Widget konfigürasyon aktivitesi sonucu. Bazı sağlayıcılar result Intent'e ID koymaz;
    // bu durumda picker aşamasından saklanan pendingWidgetId kullanılmalıdır.
    private val widgetConfigureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val returnedWidgetId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val widgetId = returnedWidgetId.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            ?: pendingWidgetId

        if (result.resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            viewModel.addWidgetId(this, widgetId)
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetHostManager.deleteId(this, widgetId)
        }
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    /** HomeScreen'den çağrılır — kullanıcıya widget seçtirme akışını başlatır. */
    fun launchWidgetPicker() {
        // Önceki yarım kalmış seçimden kalan host ID varsa sızıntı bırakma.
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetHostManager.deleteId(this, pendingWidgetId)
        }
        pendingWidgetId = WidgetHostManager.allocateId(this)
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
        }
        val launched = runCatching { widgetPickerLauncher.launch(intent) }.isSuccess
        if (!launched) {
            WidgetHostManager.deleteId(this, pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastHomePressMs = savedInstanceState?.getLong(KEY_LAST_HOME_PRESS_MS) ?: 0L
        pendingWidgetId = savedInstanceState?.getInt(
            KEY_PENDING_WIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Onboarding bitmemişse MainActivity'ye yönlendir
        if (!AppPrefs.isOnboardingDone(this)) {
            startActivity(
                android.content.Intent(this, com.armutlu.apporganizer.presentation.ui.MainActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
            finish()
            return
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val safeMode = com.armutlu.apporganizer.utils.CrashReporter.checkSafeMode(this)
        if (safeMode) {
            android.widget.Toast.makeText(
                this,
                "Güvenli mod: Ayarlar varsayılana alındı. Ayarlar menüsünden çıkabilirsiniz.",
                android.widget.Toast.LENGTH_LONG,
            ).show()
            com.armutlu.apporganizer.utils.CrashReporter.exitSafeMode(this)
        }

        // Launcher her zaman Room katalogundan aninda acilir. Tam katalog taramasi sadece
        // bootstrap/surum gecisi durumlarinda veya dusuk frekansli fallback'te calisir.
        viewModel.reconcileIfNeeded(this)
        viewModel.initFavorites(this)
        viewModel.syncUsageStats(this) { AppPrefs.markUsageStatsSynced(this) }
        viewModel.syncAppSizes(this)
        viewModel.loadWidgetIds(this)
        setContent {
            AppOrganizerTheme(darkTheme = true) {
                LauncherNavGraph(
                    viewModel = viewModel,
                    onLaunchWidgetPicker = { launchWidgetPicker() },
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_LAST_HOME_PRESS_MS, lastHomePressMs)
        outState.putInt(KEY_PENDING_WIDGET_ID, pendingWidgetId)
    }

    // Home tuşuna iki kez hızlıca basılınca (≤500ms) AllApps açılır.
    // Launcher zaten ön planda iken HOME → onNewIntent tetiklenir.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (viewModel.allAppsOpen.value) {
            viewModel.closeAllApps()
            return
        }
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
                Intent.ACTION_PACKAGE_REPLACED,
                -> viewModel.onPackageAdded(context, pkg)
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

    override fun onStart() {
        super.onStart()
        registerReceiver(packageReceiver, PACKAGE_FILTER)
    }

    override fun onResume() {
        super.onResume()
        com.armutlu.apporganizer.utils.CrashReporter.markStartedSuccessfully(this)
        WidgetHostManager.startListening(this)
        applyNavBarVisibility()
        // Son başlatılan uygulamanın timestamp'ini garantile (startActivity süreci durdurduğunda coroutine tamamlanamayabilir)
        viewModel.refreshLastLaunched()
        // Olay bazli package sync ana akistir; tam reconcile sadece 12 saatlik fallback'te calisir.
        if (AppPrefs.shouldReconcile(this)) {
            viewModel.reconcileIfNeeded(this)
        }
        // Usage stats pahalı bir sorgu — 30 dakikada bir senkronize et
        if (AppPrefs.shouldSyncUsageStats(this)) {
            viewModel.syncUsageStats(this)
            AppPrefs.markUsageStatsSynced(this)
        }
        viewModel.loadDockPackages(this)
    }

    override fun onPause() {
        super.onPause()
        WidgetHostManager.stopListening()
    }

    override fun onStop() {
        runCatching { unregisterReceiver(packageReceiver) }
        super.onStop()
    }

    // Launcher split-screen'e alınırsa tam ekrana geri döner (resizeableActivity=false yeterli değil tüm OEM'lerde)
    override fun onMultiWindowModeChanged(
        isInMultiWindowMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (isInMultiWindowMode) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                stopLockTask()
            }
        }
    }

    // Launcher'da back tuşu uygulamayı kapatmamalı
    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // intentionally no-op: HomeScreen'deki BackHandler halleder
    }

    companion object {
        private const val KEY_LAST_HOME_PRESS_MS = "last_home_press_ms"
        private const val KEY_PENDING_WIDGET_ID = "pending_widget_id"
        private val PACKAGE_FILTER = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
    }
}

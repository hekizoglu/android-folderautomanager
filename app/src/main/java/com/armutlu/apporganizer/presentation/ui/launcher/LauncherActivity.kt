package com.armutlu.apporganizer.presentation.ui.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import com.armutlu.apporganizer.AppOrganizerApp
import com.armutlu.apporganizer.utils.StartupHealthPrefs
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.WidgetHostManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    // config_navBarInteractionMode cihaz yeniden başlamadan değişmez — bir kere okumak yeterli
    private val gestureNavEnabled: Boolean by lazy {
        val resId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        resId > 0 && resources.getInteger(resId) == 2
    }

    // Widget picker/configuration iki ayrı activity sonucu döndürür. Bu ID, yapılandırma
    // tamamlanana veya kullanıcı iptal edene kadar korunmalıdır; erken sıfırlanırsa bazı OEM
    // widget'ları result Intent içinde ID döndürmediği için ana ekrana hiç eklenmez.
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    // MIUI'de ACTION_APPWIDGET_PICK bazen sessizce hiçbir Activity açmadan "başarılı" döner
    // (launch() exception fırlatmaz, ama picker ekranı hiç görünmez — bilinen MIUI 12+
    // platform kısıtlaması, üçüncü parti launcher'larda yaygın bir sorun). Bu zaman damgası,
    // launchWidgetPicker() çağrıldığı anı tutar; onResume()'da hâlâ set haldeyse (yani Activity
    // ara bir Activity'ye hiç gitmeden geri döndüyse) picker'ın gerçekte açılmadığı anlaşılır.
    private var widgetPickerLaunchedAtMs: Long = 0L

    val widgetPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        widgetPickerLaunchedAtMs = 0L
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
        val intent = createWidgetPickIntent(pendingWidgetId)
        val launched = runCatching { widgetPickerLauncher.launch(intent) }
            .onFailure { Timber.e(it, "Widget picker baslatilamadi") }
            .isSuccess
        if (!launched) {
            WidgetHostManager.deleteId(this, pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            Toast.makeText(
                this,
                "Widget seçici açılamadı. Cihaz bu widget host akışını desteklemiyor olabilir.",
                Toast.LENGTH_LONG,
            ).show()
        } else {
            // launch() exception fırlatmadı ama bazı OEM ROM'larında (özellikle MIUI 12+) picker
            // Activity'si sessizce hiç açılmayabilir — onResume() bu durumu tespit edecek.
            widgetPickerLaunchedAtMs = SystemClock.elapsedRealtime()
        }
    }

    /**
     * MIUI 12+ ve bazı OEM ROM'larında `ACTION_APPWIDGET_PICK` intent'i sessizce hiçbir Activity
     * açmadan "başarılı" dönebilir (KISS Launcher #1733 ile aynı bilinen platform kısıtlaması) —
     * exception yok, picker ekranı da yok. [widgetPickerLaunchedAtMs] hâlâ set haldeyse ve Activity
     * kesintisiz onResume'a geri döndüyse (yani araya gerçekten başka bir Activity girmediyse)
     * picker'ın hiç açılmadığı anlaşılır; kullanıcıya MIUI'ye özgü izin yönlendirmesiyle bilgi ver.
     */
    private fun checkWidgetPickerSilentFailure() {
        if (widgetPickerLaunchedAtMs == 0L) return
        val elapsed = SystemClock.elapsedRealtime() - widgetPickerLaunchedAtMs
        widgetPickerLaunchedAtMs = 0L
        if (elapsed >= WIDGET_PICKER_SILENT_FAILURE_MS) return
        WidgetHostManager.deleteId(this, pendingWidgetId)
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        val isMiui = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
        val message = if (isMiui) {
            "Widget seçici açılamadı. MIUI, üçüncü parti launcher'larda bunu bazen engelliyor — " +
                "Ayarlar > Uygulamalar > AppOrganizer > Diğer İzinler'den \"Açılır pencereler\" ve " +
                "\"Ana ekran kısayolları\" izinlerini açıp tekrar dene."
        } else {
            "Widget seçici açılamadı. Cihazın izin ayarlarını kontrol et."
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val activityStartedAt = SystemClock.elapsedRealtime()
        val coldStart = AppOrganizerApp.consumeColdStart()
        super.onCreate(savedInstanceState)
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
        applyOpenFolderIntent(intent)
        setContent {
            AppOrganizerTheme(darkTheme = true) {
                LauncherNavGraph(
                    viewModel = viewModel,
                    onLaunchWidgetPicker = { launchWidgetPicker() },
                )
            }
        }
        window.decorView.doOnPreDraw {
            StartupHealthPrefs.markReady(this, if (coldStart) AppOrganizerApp.processStartedAtElapsed else activityStartedAt, coldStart, home = true)
            reportFullyDrawn()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING_WIDGET_ID, pendingWidgetId)
    }

    // Döngü P12 — Home tuşu artık pager'a doğrudan erişmez; Activity yalnızca "All Apps zaten
    // açık mı" kısa devresini kendisi ele alır (roadmap madde 1, davranış P00'dan beri DEĞİŞMEDİ),
    // aksi halde ham sinyali LauncherViewModel.onHomePressed() ile yayınlar. Çift-basış penceresi
    // + search/modal kapatma + başlangıç sayfasına dönme kararı artık HomeCommandPolicy.
    // resolveHomeCommand() içinde — bu karar search/modal state'inin (fullScreenSearchOpen,
    // folderSearchQuery, dockEditOpen vb.) yaşadığı HomeScreen'de toplanır (bkz. HomeScreen.kt
    // `LaunchedEffect(Unit) { viewModel.homePressed.collect { ... } }`). `lastHomePressMs` de bu
    // yüzden HomeScreen'e taşındı (rememberSaveable) — Activity artık bu zaman damgasını TUTMAZ.
    // Launcher zaten ön planda iken HOME → onNewIntent tetiklenir.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyOpenFolderIntent(intent)
        if (viewModel.allAppsOpen.value) {
            viewModel.closeAllApps()
            return
        }
        viewModel.onHomePressed()
    }

    /**
     * Klasör Önerileri ekranında "Kabul Et" gibi dışarıdan gelen isteklerle
     * doğrudan hedef klasörü açar. MainActivity.applyOpenCategoryIntent() ile
     * aynı doğrulama deseni: boş veya aşırı uzun (gerçek kategori id'leri kısa
     * sabit string'lerdir) değerleri yok say.
     */
    private fun applyOpenFolderIntent(intent: Intent?) {
        val categoryId = intent?.getStringExtra(EXTRA_OPEN_FOLDER_CATEGORY_ID) ?: return
        if (categoryId.isBlank() || categoryId.length > 64) {
            Timber.w("Gecersiz open_folder_category_id extra yok sayildi (uzunluk=${categoryId.length})")
            return
        }
        // Aynı intent'in Activity recreate/configuration change sonrası tekrar
        // işlenmesini önle — singleTask launchMode'da intent uzun ömürlü kalabilir.
        intent.removeExtra(EXTRA_OPEN_FOLDER_CATEGORY_ID)
        viewModel.openFolderByCategoryId(categoryId)
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
        checkWidgetPickerSilentFailure()
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
        const val EXTRA_OPEN_FOLDER_CATEGORY_ID = "open_folder_category_id"
        private const val KEY_PENDING_WIDGET_ID = "pending_widget_id"
        // onResume() bu sürenin altında tetiklenirse (Activity'nin picker'a hiç gitmeden geri
        // dönmesi) picker'ın sessizce açılmadığı kabul edilir. 800ms, gerçek bir Activity geçişinin
        // her zaman aşacağı ama false-positive üretmeyecek kadar kısa bir eşik.
        private const val WIDGET_PICKER_SILENT_FAILURE_MS = 800L
        private val PACKAGE_FILTER = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
    }
}

/**
 * Home tuşu çift-basış politikası — LauncherActivity.onNewIntent() ile senkron tutulmalı.
 * AllApps zaten açıkken bu fonksiyon çağrılmaz (çağıran taraf önce kapatır).
 */
internal sealed interface HomePressDecision {
    val nextLastHomePressMs: Long
    data class OpenAllApps(override val nextLastHomePressMs: Long) : HomePressDecision
    data class RecordPress(override val nextLastHomePressMs: Long) : HomePressDecision
}

internal const val HOME_DOUBLE_PRESS_WINDOW_MS = 500L

internal fun homePressDecision(lastHomePressMs: Long, nowMs: Long): HomePressDecision =
    if (nowMs - lastHomePressMs <= HOME_DOUBLE_PRESS_WINDOW_MS) {
        HomePressDecision.OpenAllApps(nextLastHomePressMs = 0L)
    } else {
        HomePressDecision.RecordPress(nextLastHomePressMs = nowMs)
    }

internal fun createWidgetPickIntent(appWidgetId: Int): Intent =
    Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putParcelableArrayListExtra(
            AppWidgetManager.EXTRA_CUSTOM_INFO,
            ArrayList<AppWidgetProviderInfo>(),
        )
        putParcelableArrayListExtra(
            AppWidgetManager.EXTRA_CUSTOM_EXTRAS,
            ArrayList<Bundle>(),
        )
    }

package com.armutlu.apporganizer.presentation.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.armutlu.apporganizer.presentation.ui.MainActivity

/**
 * Launcher ile yonetim arayuzu arasindaki Bildirim Raporu acilis sozlesmesi.
 *
 * Klasor icindeki bildirim rozeti LauncherActivity'de yasarken rapor MainActivity NavHost'unda
 * bulunur. Bu nesne route + ilk sekme bilgisini tek yerde tutar; ekran bilgiyi bir kez tuketir.
 */
internal object NotificationReportLaunchContract {
    const val TAB_REPORT = 0
    const val TAB_HISTORY = 1

    private const val EXTRA_INITIAL_TAB = "notification_report_initial_tab"

    fun openHistory(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.NOTIFICATION_REPORT)
            putExtra(EXTRA_INITIAL_TAB, TAB_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Ilk sekme istegini tekrar kullanilmamasi icin Activity intent'inden silerek okur. */
    fun consumeInitialTab(context: Context): Int {
        val activity = context.findActivity() ?: return TAB_REPORT
        val launchIntent = activity.intent ?: return TAB_REPORT
        val requestedTab = launchIntent.getIntExtra(EXTRA_INITIAL_TAB, TAB_REPORT)
        launchIntent.removeExtra(EXTRA_INITIAL_TAB)
        return normalizeTab(requestedTab)
    }

    internal fun normalizeTab(requestedTab: Int): Int =
        requestedTab.coerceIn(TAB_REPORT, TAB_HISTORY)

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

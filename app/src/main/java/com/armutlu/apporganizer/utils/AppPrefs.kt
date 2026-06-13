package com.armutlu.apporganizer.utils

import android.content.Context

object AppPrefs {
    const val PREFS_NAME = "app_organizer_prefs"
    const val KEY_ONBOARDING_DONE = "onboarding_done"
    const val KEY_LAUNCHER_SETUP_SHOWN = "launcher_setup_shown"

    // Kullanılmayan uygulamaları gri göster — gün cinsinden (0 = kapalı)
    const val KEY_UNUSED_GREY_DAYS = "unused_grey_days"
    const val UNUSED_GREY_DEFAULT = 0  // varsayılan kapalı

    fun getUnusedGreyDays(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_UNUSED_GREY_DAYS, UNUSED_GREY_DEFAULT)

    fun setUnusedGreyDays(context: Context, days: Int) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_UNUSED_GREY_DAYS, days).apply()

    // Swipe-up ipucu — ilk 5 açılışta göster, sonra kaybol
    const val KEY_SWIPE_HINT_COUNT = "swipe_hint_count"
    const val SWIPE_HINT_MAX = 5

    fun shouldShowSwipeHint(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SWIPE_HINT_COUNT, 0) < SWIPE_HINT_MAX
    }

    fun incrementSwipeHintCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_SWIPE_HINT_COUNT, 0)
        prefs.edit().putInt(KEY_SWIPE_HINT_COUNT, current + 1).apply()
    }

    // Özellik toggle'ları — SettingsScreen'den yönetilir
    const val KEY_SWIPE_HINT_ENABLED   = "swipe_hint_enabled"
    const val KEY_NEW_BADGE_ENABLED    = "new_badge_enabled"
    const val KEY_FOLDER_COUNT_VISIBLE = "folder_count_visible"
    const val KEY_FOLDER_SWIPE_HINT    = "folder_swipe_hint_enabled"

    fun isSwipeHintEnabled(context: Context)   = prefs(context).getBoolean(KEY_SWIPE_HINT_ENABLED, true)
    fun isNewBadgeEnabled(context: Context)    = prefs(context).getBoolean(KEY_NEW_BADGE_ENABLED, true)
    fun isFolderCountVisible(context: Context) = prefs(context).getBoolean(KEY_FOLDER_COUNT_VISIBLE, true)
    fun isFolderSwipeHintEnabled(context: Context) = prefs(context).getBoolean(KEY_FOLDER_SWIPE_HINT, true)

    fun setSwipeHintEnabled(context: Context, v: Boolean)   = prefs(context).edit().putBoolean(KEY_SWIPE_HINT_ENABLED, v).apply()
    fun setNewBadgeEnabled(context: Context, v: Boolean)    = prefs(context).edit().putBoolean(KEY_NEW_BADGE_ENABLED, v).apply()
    fun setFolderCountVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_FOLDER_COUNT_VISIBLE, v).apply()
    fun setFolderSwipeHintEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_FOLDER_SWIPE_HINT, v).apply()

    // Otomatik yedekleme — uygulama ilk acildiginda otomatik JSON yedegi al
    const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    fun isAutoBackupEnabled(context: Context) = prefs(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
    fun setAutoBackupEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, v).apply()

    // Navigasyon/sistem butonlarini gizle
    const val KEY_HIDE_NAV_BUTTONS = "hide_nav_buttons"
    fun isNavButtonsHidden(context: Context) = prefs(context).getBoolean(KEY_HIDE_NAV_BUTTONS, false)
    fun setNavButtonsHidden(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HIDE_NAV_BUTTONS, v).apply()

    // AllAppsDrawer arka plan opakligi (0.0 - 1.0)
    const val KEY_ALLAPPS_BG_ALPHA = "allapps_bg_alpha"
    fun getAllAppsBgAlpha(context: Context) = prefs(context).getFloat(KEY_ALLAPPS_BG_ALPHA, 0.85f)
    fun setAllAppsBgAlpha(context: Context, v: Float) = prefs(context).edit().putFloat(KEY_ALLAPPS_BG_ALPHA, v).apply()

    // Bildirim metni goster (FolderTile + AllApps altinda)
    const val KEY_NOTIFICATION_TEXT_ENABLED = "notification_text_enabled"
    fun isNotificationTextEnabled(context: Context) = prefs(context).getBoolean(KEY_NOTIFICATION_TEXT_ENABLED, false)
    fun setNotificationTextEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_NOTIFICATION_TEXT_ENABLED, v).apply()

    // İkon paketi — kurulu ikon paketlerinden seçilir, "" = sistem ikonları
    const val KEY_ICON_PACK = "icon_pack_package"
    fun getIconPack(context: Context): String = prefs(context).getString(KEY_ICON_PACK, "") ?: ""
    fun setIconPack(context: Context, pkg: String) {
        prefs(context).edit().putString(KEY_ICON_PACK, pkg).apply()
        IconPackManager.clearCache()
    }

    // Widget alanı — ana ekranda widget göster
    const val KEY_WIDGET_AREA_ENABLED = "widget_area_enabled"
    fun isWidgetAreaEnabled(context: Context) = prefs(context).getBoolean(KEY_WIDGET_AREA_ENABLED, true)
    fun setWidgetAreaEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_WIDGET_AREA_ENABLED, v).apply()

    // Reconcile throttle — her 5 dakikada bir paket listesini kontrol et
    private const val KEY_LAST_RECONCILE = "last_reconcile_ms"
    private const val RECONCILE_INTERVAL_MS = 5L * 60 * 1000 // 5 dakika

    fun shouldReconcile(context: Context): Boolean {
        val last = prefs(context).getLong(KEY_LAST_RECONCILE, 0L)
        return System.currentTimeMillis() - last > RECONCILE_INTERVAL_MS
    }

    fun markReconciled(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_RECONCILE, System.currentTimeMillis()).apply()
    }

    // Usage stats sync throttle — her 30 dakikada bir senkronize et
    private const val KEY_LAST_USAGE_SYNC = "last_usage_sync_ms"
    private const val USAGE_SYNC_INTERVAL_MS = 30L * 60 * 1000 // 30 dakika

    fun shouldSyncUsageStats(context: Context): Boolean {
        val last = prefs(context).getLong(KEY_LAST_USAGE_SYNC, 0L)
        return System.currentTimeMillis() - last > USAGE_SYNC_INTERVAL_MS
    }

    fun markUsageStatsSynced(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_USAGE_SYNC, System.currentTimeMillis()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

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

    // Klasör/uygulama label rengi — hex string (varsayılan beyaz "#FFFFFF")
    const val KEY_LABEL_COLOR = "label_color"
    fun getLabelColor(context: Context): String = prefs(context).getString(KEY_LABEL_COLOR, "#FFFFFF") ?: "#FFFFFF"
    fun setLabelColor(context: Context, hex: String) = prefs(context).edit().putString(KEY_LABEL_COLOR, hex).apply()

    // Üretici bazlı sınıflandırma — Samsung/Huawei/Xiaomi uygulamaları otomatik kategorilensin mi?
    const val KEY_MANUFACTURER_CLASSIFY = "manufacturer_classify"
    fun isManufacturerClassifyEnabled(context: Context) = prefs(context).getBoolean(KEY_MANUFACTURER_CLASSIFY, true)
    fun setManufacturerClassifyEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_MANUFACTURER_CLASSIFY, v).apply()

    // Klasör boyutu — tile genişliği 56-96dp arası (varsayılan 72dp)
    const val KEY_FOLDER_SIZE = "folder_size_dp"
    fun getFolderSizeDp(context: Context): Int = prefs(context).getInt(KEY_FOLDER_SIZE, 72)
    fun setFolderSizeDp(context: Context, dp: Int) = prefs(context).edit().putInt(KEY_FOLDER_SIZE, dp).apply()

    // Klasör sıralama modu — tüm klasörler için global
    const val KEY_FOLDER_SORT_MODE = "folder_sort_mode"
    fun getFolderSortMode(context: Context): String =
        prefs(context).getString(KEY_FOLDER_SORT_MODE, "ALPHA") ?: "ALPHA"
    fun setFolderSortMode(context: Context, mode: String) =
        prefs(context).edit().putString(KEY_FOLDER_SORT_MODE, mode).apply()

    // Widget alanı — ana ekranda widget göster
    const val KEY_WIDGET_AREA_ENABLED = "widget_area_enabled"
    fun isWidgetAreaEnabled(context: Context) = prefs(context).getBoolean(KEY_WIDGET_AREA_ENABLED, true)
    fun setWidgetAreaEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_WIDGET_AREA_ENABLED, v).apply()

    // Arka plan tipi — "wallpaper" (duvar kağıdı) | "solid" (düz renk)
    const val KEY_BG_TYPE = "bg_type"
    fun getBgType(context: Context): String = prefs(context).getString(KEY_BG_TYPE, "wallpaper") ?: "wallpaper"
    fun setBgType(context: Context, type: String) = prefs(context).edit().putString(KEY_BG_TYPE, type).apply()

    // Düz renk arka plan rengi — ARGB int
    const val KEY_BG_COLOR = "bg_color"
    fun getBgColor(context: Context): Int = prefs(context).getInt(KEY_BG_COLOR, 0xFF1A1A2E.toInt())
    fun setBgColor(context: Context, color: Int) = prefs(context).edit().putInt(KEY_BG_COLOR, color).apply()

    // Yazı/ikon etiket transparanlığı — 0.0-1.0 (1.0 = tam opak)
    const val KEY_TEXT_ALPHA = "text_alpha"
    fun getTextAlpha(context: Context): Float = prefs(context).getFloat(KEY_TEXT_ALPHA, 1.0f)
    fun setTextAlpha(context: Context, v: Float) = prefs(context).edit().putFloat(KEY_TEXT_ALPHA, v).apply()

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

    // Uygulama onerileri — en sik kullanilan 4 uygulama ana ekranda gosterilir
    const val KEY_SUGGESTIONS_ENABLED = "suggestions_enabled"
    fun isSuggestionsEnabled(context: Context) = prefs(context).getBoolean(KEY_SUGGESTIONS_ENABLED, true)
    fun setSuggestionsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SUGGESTIONS_ENABLED, v).apply()

    // Klasor ozel adlari + emoji — JSON map (categoryId -> deger)
    const val KEY_FOLDER_CUSTOM_NAMES  = "folder_custom_names"
    const val KEY_FOLDER_CUSTOM_EMOJIS = "folder_custom_emojis"

    fun getFolderCustomNames(context: Context): Map<String, String> =
        (prefs(context).getString(KEY_FOLDER_CUSTOM_NAMES, null) ?: "").parseJsonMap()

    fun setFolderCustomName(context: Context, catId: String, name: String) {
        val map = getFolderCustomNames(context).toMutableMap()
        if (name.isBlank()) map.remove(catId) else map[catId] = name.trim()
        prefs(context).edit().putString(KEY_FOLDER_CUSTOM_NAMES, map.toJsonString()).apply()
    }

    fun getFolderCustomEmojis(context: Context): Map<String, String> =
        (prefs(context).getString(KEY_FOLDER_CUSTOM_EMOJIS, null) ?: "").parseJsonMap()

    fun setFolderCustomEmoji(context: Context, catId: String, emoji: String) {
        val map = getFolderCustomEmojis(context).toMutableMap()
        if (emoji.isBlank()) map.remove(catId) else map[catId] = emoji
        prefs(context).edit().putString(KEY_FOLDER_CUSTOM_EMOJIS, map.toJsonString()).apply()
    }

    // Klasor ozel renkleri -- JSON map (categoryId -> colorHex "#RRGGBB")
    const val KEY_FOLDER_CUSTOM_COLORS = "folder_custom_colors"

    fun getFolderCustomColors(context: Context): Map<String, String> =
        (prefs(context).getString(KEY_FOLDER_CUSTOM_COLORS, null) ?: "").parseJsonMap()

    fun setFolderCustomColor(context: Context, catId: String, colorHex: String) {
        val map = getFolderCustomColors(context).toMutableMap()
        if (colorHex.isBlank()) map.remove(catId) else map[catId] = colorHex
        prefs(context).edit().putString(KEY_FOLDER_CUSTOM_COLORS, map.toJsonString()).apply()
    }

    private fun String.parseJsonMap(): Map<String, String> {
        if (isBlank()) return emptyMap()
        return runCatching {
            val json = org.json.JSONObject(this)
            json.keys().asSequence().associateWith { json.getString(it) }
        }.getOrDefault(emptyMap())
    }

    private fun Map<String, String>.toJsonString(): String {
        val json = org.json.JSONObject()
        forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

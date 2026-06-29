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
    const val KEY_FOLDER_SWIPE_HINT_ENABLED = KEY_FOLDER_SWIPE_HINT  // alias

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

    // AllApps sıralama modu
    const val KEY_ALL_APPS_SORT_MODE = "all_apps_sort_mode"
    fun getAllAppsSortMode(context: Context): String =
        prefs(context).getString(KEY_ALL_APPS_SORT_MODE, "ALPHA") ?: "ALPHA"
    fun setAllAppsSortMode(context: Context, mode: String) =
        prefs(context).edit().putString(KEY_ALL_APPS_SORT_MODE, mode).apply()

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

    // Klasör şekli — "circle", "rounded", "square", "triangle"
    const val KEY_FOLDER_SHAPE = "folder_shape"
    fun getFolderShape(context: Context): String = prefs(context).getString(KEY_FOLDER_SHAPE, "circle") ?: "circle"
    fun setFolderShape(context: Context, shape: String) = prefs(context).edit().putString(KEY_FOLDER_SHAPE, shape).apply()

    // Klasör boyutu — tile genişliği 56-96dp arası (varsayılan 72dp)
    const val KEY_FOLDER_SIZE = "folder_size_dp"
    fun getFolderSizeDp(context: Context): Int = prefs(context).getInt(KEY_FOLDER_SIZE, 72)
    fun setFolderSizeDp(context: Context, dp: Int) = prefs(context).edit().putInt(KEY_FOLDER_SIZE, dp).apply()

    // İkon boyutu ölçeği — 0.7f (küçük) .. 1.3f (büyük), varsayılan 1.0f
    const val KEY_ICON_SCALE = "icon_scale"
    fun getIconScale(context: Context): Float = prefs(context).getFloat(KEY_ICON_SCALE, 1.0f)
    fun setIconScale(context: Context, scale: Float) = prefs(context).edit().putFloat(KEY_ICON_SCALE, scale).apply()

    // Otomatik klasör boyutu — ekran genişliğine göre klasörü otomatik boyutlandır
    const val KEY_AUTO_FOLDER_SIZE = "auto_folder_size"
    fun isAutoFolderSizeEnabled(context: Context) = prefs(context).getBoolean(KEY_AUTO_FOLDER_SIZE, false)
    fun setAutoFolderSizeEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_FOLDER_SIZE, v).apply()

    // Sayfa başına klasör sayısı — 4/6/8/12 (varsayılan 8 = 4x2)
    const val KEY_PAGE_SIZE = "page_folder_count"
    fun getPageSize(context: Context): Int = prefs(context).getInt(KEY_PAGE_SIZE, 8)
    fun setPageSize(context: Context, v: Int) = prefs(context).edit().putInt(KEY_PAGE_SIZE, v).apply()

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

    // Widget Auto-Resize — ekran yüksekliğine göre widget yüksekliği otomatik ayarla
    const val KEY_WIDGET_AUTO_RESIZE = "widget_auto_resize"
    fun isWidgetAutoResizeEnabled(context: Context) = prefs(context).getBoolean(KEY_WIDGET_AUTO_RESIZE, false)
    fun setWidgetAutoResizeEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_WIDGET_AUTO_RESIZE, v).apply()

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

    // Sistem uygulamalarını göster — AppListScreen filtre
    const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
    fun isShowSystemApps(context: Context) = prefs(context).getBoolean(KEY_SHOW_SYSTEM_APPS, false)
    fun setShowSystemApps(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SHOW_SYSTEM_APPS, v).apply()

    // Ana ekran klasör arama çubuğu
    const val KEY_HOME_SEARCH_ENABLED = "home_search_enabled"
    fun isHomeSearchEnabled(context: Context) = prefs(context).getBoolean(KEY_HOME_SEARCH_ENABLED, true)
    fun setHomeSearchEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HOME_SEARCH_ENABLED, v).apply()

    // Ana ekran uygulama arama çubuğu (Google Search altında, uygulama başlatır)
    const val KEY_HOME_APP_SEARCH_ENABLED = "home_app_search_enabled"
    fun isHomeAppSearchEnabled(context: Context) = prefs(context).getBoolean(KEY_HOME_APP_SEARCH_ENABLED, true)
    fun setHomeAppSearchEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HOME_APP_SEARCH_ENABLED, v).apply()

    // Uygulama önerileri — en sık kullanılan 4 uygulama ana ekranda gösterilir
    const val KEY_SUGGESTIONS_ENABLED = "suggestions_enabled"
    fun isSuggestionsEnabled(context: Context) = prefs(context).getBoolean(KEY_SUGGESTIONS_ENABLED, true)
    fun setSuggestionsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SUGGESTIONS_ENABLED, v).apply()

    // Favori uygulamalar — paket adları Set olarak saklanır
    const val KEY_FAVORITES_ENABLED          = "favorites_enabled"
    const val KEY_FAVORITES_ENABLED_ALLAPPS  = "favorites_enabled_allapps"
    const val KEY_FAVORITES_SET              = "favorites_set"
    fun isFavoritesEnabled(context: Context) = prefs(context).getBoolean(KEY_FAVORITES_ENABLED, true)
    fun setFavoritesEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_FAVORITES_ENABLED, v).apply()
    fun isFavoritesEnabledAllApps(context: Context) = prefs(context).getBoolean(KEY_FAVORITES_ENABLED_ALLAPPS, true)
    fun setFavoritesEnabledAllApps(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_FAVORITES_ENABLED_ALLAPPS, v).apply()
    fun getFavorites(context: Context): Set<String> = prefs(context).getStringSet(KEY_FAVORITES_SET, emptySet()) ?: emptySet()
    fun addFavorite(context: Context, pkg: String) {
        val set = getFavorites(context).toMutableSet().also { it.add(pkg) }
        prefs(context).edit().putStringSet(KEY_FAVORITES_SET, set).apply()
    }
    fun removeFavorite(context: Context, pkg: String) {
        val set = getFavorites(context).toMutableSet().also { it.remove(pkg) }
        prefs(context).edit().putStringSet(KEY_FAVORITES_SET, set).apply()
    }
    fun isFavorite(context: Context, pkg: String) = getFavorites(context).contains(pkg)

    // Son kullanılanlar satırı
    const val KEY_RECENT_APPS_ENABLED         = "recent_apps_enabled"
    const val KEY_RECENT_APPS_ENABLED_ALLAPPS = "recent_apps_enabled_allapps"
    fun isRecentAppsEnabled(context: Context) = prefs(context).getBoolean(KEY_RECENT_APPS_ENABLED, false)
    fun setRecentAppsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_RECENT_APPS_ENABLED, v).apply()
    fun isRecentAppsEnabledAllApps(context: Context) = prefs(context).getBoolean(KEY_RECENT_APPS_ENABLED_ALLAPPS, false)
    fun setRecentAppsEnabledAllApps(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_RECENT_APPS_ENABLED_ALLAPPS, v).apply()

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

    // Otomatik yedekleme zamanlama — WorkManager periyodik gorev
    const val KEY_BACKUP_DAY_OF_WEEK = "backup_day_of_week"   // 1=Pzt, 7=Paz
    const val KEY_BACKUP_HOUR        = "backup_hour"           // 0-23
    const val KEY_BACKUP_MINUTE      = "backup_minute"         // 0-59

    fun getBackupDayOfWeek(context: Context): Int = prefs(context).getInt(KEY_BACKUP_DAY_OF_WEEK, 1)
    fun setBackupDayOfWeek(context: Context, day: Int) = prefs(context).edit().putInt(KEY_BACKUP_DAY_OF_WEEK, day).apply()
    fun getBackupHour(context: Context): Int = prefs(context).getInt(KEY_BACKUP_HOUR, 3)
    fun setBackupHour(context: Context, hour: Int) = prefs(context).edit().putInt(KEY_BACKUP_HOUR, hour).apply()
    fun getBackupMinute(context: Context): Int = prefs(context).getInt(KEY_BACKUP_MINUTE, 0)
    fun setBackupMinute(context: Context, minute: Int) = prefs(context).edit().putInt(KEY_BACKUP_MINUTE, minute).apply()

    // Son yedekleme zamanı (epoch ms, 0 = hiç yedeklenmedi)
    const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    fun getLastBackupTime(context: Context): Long = prefs(context).getLong(KEY_LAST_BACKUP_TIME, 0L)
    fun setLastBackupTime(context: Context, ms: Long) = prefs(context).edit().putLong(KEY_LAST_BACKUP_TIME, ms).apply()

    // Google Drive / SAF klasör URI — null = seçilmemiş
    const val KEY_DRIVE_FOLDER_URI = "drive_folder_uri"
    fun getDriveFolderUri(context: Context): String? = prefs(context).getString(KEY_DRIVE_FOLDER_URI, null)
    fun setDriveFolderUri(context: Context, uri: String?) = prefs(context).edit().putString(KEY_DRIVE_FOLDER_URI, uri).apply()

    // Klasör blur efekti — HyperOS tarzı frosted glass
    const val KEY_FOLDER_BLUR = "folder_blur"
    fun isFolderBlurEnabled(context: Context) = prefs(context).getBoolean(KEY_FOLDER_BLUR, true)
    fun setFolderBlurEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_FOLDER_BLUR, v).apply()

    // DeepSeek API anahtari — LLM fallback kategorize icin
    const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
    fun getDeepSeekApiKey(context: Context): String =
        prefs(context).getString(KEY_DEEPSEEK_API_KEY, "") ?: ""
    fun setDeepSeekApiKey(context: Context, key: String) =
        prefs(context).edit().putString(KEY_DEEPSEEK_API_KEY, key.trim()).apply()

    // FCM token — push bildirim için sunucu kaydı
    const val KEY_FCM_TOKEN = "fcm_token"
    fun getFcmToken(context: Context): String =
        prefs(context).getString(KEY_FCM_TOKEN, "") ?: ""
    fun setFcmToken(context: Context, token: String) =
        prefs(context).edit().putString(KEY_FCM_TOKEN, token).apply()

    // Çift tıkla arama — HomeScreen boş alana çift tıklayınca AllAppsDrawer açılır + search odaklanır
    const val KEY_DOUBLE_TAP_SEARCH = "double_tap_search"
    fun isDoubleTapSearchEnabled(context: Context) = prefs(context).getBoolean(KEY_DOUBLE_TAP_SEARCH, true)
    fun setDoubleTapSearchEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_DOUBLE_TAP_SEARCH, v).apply()

    // Assistant Kartları — ana ekranda kullanım içgörü kartları
    const val KEY_ASSISTANT_CARDS = "assistant_cards_enabled"
    fun isAssistantCardsEnabled(context: Context) = prefs(context).getBoolean(KEY_ASSISTANT_CARDS, true)
    fun setAssistantCardsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_ASSISTANT_CARDS, v).apply()

    // Contextual Dock — 2 sabit + 2 akıllı öneri
    const val KEY_CONTEXTUAL_DOCK = "contextual_dock_enabled"
    fun isContextualDockEnabled(context: Context) = prefs(context).getBoolean(KEY_CONTEXTUAL_DOCK, true)
    fun setContextualDockEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_CONTEXTUAL_DOCK, v).apply()

    // Manuel Kategori Ezmeler — kullanıcı tarafından atanmış paket→kategori haritası
    // AppClassifier bu haritayı exactMatch'ten önce kontrol eder (en yüksek öncelik)
    const val KEY_MANUAL_CAT_OVERRIDES = "manual_category_overrides"

    fun getManualCategoryOverrides(context: Context): Map<String, String> =
        (prefs(context).getString(KEY_MANUAL_CAT_OVERRIDES, null) ?: "").parseJsonMap()

    fun setManualCategoryOverride(context: Context, packageName: String, categoryId: String) {
        val map = getManualCategoryOverrides(context).toMutableMap()
        map[packageName] = categoryId
        prefs(context).edit().putString(KEY_MANUAL_CAT_OVERRIDES, map.toJsonString()).apply()
    }

    fun clearManualCategoryOverride(context: Context, packageName: String) {
        val map = getManualCategoryOverrides(context).toMutableMap()
        map.remove(packageName)
        prefs(context).edit().putString(KEY_MANUAL_CAT_OVERRIDES, map.toJsonString()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

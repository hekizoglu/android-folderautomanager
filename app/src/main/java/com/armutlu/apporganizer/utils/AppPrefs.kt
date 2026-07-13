package com.armutlu.apporganizer.utils

import android.content.Context

object AppPrefs {
    const val PREFS_NAME = "app_organizer_prefs"
    const val KEY_ONBOARDING_DONE = "onboarding_done"
    const val KEY_LAUNCHER_SETUP_SHOWN = "launcher_setup_shown"

    // Onboarding adim kaliciligi (D240): varsayilan launcher secimi sistemin gorevi yeniden
    // baslatmasina yol acabiliyor — rememberSaveable yeni activity kaydinda korunmadigi icin
    // kurulum basa sariyordu. Adim her degisimde buraya yazilir, acilista geri yuklenir.
    const val KEY_ONBOARDING_STEP = "onboarding_step"
    fun getOnboardingStep(context: Context): Int = prefs(context).getInt(KEY_ONBOARDING_STEP, 0)
    fun setOnboardingStep(context: Context, step: Int) = prefs(context).edit().putInt(KEY_ONBOARDING_STEP, step).apply()

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

    // Arama çubuğu izin ipucu (E10) — kullanım/bildirim erişimi eksikse arama çubuğu altında
    // küçük ipucu göster. En fazla SEARCH_PERM_HINT_MAX açılışta aktif "Ver" ipucu görünür;
    // sonra rahatsız etmeyen "İzinler ayarlardan yönetilebilir →" pasif linkine döner.
    const val KEY_SEARCH_PERM_HINT_COUNT = "search_perm_hint_count"
    const val KEY_SEARCH_PERM_HINT_DISMISSED = "search_perm_hint_dismissed"
    const val SEARCH_PERM_HINT_MAX = 3

    fun getSearchPermHintCount(context: Context): Int =
        prefs(context).getInt(KEY_SEARCH_PERM_HINT_COUNT, 0)

    fun incrementSearchPermHintCount(context: Context) {
        val p = prefs(context)
        p.edit().putInt(KEY_SEARCH_PERM_HINT_COUNT, p.getInt(KEY_SEARCH_PERM_HINT_COUNT, 0) + 1).apply()
    }

    fun isSearchPermHintDismissed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SEARCH_PERM_HINT_DISMISSED, false)

    fun setSearchPermHintDismissed(context: Context, v: Boolean) =
        prefs(context).edit().putBoolean(KEY_SEARCH_PERM_HINT_DISMISSED, v).apply()

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
    // Varsayılan yükseltildi (D226): ilk kurulumda arkadaki uygulamalar çok görünüyor,
    // AllApps ekranıyla karışıyordu.
    fun getAllAppsBgAlpha(context: Context) = prefs(context).getFloat(KEY_ALLAPPS_BG_ALPHA, 0.95f)
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

    // Kullanım bilgisi alt yazısı — klasör altında "X gündür açılmadı" / "Hiç açılmadı" göster
    // Bildirim metni varsa o önceliklidir; bu bilgi yalnızca bildirim yokken görünür
    // Varsayılan kapalı (D226) — Home ekranında kalabalık/karışıklık yaratıyordu.
    const val KEY_UNUSED_INFO_ENABLED = "unused_info_enabled"
    fun isUnusedInfoEnabled(context: Context) = prefs(context).getBoolean(KEY_UNUSED_INFO_ENABLED, false)
    fun setUnusedInfoEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_UNUSED_INFO_ENABLED, v).apply()

    // Klasör üzerinde toplam bildirim rozeti (sayı) — Home'daki klasör ikonunda gösterilir.
    // Varsayılan kapalı (D226): klasör içine girmeden rozet kalabalığı olmasın; FolderScreen
    // içindeki uygulama bazlı bildirim rozetleri bundan etkilenmez, her zaman görünür kalır.
    const val KEY_FOLDER_BADGE_ENABLED = "folder_badge_enabled"
    fun isFolderBadgeEnabled(context: Context) = prefs(context).getBoolean(KEY_FOLDER_BADGE_ENABLED, false)
    fun setFolderBadgeEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_FOLDER_BADGE_ENABLED, v).apply()

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

    const val KEY_OVERRIDE_SUGGESTIONS_ENABLED = "override_suggestions_enabled"
    fun isOverrideSuggestionsEnabled(context: Context) = prefs(context).getBoolean(KEY_OVERRIDE_SUGGESTIONS_ENABLED, true)
    fun setOverrideSuggestionsEnabled(context: Context, v: Boolean) =
        prefs(context).edit().putBoolean(KEY_OVERRIDE_SUGGESTIONS_ENABLED, v).apply()

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

    // Son görüntülenen klasör sayfası — process death/geri tuşu sonrası ilk sayfaya sıfırlanmasın (D210)
    const val KEY_LAST_HOME_PAGE = "last_home_page"
    fun getLastHomePage(context: Context): Int = prefs(context).getInt(KEY_LAST_HOME_PAGE, 0)
    fun setLastHomePage(context: Context, page: Int) = prefs(context).edit().putInt(KEY_LAST_HOME_PAGE, page).apply()

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

    // Klasör içi arama çubuğu — varsayılan KAPALI (ekranı sadeleştirme)
    const val KEY_FOLDER_SEARCH_ENABLED = "folder_search_enabled"
    fun isFolderSearchEnabled(context: Context) = prefs(context).getBoolean(KEY_FOLDER_SEARCH_ENABLED, false)
    fun setFolderSearchEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_FOLDER_SEARCH_ENABLED, v).apply()

    // ── Saat ve Dijital Nabız (Pulse Clock, D244) ─────────────────────────
    // Saat stili: minimal (sadece saat+tarih) / pulse (skor+içgörü, varsayılan) / glass (cam yüzey)
    const val KEY_CLOCK_STYLE = "home_clock_style"
    const val CLOCK_STYLE_MINIMAL = "minimal"
    const val CLOCK_STYLE_PULSE = "pulse"
    const val CLOCK_STYLE_GLASS = "glass"
    fun getClockStyle(context: Context): String =
        prefs(context).getString(KEY_CLOCK_STYLE, CLOCK_STYLE_PULSE) ?: CLOCK_STYLE_PULSE
    fun setClockStyle(context: Context, v: String) = prefs(context).edit().putString(KEY_CLOCK_STYLE, v).apply()

    // Ana ekranda Dijital Yaşam Skoru halkası görünürlüğü
    const val KEY_HOME_SCORE_VISIBLE = "home_score_visible"
    fun isHomeScoreVisible(context: Context) = prefs(context).getBoolean(KEY_HOME_SCORE_VISIBLE, true)
    fun setHomeScoreVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HOME_SCORE_VISIBLE, v).apply()

    // Ana ekranda tek satırlık içgörü görünürlüğü
    const val KEY_HOME_INSIGHT_VISIBLE = "home_insight_visible"
    fun isHomeInsightVisible(context: Context) = prefs(context).getBoolean(KEY_HOME_INSIGHT_VISIBLE, true)
    fun setHomeInsightVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HOME_INSIGHT_VISIBLE, v).apply()

    // Son gösterilen içgörü id'si — aynı mesaj her açılışta tekrarlanmasın (dönüşümlü gösterim)
    const val KEY_PULSE_LAST_INSIGHT_ID = "pulse_last_insight_id"
    fun getPulseLastInsightId(context: Context): String? = prefs(context).getString(KEY_PULSE_LAST_INSIGHT_ID, null)
    fun setPulseLastInsightId(context: Context, id: String) = prefs(context).edit().putString(KEY_PULSE_LAST_INSIGHT_ID, id).apply()

    // Haber şeridi (ticker) — klasör/içgörü/bildirim haberleri ana ekranda akar
    const val KEY_TICKER_ENABLED = "home_ticker_enabled"
    fun isTickerEnabled(context: Context) = prefs(context).getBoolean(KEY_TICKER_ENABLED, true)
    fun setTickerEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_TICKER_ENABLED, v).apply()

    // Ticker sessize alma — basili tut menusunden secilen zamana kadar serit gizlenir (D233)
    const val KEY_TICKER_MUTED_UNTIL = "home_ticker_muted_until"
    fun getTickerMutedUntil(context: Context) = prefs(context).getLong(KEY_TICKER_MUTED_UNTIL, 0L)
    fun setTickerMutedUntil(context: Context, untilMillis: Long) = prefs(context).edit().putLong(KEY_TICKER_MUTED_UNTIL, untilMillis).apply()

    // Arama çubuğu elmas parlaması — 10-15 sn'de bir gradient süpürme animasyonu
    const val KEY_SEARCH_SHINE_ENABLED = "search_shine_enabled"
    fun isSearchShineEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_SHINE_ENABLED, true)
    fun setSearchShineEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_SHINE_ENABLED, v).apply()

    // Sonuç bulunamayınca web/Play Store araması öner (fallback satırları)
    const val KEY_SEARCH_WEB_FALLBACK_ENABLED = "search_web_fallback_enabled"
    fun isSearchWebFallbackEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_WEB_FALLBACK_ENABLED, true)
    fun setSearchWebFallbackEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_WEB_FALLBACK_ENABLED, v).apply()

    // Bildirim analizi — notification_events tablosuna kayıt (rapor sayfası için)
    const val KEY_NOTIF_ANALYTICS_ENABLED = "notif_analytics_enabled"
    fun isNotifAnalyticsEnabled(context: Context) = prefs(context).getBoolean(KEY_NOTIF_ANALYTICS_ENABLED, true)
    fun setNotifAnalyticsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_NOTIF_ANALYTICS_ENABLED, v).apply()

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

    fun clearFolderCustomizations(context: Context) {
        prefs(context).edit()
            .remove(KEY_FOLDER_CUSTOM_NAMES)
            .remove(KEY_FOLDER_CUSTOM_EMOJIS)
            .remove(KEY_FOLDER_CUSTOM_COLORS)
            .apply()
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

    // Arama kaynaklari ve profil ayarlari
    const val KEY_SEARCH_SOURCE_APPS = "search_source_apps"
    const val KEY_SEARCH_SOURCE_CATEGORIES = "search_source_categories"
    const val KEY_SEARCH_SOURCE_SETTINGS = "search_source_settings"
    const val KEY_SEARCH_SOURCE_CONTACTS = "search_source_contacts"
    const val KEY_SEARCH_SOURCE_FILES = "search_source_files"
    const val KEY_SEARCH_RANKING_PROFILE = "search_ranking_profile"

    enum class SearchRankingProfile {
        APPS_FIRST,
        BALANCED,
        CATEGORIES_FIRST,
    }

    fun isSearchSourceAppsEnabled(context: Context): Boolean {
        if (!prefs(context).getBoolean(KEY_SEARCH_SOURCE_APPS, true)) {
            prefs(context).edit().putBoolean(KEY_SEARCH_SOURCE_APPS, true).apply()
        }
        return true
    }
    @Suppress("UNUSED_PARAMETER")
    fun setSearchSourceAppsEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_SEARCH_SOURCE_APPS, true).apply()

    fun isSearchSourceCategoriesEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_SOURCE_CATEGORIES, true)
    fun setSearchSourceCategoriesEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_SOURCE_CATEGORIES, v).apply()

    fun isSearchSourceSettingsEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_SOURCE_SETTINGS, true)
    fun setSearchSourceSettingsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_SOURCE_SETTINGS, v).apply()

    fun isSearchSourceContactsEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_SOURCE_CONTACTS, false)
    fun hasSearchSourceContactsPreference(context: Context) = prefs(context).contains(KEY_SEARCH_SOURCE_CONTACTS)
    fun setSearchSourceContactsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_SOURCE_CONTACTS, v).apply()

    fun isSearchSourceFilesEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_SOURCE_FILES, false)
    fun setSearchSourceFilesEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_SOURCE_FILES, v).apply()

    fun getSearchRankingProfile(context: Context): SearchRankingProfile =
        runCatching { SearchRankingProfile.valueOf(prefs(context).getString(KEY_SEARCH_RANKING_PROFILE, null) ?: "") }
            .getOrDefault(SearchRankingProfile.APPS_FIRST)

    fun setSearchRankingProfile(context: Context, profile: SearchRankingProfile) =
        prefs(context).edit().putString(KEY_SEARCH_RANKING_PROFILE, profile.name).apply()

    // ── Gelişmiş Arama Ayarları ───────────────────────────────────────────────
    // Fuzzy arama — "ytb" → YouTube, "wtsp" → WhatsApp
    const val KEY_SEARCH_FUZZY = "search_fuzzy_enabled"
    fun isSearchFuzzyEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_FUZZY, true)
    fun setSearchFuzzyEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_FUZZY, v).apply()

    // Türkçe fonetik toleransı — ş→s, ü→u, ö→o, ç→c, ğ→g, ı→i (ASCII-fold)
    const val KEY_SEARCH_PHONETIC = "search_phonetic_enabled"
    fun isSearchPhoneticEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_PHONETIC, true)
    fun setSearchPhoneticEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_PHONETIC, v).apply()

    // Anlık arama — her tuş vuruşunda sonuç (false = Enter'da)
    const val KEY_SEARCH_INSTANT = "search_instant_enabled"
    fun isSearchInstantEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_INSTANT, true)
    fun setSearchInstantEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_INSTANT, v).apply()

    // Kullanım sıklığına göre önceliklendir
    const val KEY_SEARCH_SORT_BY_USAGE = "search_sort_by_usage"
    fun isSearchSortByUsage(context: Context) = prefs(context).getBoolean(KEY_SEARCH_SORT_BY_USAGE, true)
    fun setSearchSortByUsage(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_SORT_BY_USAGE, v).apply()

    // Maksimum gösterilecek sonuç sayısı (HomeAppSearchBar için)
    const val KEY_SEARCH_MAX_RESULTS = "search_max_results"
    fun getSearchMaxResults(context: Context) = prefs(context).getInt(KEY_SEARCH_MAX_RESULTS, 6)
    fun setSearchMaxResults(context: Context, n: Int) = prefs(context).edit().putInt(KEY_SEARCH_MAX_RESULTS, n).apply()

    // Sonuçlarda app ikonunu göster/gizle
    const val KEY_SEARCH_SHOW_ICONS = "search_show_icons"
    fun isSearchShowIcons(context: Context) = prefs(context).getBoolean(KEY_SEARCH_SHOW_ICONS, true)
    fun setSearchShowIcons(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_SHOW_ICONS, v).apply()

    // Kişi aramasında avatar göster/gizle
    const val KEY_SEARCH_SHOW_CONTACT_AVATAR = "search_show_contact_avatar"
    fun isSearchShowContactAvatar(context: Context) = prefs(context).getBoolean(KEY_SEARCH_SHOW_CONTACT_AVATAR, true)
    fun setSearchShowContactAvatar(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_SHOW_CONTACT_AVATAR, v).apply()

    // Assistant Kartları — ana ekranda kullanım içgörü kartları
    const val KEY_ASSISTANT_CARDS = "assistant_cards_enabled"
    fun isAssistantCardsEnabled(context: Context) = prefs(context).getBoolean(KEY_ASSISTANT_CARDS, true)
    fun setAssistantCardsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_ASSISTANT_CARDS, v).apply()

    // Klasör Rengi Otomatik — ikonlardan dominant renk hesapla ve ata
    const val KEY_AUTO_FOLDER_COLOR = "auto_folder_color_enabled"
    fun isAutoFolderColorEnabled(context: Context) = prefs(context).getBoolean(KEY_AUTO_FOLDER_COLOR, true)
    fun setAutoFolderColorEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_FOLDER_COLOR, v).apply()

    // Weekly Digest — haftalık kullanılmayan uygulama raporu bildirimi
    const val KEY_WEEKLY_DIGEST = "weekly_digest_enabled"
    fun isWeeklyDigestEnabled(context: Context) = prefs(context).getBoolean(KEY_WEEKLY_DIGEST, true)
    fun setWeeklyDigestEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_WEEKLY_DIGEST, v).apply()

    // Haftalık Rapor (Wrapped) — Spotify Wrapped tarzı haftalık özet ekranı; kapalıysa
    // ReportsCenter girişi gizlenir. Tüm veri lokal, sunucuya gönderilmez.
    const val KEY_WRAPPED_ENABLED = "wrapped_report_enabled"
    fun isWrappedEnabled(context: Context) = prefs(context).getBoolean(KEY_WRAPPED_ENABLED, true)
    fun setWrappedEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_WRAPPED_ENABLED, v).apply()

    const val KEY_WRAPPED_AI_COACH_ENABLED = "wrapped_ai_coach_enabled"
    fun isWrappedAiCoachEnabled(context: Context) = prefs(context).getBoolean(KEY_WRAPPED_AI_COACH_ENABLED, false)
    fun setWrappedAiCoachEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_WRAPPED_AI_COACH_ENABLED, v).apply()

    const val KEY_GOALS_ENABLED = "goals_enabled"
    fun isGoalsEnabled(context: Context) = prefs(context).getBoolean(KEY_GOALS_ENABLED, true)
    fun setGoalsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_GOALS_ENABLED, v).apply()

    // Gizlilik Analizi — Rapor Merkezi'nde hangi uygulamanın hassas izinlere (kamera,
    // mikrofon, konum vb.) erişebildiğini gösteren rapor. Tüm analiz cihazda yapılır,
    // hiçbir veri dışarı gönderilmez. Varsayılan açık.
    const val KEY_PRIVACY_REPORT_ENABLED = "privacy_report_enabled"
    fun isPrivacyReportEnabled(context: Context) = prefs(context).getBoolean(KEY_PRIVACY_REPORT_ENABLED, true)
    fun setPrivacyReportEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_PRIVACY_REPORT_ENABLED, v).apply()

    // Biometric Settings Lock — Ayarlar ekranını parmak izi/yüz kilidi arkasına al
    const val KEY_BIOMETRIC_SETTINGS_LOCK = "biometric_settings_lock"
    fun isBiometricSettingsLockEnabled(context: Context) = prefs(context).getBoolean(KEY_BIOMETRIC_SETTINGS_LOCK, false)
    fun setBiometricSettingsLockEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_BIOMETRIC_SETTINGS_LOCK, v).apply()

    // Badge Intelligence — bildirim badge rengi kategori bazlı (yeşil/sarı/kırmızı)
    const val KEY_BADGE_INTELLIGENCE = "badge_intelligence_enabled"
    fun isBadgeIntelligenceEnabled(context: Context) = prefs(context).getBoolean(KEY_BADGE_INTELLIGENCE, true)
    fun setBadgeIntelligenceEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_BADGE_INTELLIGENCE, v).apply()

    // Focus Mode / Minimal Mod — sadece dock + favoriler, klasör grid ve drawer gizlenir
    const val KEY_FOCUS_MODE = "focus_mode_enabled"
    fun isFocusModeEnabled(context: Context) = prefs(context).getBoolean(KEY_FOCUS_MODE, false)
    fun setFocusModeEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_FOCUS_MODE, v).apply()

    // Quick Wheel / Pie Mode — uzun bas ile radyal uygulama çarkı (varsayılan kapalı)
    const val KEY_QUICK_WHEEL = "quick_wheel_enabled"
    fun isQuickWheelEnabled(context: Context) = prefs(context).getBoolean(KEY_QUICK_WHEEL, false)
    fun setQuickWheelEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_QUICK_WHEEL, v).apply()

    // Contextual Dock — 2 sabit + 2 akıllı öneri
    const val KEY_CONTEXTUAL_DOCK = "contextual_dock_enabled"
    fun isContextualDockEnabled(context: Context) = prefs(context).getBoolean(KEY_CONTEXTUAL_DOCK, true)
    fun setContextualDockEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_CONTEXTUAL_DOCK, v).apply()

    // Gesture Aksiyon Engine — her jest için özelleştirilebilir aksiyon
    enum class GestureAction(val label: String) {
        OPEN_DRAWER("Uygulama Çekmecesi"),
        OPEN_SEARCH("Arama ile Çekmece"),
        OPEN_APP_MANAGER("App Organizer"),
        LAUNCH_CAMERA("Kamera"),
        DO_NOTHING("Hiçbir Şey Yapma")
    }

    const val KEY_GESTURE_DOUBLE_TAP   = "gesture_double_tap"
    const val KEY_GESTURE_LONG_PRESS   = "gesture_long_press_home"
    const val KEY_GESTURE_SWIPE_UP     = "gesture_swipe_up"

    fun getGestureDoubleTap(context: Context): GestureAction =
        runCatching { GestureAction.valueOf(prefs(context).getString(KEY_GESTURE_DOUBLE_TAP, null) ?: "") }
            .getOrDefault(GestureAction.OPEN_SEARCH)

    fun setGestureDoubleTap(context: Context, action: GestureAction) =
        prefs(context).edit().putString(KEY_GESTURE_DOUBLE_TAP, action.name).apply()

    fun getGestureLongPress(context: Context): GestureAction =
        runCatching { GestureAction.valueOf(prefs(context).getString(KEY_GESTURE_LONG_PRESS, null) ?: "") }
            .getOrDefault(GestureAction.OPEN_APP_MANAGER)

    fun setGestureLongPress(context: Context, action: GestureAction) =
        prefs(context).edit().putString(KEY_GESTURE_LONG_PRESS, action.name).apply()

    fun getGestureSwipeUp(context: Context): GestureAction =
        runCatching { GestureAction.valueOf(prefs(context).getString(KEY_GESTURE_SWIPE_UP, null) ?: "") }
            .getOrDefault(GestureAction.OPEN_DRAWER)

    fun setGestureSwipeUp(context: Context, action: GestureAction) =
        prefs(context).edit().putString(KEY_GESTURE_SWIPE_UP, action.name).apply()

    // Manuel Kategori Ezmeler — kullanıcı tarafından atanmış paket→kategori haritası
    // AppClassifier bu haritayı exactMatch'ten önce kontrol eder (en yüksek öncelik)
    const val KEY_MANUAL_CAT_OVERRIDES = "manual_category_overrides"
    const val KEY_ACCEPTED_OVERRIDE_PATTERNS = "accepted_override_patterns"

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

    fun clearManualCategoryOverrides(context: Context) =
        prefs(context).edit().remove(KEY_MANUAL_CAT_OVERRIDES).apply()

    fun getAcceptedOverridePatterns(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_ACCEPTED_OVERRIDE_PATTERNS, emptySet()) ?: emptySet()

    fun addAcceptedOverridePattern(context: Context, categoryId: String, packages: List<String>) {
        if (packages.isEmpty()) return
        val next = getAcceptedOverridePatterns(context).toMutableSet()
        next += "$categoryId:${packages.sorted().joinToString(",")}"
        prefs(context).edit().putStringSet(KEY_ACCEPTED_OVERRIDE_PATTERNS, next).apply()
    }

    // DeepSeek LLM kategorileme sonucu kalıcı cache — kurulum sonrası her acilista ayni
    // bilinmeyen paketler icin tekrar API cagrisi yapilmasin (K1, Dongu 227, Fable danismanligi).
    const val KEY_LLM_CATEGORY_CACHE = "llm_category_cache"

    fun getLlmCategoryCache(context: Context): Map<String, String> =
        (prefs(context).getString(KEY_LLM_CATEGORY_CACHE, null) ?: "").parseJsonMap()

    fun putLlmCategoryCache(context: Context, packageName: String, categoryId: String) {
        val map = getLlmCategoryCache(context).toMutableMap()
        map[packageName] = categoryId
        prefs(context).edit().putString(KEY_LLM_CATEGORY_CACHE, map.toJsonString()).apply()
    }

    fun putLlmCategoryCacheAll(context: Context, entries: Map<String, String>) {
        if (entries.isEmpty()) return
        val map = getLlmCategoryCache(context).toMutableMap()
        map.putAll(entries)
        prefs(context).edit().putString(KEY_LLM_CATEGORY_CACHE, map.toJsonString()).apply()
    }

    // Search bar konumu — TOP veya BOTTOM snap noktası
    const val KEY_SEARCH_BAR_POSITION = "search_bar_position"
    const val SEARCH_BAR_POS_TOP = "TOP"
    const val SEARCH_BAR_POS_BOTTOM = "BOTTOM"

    fun getSearchBarPosition(context: Context): String =
        prefs(context).getString(KEY_SEARCH_BAR_POSITION, SEARCH_BAR_POS_BOTTOM) ?: SEARCH_BAR_POS_BOTTOM

    fun setSearchBarPosition(context: Context, position: String) =
        prefs(context).edit().putString(KEY_SEARCH_BAR_POSITION, position).apply()

    // Akıllı Bildirimler (SmartInsight) — ana toggle + alt seçenekler
    const val KEY_SMART_NOTIF_ENABLED       = "smart_notif_enabled"
    const val KEY_SMART_NOTIF_DAILY_USAGE   = "smart_notif_daily_usage"
    const val KEY_SMART_NOTIF_UNUSED_APPS   = "smart_notif_unused_apps"
    const val KEY_SMART_NOTIF_CAT_STATS     = "smart_notif_cat_stats"
    const val KEY_SMART_NOTIF_HOUR          = "smart_notif_hour"  // 0-23

    fun isSmartNotifEnabled(context: Context)     = prefs(context).getBoolean(KEY_SMART_NOTIF_ENABLED, true)
    fun setSmartNotifEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_NOTIF_ENABLED, v).apply()

    fun isSmartNotifDailyUsage(context: Context)  = prefs(context).getBoolean(KEY_SMART_NOTIF_DAILY_USAGE, true)
    fun setSmartNotifDailyUsage(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_NOTIF_DAILY_USAGE, v).apply()

    fun isSmartNotifUnusedApps(context: Context)  = prefs(context).getBoolean(KEY_SMART_NOTIF_UNUSED_APPS, true)
    fun setSmartNotifUnusedApps(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_NOTIF_UNUSED_APPS, v).apply()

    fun isSmartNotifCatStats(context: Context)    = prefs(context).getBoolean(KEY_SMART_NOTIF_CAT_STATS, true)
    fun setSmartNotifCatStats(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_NOTIF_CAT_STATS, v).apply()

    fun getSmartNotifHour(context: Context): Int  = prefs(context).getInt(KEY_SMART_NOTIF_HOUR, 20)
    fun setSmartNotifHour(context: Context, h: Int) = prefs(context).edit().putInt(KEY_SMART_NOTIF_HOUR, h).apply()

    // Arama istatistikleri - anonim sayaclar (SearchStatsPrefs). Kapatilinca loglama durur.
    const val KEY_SEARCH_STATS_ENABLED = "search_stats_enabled"
    fun isSearchStatsEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_STATS_ENABLED, true)
    fun setSearchStatsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_STATS_ENABLED, v).apply()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

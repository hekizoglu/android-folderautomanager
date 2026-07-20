package com.armutlu.apporganizer.utils

import android.content.Context

object AppPrefs {
    const val PREFS_NAME = "app_organizer_prefs"
    const val KEY_ONBOARDING_DONE = "onboarding_done"
    const val KEY_LAUNCHER_SETUP_SHOWN = "launcher_setup_shown"

    // P24/P25 tamamlandı: pager v2 varsayılan AÇIK; safe-mode sorun çıkarsa eski davranışa döndürür.
    const val KEY_HOME_PAGER_V2_ENABLED = "home_pager_v2_enabled"
    const val KEY_HOME_PAGER_V2_SAFE_MODE = "home_pager_v2_safe_mode"

    fun isHomePagerV2Enabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HOME_PAGER_V2_ENABLED, true)

    fun setHomePagerV2Enabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_HOME_PAGER_V2_ENABLED, enabled).apply()

    fun isHomePagerV2SafeMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HOME_PAGER_V2_SAFE_MODE, false)

    fun setHomePagerV2SafeMode(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_HOME_PAGER_V2_SAFE_MODE, enabled).apply()

    const val KEY_TELEMETRY_CONSENT_DECIDED = "telemetry_consent_decided"
    const val KEY_TELEMETRY_ENABLED = "telemetry_enabled"
    const val KEY_TELEMETRY_CONSENT_VERSION = "telemetry_consent_version"
    const val KEY_TELEMETRY_LAST_CHANGED_AT = "telemetry_last_changed_at"

    fun isTelemetryConsentDecided(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TELEMETRY_CONSENT_DECIDED, false)

    fun isTelemetryEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TELEMETRY_ENABLED, false)

    fun getTelemetryConsentVersion(context: Context): Int =
        prefs(context).getInt(KEY_TELEMETRY_CONSENT_VERSION, 0)

    fun getTelemetryLastChangedAt(context: Context): Long =
        prefs(context).getLong(KEY_TELEMETRY_LAST_CHANGED_AT, 0L)

    fun setTelemetryConsent(context: Context, enabled: Boolean, version: Int, changedAt: Long) {
        prefs(context).edit()
            .putBoolean(KEY_TELEMETRY_CONSENT_DECIDED, true)
            .putBoolean(KEY_TELEMETRY_ENABLED, enabled)
            .putInt(KEY_TELEMETRY_CONSENT_VERSION, version)
            .putLong(KEY_TELEMETRY_LAST_CHANGED_AT, changedAt)
            .apply()
    }

    // Silinip yeniden kurulunca onboarding'in tekrar baslamasi icin cihaza-ozel isaretci
    // (F2 / Hüseyin geribildirimi madde 2). Android Auto Backup butun AppPrefs SharedPreferences
    // dosyasini (tema, ayarlar dahil) yedekleyip yeni kurulumda geri yukluyor; bu yuzden
    // KEY_ONBOARDING_DONE tek basina guvenilir degil — restore edilen prefs'te true gelebilir.
    // Cozum: tum AppPrefs dosyasini yedekten haric tutmak yerine (tema/ayarlar sifirlanir),
    // sadece bu marker dosyasini backup/device-transfer kurallarindan haric tutuyoruz
    // (backup_rules.xml + data_extraction_rules.xml, domain="file" path="install_marker").
    // Marker dosyasi yoksa kurulum "taze" kabul edilir ve onboarding_done flag'i restore
    // olmus olsa bile onboarding tekrar gosterilir.
    private const val INSTALL_MARKER_FILE = "install_marker"

    /** Onboarding daha once TAMAMLANDI mi (hem prefs flag hem cihaza-ozel marker gecerli olmali). */
    fun isOnboardingDone(context: Context): Boolean {
        val flagDone = prefs(context).getBoolean(KEY_ONBOARDING_DONE, false)
        return flagDone && hasInstallMarker(context)
    }

    /** Onboarding tamamlaninca hem flag'i hem cihaza-ozel marker'i yazar. */
    fun markOnboardingDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        writeInstallMarker(context)
    }

    private fun hasInstallMarker(context: Context): Boolean =
        java.io.File(context.filesDir, INSTALL_MARKER_FILE).exists()

    private fun writeInstallMarker(context: Context) {
        runCatching {
            java.io.File(context.filesDir, INSTALL_MARKER_FILE).writeText(
                System.currentTimeMillis().toString()
            )
        }
    }

    /** Ayarlar > "Kurulum Sihirbazı"nı sıfırla — hem flag hem marker temizlenir. */
    fun resetOnboarding(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, false).apply()
        runCatching { java.io.File(context.filesDir, INSTALL_MARKER_FILE).delete() }
    }

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
    // Varsayılan yükseltildi (D226, sonra D245): ilk kurulumda arkadaki uygulamalar çok
    // görünüyordu, AllApps ekranıyla karışıyordu. Kullanıcı talebiyle neredeyse tam opak
    // yapıldı — Settings'ten hâlâ şeffaflaştırılabilir.
    fun getAllAppsBgAlpha(context: Context) = prefs(context).getFloat(KEY_ALLAPPS_BG_ALPHA, 0.98f)
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
    const val KEY_NOTIFICATION_PREVIEW_BLOCKED_PACKAGES = "notification_preview_blocked_packages"
    fun getNotificationPreviewBlockedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_NOTIFICATION_PREVIEW_BLOCKED_PACKAGES, emptySet())
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    fun setNotificationPreviewBlockedPackages(context: Context, packages: Set<String>) =
        prefs(context).edit()
            .putStringSet(KEY_NOTIFICATION_PREVIEW_BLOCKED_PACKAGES, packages.map { it.trim() }.filter { it.isNotBlank() }.toSet())
            .apply()

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
    // P0.6 (ROADMAP_AI_AUDIT): Bu toggle KEY_CLASSIFICATION_MODE ile birlikte artik dogrudan
    // motor secimi icin kullanilmiyor — sadece migration kaynagi olarak okunuyor. Eski cagri
    // noktalari (varsa) hala calisir ama yeni kod getClassificationMode() kullanmali.
    const val KEY_MANUFACTURER_CLASSIFY = "manufacturer_classify"
    fun isManufacturerClassifyEnabled(context: Context) = prefs(context).getBoolean(KEY_MANUFACTURER_CLASSIFY, true)
    fun setManufacturerClassifyEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_MANUFACTURER_CLASSIFY, v).apply()

    // ── Siniflandirma Modu (P0.6, ROADMAP_AI_AUDIT) ─────────────────────────
    // Onceden paralel toggle'lar (manufacturer_classify + DeepSeek API key varligi) birbirini
    // ezebiliyordu; Ayarlar'da "kapali" gorunen motor arka planda hala calisabiliyordu.
    // Tek karar noktasi: KEY_CLASSIFICATION_MODE. Kayit yoksa eski toggle kombinasyonundan
    // MIGRATE edilir (mevcut kullanici davranisi degismesin) ve sonuc kalici yazilir.
    enum class ClassificationMode {
        /** Sadece yerel kurallar (bundled katalog + Android kategori + keyword). Uretici/LLM atlanir. */
        LOCAL_ONLY,
        /** Yerel kurallar + uretici prefix/ad kurali. LLM atlanir. */
        LOCAL_WITH_MANUFACTURER,
        /** Yerel kurallar + uretici kurali + bilinmeyenler icin DeepSeek LLM fallback. */
        LOCAL_WITH_LLM_FALLBACK,
        /** Otomatik siniflandirma yapilmaz — yeni uygulamalar REVIEW_PENDING olarak isaretlenir. */
        MANUAL_REVIEW_ONLY,
    }

    const val KEY_CLASSIFICATION_MODE = "classification_mode"

    /**
     * Mevcut siniflandirma modunu dondurur. Kayit yoksa eski toggle'lardan (manufacturer_classify
     * + LLM daha once hic kullanilmis mi -> legacy cache/API key varligi) hesaplanir ve YAZILIR,
     * boylece migration bir kez calisir ve davranis kararli kalir.
     */
    fun getClassificationMode(context: Context): ClassificationMode {
        val stored = prefs(context).getString(KEY_CLASSIFICATION_MODE, null)
        val parsed = stored?.let { s -> runCatching { ClassificationMode.valueOf(s) }.getOrNull() }
        if (parsed != null) return parsed

        val migrated = migrateClassificationModeFromLegacyToggles(context)
        setClassificationMode(context, migrated)
        return migrated
    }

    fun setClassificationMode(context: Context, mode: ClassificationMode) =
        prefs(context).edit().putString(KEY_CLASSIFICATION_MODE, mode.name).apply()

    /**
     * Eski davranistan mod turetir (migration kurali, P0.6):
     * - LLM daha once kullanilmissa (API key kayitli VEYA LLM cache doluysa) -> LOCAL_WITH_LLM_FALLBACK
     * - Aksi halde uretici siniflandirmasi aciksa -> LOCAL_WITH_MANUFACTURER
     * - Ikisi de kapaliysa -> LOCAL_ONLY
     * Saf/test edilebilir cekirdek mantik parametreli overload'da (bkz. deriveClassificationMode).
     */
    private fun migrateClassificationModeFromLegacyToggles(context: Context): ClassificationMode {
        val manufacturerEnabled = isManufacturerClassifyEnabled(context)
        val llmEverUsed = getDeepSeekApiKey(context).isNotBlank() || getLlmCategoryCache(context).isNotEmpty()
        return deriveClassificationMode(manufacturerEnabled = manufacturerEnabled, llmEverUsed = llmEverUsed)
    }

    /** Saf fonksiyon — Context bagimliligi yok, unit test edilebilir. */
    fun deriveClassificationMode(manufacturerEnabled: Boolean, llmEverUsed: Boolean): ClassificationMode = when {
        llmEverUsed -> ClassificationMode.LOCAL_WITH_LLM_FALLBACK
        manufacturerEnabled -> ClassificationMode.LOCAL_WITH_MANUFACTURER
        else -> ClassificationMode.LOCAL_ONLY
    }

    const val KEY_OVERRIDE_SUGGESTIONS_ENABLED = "override_suggestions_enabled"
    fun isOverrideSuggestionsEnabled(context: Context) = prefs(context).getBoolean(KEY_OVERRIDE_SUGGESTIONS_ENABLED, true)
    fun setOverrideSuggestionsEnabled(context: Context, v: Boolean) =
        prefs(context).edit().putBoolean(KEY_OVERRIDE_SUGGESTIONS_ENABLED, v).apply()

    // Klasör şekli — "circle", "rounded", "square", "triangle"
    const val KEY_FOLDER_SUGGESTIONS_ENABLED = "folder_suggestions_enabled"
    const val KEY_FOLDER_SUGGESTIONS_INFO_DISMISSED = "folder_suggestions_info_dismissed"
    internal fun resolveFolderSuggestionsEnabled(hasStoredValue: Boolean, storedValue: Boolean): Boolean =
        if (hasStoredValue) storedValue else true
    fun isFolderSuggestionsEnabled(context: Context): Boolean {
        val sharedPrefs = prefs(context)
        return resolveFolderSuggestionsEnabled(
            hasStoredValue = sharedPrefs.contains(KEY_FOLDER_SUGGESTIONS_ENABLED),
            storedValue = sharedPrefs.getBoolean(KEY_FOLDER_SUGGESTIONS_ENABLED, true)
        )
    }
    fun setFolderSuggestionsEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_FOLDER_SUGGESTIONS_ENABLED, enabled).apply()
    fun isFolderSuggestionsInfoDismissed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FOLDER_SUGGESTIONS_INFO_DISMISSED, false)
    fun setFolderSuggestionsInfoDismissed(context: Context, dismissed: Boolean) =
        prefs(context).edit().putBoolean(KEY_FOLDER_SUGGESTIONS_INFO_DISMISSED, dismissed).apply()

    const val DEFAULT_FOLDER_SHAPE = "rounded"
    const val KEY_FOLDER_SHAPE = "folder_shape"
    internal fun resolveFolderShapePreference(hasStoredValue: Boolean, storedShape: String?): String =
        if (hasStoredValue) storedShape ?: DEFAULT_FOLDER_SHAPE else DEFAULT_FOLDER_SHAPE
    fun getFolderShape(context: Context): String {
        val sharedPrefs = prefs(context)
        return resolveFolderShapePreference(
            hasStoredValue = sharedPrefs.contains(KEY_FOLDER_SHAPE),
            storedShape = sharedPrefs.getString(KEY_FOLDER_SHAPE, DEFAULT_FOLDER_SHAPE)
        )
    }
    fun setFolderShape(context: Context, shape: String) = prefs(context).edit().putString(KEY_FOLDER_SHAPE, shape).apply()

    // Klasör boyutu — tile genişliği 56-96dp arası (varsayılan 96dp = en büyük, Hüseyin kararı D257)
    const val KEY_FOLDER_SIZE = "folder_size_dp"
    fun getFolderSizeDp(context: Context): Int = prefs(context).getInt(KEY_FOLDER_SIZE, 96)
    fun setFolderSizeDp(context: Context, dp: Int) = prefs(context).edit().putInt(KEY_FOLDER_SIZE, dp).apply()

    // Öneriler satırı ikon boyutu — 32-52dp arası (varsayılan 40dp, önceki hardcoded 48dp'den
    // küçültüldü — bölüm çok yer kaplıyordu). Ayarlardan değiştirilebilir.
    const val KEY_SUGGESTIONS_ICON_SIZE = "suggestions_icon_size_dp"
    fun getSuggestionsIconSizeDp(context: Context): Int = prefs(context).getInt(KEY_SUGGESTIONS_ICON_SIZE, 40)
    fun setSuggestionsIconSizeDp(context: Context, dp: Int) = prefs(context).edit().putInt(KEY_SUGGESTIONS_ICON_SIZE, dp).apply()

    // İkon boyutu ölçeği — 0.7f (küçük) .. 1.3f (büyük), varsayılan 1.0f
    const val DEFAULT_ICON_SCALE = 1.3f
    const val KEY_ICON_SCALE = "icon_scale"
    internal fun resolveIconScalePreference(hasStoredValue: Boolean, storedScale: Float): Float =
        if (hasStoredValue) storedScale else DEFAULT_ICON_SCALE
    fun getIconScale(context: Context): Float {
        val sharedPrefs = prefs(context)
        val storedScale = sharedPrefs.getFloat(KEY_ICON_SCALE, DEFAULT_ICON_SCALE)
        return resolveIconScalePreference(
            hasStoredValue = sharedPrefs.contains(KEY_ICON_SCALE),
            storedScale = storedScale
        )
    }
    fun setIconScale(context: Context, scale: Float) = prefs(context).edit().putFloat(KEY_ICON_SCALE, scale).apply()

    // Otomatik klasör boyutu — ekran genişliğine göre klasörü otomatik boyutlandır
    const val KEY_AUTO_FOLDER_SIZE = "auto_folder_size"
    fun isAutoFolderSizeEnabled(context: Context) = prefs(context).getBoolean(KEY_AUTO_FOLDER_SIZE, false)
    fun setAutoFolderSizeEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_FOLDER_SIZE, v).apply()

    // Son görüntülenen klasör sayfası — process death/geri tuşu sonrası ilk sayfaya sıfırlanmasın (D210)
    // DEPRECATED (P02, ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md): ham Int index
    // yerine semantik HomePageAnchor kullanılmalı — bkz. HomePagePrefs.getLastHomePageAnchor().
    // Bu iki fonksiyon SADECE (a) migration kaynağı ve (b) P00 regresyon testinin (AppPrefsLastHomePageTest)
    // kilitlediği eski davranış için köprü olarak kalıyor; SİLİNMEDİ. Yeni kod HomePagePrefs kullanmalı.
    const val KEY_LAST_HOME_PAGE = "last_home_page"
    fun getLastHomePage(context: Context): Int = prefs(context).getInt(KEY_LAST_HOME_PAGE, 0)
    fun setLastHomePage(context: Context, page: Int) = prefs(context).edit().putInt(KEY_LAST_HOME_PAGE, page).apply()

    // Sayfa başına klasör sayısı — 4/6/8/12 (varsayılan 8 = 4x2)
    const val KEY_PAGE_SIZE = "page_folder_count"
    fun getPageSize(context: Context): Int = prefs(context).getInt(KEY_PAGE_SIZE, 8)
    fun setPageSize(context: Context, v: Int) = prefs(context).edit().putInt(KEY_PAGE_SIZE, v).apply()

    // Klasor sayfasi duzen onerileri: kucuk klasorleri birlestirme ve tanim bekleyen
    // uygulamalari hatirlatma. Kullanici kapatabilir ya da gecici sessize alabilir.
    const val KEY_FOLDER_PAGE_INSIGHTS_ENABLED = "folder_page_insights_enabled"
    const val KEY_FOLDER_PAGE_INSIGHTS_MUTED_UNTIL = "folder_page_insights_muted_until"
    fun isFolderPageInsightsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FOLDER_PAGE_INSIGHTS_ENABLED, true)
    fun setFolderPageInsightsEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_FOLDER_PAGE_INSIGHTS_ENABLED, enabled).apply()
    fun getFolderPageInsightsMutedUntil(context: Context): Long =
        prefs(context).getLong(KEY_FOLDER_PAGE_INSIGHTS_MUTED_UNTIL, 0L)
    fun muteFolderPageInsights(context: Context, until: Long) =
        prefs(context).edit().putLong(KEY_FOLDER_PAGE_INSIGHTS_MUTED_UNTIL, until).apply()

    // Klasör sıralama modu — tüm klasörler için global
    const val KEY_FOLDER_SORT_MODE = "folder_sort_mode"
    fun getFolderSortMode(context: Context): String =
        prefs(context).getString(KEY_FOLDER_SORT_MODE, "ALPHA") ?: "ALPHA"
    fun setFolderSortMode(context: Context, mode: String) =
        prefs(context).edit().putString(KEY_FOLDER_SORT_MODE, mode).apply()

    const val KEY_FOLDER_CAROUSEL_ENABLED = "folder_carousel_enabled"
    fun isFolderCarouselEnabled(context: Context) =
        prefs(context).getBoolean(KEY_FOLDER_CAROUSEL_ENABLED, true)
    fun setFolderCarouselEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_FOLDER_CAROUSEL_ENABLED, enabled).apply()
    const val KEY_FOLDER_CAROUSEL_POSITION = "folder_carousel_position"
    const val FOLDER_CAROUSEL_POS_TOP = "TOP"
    const val FOLDER_CAROUSEL_POS_MIDDLE = "MIDDLE"
    const val FOLDER_CAROUSEL_POS_BOTTOM = "BOTTOM"
    fun getFolderCarouselPosition(context: Context): String =
        prefs(context).getString(KEY_FOLDER_CAROUSEL_POSITION, FOLDER_CAROUSEL_POS_BOTTOM)
            ?: FOLDER_CAROUSEL_POS_BOTTOM
    fun setFolderCarouselPosition(context: Context, position: String) =
        prefs(context).edit().putString(KEY_FOLDER_CAROUSEL_POSITION, position).apply()

    // Klasör geçiş efekti — sayfa çevirme (varsayılan, D253) / kaydırma-parallax / yakınlaş-sol
    const val KEY_FOLDER_TRANSITION_EFFECT = "folder_transition_effect"
    const val FOLDER_TRANSITION_ANDROID_SMOOTH = "android_smooth"
    const val FOLDER_TRANSITION_IOS_ZOOM_FADE = "ios_zoom_fade"
    const val FOLDER_TRANSITION_PAGE_TURN = FOLDER_TRANSITION_ANDROID_SMOOTH
    const val FOLDER_TRANSITION_SLIDE_PARALLAX = FOLDER_TRANSITION_ANDROID_SMOOTH
    const val FOLDER_TRANSITION_ZOOM_FADE = FOLDER_TRANSITION_IOS_ZOOM_FADE
    internal fun resolveFolderTransitionEffectPreference(storedEffect: String?): String =
        when (storedEffect) {
            FOLDER_TRANSITION_IOS_ZOOM_FADE, "zoom_fade" -> FOLDER_TRANSITION_IOS_ZOOM_FADE
            else -> FOLDER_TRANSITION_ANDROID_SMOOTH
        }
    fun getFolderTransitionEffect(context: Context): String =
        resolveFolderTransitionEffectPreference(
            prefs(context).getString(KEY_FOLDER_TRANSITION_EFFECT, FOLDER_TRANSITION_ANDROID_SMOOTH)
        )
    fun setFolderTransitionEffect(context: Context, effect: String) =
        prefs(context).edit().putString(
            KEY_FOLDER_TRANSITION_EFFECT,
            resolveFolderTransitionEffectPreference(effect)
        ).apply()

    // Widget alanı — ana ekranda widget göster
    const val KEY_WIDGET_AREA_ENABLED = "widget_area_enabled"
    fun isWidgetAreaEnabled(context: Context) = prefs(context).getBoolean(KEY_WIDGET_AREA_ENABLED, true)
    fun setWidgetAreaEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_WIDGET_AREA_ENABLED, v).apply()

    // Widget Auto-Resize — ekran yüksekliğine göre widget yüksekliği otomatik ayarla
    const val KEY_WIDGET_AUTO_RESIZE = "widget_auto_resize"
    fun isWidgetAutoResizeEnabled(context: Context) = prefs(context).getBoolean(KEY_WIDGET_AUTO_RESIZE, true)
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

    // Katalog reconcile throttle — olay bazli sync'e ek dusuk frekansli guvenlik agi
    private const val KEY_LAST_RECONCILE = "last_reconcile_ms"
    private const val KEY_APP_CATALOG_SCHEMA_VERSION = "app_catalog_schema_version"
    private const val APP_CATALOG_SCHEMA_VERSION = 1
    private const val RECONCILE_INTERVAL_MS = 12L * 60L * 60L * 1000L // 12 saat

    fun shouldReconcile(context: Context): Boolean {
        val last = prefs(context).getLong(KEY_LAST_RECONCILE, 0L)
        return System.currentTimeMillis() - last > RECONCILE_INTERVAL_MS
    }

    fun markReconciled(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_RECONCILE, System.currentTimeMillis()).apply()
    }

    fun getLastReconcileTime(context: Context): Long =
        prefs(context).getLong(KEY_LAST_RECONCILE, 0L)

    fun isAppCatalogSchemaCurrent(context: Context): Boolean =
        prefs(context).getInt(KEY_APP_CATALOG_SCHEMA_VERSION, 0) == APP_CATALOG_SCHEMA_VERSION

    fun markAppCatalogSchemaCurrent(context: Context) {
        prefs(context).edit().putInt(KEY_APP_CATALOG_SCHEMA_VERSION, APP_CATALOG_SCHEMA_VERSION).apply()
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

    fun getLastUsageSyncTime(context: Context): Long =
        prefs(context).getLong(KEY_LAST_USAGE_SYNC, 0L)

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

    // P1.1 - Ana ekrandaki arama cubugu dokununca tam ekran arama overlay'i acilsin.
    const val KEY_FULLSCREEN_SEARCH_ENABLED = "fullscreen_search_enabled"
    fun isFullscreenSearchEnabled(context: Context) = prefs(context).getBoolean(KEY_FULLSCREEN_SEARCH_ENABLED, true)
    fun setFullscreenSearchEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_FULLSCREEN_SEARCH_ENABLED, v).apply()

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

    // Ana ekranda Dijital Yaşam Skoru halkası görünürlüğü — D03 itibariyle PulseClockWidget'taki
    // PulseScoreRing artık çağrılmıyor (deprecated), bu yüzden bu ayar davranışsal olarak
    // pasiftir. Geriye dönük okuma/migration kaynağı olarak SİLİNMEDEN bırakıldı.
    const val KEY_HOME_SCORE_VISIBLE = "home_score_visible"
    fun isHomeScoreVisible(context: Context) = prefs(context).getBoolean(KEY_HOME_SCORE_VISIBLE, true)
    fun setHomeScoreVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HOME_SCORE_VISIBLE, v).apply()

    // ── Dijital Yaşam Kartı görünürlüğü (D03) ─────────────────────────────
    // Skor artık TEK yerde: DigitalLifeCard (HomeScreen). PulseClockWidget içindeki
    // PulseScoreRing kaldırıldı (deprecated). Migration: eski KEY_HOME_SCORE_VISIBLE bir
    // kez okunur — eski skor görünürse yeni kart görünür başlar; pulse ring zaten kod
    // seviyesinde her durumda kapalı. Migration bayrağı ile tek sefer çalışır.
    const val KEY_DIGITAL_LIFE_CARD_VISIBLE = "digital_life_card_visible"
    private const val KEY_DIGITAL_LIFE_CARD_MIGRATION_DONE = "digital_life_card_migration_done"

    fun isDigitalLifeCardVisible(context: Context): Boolean {
        migrateDigitalLifeCardVisibilityIfNeeded(context)
        return prefs(context).getBoolean(KEY_DIGITAL_LIFE_CARD_VISIBLE, true)
    }

    fun setDigitalLifeCardVisible(context: Context, v: Boolean) {
        // Kullanıcı elle değiştirdiyse migration'ın üzerine yazmasını da engellemiş oluyoruz —
        // migration zaten tek seferlik bayrakla korunuyor, burada sadece garanti altına alıyoruz.
        prefs(context).edit()
            .putBoolean(KEY_DIGITAL_LIFE_CARD_VISIBLE, v)
            .putBoolean(KEY_DIGITAL_LIFE_CARD_MIGRATION_DONE, true)
            .apply()
    }

    /**
     * D03 migration: eski `KEY_HOME_SCORE_VISIBLE` tercihini yeni `KEY_DIGITAL_LIFE_CARD_VISIBLE`e
     * tek sefer taşır. Bayrak (`KEY_DIGITAL_LIFE_CARD_MIGRATION_DONE`) set edilmişse tekrar çalışmaz.
     */
    internal fun migrateDigitalLifeCardVisibilityIfNeeded(context: Context) {
        val sharedPrefs = prefs(context)
        if (sharedPrefs.getBoolean(KEY_DIGITAL_LIFE_CARD_MIGRATION_DONE, false)) return
        val legacyScoreVisible = sharedPrefs.getBoolean(KEY_HOME_SCORE_VISIBLE, true)
        sharedPrefs.edit()
            .putBoolean(KEY_DIGITAL_LIFE_CARD_VISIBLE, legacyScoreVisible)
            .putBoolean(KEY_DIGITAL_LIFE_CARD_MIGRATION_DONE, true)
            .apply()
    }

    // Ana ekranda tek satırlık içgörü görünürlüğü
    const val KEY_HOME_INSIGHT_VISIBLE = "home_insight_visible"
    fun isHomeInsightVisible(context: Context) = prefs(context).getBoolean(KEY_HOME_INSIGHT_VISIBLE, true)
    fun setHomeInsightVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HOME_INSIGHT_VISIBLE, v).apply()

    // Skor halkası ("Denge") altında 24 saatlik mini kullanım grafiği görünürlüğü
    const val KEY_HOME_USAGE_CHART_VISIBLE = "home_usage_chart_visible"
    fun isHomeUsageChartVisible(context: Context) = prefs(context).getBoolean(KEY_HOME_USAGE_CHART_VISIBLE, true)
    fun setHomeUsageChartVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HOME_USAGE_CHART_VISIBLE, v).apply()

    // Son gösterilen içgörü id'si — aynı mesaj her açılışta tekrarlanmasın (dönüşümlü gösterim)
    const val KEY_PULSE_LAST_INSIGHT_ID = "pulse_last_insight_id"
    fun getPulseLastInsightId(context: Context): String? = prefs(context).getString(KEY_PULSE_LAST_INSIGHT_ID, null)
    fun setPulseLastInsightId(context: Context, id: String) = prefs(context).edit().putString(KEY_PULSE_LAST_INSIGHT_ID, id).apply()

    // Haber şeridi (ticker) — klasör/içgörü/bildirim haberleri ana ekranda akar
    // T05 (Akıllı Nabız ayarları, ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
    // satır 1848-1905): eski KEY_TICKER_ENABLED yeni KEY_SMART_TICKER_ENABLED'e migrate edilir.
    // Eski anahtar SİLİNMEDİ — geri okuma/roll-back güvenliği ve tek seferlik migration kaynağı.
    @Deprecated("KEY_SMART_TICKER_ENABLED kullan — bu yalnızca migration kaynağı olarak kalır")
    const val KEY_TICKER_ENABLED = "home_ticker_enabled"
    const val KEY_SMART_TICKER_ENABLED = "smart_ticker_enabled"
    private const val KEY_SMART_TICKER_ENABLED_MIGRATION_DONE = "smart_ticker_enabled_migration_done"

    fun isTickerEnabled(context: Context): Boolean {
        migrateSmartTickerEnabledIfNeeded(context)
        return prefs(context).getBoolean(KEY_SMART_TICKER_ENABLED, true)
    }

    fun setTickerEnabled(context: Context, v: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_SMART_TICKER_ENABLED, v)
            .putBoolean(KEY_SMART_TICKER_ENABLED_MIGRATION_DONE, true)
            .apply()
    }

    /** T05 migration: eski `KEY_TICKER_ENABLED` tercihini yeni anahtara tek sefer taşır. */
    internal fun migrateSmartTickerEnabledIfNeeded(context: Context) {
        val sharedPrefs = prefs(context)
        if (sharedPrefs.getBoolean(KEY_SMART_TICKER_ENABLED_MIGRATION_DONE, false)) return
        @Suppress("DEPRECATION")
        val legacyEnabled = sharedPrefs.getBoolean(KEY_TICKER_ENABLED, true)
        sharedPrefs.edit()
            .putBoolean(KEY_SMART_TICKER_ENABLED, legacyEnabled)
            .putBoolean(KEY_SMART_TICKER_ENABLED_MIGRATION_DONE, true)
            .apply()
    }

    // T05 — içerik türü bazlı görünürlük anahtarları. SmartTickerType enum adları iç mantık
    // olduğu için kullanıcıya HİÇBİR YERDE gösterilmez (CLAUDE.md "iç mantık sızdırılmaz");
    // Ayarlar ekranı yalnızca kullanıcı dili etiketleri kullanır (bkz. SmartTickerSettingsScreen).
    // Roadmap mock'ta 7 kullanıcı görünür satır var; MISSION_PROGRESS + MISSION_ACHIEVEMENT tek
    // "Görev uyarıları ve başarılar" switch'i altında BİRLEŞTİRİLİR (aynı switch iki türü de
    // kontrol eder) — roadmap listesinde ayrı satır olarak yer almıyor, kullanıcı için tek
    // kavram (görevler). Diğer 6 tür 1:1 eşlenir.
    const val KEY_SMART_TICKER_ACTIONS = "smart_ticker_show_actions"           // Yapılması gerekenler (ACTION_REQUIRED)
    const val KEY_SMART_TICKER_MISSIONS = "smart_ticker_show_missions"        // Görev uyarıları ve başarılar (MISSION_PROGRESS + MISSION_ACHIEVEMENT)
    const val KEY_SMART_TICKER_PULSE = "smart_ticker_show_pulse"              // Dijital Yaşam değişimleri (PULSE_CHANGE)
    const val KEY_SMART_TICKER_REPORTS = "smart_ticker_show_reports"          // Haftalık rapor (WEEKLY_REPORT)
    const val KEY_SMART_TICKER_CONTEXTUAL = "smart_ticker_show_contextual"    // Zaman bazlı öneriler (CONTEXTUAL_SUGGESTION)
    const val KEY_SMART_TICKER_DISCOVERY = "smart_ticker_show_discovery"      // Özellik ipuçları (FEATURE_DISCOVERY) — roadmap mock'ta varsayılan KAPALI
    const val KEY_SMART_TICKER_HEALTH = "smart_ticker_show_health"            // Sistem sağlık uyarıları (CRITICAL_HEALTH)

    fun isSmartTickerActionsVisible(context: Context) = prefs(context).getBoolean(KEY_SMART_TICKER_ACTIONS, true)
    fun setSmartTickerActionsVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_TICKER_ACTIONS, v).apply()

    fun isSmartTickerMissionsVisible(context: Context) = prefs(context).getBoolean(KEY_SMART_TICKER_MISSIONS, true)
    fun setSmartTickerMissionsVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_TICKER_MISSIONS, v).apply()

    fun isSmartTickerPulseVisible(context: Context) = prefs(context).getBoolean(KEY_SMART_TICKER_PULSE, true)
    fun setSmartTickerPulseVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_TICKER_PULSE, v).apply()

    fun isSmartTickerReportsVisible(context: Context) = prefs(context).getBoolean(KEY_SMART_TICKER_REPORTS, true)
    fun setSmartTickerReportsVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_TICKER_REPORTS, v).apply()

    fun isSmartTickerContextualVisible(context: Context) = prefs(context).getBoolean(KEY_SMART_TICKER_CONTEXTUAL, true)
    fun setSmartTickerContextualVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_TICKER_CONTEXTUAL, v).apply()

    // Döngü G8 — Cihaz Düzeni İçgörüleri (depolama/kullanılmayan uygulama/bildirim yükü/öz-tanı
    // fırsatları, GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md G8). Varsayılan AÇIK, tek toggle ile
    // tamamen kapanır — kapalıyken DeviceTidinessInsights.all() hiç çağrılmaz.
    const val KEY_DEVICE_TIDINESS_INSIGHTS = "device_tidiness_insights_enabled"
    fun isDeviceTidinessInsightsEnabled(context: Context) = prefs(context).getBoolean(KEY_DEVICE_TIDINESS_INSIGHTS, true)
    fun setDeviceTidinessInsightsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_DEVICE_TIDINESS_INSIGHTS, v).apply()

    // Roadmap mock (satır 1863): "[ ] Özellik ipuçları" — bu satır tek başlangıçta kapalı olan.
    fun isSmartTickerDiscoveryVisible(context: Context) = prefs(context).getBoolean(KEY_SMART_TICKER_DISCOVERY, false)
    fun setSmartTickerDiscoveryVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_TICKER_DISCOVERY, v).apply()

    fun isSmartTickerHealthVisible(context: Context) = prefs(context).getBoolean(KEY_SMART_TICKER_HEALTH, true)
    fun setSmartTickerHealthVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SMART_TICKER_HEALTH, v).apply()

    // Görev S2 — Usta (100⭐) ödülü: opsiyonel altın saat aksanı. Varsayılan KAPALI; yalnızca
    // StarLevelSystem.Level.MASTER seviyesine ulaşan kullanıcılar için Ayarlar'da görünür ve
    // etkinleştirilebilir. Seviye şartı UI katmanında (SmartDashboardPage) ayrıca kontrol edilir —
    // bu pref tek başına altın rengi göstermeye yetmez (kilit açılmadıysa etkisiz kalır).
    const val KEY_MASTER_CLOCK_STYLE_ENABLED = "master_clock_style_enabled"
    fun isMasterClockStyleEnabled(context: Context) = prefs(context).getBoolean(KEY_MASTER_CLOCK_STYLE_ENABLED, false)
    fun setMasterClockStyleEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_MASTER_CLOCK_STYLE_ENABLED, v).apply()

    /**
     * Belirli bir [SmartTickerType] adının (iç enum ismi) kullanıcı tercihine göre görünür olup
     * olmadığını döndürür — T04'ün `KEY_TICKER_HIDDEN_TYPES` (tekil kapatma menüsü) ile BİRLİKTE
     * çalışır: bu fonksiyon T05 toplu ayar ekranının sonucu, hidden-types ise "bu tür bilgileri
     * gösterme" hızlı kapatmanın sonucudur — ViewModel ikisini de AND ile birleştirir.
     */
    fun isSmartTickerTypeVisible(context: Context, typeName: String): Boolean = when (typeName) {
        "ACTION_REQUIRED" -> isSmartTickerActionsVisible(context)
        "MISSION_PROGRESS", "MISSION_ACHIEVEMENT" -> isSmartTickerMissionsVisible(context)
        "PULSE_CHANGE" -> isSmartTickerPulseVisible(context)
        "WEEKLY_REPORT" -> isSmartTickerReportsVisible(context)
        "CONTEXTUAL_SUGGESTION" -> isSmartTickerContextualVisible(context)
        "FEATURE_DISCOVERY" -> isSmartTickerDiscoveryVisible(context)
        "CRITICAL_HEALTH" -> isSmartTickerHealthVisible(context)
        else -> true
    }

    // T05 — otomatik geçiş aç/kapat + geçiş süresi (saniye, 5-20 arası, varsayılan 10 —
    // HomeTickerRow.AUTO_ADVANCE_INTERVAL_MS ile aynı varsayılan).
    const val KEY_TICKER_AUTO_ADVANCE = "smart_ticker_auto_advance"
    const val KEY_TICKER_INTERVAL_SECONDS = "smart_ticker_interval_seconds"
    const val TICKER_INTERVAL_DEFAULT_SECONDS = 10

    fun isTickerAutoAdvanceEnabled(context: Context) = prefs(context).getBoolean(KEY_TICKER_AUTO_ADVANCE, true)
    fun setTickerAutoAdvanceEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_TICKER_AUTO_ADVANCE, v).apply()

    fun getTickerIntervalSeconds(context: Context): Int =
        prefs(context).getInt(KEY_TICKER_INTERVAL_SECONDS, TICKER_INTERVAL_DEFAULT_SECONDS)
    fun setTickerIntervalSeconds(context: Context, seconds: Int) =
        prefs(context).edit().putInt(KEY_TICKER_INTERVAL_SECONDS, seconds.coerceIn(5, 20)).apply()

    // T05 — "Hassas bilgileri göster": kapalıyken sensitive=true işaretli haberler (ör. aktif
    // bildirim sayısı) şeritte gösterilmez. Varsayılan KAPALI (roadmap mock satır 1868) — kilit
    // ekranı yakınında/başkasının görebileceği ortamda gizlilik varsayılan olarak korunur.
    const val KEY_TICKER_SENSITIVE_VISIBLE = "smart_ticker_sensitive_visible"
    fun isTickerSensitiveVisible(context: Context) = prefs(context).getBoolean(KEY_TICKER_SENSITIVE_VISIBLE, false)
    fun setTickerSensitiveVisible(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_TICKER_SENSITIVE_VISIBLE, v).apply()

    // Ana ekran arka plan stili — 3 hazir gradient secenegi (D260)
    const val KEY_HOME_BACKGROUND_STYLE = "home_background_style"
    const val HOME_BG_TURKUAZ = "turkuaz"
    const val HOME_BG_GECE_MAVISI = "gece_mavisi"
    const val HOME_BG_MINIMAL_GRI = "minimal_gri"
    fun getHomeBackgroundStyle(context: Context): String =
        prefs(context).getString(KEY_HOME_BACKGROUND_STYLE, HOME_BG_TURKUAZ) ?: HOME_BG_TURKUAZ
    fun setHomeBackgroundStyle(context: Context, style: String) =
        prefs(context).edit().putString(KEY_HOME_BACKGROUND_STYLE, style).apply()

    // Gorev/yildiz sistemi (gamification) — kapaliyken kisilik etiketi ve gorev UI'lari gizlenir (D257)
    const val KEY_MISSIONS_ENABLED = "missions_enabled"
    fun isMissionsEnabled(context: Context) = prefs(context).getBoolean(KEY_MISSIONS_ENABLED, true)
    fun setMissionsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_MISSIONS_ENABLED, v).apply()

    // Görev S1 — Dashboard'da tek bağlamsal "BUGÜN" kartı. Açıkken HomeMissionCard/DigitalLifeCard/
    // AssistantInsightRow/bugün yüklenenler satırı yerine TodayCard çizilir; kapalıyken mevcut
    // bireysel kart toggle'ları (missions_enabled, digital_life_card_visible, ...) AYNEN çalışır.
    const val KEY_TODAY_CARD_ENABLED = "today_card_enabled"
    fun isTodayCardEnabled(context: Context) = prefs(context).getBoolean(KEY_TODAY_CARD_ENABLED, true)
    fun setTodayCardEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_TODAY_CARD_ENABLED, v).apply()

    // Dongu G5 — Kutlama & Mikro-etkilesim (GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md G5).
    // Kapaliyken: tamamlanma animasyonu, 3/3 gunu parilti VE sabah ozeti sirit ogesi TAMAMEN
    // kapanir (haptic de dahil — kullanici tum mikro-etkilesimi istemiyorsa hicbiri kalmaz).
    const val KEY_MISSION_CELEBRATIONS = "mission_celebrations_enabled"
    fun isMissionCelebrationsEnabled(context: Context) = prefs(context).getBoolean(KEY_MISSION_CELEBRATIONS, true)
    fun setMissionCelebrationsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_MISSION_CELEBRATIONS, v).apply()

    // Gun basina bir kez "3/3 gunu" parilti + "Bugunu topladin" mesaji gostermek icin bayrak —
    // epochDay saklanir, ayni gun tekrar tetiklenmez (HomeMissionCard, G5).
    private const val KEY_MISSION_ALL_COMPLETED_CELEBRATED_EPOCH_DAY = "mission_all_completed_celebrated_epoch_day"
    private const val NO_EPOCH_DAY = -1L

    /** [epochDay] icin 3/3 kutlamasi daha once gosterildi mi. */
    fun wasAllCompletedCelebrated(context: Context, epochDay: Long): Boolean =
        prefs(context).getLong(KEY_MISSION_ALL_COMPLETED_CELEBRATED_EPOCH_DAY, NO_EPOCH_DAY) == epochDay

    /** [epochDay] icin 3/3 kutlamasi gosterildi olarak isaretle (idempotent — ayni gun tekrar yazilabilir). */
    fun markAllCompletedCelebrated(context: Context, epochDay: Long) =
        prefs(context).edit().putLong(KEY_MISSION_ALL_COMPLETED_CELEBRATED_EPOCH_DAY, epochDay).apply()

    // Dongu G6 — Yildiz Ekonomisi: seviye atlama tespiti icin en son bilinen seviye adi
    // (StarLevelSystem.Level.name) saklanir. Ilk okumada deger yoksa (-1 sentinel yerine bos
    // string) mevcut seviye "gecis" olarak SAYILMAZ — kurulumdan itibaren zaten o seviyedeki
    // kullanicilar sahte bir "yeni seviye!" bildirimi gormez (yalniz ileri git-gel gercek bir
    // atlama tetikler).
    private const val KEY_LAST_KNOWN_STAR_LEVEL = "last_known_star_level"
    fun getLastKnownStarLevel(context: Context): String? = prefs(context).getString(KEY_LAST_KNOWN_STAR_LEVEL, null)
    fun setLastKnownStarLevel(context: Context, levelName: String) =
        prefs(context).edit().putString(KEY_LAST_KNOWN_STAR_LEVEL, levelName).apply()

    // "Bugun Yuklenenler" — ana ekran kompakt girisi + cekmece bolumu (EX01, kullanici talebi)
    const val KEY_RECENT_INSTALLS_ENABLED = "recent_installs_enabled"
    fun isRecentInstallsEnabled(context: Context) = prefs(context).getBoolean(KEY_RECENT_INSTALLS_ENABLED, true)
    fun setRecentInstallsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_RECENT_INSTALLS_ENABLED, v).apply()

    // Ana ekran hava durumu (P1.7)
    const val KEY_HOME_WEATHER_ENABLED = "home_weather_enabled"
    const val KEY_HOME_WEATHER_USE_LOCATION = "home_weather_use_location"
    const val KEY_HOME_WEATHER_MANUAL_CITY = "home_weather_manual_city"
    fun isHomeWeatherEnabled(context: Context) = prefs(context).getBoolean(KEY_HOME_WEATHER_ENABLED, true)
    fun setHomeWeatherEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HOME_WEATHER_ENABLED, v).apply()
    fun isHomeWeatherUseLocation(context: Context) = prefs(context).getBoolean(KEY_HOME_WEATHER_USE_LOCATION, false)
    fun setHomeWeatherUseLocation(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_HOME_WEATHER_USE_LOCATION, v).apply()
    fun getHomeWeatherManualCity(context: Context) = prefs(context).getString(KEY_HOME_WEATHER_MANUAL_CITY, "") ?: ""
    fun setHomeWeatherManualCity(context: Context, city: String) = prefs(context).edit().putString(KEY_HOME_WEATHER_MANUAL_CITY, city.trim()).apply()

    // Ticker sessize alma — basili tut menusunden secilen zamana kadar serit gizlenir (D233)
    const val KEY_TICKER_MUTED_UNTIL = "home_ticker_muted_until"
    fun getTickerMutedUntil(context: Context) = prefs(context).getLong(KEY_TICKER_MUTED_UNTIL, 0L)
    fun setTickerMutedUntil(context: Context, untilMillis: Long) = prefs(context).edit().putLong(KEY_TICKER_MUTED_UNTIL, untilMillis).apply()
    /** T05 — Ayarlar ekranındaki "Sessiz saatler" göstergesinden erken kaldırma. */
    fun clearTickerMutedUntil(context: Context) = prefs(context).edit().putLong(KEY_TICKER_MUTED_UNTIL, 0L).apply()

    // Icerik bazli ticker bastirma (Dongu T04) — "Bu tur bilgileri gosterme" secilince
    // SmartTickerType.name bu sete eklenir; TickerRow'a dokunmadan LauncherViewModel
    // tickerItems akisinda filtrelenir (dismissedTickerKeys ile ayni desen, tur bazli).
    const val KEY_TICKER_HIDDEN_TYPES = "home_ticker_hidden_types"
    fun getTickerHiddenTypes(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_TICKER_HIDDEN_TYPES, emptySet()) ?: emptySet()
    fun addTickerHiddenType(context: Context, typeName: String) =
        prefs(context).edit().putStringSet(KEY_TICKER_HIDDEN_TYPES, getTickerHiddenTypes(context) + typeName).apply()
    /** T05 — toplu ayar ekranından tekil "Bu türü gösterme" kapatmasını geri açar. */
    fun removeTickerHiddenType(context: Context, typeName: String) =
        prefs(context).edit().putStringSet(KEY_TICKER_HIDDEN_TYPES, getTickerHiddenTypes(context) - typeName).apply()

    // Arama çubuğu elmas parlaması — 10-15 sn'de bir gradient süpürme animasyonu
    const val KEY_SEARCH_SHINE_ENABLED = "search_shine_enabled"
    fun isSearchShineEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_SHINE_ENABLED, true)
    fun setSearchShineEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_SHINE_ENABLED, v).apply()

    // Sonuç bulunamayınca web/Play Store araması öner (fallback satırları)
    const val KEY_SEARCH_WEB_FALLBACK_ENABLED = "search_web_fallback_enabled"
    fun isSearchWebFallbackEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_WEB_FALLBACK_ENABLED, true)
    fun setSearchWebFallbackEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_WEB_FALLBACK_ENABLED, v).apply()

    // S3 — çekmece sadeleştirme: hızlı filtre + sıralama chip satırları varsayılan olarak
    // kapalı; sade modda tek kompakt menü butonu (Tune ikonu) aynı işlevi görür.
    const val KEY_DRAWER_CHIP_ROWS_ENABLED = "drawer_chip_rows_enabled"
    fun isDrawerChipRowsEnabled(context: Context) = prefs(context).getBoolean(KEY_DRAWER_CHIP_ROWS_ENABLED, false)
    fun setDrawerChipRowsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_DRAWER_CHIP_ROWS_ENABLED, v).apply()

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
    private const val LEGACY_KEY_FOLDER_BLUR = "folder_blur"
    fun clearLegacyFolderBlurPreference(context: Context) =
        prefs(context).edit().remove(LEGACY_KEY_FOLDER_BLUR).apply()

    // DeepSeek API anahtari — LLM fallback kategorize icin
    // F1 denetimi (P0): anahtar eskiden ana app_organizer_prefs dosyasindaydi ve Auto Backup'a
    // dahildi (data_extraction_rules.xml'deki exclusion yanlis dosya adini hedefliyordu — bkz.
    // fix). Artik AYRI "deepseek_prefs" dosyasinda tutuluyor, o dosya backup/device-transfer
    // kurallarindan tamamen haric tutuluyor. migrateSensitivePrefsIfNeeded() eski degeri tasir.
    private const val DEEPSEEK_PREFS_NAME = "deepseek_prefs"
    const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
    private fun deepSeekPrefs(context: Context) =
        context.getSharedPreferences(DEEPSEEK_PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeepSeekApiKey(context: Context): String {
        migrateSensitivePrefsIfNeeded(context)
        return deepSeekPrefs(context).getString(KEY_DEEPSEEK_API_KEY, "") ?: ""
    }
    fun setDeepSeekApiKey(context: Context, key: String) {
        migrateSensitivePrefsIfNeeded(context)
        deepSeekPrefs(context).edit().putString(KEY_DEEPSEEK_API_KEY, key.trim()).apply()
    }

    // FCM kaldirildi (D-S6) — device_prefs dosyasi ve KEY_FCM_TOKEN sabiti yalnizca asagidaki
    // migrateSensitivePrefsIfNeeded legacy temizligi icin korunuyor: eski cihazlarda
    // app_organizer_prefs icinde hala fcm_token degeri kalmis olabilir, bu deger sessizce
    // device_prefs'e tasinip ana dosyadan silinir (F1 denetimiyle uyumlu, veri sizdirmaz).
    private const val DEVICE_PREFS_NAME = "device_prefs"
    const val KEY_FCM_TOKEN = "fcm_token"
    private fun devicePrefs(context: Context) =
        context.getSharedPreferences(DEVICE_PREFS_NAME, Context.MODE_PRIVATE)

    // Tek seferlik migration bayragi: eski app_organizer_prefs icindeki deepseek_api_key ve
    // fcm_token degerlerini yeni ayri dosyalara tasir, eskisini siler. Idempotent — bayrak
    // set edildikten sonra tekrar calismaz.
    private const val KEY_SENSITIVE_PREFS_MIGRATED = "sensitive_prefs_migrated_v1"
    private fun migrateSensitivePrefsIfNeeded(context: Context) {
        val main = prefs(context)
        if (main.getBoolean(KEY_SENSITIVE_PREFS_MIGRATED, false)) return

        val legacyKey = main.getString(KEY_DEEPSEEK_API_KEY, null)
        if (!legacyKey.isNullOrBlank()) {
            deepSeekPrefs(context).edit().putString(KEY_DEEPSEEK_API_KEY, legacyKey).apply()
        }
        val legacyToken = main.getString(KEY_FCM_TOKEN, null)
        if (!legacyToken.isNullOrBlank()) {
            devicePrefs(context).edit().putString(KEY_FCM_TOKEN, legacyToken).apply()
        }

        main.edit()
            .remove(KEY_DEEPSEEK_API_KEY)
            .remove(KEY_FCM_TOKEN)
            .putBoolean(KEY_SENSITIVE_PREFS_MIGRATED, true)
            .apply()
    }

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

    // P0.3: Dosya indeksi kalıcı durumu — FileIndexState.Ready/Failed için Settings ekranı arasında hayatta kalır
    const val KEY_FILE_INDEX_ITEM_COUNT = "file_index_item_count"
    const val KEY_FILE_INDEX_LAST_INDEXED_AT = "file_index_last_indexed_at"
    const val KEY_FILE_INDEX_FAILURE_REASON = "file_index_failure_reason"

    fun getFileIndexItemCount(context: Context) = prefs(context).getInt(KEY_FILE_INDEX_ITEM_COUNT, 0)
    fun getFileIndexLastIndexedAt(context: Context) = prefs(context).getLong(KEY_FILE_INDEX_LAST_INDEXED_AT, 0L)
    fun getFileIndexFailureReason(context: Context): String? = prefs(context).getString(KEY_FILE_INDEX_FAILURE_REASON, null)

    fun setFileIndexSuccess(context: Context, itemCount: Int, lastIndexedAt: Long) =
        prefs(context).edit()
            .putInt(KEY_FILE_INDEX_ITEM_COUNT, itemCount)
            .putLong(KEY_FILE_INDEX_LAST_INDEXED_AT, lastIndexedAt)
            .putString(KEY_FILE_INDEX_FAILURE_REASON, null)
            .apply()

    fun setFileIndexFailure(context: Context, reason: String) =
        prefs(context).edit().putString(KEY_FILE_INDEX_FAILURE_REASON, reason).apply()

    fun clearFileIndexState(context: Context) =
        prefs(context).edit()
            .remove(KEY_FILE_INDEX_ITEM_COUNT)
            .remove(KEY_FILE_INDEX_LAST_INDEXED_AT)
            .remove(KEY_FILE_INDEX_FAILURE_REASON)
            .apply()

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

    // Dongu G3a — DAILY_FOCUS_SESSION gorevi icin basit prefs sayaci. Focus Mode acilinca
    // KEY_FOCUS_SESSION_START_AT o anin epoch-milli degerini tutar; kapaninca gecen sure
    // gunun toplamina (KEY_FOCUS_MINUTES_TODAY_<epochDay>) eklenir. Gun degisince eski
    // gunun sayaci otomatik gecersiz sayilir (anahtar epochDay'e gore degisir) — ekstra
    // temizlik/migration gerekmez, SharedPreferences dosyasinda birikip kalsa da onemsizdir
    // (kucuk int degerler). Buyuk Room tablosu GEREKMEZ - bu tek kullanicilik yerel sayac.
    private const val KEY_FOCUS_SESSION_START_AT = "focus_session_start_at_ms"
    private fun focusMinutesTodayKey(epochDay: Long) = "focus_minutes_today_$epochDay"

    /** Focus Mode ACILDIGINDA cagrilir — aktif oturum baslangicini kaydeder. */
    fun startFocusSession(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(KEY_FOCUS_SESSION_START_AT, nowMillis).apply()
    }

    /**
     * Focus Mode KAPANDIGINDA cagrilir — acik kalan sureyi (dakika) gunlere BOLEREK yazar.
     * F5: gece yarisini asan oturum (orn. 23:50-00:20) eskiden 30dk'nin tamamini yeni gune
     * yaziyordu; artik her gun kendi 00:00 sinirina kadar olan payini alir (10dk dun, 20dk
     * bugun). Baslangic kaydi yoksa (orn. process restart) hicbir sey eklenmez — sahte sure
     * UYDURULMAZ.
     */
    fun endFocusSession(context: Context, nowMillis: Long = System.currentTimeMillis(), zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()) {
        val p = prefs(context)
        val startAt = p.getLong(KEY_FOCUS_SESSION_START_AT, 0L)
        if (startAt <= 0L) return
        p.edit().remove(KEY_FOCUS_SESSION_START_AT).apply()
        if (nowMillis <= startAt) return
        val editor = p.edit()
        var changed = false
        var cursor = startAt
        while (cursor < nowMillis) {
            val day = java.time.Instant.ofEpochMilli(cursor).atZone(zoneId).toLocalDate()
            val nextMidnight = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val segmentEnd = minOf(nextMidnight, nowMillis)
            val segmentMinutes = (segmentEnd - cursor) / 60_000L
            if (segmentMinutes > 0L) {
                val key = focusMinutesTodayKey(day.toEpochDay())
                editor.putLong(key, p.getLong(key, 0L) + segmentMinutes)
                changed = true
            }
            cursor = segmentEnd
        }
        if (changed) editor.apply()
    }

    /**
     * Bugune kadar biriken Focus Mode dakikasi — devam eden (henuz kapanmamis) oturum varsa
     * o da dahil edilir (canli ilerleme goruntusu icin). F5: devam eden oturum gece yarisini
     * astiysa yalniz BUGUNUN 00:00 sonrasi sayilir — dunku pay bugune yazilmaz. Gorev
     * degerlendirmesi bu toplami DAILY_FOCUS_SESSION hedefiyle (30dk) karsilastirir.
     */
    fun getFocusMinutesToday(context: Context, nowMillis: Long = System.currentTimeMillis(), zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long {
        val p = prefs(context)
        val today = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val stored = p.getLong(focusMinutesTodayKey(today.toEpochDay()), 0L)
        val startAt = p.getLong(KEY_FOCUS_SESSION_START_AT, 0L)
        val activeMinutes = if (startAt > 0L) {
            val todayStartMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
            ((nowMillis - maxOf(startAt, todayStartMillis)).coerceAtLeast(0L)) / 60_000L
        } else 0L
        return stored + activeMinutes
    }

    // Dongu G3b — DAILY_APP_LIMIT gorevi icin gunluk hedef PAKET adi saklanir. DB semasi
    // DEGISMEZ (mission_instances yeni kolon almaz) — en temiz yol epochDay-anahtarli basit
    // SharedPreferences kaydi (KEY_FOCUS_MINUTES_TODAY ile AYNI desen). Paket adi SADECE bu
    // prefs anahtarinda ve gorev BASLIGINDA gorunur, hicbir telemetri/diagnostics olayina
    // yazilmaz (U02). Gun degisince eski anahtar otomatik gecersiz sayilir (yeni epochDay farkli
    // key uretir) — eski kayitlar dosyada birikip kalsa da onemsizdir (tek String deger).
    private fun appLimitTargetPackageKey(epochDay: Long) = "app_limit_target_pkg_$epochDay"

    /** [epochDay] icin secilen aday PAKET adini kaydeder (MissionSummaryUseCase.compute() cagirir). */
    fun setAppLimitTargetPackage(context: Context, epochDay: Long, packageName: String) {
        prefs(context).edit().putString(appLimitTargetPackageKey(epochDay), packageName).apply()
    }

    /** [epochDay] icin daha once kaydedilmis aday PAKET adi - donem boyunca SABIT kalmasi icin
     * (gun ortasinda kullanim degisip farkli bir uygulama one cikarsa hedef KAYMASIN diye)
     * MissionSummaryUseCase her cagrida ONCE burayi okur, yoksa yeni secim yapar. */
    fun getAppLimitTargetPackage(context: Context, epochDay: Long): String? =
        prefs(context).getString(appLimitTargetPackageKey(epochDay), null)

    // Quick Wheel / Pie Mode — uzun bas ile radyal uygulama çarkı (varsayılan kapalı)
    const val KEY_QUICK_WHEEL = "quick_wheel_enabled"
    fun isQuickWheelEnabled(context: Context) = prefs(context).getBoolean(KEY_QUICK_WHEEL, false)
    fun setQuickWheelEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_QUICK_WHEEL, v).apply()

    // Contextual Dock — 2 sabit + 2 akıllı öneri
    const val KEY_CONTEXTUAL_DOCK = "contextual_dock_enabled"
    fun isContextualDockEnabled(context: Context) = prefs(context).getBoolean(KEY_CONTEXTUAL_DOCK, true)
    fun setContextualDockEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_CONTEXTUAL_DOCK, v).apply()

    const val KEY_RECENT_NOTIFICATION_APPS_ROW = "recent_notification_apps_row_enabled"
    fun isRecentNotificationAppsRowEnabled(context: Context) = prefs(context).getBoolean(KEY_RECENT_NOTIFICATION_APPS_ROW, false)
    fun setRecentNotificationAppsRowEnabled(context: Context, v: Boolean) =
        prefs(context).edit().putBoolean(KEY_RECENT_NOTIFICATION_APPS_ROW, v).apply()

    const val KEY_LOW_CONFIDENCE_REVIEW = "low_confidence_classification_review_enabled"
    fun isLowConfidenceReviewEnabled(context: Context) = prefs(context).getBoolean(KEY_LOW_CONFIDENCE_REVIEW, true)
    fun setLowConfidenceReviewEnabled(context: Context, v: Boolean) =
        prefs(context).edit().putBoolean(KEY_LOW_CONFIDENCE_REVIEW, v).apply()

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
    const val KEY_MANUAL_OVERRIDES_ROOM_MIGRATED = "manual_overrides_room_migrated"
    const val KEY_DISMISSED_FOLDER_SUGGESTIONS = "dismissed_folder_suggestions"
    const val KEY_SNOOZED_FOLDER_SUGGESTIONS = "snoozed_folder_suggestions"

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

    fun isManualOverridesRoomMigrated(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MANUAL_OVERRIDES_ROOM_MIGRATED, false)

    fun setManualOverridesRoomMigrated(context: Context, migrated: Boolean) =
        prefs(context).edit().putBoolean(KEY_MANUAL_OVERRIDES_ROOM_MIGRATED, migrated).apply()

    fun getAcceptedOverridePatterns(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_ACCEPTED_OVERRIDE_PATTERNS, emptySet()) ?: emptySet()

    fun addAcceptedOverridePattern(context: Context, categoryId: String, packages: List<String>) {
        if (packages.isEmpty()) return
        val next = getAcceptedOverridePatterns(context).toMutableSet()
        next += "$categoryId:${packages.sorted().joinToString(",")}"
        prefs(context).edit().putStringSet(KEY_ACCEPTED_OVERRIDE_PATTERNS, next).apply()
    }

    fun getDismissedFolderSuggestions(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_DISMISSED_FOLDER_SUGGESTIONS, emptySet()) ?: emptySet()

    fun setDismissedFolderSuggestions(context: Context, values: Set<String>) =
        prefs(context).edit().putStringSet(KEY_DISMISSED_FOLDER_SUGGESTIONS, values).apply()

    fun dismissFolderSuggestion(context: Context, suggestionId: String) {
        val next = getDismissedFolderSuggestions(context).toMutableSet().also { it.add(suggestionId) }
        setDismissedFolderSuggestions(context, next)
    }

    fun getSnoozedFolderSuggestions(context: Context): Map<String, String> =
        (prefs(context).getString(KEY_SNOOZED_FOLDER_SUGGESTIONS, null) ?: "").parseJsonMap()

    fun setSnoozedFolderSuggestions(context: Context, values: Map<String, String>) =
        prefs(context).edit().putString(KEY_SNOOZED_FOLDER_SUGGESTIONS, values.toJsonString()).apply()

    fun snoozeFolderSuggestion(context: Context, suggestionId: String, untilMillis: Long) {
        val next = getSnoozedFolderSuggestions(context).toMutableMap()
        next[suggestionId] = untilMillis.toString()
        setSnoozedFolderSuggestions(context, next)
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

    // Siniflandirma/klasor onerileri sistem bildirimi (ROADMAP #26) — varsayilan KAPALI (yeni ozellik kurali)
    const val KEY_SUGGESTION_NOTIFICATIONS_ENABLED = "suggestion_notifications_enabled"
    const val KEY_SUGGESTION_NOTIF_LAST_COUNT = "suggestion_notif_last_count"

    fun isSuggestionNotificationsEnabled(context: Context) =
        prefs(context).getBoolean(KEY_SUGGESTION_NOTIFICATIONS_ENABLED, false)
    fun setSuggestionNotificationsEnabled(context: Context, v: Boolean) =
        prefs(context).edit().putBoolean(KEY_SUGGESTION_NOTIFICATIONS_ENABLED, v).apply()

    fun getSuggestionNotifLastCount(context: Context): Int =
        prefs(context).getInt(KEY_SUGGESTION_NOTIF_LAST_COUNT, 0)
    fun setSuggestionNotifLastCount(context: Context, count: Int) =
        prefs(context).edit().putInt(KEY_SUGGESTION_NOTIF_LAST_COUNT, count).apply()

    // Arama istatistikleri - anonim sayaclar (SearchStatsPrefs). Kapatilinca loglama durur.
    const val KEY_SEARCH_STATS_ENABLED = "search_stats_enabled"
    fun isSearchStatsEnabled(context: Context) = prefs(context).getBoolean(KEY_SEARCH_STATS_ENABLED, true)
    fun setSearchStatsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_SEARCH_STATS_ENABLED, v).apply()

    // P1.3: Saat bazli kisi onerileri - launcher icinden baslatilan kisi aksiyonlarindan (Ara/SMS/
    // WhatsApp) ogrenir. READ_CALL_LOG ISTENMEZ - sadece kendi olay logumuz (ContactActionPrefs).
    // Varsayilan ACIK (Yeni Ozellik = Ayarlar Kurali).
    const val KEY_CONTACT_SUGGESTIONS_ENABLED = "contact_suggestions_enabled"
    fun isContactSuggestionsEnabled(context: Context) = prefs(context).getBoolean(KEY_CONTACT_SUGGESTIONS_ENABLED, true)
    fun setContactSuggestionsEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(KEY_CONTACT_SUGGESTIONS_ENABLED, v).apply()

    // ── Gorev Temposu (G1/G7, GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md) ────────────────────
    // Kisisel gorev hedefi formulunun katsayisi: hedef = son 7 gun medyani x tempo.
    // Rahat=1.0 (medyan aynen), Dengeli=0.9 (varsayilan, "dunden biraz az" hissi),
    // Iddiali=0.8 (daha zorlayici sinirlama). PersonalTargetCalculator bu katsayiyi tuketir.
    enum class MissionTempo(val coefficient: Double) {
        RAHAT(1.0),
        DENGELI(0.9),
        IDDIALI(0.8),
    }

    const val KEY_MISSION_TEMPO = "mission_tempo"
    val DEFAULT_MISSION_TEMPO = MissionTempo.DENGELI

    fun getMissionTempo(context: Context): MissionTempo {
        val stored = prefs(context).getString(KEY_MISSION_TEMPO, null)
        return stored?.let { s -> runCatching { MissionTempo.valueOf(s) }.getOrNull() } ?: DEFAULT_MISSION_TEMPO
    }

    fun setMissionTempo(context: Context, tempo: MissionTempo) =
        prefs(context).edit().putString(KEY_MISSION_TEMPO, tempo.name).apply()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

package com.armutlu.apporganizer.utils

import android.content.Context
import androidx.core.content.edit
import com.armutlu.apporganizer.presentation.ui.launcher.AppFolder
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageAnchor

/**
 * Ana ekran "son sayfa" ve başlangıç sayfası tercihlerinin SEMANTİK deposu.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P02 (satır 449-501).
 *
 * Eski `AppPrefs.getLastHomePage()`/`setLastHomePage()` (ham `Int`) DEPRECATED KÖPRÜ olarak
 * korunur — P00 regresyon testi (`AppPrefsLastHomePageTest`) hâlâ bu ham davranışı kilitliyor ve
 * P05/P13 UI'yı anchor'a taşıyana kadar mevcut çağıranlar kırılmamalı. Bu dosya yalnızca YENİ
 * semantik anchor okuma/yazma + tek seferlik migration'ı sağlar; UI bağlama bu döngünün kapsamı
 * dışındadır.
 */
object HomePagePrefs {
    internal const val PREFS_NAME = "home_page_prefs"

    const val KEY_HOME_START_PAGE_MODE = "home_start_page_mode_v2"
    const val KEY_LAST_HOME_PAGE_ANCHOR = "last_home_page_anchor_v2"
    const val KEY_HOME_PAGER_MIGRATED = "home_pager_migrated_v2"
    const val KEY_SMART_DASHBOARD_ENABLED = "smart_dashboard_enabled"

    /** Ana ekran açılışında hangi sayfadan başlanacağı. */
    enum class StartPageMode {
        /** Akıllı Dashboard varsa her zaman Dashboard'dan başla. */
        SMART_DASHBOARD,
        /** Kullanıcının en son bıraktığı sayfadan devam et. */
        RESTORE_LAST_PAGE,
        /** Her zaman ilk klasör sayfasından başla (klasik davranış). */
        FIRST_FOLDER_PAGE,
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Başlangıç sayfa modu ────────────────────────────────────────────────

    fun getStartPageMode(context: Context): StartPageMode {
        val stored = prefs(context).getString(KEY_HOME_START_PAGE_MODE, null)
        val parsed = stored?.let { s -> runCatching { StartPageMode.valueOf(s) }.getOrNull() }
        if (parsed != null) return parsed
        // Yeni kurulum (hiç kayıt yok) -> roadmap kuralı: SMART_DASHBOARD yazılır.
        setStartPageMode(context, StartPageMode.SMART_DASHBOARD)
        return StartPageMode.SMART_DASHBOARD
    }

    fun setStartPageMode(context: Context, mode: StartPageMode) =
        prefs(context).edit { putString(KEY_HOME_START_PAGE_MODE, mode.name) }

    // ── Akıllı Dashboard aç/kapat ────────────────────────────────────────────

    fun isSmartDashboardEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SMART_DASHBOARD_ENABLED, true)

    fun setSmartDashboardEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_SMART_DASHBOARD_ENABLED, enabled) }

    // ── Semantik "son sayfa" anchor'ı ────────────────────────────────────────

    /**
     * Kayıtlı anchor'ı döner. Kayıt yoksa (henüz migration çalışmadıysa) eski ham `AppPrefs`
     * index'inden tek seferlik migration tetiklenir ([migrateLegacyLastPageIfNeeded]).
     * Migration da anchor üretemezse (bozuk veri) [HomePageAnchor.Dashboard]'a düşer.
     */
    fun getLastHomePageAnchor(context: Context, folders: List<AppFolder>, pageSize: Int): HomePageAnchor {
        migrateLegacyLastPageIfNeeded(context, folders, pageSize)
        val raw = prefs(context).getString(KEY_LAST_HOME_PAGE_ANCHOR, null)
        return HomePageAnchor.deserialize(raw) ?: HomePageAnchor.Dashboard
    }

    fun setLastHomePageAnchor(context: Context, anchor: HomePageAnchor) {
        prefs(context).edit {
            putString(KEY_LAST_HOME_PAGE_ANCHOR, HomePageAnchor.serialize(anchor))
            putBoolean(KEY_HOME_PAGER_MIGRATED, true)
        }
    }

    /**
     * Eski `AppPrefs.getLastHomePage()` (ham Int, `KEY_LAST_HOME_PAGE`) değerini tek sefer okuyup
     * semantik anchor'a çevirir ve yazar. [KEY_HOME_PAGER_MIGRATED] bayrağı set edilmişse tekrar
     * çalışmaz (D03/PulseHistory migration bayrağı deseni).
     */
    internal fun migrateLegacyLastPageIfNeeded(context: Context, folders: List<AppFolder>, pageSize: Int) {
        val sharedPrefs = prefs(context)
        if (sharedPrefs.getBoolean(KEY_HOME_PAGER_MIGRATED, false)) return

        val legacyIndex = AppPrefs.getLastHomePage(context)
        val anchor = deriveAnchorFromLegacyIndex(legacyIndex, folders, pageSize)
        sharedPrefs.edit {
            putString(KEY_LAST_HOME_PAGE_ANCHOR, HomePageAnchor.serialize(anchor))
            putBoolean(KEY_HOME_PAGER_MIGRATED, true)
        }
    }

    /**
     * Saf fonksiyon (Context bağımlılığı yok, birim testinde doğrudan çağrılır) — roadmap P02
     * kuralı: eski sayfanın İLK klasörünün categoryId'si `folder:<id>` olarak saklanır. Klasör
     * listesi boşsa veya index klasör kapsamı dışındaysa Dashboard'a düşer.
     */
    internal fun deriveAnchorFromLegacyIndex(
        legacyIndex: Int,
        folders: List<AppFolder>,
        pageSize: Int,
    ): HomePageAnchor {
        if (folders.isEmpty() || pageSize <= 0 || legacyIndex < 0) return HomePageAnchor.Dashboard
        val pageFolders = folders.chunked(pageSize)
        val page = pageFolders.getOrNull(legacyIndex) ?: return HomePageAnchor.Dashboard
        val firstCategoryId = page.firstOrNull()?.category?.categoryId
        return firstCategoryId?.let { HomePageAnchor.Folder(it) } ?: HomePageAnchor.Dashboard
    }

    // ── Backup/restore köprüsü ───────────────────────────────────────────────

    internal data class BackupFields(
        val startPageMode: String?,
        val lastPageAnchor: String?,
        val smartDashboardEnabled: Boolean?,
    )

    internal fun toBackupFields(context: Context): BackupFields {
        val sharedPrefs = prefs(context)
        return BackupFields(
            startPageMode = sharedPrefs.getString(KEY_HOME_START_PAGE_MODE, null),
            lastPageAnchor = sharedPrefs.getString(KEY_LAST_HOME_PAGE_ANCHOR, null),
            smartDashboardEnabled = if (sharedPrefs.contains(KEY_SMART_DASHBOARD_ENABLED))
                sharedPrefs.getBoolean(KEY_SMART_DASHBOARD_ENABLED, true) else null,
        )
    }

    internal fun fromBackupFields(context: Context, fields: BackupFields) {
        prefs(context).edit {
            fields.startPageMode?.let { putString(KEY_HOME_START_PAGE_MODE, it) }
            fields.lastPageAnchor?.let {
                putString(KEY_LAST_HOME_PAGE_ANCHOR, it)
                putBoolean(KEY_HOME_PAGER_MIGRATED, true)
            }
            fields.smartDashboardEnabled?.let { putBoolean(KEY_SMART_DASHBOARD_ENABLED, it) }
        }
    }

    // ── Diagnostics köprüsü ──────────────────────────────────────────────────

    /** Diagnostics raporuna yalnız TİP gönderilir — categoryId gibi kullanıcı verisi asla yazılmaz. */
    internal fun anchorTypeLabel(anchor: HomePageAnchor): String = when (anchor) {
        is HomePageAnchor.Dashboard -> "DASHBOARD"
        is HomePageAnchor.Folder -> "FOLDER"
        is HomePageAnchor.PageIndex -> "PAGE_INDEX"
    }

    /**
     * Diagnostics-güvenli TİP okuması. `folders` gerektirmez (migration'ı klasör listesi olmadan
     * ZORLAMAZ — boş liste vererek migration'ı tetiklemek yanlış anchor üretir). Migration henüz
     * çalışmadıysa (bayrak set değilse) gerçek anchor bilinmediği için "UNMIGRATED" döner; aksi
     * halde kayıtlı anchor'ın tipini döner.
     */
    fun peekLastHomePageAnchorType(context: Context): String {
        val sharedPrefs = prefs(context)
        if (!sharedPrefs.getBoolean(KEY_HOME_PAGER_MIGRATED, false)) return "UNMIGRATED"
        val raw = sharedPrefs.getString(KEY_LAST_HOME_PAGE_ANCHOR, null)
        val anchor = HomePageAnchor.deserialize(raw) ?: HomePageAnchor.Dashboard
        return anchorTypeLabel(anchor)
    }
}

package com.armutlu.apporganizer.utils

import android.content.Context
import com.armutlu.apporganizer.domain.models.NotificationBadgeMode
import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotificationSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Smart Notification Engine ayarları için AppPrefs'ten bağımsız küçük preference alanı.
 * Bildirim içeriği, paket adı veya kullanım geçmişi burada tutulmaz.
 */
object SmartNotificationPrefs {
    private const val FILE_NAME = "smart_notification_prefs"
    private const val KEY_INITIALIZED = "initialized_v1"
    private const val KEY_ENGINE_ENABLED = "engine_enabled"
    private const val KEY_FILTER_PROMOTIONS = "filter_promotions"
    private const val KEY_HIDE_SENSITIVE = "hide_sensitive_content"
    private const val KEY_VISIBLE_CATEGORIES = "visible_categories"
    private const val KEY_BADGE_MODE = "badge_mode"

    private val lock = Any()
    private val _settings = MutableStateFlow(SmartNotificationSettings.defaults(engineEnabled = false))
    val settings: StateFlow<SmartNotificationSettings> = _settings.asStateFlow()

    @Volatile
    private var loaded = false

    fun initialize(context: Context) {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
                val existingInstall = appContext
                    .getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
                    .all
                    .isNotEmpty()
                val initial = SmartNotificationSettings.defaults(
                    engineEnabled = initialEngineEnabled(existingInstall),
                )
                writeAll(prefs, initial, synchronous = true)
            }
            _settings.value = readFromDisk(prefs)
            loaded = true
        }
    }

    fun read(context: Context): SmartNotificationSettings {
        initialize(context)
        return _settings.value
    }

    fun setEngineEnabled(context: Context, enabled: Boolean) =
        update(context) { it.copy(engineEnabled = enabled) }

    fun setFilterPromotions(context: Context, enabled: Boolean) =
        update(context) { it.copy(filterPromotions = enabled) }

    fun setHideSensitiveContent(context: Context, enabled: Boolean) =
        update(context) { it.copy(hideSensitiveContent = enabled) }

    fun setVisibleCategories(context: Context, categories: Set<NotificationCategory>) =
        update(context) { it.copy(visibleCategories = categories.toSet()) }

    fun setBadgeMode(context: Context, mode: NotificationBadgeMode) =
        update(context) { it.copy(badgeMode = mode) }

    fun toBackupFields(context: Context): BackupFields {
        val current = read(context)
        return BackupFields(
            engineEnabled = current.engineEnabled,
            filterPromotions = current.filterPromotions,
            hideSensitiveContent = current.hideSensitiveContent,
            visibleCategoryNames = current.visibleCategories.map { it.name }.sorted(),
            badgeMode = current.badgeMode.name,
        )
    }

    fun restoreFromBackup(context: Context, fields: BackupFields) {
        val categories = fields.visibleCategoryNames
            .mapNotNull { name -> runCatching { NotificationCategory.valueOf(name) }.getOrNull() }
            .toSet()
        val badgeMode = runCatching { NotificationBadgeMode.valueOf(fields.badgeMode) }
            .getOrDefault(NotificationBadgeMode.CLASSIC_APP)
        update(context) {
            SmartNotificationSettings(
                engineEnabled = fields.engineEnabled,
                filterPromotions = fields.filterPromotions,
                hideSensitiveContent = fields.hideSensitiveContent,
                visibleCategories = categories,
                badgeMode = badgeMode,
            )
        }
    }

    internal fun initialEngineEnabled(existingInstall: Boolean): Boolean = !existingInstall

    private fun update(
        context: Context,
        transform: (SmartNotificationSettings) -> SmartNotificationSettings,
    ) {
        initialize(context)
        synchronized(lock) {
            val updated = transform(_settings.value)
            writeAll(
                context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
                updated,
                synchronous = false,
            )
            _settings.value = updated
        }
    }

    private fun readFromDisk(
        prefs: android.content.SharedPreferences,
    ): SmartNotificationSettings {
        val default = SmartNotificationSettings.defaults(engineEnabled = false)
        val storedCategoryNames = prefs.getStringSet(KEY_VISIBLE_CATEGORIES, null)
        val visibleCategories = if (storedCategoryNames == null) {
            default.visibleCategories
        } else {
            storedCategoryNames
                .mapNotNull { name -> runCatching { NotificationCategory.valueOf(name) }.getOrNull() }
                .toSet()
        }
        val badgeMode = prefs.getString(KEY_BADGE_MODE, null)
            ?.let { name -> runCatching { NotificationBadgeMode.valueOf(name) }.getOrNull() }
            ?: default.badgeMode
        return SmartNotificationSettings(
            engineEnabled = prefs.getBoolean(KEY_ENGINE_ENABLED, default.engineEnabled),
            filterPromotions = prefs.getBoolean(KEY_FILTER_PROMOTIONS, default.filterPromotions),
            hideSensitiveContent = prefs.getBoolean(KEY_HIDE_SENSITIVE, default.hideSensitiveContent),
            visibleCategories = visibleCategories,
            badgeMode = badgeMode,
        )
    }

    private fun writeAll(
        prefs: android.content.SharedPreferences,
        settings: SmartNotificationSettings,
        synchronous: Boolean,
    ) {
        val editor = prefs.edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putBoolean(KEY_ENGINE_ENABLED, settings.engineEnabled)
            .putBoolean(KEY_FILTER_PROMOTIONS, settings.filterPromotions)
            .putBoolean(KEY_HIDE_SENSITIVE, settings.hideSensitiveContent)
            .putStringSet(KEY_VISIBLE_CATEGORIES, settings.visibleCategories.map { it.name }.toSet())
            .putString(KEY_BADGE_MODE, settings.badgeMode.name)
        if (synchronous) editor.commit() else editor.apply()
    }

    data class BackupFields(
        val engineEnabled: Boolean,
        val filterPromotions: Boolean,
        val hideSensitiveContent: Boolean,
        val visibleCategoryNames: List<String>,
        val badgeMode: String,
    )
}

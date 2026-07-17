package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.data.repository.SearchRepository
import com.armutlu.apporganizer.workers.BackupWorker
import com.armutlu.apporganizer.workers.SmartInsightWorker
import com.armutlu.apporganizer.workers.WeeklyDigestWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private val dateFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private const val BACKUP_VERSION = 6

    /**
     * Uygulama kategori atamaları, ayarlar ve özelleştirmeleri JSON olarak dışa aktarır.
     * v3: dock, klasör özelleştirme, gesture, manuel override, gizlilik eklendi.
     */
    suspend fun exportToJson(context: Context, repository: AppRepository): String =
        withContext(Dispatchers.IO) {
            val apps = repository.getAllApps()
            val root = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("exportedAt", System.currentTimeMillis())

                // ── Uygulama listesi ──────────────────────────────────────────────
                put("apps", JSONArray().apply {
                    apps.forEach { app ->
                        put(JSONObject().apply {
                            put("packageName", app.packageName)
                            put("categoryId", app.categoryId)
                            put("isHidden", app.isHidden)
                            put("usageCount", app.usageCount)
                            put("launchCount", app.launchCount)
                            put("lastUsedTimestamp", app.lastUsedTimestamp)
                            put("notificationCount", app.notificationCount)
                            put("classificationSource", app.classificationSource)
                            put("classificationConfidence", app.classificationConfidence)
                            put("classificationReason", app.classificationReason)
                            put("classificationReviewState", app.classificationReviewState)
                            put("isCategoryLocked", app.isCategoryLocked)
                            put("classificationVersion", app.classificationVersion)
                            put("lastClassifiedAt", app.lastClassifiedAt)
                            put("lastReviewedAt", app.lastReviewedAt)
                            put("reviewSnoozedUntil", app.reviewSnoozedUntil)
                        })
                    }
                })

                // ── Dock ──────────────────────────────────────────────────────────
                put("dockPackages", JSONArray(DockPrefs.getDockPackages(context)))

                // ── Klasör özelleştirmeleri ───────────────────────────────────────
                put("folderCustomNames", JSONObject(AppPrefs.getFolderCustomNames(context)))
                put("folderCustomEmojis", JSONObject(AppPrefs.getFolderCustomEmojis(context)))
                put("folderCustomColors", JSONObject(AppPrefs.getFolderCustomColors(context)))

                // ── Manuel kategori ezmeler ───────────────────────────────────────
                put("manualCategoryOverrides", JSONObject(AppPrefs.getManualCategoryOverrides(context)))
                put("dismissedFolderSuggestions", JSONArray(AppPrefs.getDismissedFolderSuggestions(context).toList()))
                put("snoozedFolderSuggestions", JSONObject(AppPrefs.getSnoozedFolderSuggestions(context)))

                // ── Gesture aksiyonları ───────────────────────────────────────────
                put("gestures", JSONObject().apply {
                    put("doubleTap", AppPrefs.getGestureDoubleTap(context).name)
                    put("longPress", AppPrefs.getGestureLongPress(context).name)
                    put("swipeUp", AppPrefs.getGestureSwipeUp(context).name)
                })

                // ── Tema ve görünüm ayarları ──────────────────────────────────────
                put("settings", JSONObject().apply {
                    put("allAppsSortMode", AppPrefs.getAllAppsSortMode(context))
                    put("folderSortMode", AppPrefs.getFolderSortMode(context))
                    put("folderCarouselEnabled", AppPrefs.isFolderCarouselEnabled(context))
                    put("folderCarouselPosition", AppPrefs.getFolderCarouselPosition(context))
                    put("autoBackupEnabled", AppPrefs.isAutoBackupEnabled(context))
                    put("backupDayOfWeek", AppPrefs.getBackupDayOfWeek(context))
                    put("backupHour", AppPrefs.getBackupHour(context))
                    put("backupMinute", AppPrefs.getBackupMinute(context))
                    put("driveFolderUri", AppPrefs.getDriveFolderUri(context) ?: "")
                    put("iconPack", AppPrefs.getIconPack(context))
                    put("labelColor", AppPrefs.getLabelColor(context))
                    put("folderShape", AppPrefs.getFolderShape(context))
                    put("folderSizeDp", AppPrefs.getFolderSizeDp(context))
                    put("bgType", AppPrefs.getBgType(context))
                    put("bgColor", AppPrefs.getBgColor(context))
                    put("textAlpha", AppPrefs.getTextAlpha(context))
                    put("iconScale", AppPrefs.getIconScale(context))
                    put("pageSize", AppPrefs.getPageSize(context))
                    put("homeSearchEnabled", AppPrefs.isHomeSearchEnabled(context))
                    put("homeAppSearchEnabled", AppPrefs.isHomeAppSearchEnabled(context))
                    put("fullscreenSearchEnabled", AppPrefs.isFullscreenSearchEnabled(context))
                    put("folderSearchEnabled", AppPrefs.isFolderSearchEnabled(context))
                    put("tickerEnabled", AppPrefs.isTickerEnabled(context))
                    put("widgetAreaEnabled", AppPrefs.isWidgetAreaEnabled(context))
                    put("widgetAutoResizeEnabled", AppPrefs.isWidgetAutoResizeEnabled(context))
                    put("favoritesEnabled", AppPrefs.isFavoritesEnabled(context))
                    put("favoritesEnabledAllApps", AppPrefs.isFavoritesEnabledAllApps(context))
                    put("recentAppsEnabled", AppPrefs.isRecentAppsEnabled(context))
                    put("recentAppsEnabledAllApps", AppPrefs.isRecentAppsEnabledAllApps(context))
                    put("notifAnalyticsEnabled", AppPrefs.isNotifAnalyticsEnabled(context))
                    put("suggestionsEnabled", AppPrefs.isSuggestionsEnabled(context))
                    put("searchBarPosition", AppPrefs.getSearchBarPosition(context))
                    put("searchFuzzyEnabled", AppPrefs.isSearchFuzzyEnabled(context))
                    put("searchPhoneticEnabled", AppPrefs.isSearchPhoneticEnabled(context))
                    put("searchInstantEnabled", AppPrefs.isSearchInstantEnabled(context))
                    put("searchSortByUsage", AppPrefs.isSearchSortByUsage(context))
                    put("searchMaxResults", AppPrefs.getSearchMaxResults(context))
                    put("searchShowIcons", AppPrefs.isSearchShowIcons(context))
                    put("searchShowContactAvatar", AppPrefs.isSearchShowContactAvatar(context))
                    put("weeklyDigestEnabled", AppPrefs.isWeeklyDigestEnabled(context))
                    put("wrappedEnabled", AppPrefs.isWrappedEnabled(context))
                    put("wrappedAiCoachEnabled", AppPrefs.isWrappedAiCoachEnabled(context))
                    put("goalsEnabled", AppPrefs.isGoalsEnabled(context))
                    put("privacyReportEnabled", AppPrefs.isPrivacyReportEnabled(context))
                    put("contextualDockEnabled", AppPrefs.isContextualDockEnabled(context))
                    put("assistantCardsEnabled", AppPrefs.isAssistantCardsEnabled(context))
                    put("clockStyle", AppPrefs.getClockStyle(context))
                    put("folderTransitionEffect", AppPrefs.getFolderTransitionEffect(context))
                    put("homeScoreVisible", AppPrefs.isHomeScoreVisible(context))
                    put("digitalLifeCardVisible", AppPrefs.isDigitalLifeCardVisible(context))
                    put("homeInsightVisible", AppPrefs.isHomeInsightVisible(context))
                    put("homeUsageChartVisible", AppPrefs.isHomeUsageChartVisible(context))
                    put("missionsEnabled", AppPrefs.isMissionsEnabled(context))
                    put("homeWeatherEnabled", AppPrefs.isHomeWeatherEnabled(context))
                    put("homeWeatherUseLocation", AppPrefs.isHomeWeatherUseLocation(context))
                    put("homeWeatherManualCity", AppPrefs.getHomeWeatherManualCity(context))
                    put("searchShineEnabled", AppPrefs.isSearchShineEnabled(context))
                    put("autoFolderColorEnabled", AppPrefs.isAutoFolderColorEnabled(context))
                    put("biometricSettingsLockEnabled", AppPrefs.isBiometricSettingsLockEnabled(context))
                    put("quickWheelEnabled", AppPrefs.isQuickWheelEnabled(context))
                    put("focusModeEnabled", AppPrefs.isFocusModeEnabled(context))
                    put("searchWebFallbackEnabled", AppPrefs.isSearchWebFallbackEnabled(context))
                    put("searchStatsEnabled", AppPrefs.isSearchStatsEnabled(context))
                    put("searchSourceAppsEnabled", AppPrefs.isSearchSourceAppsEnabled(context))
                    put("searchSourceCategoriesEnabled", AppPrefs.isSearchSourceCategoriesEnabled(context))
                    put("searchSourceSettingsEnabled", AppPrefs.isSearchSourceSettingsEnabled(context))
                    put("searchSourceContactsEnabled", AppPrefs.isSearchSourceContactsEnabled(context))
                    put("searchSourceFilesEnabled", AppPrefs.isSearchSourceFilesEnabled(context))
                    put("smartNotifEnabled", AppPrefs.isSmartNotifEnabled(context))
                    put("smartNotifDailyUsage", AppPrefs.isSmartNotifDailyUsage(context))
                    put("smartNotifUnusedApps", AppPrefs.isSmartNotifUnusedApps(context))
                    put("smartNotifCatStats", AppPrefs.isSmartNotifCatStats(context))
                    put("smartNotifHour", AppPrefs.getSmartNotifHour(context))
                    // P0.6: tek siniflandirma modu — eski manufacturerClassifyEnabled alaninin yerine gecti
                    put("classificationMode", AppPrefs.getClassificationMode(context).name)
                })
                put("homeLayout", homeLayoutToJson(HomeLayoutPrefs.read(context)))
            }
            root.toString(2)
        }

    // Geriye dönük uyumluluk: context gerektirmeyen eski imza (v2)
    suspend fun exportToJson(repository: AppRepository): String =
        withContext(Dispatchers.IO) {
            val apps = repository.getAllApps()
            JSONObject().apply {
                put("version", 2)
                put("exportedAt", System.currentTimeMillis())
                put("apps", JSONArray().apply {
                    apps.forEach { app ->
                        put(JSONObject().apply {
                            put("packageName", app.packageName)
                            put("categoryId", app.categoryId)
                            put("isHidden", app.isHidden)
                            put("usageCount", app.usageCount)
                            put("launchCount", app.launchCount)
                            put("lastUsedTimestamp", app.lastUsedTimestamp)
                            put("notificationCount", app.notificationCount)
                        })
                    }
                })
            }.toString(2)
        }

    /** JSON dosyasını dışa aktarır ve paylaşım intent'i döner. */
    suspend fun exportAndShare(context: Context, repository: AppRepository): Intent? =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = exportToJson(context, repository)
                val fileName = "apporganizer_backup_${dateFmt.format(Date())}.json"
                val file = File(context.cacheDir, fileName)
                file.writeText(json, Charsets.UTF_8)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "AppOrganizer Yedeği v$BACKUP_VERSION")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }.getOrElse { e ->
                Timber.e(e, "Export failed")
                null
            }
        }

    /** JSON içeriğini içe aktarır — tüm ayarları geri yükler. */
    suspend fun importFromJson(
        context: Context,
        json: String,
        repository: AppRepository,
        searchRepository: SearchRepository? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(json)
            val version = root.optInt("version", 1)
            val appsArray = root.optJSONArray("apps")
            var updated = 0
            val missing = mutableListOf<String>()

            // ── Uygulama listesi ──────────────────────────────────────────────
            if (appsArray != null) {
                val currentApps = repository.getAllApps()
                val currentByPackage = currentApps.associateBy { it.packageName }
                for (i in 0 until appsArray.length()) {
                    val obj = appsArray.getJSONObject(i)
                    val pkg = obj.getString("packageName")
                    val cat = obj.optString("categoryId", "uncategorized")
                    val hidden = obj.optBoolean("isHidden", false)
                    val current = currentByPackage[pkg] ?: repository.getAppByPackageName(pkg)
                    if (current != null) {
                        if (version >= 4 || obj.has("classificationSource")) {
                            repository.restoreClassificationFromBackup(
                                current.copy(
                                    categoryId = cat,
                                    classificationSource = obj.optString("classificationSource", current.classificationSource),
                                    classificationConfidence = obj.optInt("classificationConfidence", current.classificationConfidence),
                                    classificationReason = obj.optString("classificationReason", current.classificationReason),
                                    classificationReviewState = obj.optString("classificationReviewState", current.classificationReviewState),
                                    isCategoryLocked = obj.optBoolean("isCategoryLocked", current.isCategoryLocked),
                                    classificationVersion = obj.optInt("classificationVersion", current.classificationVersion),
                                    lastClassifiedAt = obj.optLong("lastClassifiedAt", current.lastClassifiedAt),
                                    lastReviewedAt = obj.optLong("lastReviewedAt", current.lastReviewedAt),
                                    reviewSnoozedUntil = obj.optLong("reviewSnoozedUntil", current.reviewSnoozedUntil),
                                ),
                                cat
                            )
                        } else {
                            repository.updateAppCategory(pkg, cat)
                        }
                        repository.updateAppHidden(pkg, hidden)
                        val usageCount = obj.optLong("usageCount", 0L)   // ms (eski yedekler de ms tutar)
                        val launchCount = obj.optLong("launchCount", 0L)
                        val lastUsed   = obj.optLong("lastUsedTimestamp", 0L)
                        val notificationCount = obj.optInt("notificationCount", 0)
                        if (obj.has("usageCount")) repository.updateUsageTimeMs(pkg, usageCount)
                        if (obj.has("launchCount")) repository.updateLaunchCount(pkg, launchCount)
                        if (obj.has("lastUsedTimestamp")) repository.updateLastUsedTimestamp(pkg, lastUsed)
                        repository.updateNotificationCount(pkg, notificationCount)
                        updated++
                    } else {
                        missing.add(pkg)
                    }
                }
            }

            if (version >= 3) {
                DockPrefs.saveDockPackages(context, emptyList())
                AppPrefs.clearFolderCustomizations(context)
                AppPrefs.clearManualCategoryOverrides(context)
                // ── Dock ──────────────────────────────────────────────────────
                root.optJSONArray("dockPackages")?.let { arr ->
                    val pkgs = (0 until arr.length()).map { arr.getString(it) }
                    DockPrefs.saveDockPackages(context, pkgs)
                }

                // ── Klasör özelleştirmeleri ───────────────────────────────────
                root.optJSONObject("folderCustomNames")?.let { obj ->
                    obj.keys().forEach { k -> AppPrefs.setFolderCustomName(context, k, obj.getString(k)) }
                }
                root.optJSONObject("folderCustomEmojis")?.let { obj ->
                    obj.keys().forEach { k -> AppPrefs.setFolderCustomEmoji(context, k, obj.getString(k)) }
                }
                root.optJSONObject("folderCustomColors")?.let { obj ->
                    obj.keys().forEach { k -> AppPrefs.setFolderCustomColor(context, k, obj.getString(k)) }
                }

                // ── Manuel kategori ezmeler ───────────────────────────────────
                root.optJSONObject("manualCategoryOverrides")?.let { obj ->
                    obj.keys().forEach { k -> AppPrefs.setManualCategoryOverride(context, k, obj.getString(k)) }
                }
                root.optJSONArray("dismissedFolderSuggestions")?.let { arr ->
                    AppPrefs.setDismissedFolderSuggestions(
                        context,
                        (0 until arr.length()).map { arr.getString(it) }.toSet()
                    )
                }
                root.optJSONObject("snoozedFolderSuggestions")?.let { obj ->
                    AppPrefs.setSnoozedFolderSuggestions(
                        context,
                        obj.keys().asSequence().associateWith { obj.getString(it) }
                    )
                }

                // ── Gesture aksiyonları ───────────────────────────────────────
                root.optJSONObject("gestures")?.let { gestures ->
                    runCatching { AppPrefs.GestureAction.valueOf(gestures.optString("doubleTap")) }
                        .getOrNull()?.let { AppPrefs.setGestureDoubleTap(context, it) }
                    runCatching { AppPrefs.GestureAction.valueOf(gestures.optString("longPress")) }
                        .getOrNull()?.let { AppPrefs.setGestureLongPress(context, it) }
                    runCatching { AppPrefs.GestureAction.valueOf(gestures.optString("swipeUp")) }
                        .getOrNull()?.let { AppPrefs.setGestureSwipeUp(context, it) }
                }

                // ── Tema/görünüm ayarları ─────────────────────────────────────
                root.optJSONObject("settings")?.let { s ->
                    s.optString("allAppsSortMode").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setAllAppsSortMode(context, it) }
                    s.optString("folderSortMode").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setFolderSortMode(context, it) }
                    if (s.has("folderCarouselEnabled")) AppPrefs.setFolderCarouselEnabled(context, s.getBoolean("folderCarouselEnabled"))
                    s.optString("folderCarouselPosition").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setFolderCarouselPosition(context, it) }
                    if (s.has("autoBackupEnabled")) AppPrefs.setAutoBackupEnabled(context, s.getBoolean("autoBackupEnabled"))
                    if (s.has("backupDayOfWeek")) AppPrefs.setBackupDayOfWeek(context, s.getInt("backupDayOfWeek"))
                    if (s.has("backupHour")) AppPrefs.setBackupHour(context, s.getInt("backupHour"))
                    if (s.has("backupMinute")) AppPrefs.setBackupMinute(context, s.getInt("backupMinute"))
                    if (s.has("driveFolderUri")) {
                        AppPrefs.setDriveFolderUri(context, s.optString("driveFolderUri").takeIf { it.isNotBlank() })
                    }
                    s.optString("iconPack").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setIconPack(context, it) }
                    s.optString("labelColor").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setLabelColor(context, it) }
                    s.optString("folderShape").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setFolderShape(context, it) }
                    if (s.has("folderSizeDp")) AppPrefs.setFolderSizeDp(context, s.getInt("folderSizeDp"))
                    s.optString("bgType").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setBgType(context, it) }
                    if (s.has("bgColor")) AppPrefs.setBgColor(context, s.getInt("bgColor"))
                    if (s.has("textAlpha")) AppPrefs.setTextAlpha(context, s.getDouble("textAlpha").toFloat())
                    if (s.has("iconScale")) AppPrefs.setIconScale(context, s.getDouble("iconScale").toFloat())
                    if (s.has("pageSize")) AppPrefs.setPageSize(context, s.getInt("pageSize"))
                    if (s.has("homeSearchEnabled")) AppPrefs.setHomeSearchEnabled(context, s.getBoolean("homeSearchEnabled"))
                    if (s.has("homeAppSearchEnabled")) AppPrefs.setHomeAppSearchEnabled(context, s.getBoolean("homeAppSearchEnabled"))
                    if (s.has("fullscreenSearchEnabled")) AppPrefs.setFullscreenSearchEnabled(context, s.getBoolean("fullscreenSearchEnabled"))
                    if (s.has("folderSearchEnabled")) AppPrefs.setFolderSearchEnabled(context, s.getBoolean("folderSearchEnabled"))
                    if (s.has("tickerEnabled")) AppPrefs.setTickerEnabled(context, s.getBoolean("tickerEnabled"))
                    if (s.has("widgetAreaEnabled")) AppPrefs.setWidgetAreaEnabled(context, s.getBoolean("widgetAreaEnabled"))
                    AppPrefs.clearLegacyFolderBlurPreference(context)
                    if (s.has("widgetAutoResizeEnabled")) AppPrefs.setWidgetAutoResizeEnabled(context, s.getBoolean("widgetAutoResizeEnabled"))
                    if (s.has("favoritesEnabled")) AppPrefs.setFavoritesEnabled(context, s.getBoolean("favoritesEnabled"))
                    if (s.has("favoritesEnabledAllApps")) AppPrefs.setFavoritesEnabledAllApps(context, s.getBoolean("favoritesEnabledAllApps"))
                    if (s.has("recentAppsEnabled")) AppPrefs.setRecentAppsEnabled(context, s.getBoolean("recentAppsEnabled"))
                    if (s.has("recentAppsEnabledAllApps")) AppPrefs.setRecentAppsEnabledAllApps(context, s.getBoolean("recentAppsEnabledAllApps"))
                    if (s.has("notifAnalyticsEnabled")) AppPrefs.setNotifAnalyticsEnabled(context, s.getBoolean("notifAnalyticsEnabled"))
                    if (s.has("suggestionsEnabled")) AppPrefs.setSuggestionsEnabled(context, s.getBoolean("suggestionsEnabled"))
                    s.optString("searchBarPosition").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setSearchBarPosition(context, it) }
                    if (s.has("searchFuzzyEnabled")) AppPrefs.setSearchFuzzyEnabled(context, s.getBoolean("searchFuzzyEnabled"))
                    if (s.has("searchPhoneticEnabled")) AppPrefs.setSearchPhoneticEnabled(context, s.getBoolean("searchPhoneticEnabled"))
                    if (s.has("searchInstantEnabled")) AppPrefs.setSearchInstantEnabled(context, s.getBoolean("searchInstantEnabled"))
                    if (s.has("searchSortByUsage")) AppPrefs.setSearchSortByUsage(context, s.getBoolean("searchSortByUsage"))
                    if (s.has("searchMaxResults")) AppPrefs.setSearchMaxResults(context, s.getInt("searchMaxResults"))
                    if (s.has("searchShowIcons")) AppPrefs.setSearchShowIcons(context, s.getBoolean("searchShowIcons"))
                    if (s.has("searchShowContactAvatar")) AppPrefs.setSearchShowContactAvatar(context, s.getBoolean("searchShowContactAvatar"))
                    if (s.has("weeklyDigestEnabled")) AppPrefs.setWeeklyDigestEnabled(context, s.getBoolean("weeklyDigestEnabled"))
                    if (s.has("wrappedEnabled")) AppPrefs.setWrappedEnabled(context, s.getBoolean("wrappedEnabled"))
                    if (s.has("wrappedAiCoachEnabled")) AppPrefs.setWrappedAiCoachEnabled(context, s.getBoolean("wrappedAiCoachEnabled"))
                    if (s.has("goalsEnabled")) AppPrefs.setGoalsEnabled(context, s.getBoolean("goalsEnabled"))
                    if (s.has("privacyReportEnabled")) AppPrefs.setPrivacyReportEnabled(context, s.getBoolean("privacyReportEnabled"))
                    if (s.has("contextualDockEnabled")) AppPrefs.setContextualDockEnabled(context, s.getBoolean("contextualDockEnabled"))
                    if (s.has("assistantCardsEnabled")) AppPrefs.setAssistantCardsEnabled(context, s.getBoolean("assistantCardsEnabled"))
                    s.optString("clockStyle").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setClockStyle(context, it) }
                    s.optString("folderTransitionEffect").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setFolderTransitionEffect(context, it) }
                    if (s.has("homeScoreVisible")) AppPrefs.setHomeScoreVisible(context, s.getBoolean("homeScoreVisible"))
                    if (s.has("digitalLifeCardVisible")) AppPrefs.setDigitalLifeCardVisible(context, s.getBoolean("digitalLifeCardVisible"))
                    if (s.has("homeInsightVisible")) AppPrefs.setHomeInsightVisible(context, s.getBoolean("homeInsightVisible"))
                    if (s.has("homeUsageChartVisible")) AppPrefs.setHomeUsageChartVisible(context, s.getBoolean("homeUsageChartVisible"))
                    if (s.has("missionsEnabled")) AppPrefs.setMissionsEnabled(context, s.getBoolean("missionsEnabled"))
                    if (s.has("homeWeatherEnabled")) AppPrefs.setHomeWeatherEnabled(context, s.getBoolean("homeWeatherEnabled"))
                    if (s.has("homeWeatherUseLocation")) AppPrefs.setHomeWeatherUseLocation(context, s.getBoolean("homeWeatherUseLocation"))
                    if (s.has("homeWeatherManualCity")) AppPrefs.setHomeWeatherManualCity(context, s.optString("homeWeatherManualCity"))
                    if (s.has("searchShineEnabled")) AppPrefs.setSearchShineEnabled(context, s.getBoolean("searchShineEnabled"))
                    if (s.has("autoFolderColorEnabled")) AppPrefs.setAutoFolderColorEnabled(context, s.getBoolean("autoFolderColorEnabled"))
                    if (s.has("biometricSettingsLockEnabled")) AppPrefs.setBiometricSettingsLockEnabled(context, s.getBoolean("biometricSettingsLockEnabled"))
                    if (s.has("quickWheelEnabled")) AppPrefs.setQuickWheelEnabled(context, s.getBoolean("quickWheelEnabled"))
                    if (s.has("focusModeEnabled")) AppPrefs.setFocusModeEnabled(context, s.getBoolean("focusModeEnabled"))
                    if (s.has("searchWebFallbackEnabled")) AppPrefs.setSearchWebFallbackEnabled(context, s.getBoolean("searchWebFallbackEnabled"))
                    if (s.has("searchStatsEnabled")) AppPrefs.setSearchStatsEnabled(context, s.getBoolean("searchStatsEnabled"))
                    if (s.has("searchSourceAppsEnabled")) AppPrefs.setSearchSourceAppsEnabled(context, s.getBoolean("searchSourceAppsEnabled"))
                    if (s.has("searchSourceCategoriesEnabled")) AppPrefs.setSearchSourceCategoriesEnabled(context, s.getBoolean("searchSourceCategoriesEnabled"))
                    if (s.has("searchSourceSettingsEnabled")) AppPrefs.setSearchSourceSettingsEnabled(context, s.getBoolean("searchSourceSettingsEnabled"))
                    if (s.has("searchSourceContactsEnabled")) AppPrefs.setSearchSourceContactsEnabled(context, s.getBoolean("searchSourceContactsEnabled"))
                    if (s.has("searchSourceFilesEnabled")) AppPrefs.setSearchSourceFilesEnabled(context, s.getBoolean("searchSourceFilesEnabled"))
                    if (s.has("smartNotifEnabled")) AppPrefs.setSmartNotifEnabled(context, s.getBoolean("smartNotifEnabled"))
                    if (s.has("smartNotifDailyUsage")) AppPrefs.setSmartNotifDailyUsage(context, s.getBoolean("smartNotifDailyUsage"))
                    if (s.has("smartNotifUnusedApps")) AppPrefs.setSmartNotifUnusedApps(context, s.getBoolean("smartNotifUnusedApps"))
                    if (s.has("smartNotifCatStats")) AppPrefs.setSmartNotifCatStats(context, s.getBoolean("smartNotifCatStats"))
                    if (s.has("smartNotifHour")) AppPrefs.setSmartNotifHour(context, s.getInt("smartNotifHour"))

                    // P0.6: siniflandirma modu — yeni alan varsa dogrudan uygula, yoksa eski
                    // toggle alanlarindan (manufacturerClassifyEnabled / DeepSeek key varligi)
                    // migration kurali ile turet. Boylece eski yedekler geri yuklendiginde
                    // kullanici davranisi degismez.
                    val restoredMode = s.optString("classificationMode", "").takeIf { it.isNotBlank() }
                        ?.let { modeName -> runCatching { AppPrefs.ClassificationMode.valueOf(modeName) }.getOrNull() }
                    if (restoredMode != null) {
                        AppPrefs.setClassificationMode(context, restoredMode)
                    } else {
                        val manufacturerEnabled = if (s.has("manufacturerClassifyEnabled")) {
                            s.getBoolean("manufacturerClassifyEnabled")
                        } else {
                            AppPrefs.isManufacturerClassifyEnabled(context)
                        }
                        val llmEverUsed = AppPrefs.getDeepSeekApiKey(context).isNotBlank() ||
                            AppPrefs.getLlmCategoryCache(context).isNotEmpty()
                        AppPrefs.setClassificationMode(
                            context,
                            AppPrefs.deriveClassificationMode(manufacturerEnabled, llmEverUsed)
                        )
                    }
                }
                BackupWorker.schedule(context)
                WeeklyDigestWorker.schedule(context)
                SmartInsightWorker.schedule(context)
                syncSearchSourcesAfterRestore(context, searchRepository)
            }

            root.optJSONObject("homeLayout")?.let { layout ->
                HomeLayoutPrefs.write(context, homeLayoutFromJson(layout))
            } ?: HomeLayoutPrefs.read(context)

            ImportResult(success = true, updatedCount = updated, missingPackages = missing, restoredVersion = version)
        }.getOrElse { e ->
            Timber.e(e, "Import failed")
            ImportResult(success = false, error = e.message)
        }
    }

    internal fun homeLayoutToJson(state: HomeLayoutPrefs.State): JSONObject {
        val fields = HomeLayoutPrefs.toBackupFields(state)
        return JSONObject().apply {
            put("homeLayoutVersion", fields.version)
            put("homeHeaderOrder", fields.headerOrder)
            put("homeFooterOrder", fields.footerOrder)
            put("homeHiddenSections", fields.hiddenSections)
            put("homeLayoutCustomized", fields.customized)
        }
    }

    internal fun homeLayoutFromJson(json: JSONObject): HomeLayoutPrefs.State =
        HomeLayoutPrefs.fromBackupFields(
            HomeLayoutPrefs.BackupFields(
                version = json.optInt("homeLayoutVersion", 0),
                headerOrder = json.opt("homeHeaderOrder") as? String,
                footerOrder = json.opt("homeFooterOrder") as? String,
                hiddenSections = json.opt("homeHiddenSections") as? String,
                customized = json.opt("homeLayoutCustomized") as? Boolean,
            )
        )

    // Geriye dönük uyumluluk: context gerektirmeyen eski imza
    suspend fun importFromJson(json: String, repository: AppRepository): ImportResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = JSONObject(json)
                val appsArray = root.getJSONArray("apps")
                var updated = 0
                val missing = mutableListOf<String>()
                for (i in 0 until appsArray.length()) {
                    val obj = appsArray.getJSONObject(i)
                    val pkg = obj.getString("packageName")
                    val cat = obj.optString("categoryId", "uncategorized")
                    val hidden = obj.optBoolean("isHidden", false)
                    if (repository.appExists(pkg)) {
                        repository.updateAppCategory(pkg, cat)
                        repository.updateAppHidden(pkg, hidden)
                        val usageCount = obj.optLong("usageCount", 0L)   // ms (eski yedekler de ms tutar)
                        val launchCount = obj.optLong("launchCount", 0L)
                        val lastUsed   = obj.optLong("lastUsedTimestamp", 0L)
                        if (obj.has("usageCount")) repository.updateUsageTimeMs(pkg, usageCount)
                        if (obj.has("launchCount")) repository.updateLaunchCount(pkg, launchCount)
                        if (obj.has("lastUsedTimestamp")) repository.updateLastUsedTimestamp(pkg, lastUsed)
                        updated++
                    } else {
                        missing.add(pkg)
                    }
                }
                ImportResult(success = true, updatedCount = updated, missingPackages = missing)
            }.getOrElse { e ->
                Timber.e(e, "Import failed")
                ImportResult(success = false, error = e.message)
            }
        }

    private suspend fun syncSearchSourcesAfterRestore(
        context: Context,
        searchRepository: SearchRepository?
    ) {
        if (searchRepository == null) {
            Timber.w("Backup restore: SearchRepository yok, arama kaynak lifecycle senkronu atlandi")
            return
        }
        if (AppPrefs.isSearchSourceContactsEnabled(context)) {
            searchRepository.enableContactsSource()
        } else {
            searchRepository.disableContactsSource()
        }
        if (AppPrefs.isSearchSourceFilesEnabled(context)) {
            searchRepository.enableFilesSource()
        } else {
            searchRepository.disableFilesSource()
        }
    }

    data class ImportResult(
        val success: Boolean,
        val updatedCount: Int = 0,
        val error: String? = null,
        val missingPackages: List<String> = emptyList(),
        val restoredVersion: Int = 0
    )
}

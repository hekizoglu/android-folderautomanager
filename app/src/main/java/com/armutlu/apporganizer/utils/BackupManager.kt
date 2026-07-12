package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.armutlu.apporganizer.data.repository.AppRepository
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
    private const val BACKUP_VERSION = 3

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
                    put("iconPack", AppPrefs.getIconPack(context))
                    put("labelColor", AppPrefs.getLabelColor(context))
                    put("folderShape", AppPrefs.getFolderShape(context))
                    put("folderSizeDp", AppPrefs.getFolderSizeDp(context))
                    put("bgType", AppPrefs.getBgType(context))
                    put("contextualDockEnabled", AppPrefs.isContextualDockEnabled(context))
                    put("assistantCardsEnabled", AppPrefs.isAssistantCardsEnabled(context))
                })
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
        repository: AppRepository
    ): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(json)
            val version = root.optInt("version", 1)
            val appsArray = root.optJSONArray("apps")
            var updated = 0
            val missing = mutableListOf<String>()

            // ── Uygulama listesi ──────────────────────────────────────────────
            if (appsArray != null) {
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
                        if (usageCount > 0) repository.updateUsageTimeMs(pkg, usageCount)
                        if (launchCount > 0) repository.updateLaunchCount(pkg, launchCount)
                        if (lastUsed > 0)   repository.updateLastUsedTimestamp(pkg, lastUsed)
                        updated++
                    } else {
                        missing.add(pkg)
                    }
                }
            }

            if (version >= 3) {
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
                    s.optString("iconPack").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setIconPack(context, it) }
                    s.optString("labelColor").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setLabelColor(context, it) }
                    s.optString("folderShape").takeIf { it.isNotEmpty() }
                        ?.let { AppPrefs.setFolderShape(context, it) }
                    if (s.has("folderSizeDp")) AppPrefs.setFolderSizeDp(context, s.getInt("folderSizeDp"))
                    if (s.has("contextualDockEnabled")) AppPrefs.setContextualDockEnabled(context, s.getBoolean("contextualDockEnabled"))
                    if (s.has("assistantCardsEnabled")) AppPrefs.setAssistantCardsEnabled(context, s.getBoolean("assistantCardsEnabled"))
                }
            }

            ImportResult(success = true, updatedCount = updated, missingPackages = missing, restoredVersion = version)
        }.getOrElse { e ->
            Timber.e(e, "Import failed")
            ImportResult(success = false, error = e.message)
        }
    }

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
                        if (usageCount > 0) repository.updateUsageTimeMs(pkg, usageCount)
                        if (launchCount > 0) repository.updateLaunchCount(pkg, launchCount)
                        if (lastUsed > 0)   repository.updateLastUsedTimestamp(pkg, lastUsed)
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

    data class ImportResult(
        val success: Boolean,
        val updatedCount: Int = 0,
        val error: String? = null,
        val missingPackages: List<String> = emptyList(),
        val restoredVersion: Int = 0
    )
}

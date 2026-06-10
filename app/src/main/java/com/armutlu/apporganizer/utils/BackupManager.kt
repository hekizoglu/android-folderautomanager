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

    /** Tüm uygulama kategori atamalarını JSON olarak dışa aktarır. */
    suspend fun exportToJson(repository: AppRepository): String = withContext(Dispatchers.IO) {
        val apps = repository.getAllApps()
        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("apps", JSONArray().apply {
                apps.forEach { app ->
                    put(JSONObject().apply {
                        put("packageName", app.packageName)
                        put("categoryId", app.categoryId)
                        put("isHidden", app.isHidden)
                    })
                }
            })
            put("dockOrder", JSONArray().apply {
                // DockPrefs is context-dependent, added separately
            })
        }
        root.toString(2)
    }

    /** JSON dosyasını dışa aktarır ve paylaşım intent'i döner. */
    suspend fun exportAndShare(context: Context, repository: AppRepository): Intent? =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = exportToJson(repository)
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
                    putExtra(Intent.EXTRA_SUBJECT, "AppOrganizer Yedeği")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }.getOrElse { e ->
                Timber.e(e, "Export failed")
                null
            }
        }

    /** JSON içeriğini içe aktarır — kategori atamaları ve gizleme durumunu geri yükler. */
    suspend fun importFromJson(json: String, repository: AppRepository): ImportResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = JSONObject(json)
                val appsArray = root.getJSONArray("apps")
                var updated = 0
                for (i in 0 until appsArray.length()) {
                    val obj = appsArray.getJSONObject(i)
                    val pkg = obj.getString("packageName")
                    val cat = obj.optString("categoryId", "uncategorized")
                    val hidden = obj.optBoolean("isHidden", false)
                    if (repository.appExists(pkg)) {
                        repository.updateAppCategory(pkg, cat)
                        repository.updateAppHidden(pkg, hidden)
                        updated++
                    }
                }
                ImportResult(success = true, updatedCount = updated)
            }.getOrElse { e ->
                Timber.e(e, "Import failed")
                ImportResult(success = false, error = e.message)
            }
        }

    data class ImportResult(
        val success: Boolean,
        val updatedCount: Int = 0,
        val error: String? = null
    )
}

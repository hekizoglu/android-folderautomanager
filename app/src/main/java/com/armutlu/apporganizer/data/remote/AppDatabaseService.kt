package com.armutlu.apporganizer.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val DB_URL = "https://raw.githubusercontent.com/hekizoglu/android-folderautomanager/main/app_database.json"
private const val PREFS_NAME = "app_db_cache"
private const val KEY_DB_JSON = "db_json"
private const val KEY_DB_VERSION = "db_version"

@Singleton
class AppDatabaseService @Inject constructor(
    private val context: Context
) {
    private var cachedMap: Map<String, String>? = null

    /**
     * Paket adına göre Play Store kategorisini döndürür.
     * Önce bellekte, sonra SharedPrefs cache'inde arar.
     * Bulamazsa null döner (classifier kendi keyword mantığını kullanır).
     */
    fun getCategoryForPackage(packageName: String): String? {
        return cachedMap?.get(packageName)
    }

    /**
     * Veritabanını GitHub'dan indir ve cache'e kaydet.
     * Uygulama açılışında bir kez çağrılır.
     */
    suspend fun fetchAndCache(): FetchResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("AppDatabase: indirme başlıyor — $DB_URL")
            val json = URL(DB_URL).readText(Charsets.UTF_8)
            val obj  = JSONObject(json)
            val version = obj.optInt("version", 0)
            val appsObj = obj.getJSONObject("apps")

            val map = mutableMapOf<String, String>()
            appsObj.keys().forEach { pkg -> map[pkg] = appsObj.getString(pkg) }

            // Mevcut bellekte daha fazla uygulama varsa (assets'ten yüklendi) merge et
            val existing = cachedMap
            val merged = if (existing != null && existing.size > map.size) {
                Timber.d("AppDatabase: GitHub v$version (${map.size}) < assets (${existing.size}) — merge ediliyor")
                existing.toMutableMap().apply { putAll(map) }
            } else {
                map
            }

            cachedMap = merged

            // SharedPrefs'e kaydet (merge edilmiş versiyon)
            val mergedJson = JSONObject().apply {
                put("version", version)
                put("updated", obj.optString("updated", ""))
                put("apps", JSONObject().apply { merged.forEach { (k, v) -> put(k, v) } })
            }.toString()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_DB_JSON, mergedJson)
                .putInt(KEY_DB_VERSION, version)
                .apply()

            Timber.d("AppDatabase: ${merged.size} uygulama hazır (GitHub: ${map.size}, v$version)")
            FetchResult.Success(merged.size, version)
        } catch (e: Exception) {
            Timber.d("AppDatabase: indirme başarısız (${e.javaClass.simpleName}), cache deneniyor")
            val cacheResult = loadFromCache()
            if (cacheResult is FetchResult.NoCache || cacheResult is FetchResult.Error) {
                loadFromAssets()
                FetchResult.FromCache(cachedMap?.size ?: 0, 0)
            } else {
                cacheResult
            }
        }
    }

    /**
     * Uygulama açılışında cache'den yükle (internet yoksa da çalışır).
     * Cache yoksa assets/app_database.json'dan yükler.
     */
    fun loadFromCacheSync() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json  = prefs.getString(KEY_DB_JSON, null)
            if (json != null) {
                val obj = JSONObject(json)
                val appsObj = obj.getJSONObject("apps")
                val map = mutableMapOf<String, String>()
                appsObj.keys().forEach { pkg -> map[pkg] = appsObj.getString(pkg) }
                cachedMap = map
                Timber.d("AppDatabase: cache'den ${map.size} uygulama yüklendi")
            } else {
                loadFromAssets()
            }
        } catch (e: Exception) {
            Timber.w(e, "AppDatabase: cache yüklenemedi, assets deneniyor")
            loadFromAssets()
        }
    }

    private fun loadFromAssets() {
        try {
            val json = context.assets.open("app_database.json").bufferedReader().readText()
            val obj = JSONObject(json)
            val appsObj = obj.getJSONObject("apps")
            val map = mutableMapOf<String, String>()
            appsObj.keys().forEach { pkg -> map[pkg] = appsObj.getString(pkg) }
            cachedMap = map
            Timber.d("AppDatabase: assets'ten ${map.size} uygulama yüklendi")
        } catch (e: Exception) {
            Timber.d("AppDatabase: assets/app_database.json bulunamadı — sadece AppClassifier kullanılacak")
        }
    }

    private fun loadFromCache(): FetchResult {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json  = prefs.getString(KEY_DB_JSON, null)
                ?: return FetchResult.NoCache
            val obj   = JSONObject(json)
            val version = obj.optInt("version", 0)
            val appsObj = obj.getJSONObject("apps")
            val map = mutableMapOf<String, String>()
            appsObj.keys().forEach { pkg -> map[pkg] = appsObj.getString(pkg) }
            cachedMap = map
            Timber.d("AppDatabase: cache'den ${map.size} uygulama yüklendi")
            FetchResult.FromCache(map.size, version)
        } catch (e: Exception) {
            Timber.e(e, "AppDatabase: cache de başarısız")
            FetchResult.Error(e.message ?: "bilinmeyen hata")
        }
    }

    val isLoaded: Boolean get() = cachedMap != null
    val size: Int get() = cachedMap?.size ?: 0
}

sealed class FetchResult {
    data class Success(val count: Int, val version: Int) : FetchResult()
    data class FromCache(val count: Int, val version: Int) : FetchResult()
    data class Error(val message: String) : FetchResult()
    object NoCache : FetchResult()
}

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

            // Belleğe al
            cachedMap = map

            // SharedPrefs'e kaydet
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_DB_JSON, json)
                .putInt(KEY_DB_VERSION, version)
                .apply()

            Timber.d("AppDatabase: ${map.size} uygulama indirildi (v$version)")
            FetchResult.Success(map.size, version)
        } catch (e: Exception) {
            Timber.w(e, "AppDatabase: indirme başarısız, cache deneniyor")
            loadFromCache()
        }
    }

    /**
     * Uygulama açılışında cache'den yükle (internet yoksa da çalışır).
     */
    fun loadFromCacheSync() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json  = prefs.getString(KEY_DB_JSON, null) ?: return
            val obj   = JSONObject(json)
            val appsObj = obj.getJSONObject("apps")
            val map = mutableMapOf<String, String>()
            appsObj.keys().forEach { pkg -> map[pkg] = appsObj.getString(pkg) }
            cachedMap = map
            Timber.d("AppDatabase: cache'den ${map.size} uygulama yüklendi")
        } catch (e: Exception) {
            Timber.w(e, "AppDatabase: cache yüklenemedi")
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

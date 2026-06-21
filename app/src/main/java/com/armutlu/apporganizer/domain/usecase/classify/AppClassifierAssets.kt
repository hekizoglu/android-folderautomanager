package com.armutlu.apporganizer.domain.usecase.classify

import android.content.Context
import org.json.JSONObject

/**
 * app/src/main/assets/app_categories.json'u okur ve paket→kategoriId eşlemesini döner.
 * İlk çağrıda lazy parse edilir; sonraki çağrılarda bellek içi map kullanılır.
 * Thread-safe: @Volatile + synchronized çift kontrol.
 */
object AppClassifierAssets {

    @Volatile
    private var cachedMap: Map<String, String>? = null

    fun getExactMatchMap(context: Context): Map<String, String> {
        cachedMap?.let { return it }
        return synchronized(this) {
            cachedMap ?: loadFromAssets(context).also { cachedMap = it }
        }
    }

    private fun loadFromAssets(context: Context): Map<String, String> {
        val json = context.assets.open("app_categories.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val obj = JSONObject(json)
        val map = HashMap<String, String>(obj.length() * 2)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val pkg = keys.next()
            map[pkg] = obj.getString(pkg)
        }
        return map
    }
}

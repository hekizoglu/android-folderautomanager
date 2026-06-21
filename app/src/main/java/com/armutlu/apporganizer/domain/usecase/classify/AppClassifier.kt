package com.armutlu.apporganizer.domain.usecase.classify

import android.content.Context
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Üretici bazlı sınıflandırma toggle — AppListViewModel tarafından set edilir
    // @Volatile: farklı thread'lerden (IO/Main) okunup yazılabileceği için gerekli
    @Volatile
    var manufacturerClassifyEnabled: Boolean = true

    // Üretici prefix → üretici kategorisi: exactMap'ten sonra, keyword'den önce kontrol edilir
    // manufacturerClassifyEnabled=false iken hiç çağrılmaz (classifyApp() içinde guard var)
    private val MANUFACTURER_PREFIX_MAP = mapOf(
        // Google
        "com.google"                            to Category.CAT_GOOGLE,
        "com.android.google"                    to Category.CAT_GOOGLE,
        // Samsung
        "com.samsung"                           to Category.CAT_SAMSUNG,
        "com.sec.android"                       to Category.CAT_SAMSUNG,
        // Microsoft
        "com.microsoft"                         to Category.CAT_MICROSOFT,
        // Xiaomi / MIUI
        "com.xiaomi"                            to Category.CAT_XIAOMI,
        "com.miui"                              to Category.CAT_XIAOMI,
        // Huawei / Honor
        "com.huawei"                            to Category.CAT_HUAWEI,
        "com.hihonor"                           to Category.CAT_HUAWEI,
        // Meta (Facebook ekosistemi)
        "com.meta"                              to Category.CAT_META,
        "com.facebook"                          to Category.CAT_META,
        "com.instagram"                         to Category.CAT_META,
        // Spotify
        "com.spotify"                           to Category.CAT_SPOTIFY,
        // Amazon
        "com.amazon"                            to Category.CAT_AMAZON,
        // Apple
        "com.apple"                             to Category.CAT_APPLE,
    )

    // Paket adına göre kesin kategori eşlemesi — assets/app_categories.json'dan lazy yüklenir
    private val exactMatchMap: Map<String, String> get() = AppClassifierAssets.getExactMatchMap(context)


    fun classifyApp(appInfo: AppInfo): String {
        exactMatchMap[appInfo.packageName]?.let { return it }
        if (manufacturerClassifyEnabled) {
            classifyByManufacturerPrefix(appInfo.packageName)?.let { return it }
        }
        return classifyByKeywords(appInfo.appName, appInfo.packageName) ?: Category.CAT_OTHER
    }

    fun classifyApps(apps: List<AppInfo>): Map<String, String> =
        apps.associateBy({ it.packageName }, { classifyApp(it) })

    fun getConfidence(appInfo: AppInfo, categoryId: String): Int = when {
        categoryId == Category.CAT_OTHER -> 30
        hasExactMatch(appInfo.packageName, categoryId) -> 95
        hasKeywordMatch(appInfo.appName, categoryId) -> 80
        hasPackageKeywordMatch(appInfo.packageName, categoryId) -> 70
        else -> 50
    }

    // Üretici paket prefix'i → kategori eşleşmesi (exactMap'ten sonra, keyword'den önce)
    private fun classifyByManufacturerPrefix(packageName: String): String? {
        val pkg = packageName.lowercase()
        return MANUFACTURER_PREFIX_MAP.entries.firstOrNull { (prefix, _) -> pkg.startsWith(prefix) }?.value
    }

    private fun classifyByKeywords(appName: String, packageName: String): String? {
        val lowerName = appName.lowercase()
        val lowerPkg  = packageName.lowercase()
        KeywordDatabase.getKeywordMap().forEach { (category, keywords) ->
            keywords.forEach { kw ->
                if (lowerName.contains(kw) || lowerPkg.contains(kw)) return category
            }
        }
        return null
    }

    private fun hasExactMatch(packageName: String, categoryId: String) =
        exactMatchMap[packageName] == categoryId

    private fun hasKeywordMatch(appName: String, categoryId: String) =
        KeywordDatabase.getKeywords(categoryId).any { appName.lowercase().contains(it) }

    private fun hasPackageKeywordMatch(packageName: String, categoryId: String) =
        KeywordDatabase.getKeywords(categoryId).any { packageName.lowercase().contains(it) }
}



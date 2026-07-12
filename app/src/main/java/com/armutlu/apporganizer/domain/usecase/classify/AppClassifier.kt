package com.armutlu.apporganizer.domain.usecase.classify

import android.content.Context
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Üretici prefix → üretici kategorisi: exactMap'ten sonra, keyword'den önce kontrol edilir
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

    // Üretici kategorileri kümesi — tek uygulamalı üretici klasörlerini CAT_OTHER'a almak için
    private val MANUFACTURER_CATEGORIES = MANUFACTURER_PREFIX_MAP.values.toSet()

    // Üretici adı → üretici kategorisi: uygulama adında üretici adı geçiyorsa da eşleştirir
    private val MANUFACTURER_NAME_MAP = mapOf(
        "samsung"   to Category.CAT_SAMSUNG,
        "xiaomi"    to Category.CAT_XIAOMI,
        "miui"      to Category.CAT_XIAOMI,
        "huawei"    to Category.CAT_HUAWEI,
        "honor"     to Category.CAT_HUAWEI,
        "microsoft" to Category.CAT_MICROSOFT,
        "amazon"    to Category.CAT_AMAZON,
        "apple"     to Category.CAT_APPLE,
        "meta"      to Category.CAT_META,
        "spotify"   to Category.CAT_SPOTIFY,
    )

    // Paket adına göre kesin kategori eşlemesi — assets/app_categories.json'dan lazy yüklenir
    private val exactMatchMap: Map<String, String> get() = AppClassifierAssets.getExactMatchMap(context)


    fun classifyApp(appInfo: AppInfo, manufacturerClassifyEnabled: Boolean = true): String {
        // Manuel override — kullanici secimi tum otomatik siniflandirmanin onunde gelir
        AppPrefs.getManualCategoryOverrides(context)[appInfo.packageName]?.let { return it }
        exactMatchMap[appInfo.packageName]?.let { return it }
        if (manufacturerClassifyEnabled) {
            classifyByManufacturerPrefix(appInfo.packageName, appInfo.appName)?.let { return it }
        }
        // Android 8+ ücretsiz/offline Play Store kategori sinyali — exactMap'te olmayan
        // paketler icin keyword'den once denenir (K1, Dongu 227, Fable danismanligi).
        classifyByPlayStoreCategory(appInfo.packageName)?.let { return it }
        return classifyByKeywords(appInfo.appName, appInfo.packageName) ?: Category.CAT_OTHER
    }

    // ApplicationInfo.category (API 26+) — uygulama gelistiricisinin manifestte beyan ettigi
    // resmi Play Store kategorisi. Cihaz icinde, agsiz, ucretsiz bir sinyal.
    private fun classifyByPlayStoreCategory(packageName: String): String? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return null
        val category = runCatching {
            context.packageManager.getApplicationInfo(packageName, 0).category
        }.getOrNull() ?: return null
        return when (category) {
            android.content.pm.ApplicationInfo.CATEGORY_GAME -> Category.CAT_GAMES
            android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> Category.CAT_MUSIC
            android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> Category.CAT_VIDEO
            android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> Category.CAT_PHOTOGRAPHY
            android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> Category.CAT_SOCIAL
            android.content.pm.ApplicationInfo.CATEGORY_NEWS -> Category.CAT_NEWS
            android.content.pm.ApplicationInfo.CATEGORY_MAPS -> Category.CAT_MAPS
            android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> Category.CAT_PRODUCTIVITY
            else -> null
        }
    }

    fun classifyApps(
        apps: List<AppInfo>,
        manufacturerClassifyEnabled: Boolean = true
    ): Map<String, String> {
        val raw = apps.associateBy({ it.packageName }, { classifyApp(it, manufacturerClassifyEnabled) })
        if (!manufacturerClassifyEnabled) return raw
        // Tek uygulamalı üretici klasörlerini CAT_OTHER'a at
        val manufacturerCounts = raw.values.filter { it in MANUFACTURER_CATEGORIES }.groupingBy { it }.eachCount()
        return raw.mapValues { (_, cat) ->
            if (cat in MANUFACTURER_CATEGORIES && (manufacturerCounts[cat] ?: 0) < 2) Category.CAT_OTHER else cat
        }
    }

    fun getConfidence(appInfo: AppInfo, categoryId: String): Int = when {
        categoryId == Category.CAT_OTHER -> 30
        hasExactMatch(appInfo.packageName, categoryId) -> 95
        hasKeywordMatch(appInfo.appName, categoryId) -> 80
        hasPackageKeywordMatch(appInfo.packageName, categoryId) -> 70
        else -> 50
    }

    fun isLowConfidence(appInfo: AppInfo, categoryId: String): Boolean =
        getConfidence(appInfo, categoryId) < LOW_CONFIDENCE_THRESHOLD

    fun findSimilarApps(
        packageName: String,
        categoryId: String,
        allApps: List<AppInfo>
    ): List<AppInfo> {
        val source = allApps.firstOrNull { it.packageName == packageName } ?: return emptyList()
        val manualOverrides = AppPrefs.getManualCategoryOverrides(context)
        return allApps
            .asSequence()
            .filter { it.packageName != packageName }
            .filter { it.packageName !in manualOverrides }
            .filter { it.categoryId != categoryId }
            .filter { candidate ->
                hasSameManufacturerSignal(source, candidate) ||
                    hasKeywordMatch(candidate.appName, categoryId) ||
                    hasPackageKeywordMatch(candidate.packageName, categoryId) ||
                    classifyByPlayStoreCategory(candidate.packageName) == categoryId
            }
            .sortedWith(compareBy<AppInfo> { it.appName.lowercase(java.util.Locale("tr")) })
            .take(8)
            .toList()
    }

    // Üretici paket prefix'i veya uygulama adı → kategori eşleşmesi (exactMap'ten sonra, keyword'den önce)
    // Nokta/tire normalize edilir; büyük/küçük harf toleransı sağlanır.
    private fun classifyByManufacturerPrefix(packageName: String, appName: String = ""): String? {
        val pkg = packageName.lowercase()
        val prefixMatch = MANUFACTURER_PREFIX_MAP.entries.firstOrNull { (prefix, _) -> pkg.startsWith(prefix) }?.value
        if (prefixMatch != null) return prefixMatch
        // Uygulama adında üretici adı varsa da eşleştir (Samsung/SAMSUNG/samsung toleranslı)
        val lowerName = appName.lowercase(java.util.Locale("tr"))
        return MANUFACTURER_NAME_MAP.entries.firstOrNull { (mfr, _) -> lowerName.contains(mfr) }?.value
    }

    private fun hasSameManufacturerSignal(a: AppInfo, b: AppInfo): Boolean {
        val aCategory = classifyByManufacturerPrefix(a.packageName, a.appName)
        val bCategory = classifyByManufacturerPrefix(b.packageName, b.appName)
        return aCategory != null && aCategory == bCategory
    }

    private fun classifyByKeywords(appName: String, packageName: String): String? {
        val lowerName = appName.lowercase(java.util.Locale("tr"))
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

    private companion object {
        const val LOW_CONFIDENCE_THRESHOLD = 60
    }
}



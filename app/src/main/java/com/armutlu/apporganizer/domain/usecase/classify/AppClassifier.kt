package com.armutlu.apporganizer.domain.usecase.classify

import android.content.Context
import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabaseService: AppDatabaseService
) {
    constructor(context: Context) : this(context, AppDatabaseService(context))

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


    fun classifyApp(appInfo: AppInfo, manufacturerClassifyEnabled: Boolean = true): String =
        classifyAppDecision(appInfo, manufacturerClassifyEnabled).categoryId

    fun classifyAppDecision(
        appInfo: AppInfo,
        manufacturerClassifyEnabled: Boolean = true
    ): ClassificationDecision {
        userDecision(appInfo)?.let { return it }
        remoteCatalogDecision(appInfo.packageName)?.let { return it }
        bundledCatalogDecision(appInfo.packageName)?.let { return it }
        val androidDecision = androidCategoryDecision(appInfo.packageName)
        val manufacturerDecision = if (manufacturerClassifyEnabled) {
            manufacturerDecision(appInfo.packageName, appInfo.appName)
        } else {
            null
        }
        val appNameDecision = appNameKeywordDecision(appInfo.appName)
        val packageDecision = packageKeywordDecision(appInfo.packageName)
        val keywordDecision = strongestKeywordDecision(appNameDecision, packageDecision)
        val conflict = hasConflictingSignals(androidDecision, keywordDecision)
        if (conflict && androidDecision != null) {
            return androidDecision.copy(
                confidence = ClassificationConfidence.clampAutomatic(androidDecision.confidence - 20),
                reasonCode = ClassificationReason.CONFLICTING_SIGNALS,
                requiresReview = true,
                reviewState = ClassificationReviewState.PENDING,
            )
        }
        androidDecision?.let { return it }
        manufacturerDecision?.let { return it }
        keywordDecision?.let { return it }
        legacyLlmDecision(appInfo.packageName)?.let { return it }
        return fallbackDecision()
    }

    private fun userDecision(appInfo: AppInfo): ClassificationDecision? {
        if (appInfo.isCategoryLocked) {
            val source = runCatching {
                ClassificationSource.valueOf(appInfo.classificationSource)
            }.getOrDefault(ClassificationSource.USER_CORRECTED)
            if (source == ClassificationSource.USER_CONFIRMED || source == ClassificationSource.USER_CORRECTED) {
                return userDecision(appInfo.categoryId, source)
            }
        }
        return AppPrefs.getManualCategoryOverrides(context)[appInfo.packageName]
            ?.let { userDecision(it, ClassificationSource.USER_CORRECTED) }
    }

    private fun userDecision(categoryId: String, source: ClassificationSource): ClassificationDecision =
        ClassificationDecision(
            categoryId = categoryId,
            confidence = ClassificationConfidence.USER_DECISION,
            source = source,
            reasonCode = ClassificationReason.USER_SELECTION,
            requiresReview = false,
            reviewState = if (source == ClassificationSource.USER_CONFIRMED) {
                ClassificationReviewState.CONFIRMED
            } else {
                ClassificationReviewState.CORRECTED
            },
        )

    private fun remoteCatalogDecision(packageName: String): ClassificationDecision? {
        val categoryId = appDatabaseService.getCategoryForPackage(packageName) ?: return null
        return autoDecision(
            categoryId = categoryId,
            confidence = ClassificationConfidence.REMOTE_CATALOG_EXACT,
            source = ClassificationSource.REMOTE_CATALOG,
            reasonCode = ClassificationReason.UPDATED_CATALOG_MATCH,
        )
    }

    private fun bundledCatalogDecision(packageName: String): ClassificationDecision? =
        exactMatchMap[packageName]?.let { categoryId ->
            autoDecision(
                categoryId = categoryId,
                confidence = ClassificationConfidence.BUNDLED_CATALOG_EXACT,
                source = ClassificationSource.BUNDLED_CATALOG,
                reasonCode = ClassificationReason.EXACT_PACKAGE_MATCH,
            )
        }

    private fun androidCategoryDecision(packageName: String): ClassificationDecision? =
        classifyByPlayStoreCategory(packageName)?.let { categoryId ->
            autoDecision(
                categoryId = categoryId,
                confidence = ClassificationConfidence.ANDROID_CATEGORY,
                source = ClassificationSource.ANDROID_CATEGORY,
                reasonCode = ClassificationReason.ANDROID_DECLARED_CATEGORY,
            )
        }

    private fun manufacturerDecision(packageName: String, appName: String): ClassificationDecision? =
        classifyByManufacturerPrefix(packageName, appName)?.let { categoryId ->
            autoDecision(
                categoryId = categoryId,
                confidence = ClassificationConfidence.MANUFACTURER_RULE,
                source = ClassificationSource.MANUFACTURER_RULE,
                reasonCode = ClassificationReason.MANUFACTURER_PACKAGE_MATCH,
            )
        }

    private fun appNameKeywordDecision(appName: String): ClassificationDecision? {
        val lowerName = appName.lowercase(java.util.Locale("tr"))
        KeywordDatabase.getKeywordMap().forEach { (category, keywords) ->
            if (keywords.any { lowerName.contains(it) }) {
                return autoDecision(
                    categoryId = category,
                    confidence = ClassificationConfidence.APP_NAME_KEYWORD,
                    source = ClassificationSource.APP_NAME_KEYWORD,
                    reasonCode = ClassificationReason.APP_NAME_MATCH,
                )
            }
        }
        return null
    }

    private fun packageKeywordDecision(packageName: String): ClassificationDecision? {
        val lowerPkg = packageName.lowercase()
        KeywordDatabase.getKeywordMap().forEach { (category, keywords) ->
            if (keywords.any { lowerPkg.contains(it) }) {
                return autoDecision(
                    categoryId = category,
                    confidence = ClassificationConfidence.PACKAGE_NAME_KEYWORD,
                    source = ClassificationSource.PACKAGE_NAME_KEYWORD,
                    reasonCode = ClassificationReason.PACKAGE_NAME_MATCH,
                )
            }
        }
        return null
    }

    private fun strongestKeywordDecision(
        appNameDecision: ClassificationDecision?,
        packageDecision: ClassificationDecision?
    ): ClassificationDecision? {
        if (appNameDecision == null) return packageDecision
        if (packageDecision == null) return appNameDecision
        if (appNameDecision.categoryId == packageDecision.categoryId) {
            return appNameDecision.copy(
                confidence = ClassificationConfidence.clampAutomatic(appNameDecision.confidence + 5)
            )
        }
        return appNameDecision.copy(
            confidence = ClassificationConfidence.clampAutomatic(appNameDecision.confidence - 15),
            reasonCode = ClassificationReason.CONFLICTING_SIGNALS,
            requiresReview = true,
            reviewState = ClassificationReviewState.PENDING,
        )
    }

    private fun legacyLlmDecision(packageName: String): ClassificationDecision? =
        AppPrefs.getLlmCategoryCache(context)[packageName]?.let { categoryId ->
            autoDecision(
                categoryId = categoryId,
                confidence = ClassificationConfidence.LLM_LEGACY,
                source = ClassificationSource.LLM_LEGACY,
                reasonCode = ClassificationReason.LEGACY_AI_RESULT,
            )
        }

    private fun fallbackDecision(): ClassificationDecision =
        autoDecision(
            categoryId = Category.CAT_OTHER,
            confidence = ClassificationConfidence.FALLBACK_OTHER,
            source = ClassificationSource.FALLBACK_OTHER,
            reasonCode = ClassificationReason.NO_RELIABLE_MATCH,
        )

    private fun autoDecision(
        categoryId: String,
        confidence: Int,
        source: ClassificationSource,
        reasonCode: ClassificationReason,
    ): ClassificationDecision {
        val safeConfidence = ClassificationConfidence.clampAutomatic(confidence)
        val (requiresReview, reviewState) = ClassificationReviewPolicy.resolve(
            categoryId = categoryId,
            confidence = safeConfidence,
            source = source,
        )
        return ClassificationDecision(
            categoryId = categoryId,
            confidence = safeConfidence,
            source = source,
            reasonCode = reasonCode,
            requiresReview = requiresReview,
            reviewState = reviewState,
        )
    }

    private fun hasConflictingSignals(
        first: ClassificationDecision?,
        second: ClassificationDecision?
    ): Boolean = first != null && second != null && first.categoryId != second.categoryId

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

    fun getConfidence(appInfo: AppInfo, categoryId: String): Int {
        val decision = classifyAppDecision(appInfo)
        if (decision.categoryId == categoryId) return decision.confidence
        return when {
            categoryId == Category.CAT_OTHER -> ClassificationConfidence.FALLBACK_OTHER
            hasExactMatch(appInfo.packageName, categoryId) -> ClassificationConfidence.BUNDLED_CATALOG_EXACT
            hasKeywordMatch(appInfo.appName, categoryId) -> ClassificationConfidence.APP_NAME_KEYWORD
            hasPackageKeywordMatch(appInfo.packageName, categoryId) -> ClassificationConfidence.PACKAGE_NAME_KEYWORD
            else -> 50
        }
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

    /**
     * K2 — Override'lardan öğrenen öneri katmanı (kısmi): kullanıcı bir uygulamayı elle
     * yeni bir kategoriye taşıdığında, aynı "benzerlik sinyaline" sahip (üretici paket
     * prefix'i aynı VEYA aynı kategoriye düşen keyword eşleşmesi) ve HÂLÂ eski kategoride
     * duran, henüz manuel override'ı olmayan uygulamaları bulur.
     *
     * Saf/test edilebilir fonksiyon — Context/AppPrefs bağımlılığı yok, tüm girdiler parametre.
     * UI katmanı bu adayları tek tek (checkbox ile) seçilebilir bir öneri listesinde gösterir.
     *
     * @param changedApp kategorisi az önce değiştirilen uygulama
     * @param oldCategoryId uygulamanın önceki kategorisi
     * @param newCategoryId uygulamanın yeni (hedef) kategorisi
     * @param allApps cihazdaki tüm uygulamalar
     * @param manualOverrides paket adı → kategori id (zaten elle override edilmiş uygulamalar)
     * @param limit döndürülecek maksimum aday sayısı (varsayılan 10)
     */
    fun findSimilarUnclassifiedApps(
        changedApp: AppInfo,
        oldCategoryId: String,
        newCategoryId: String,
        allApps: List<AppInfo>,
        manualOverrides: Map<String, String>,
        limit: Int = 10
    ): List<AppInfo> {
        if (oldCategoryId == newCategoryId) return emptyList()
        return allApps
            .asSequence()
            .filter { it.packageName != changedApp.packageName }
            .filter { it.packageName !in manualOverrides }
            .filter { it.categoryId == oldCategoryId }
            .filter { candidate ->
                hasSameManufacturerSignal(changedApp, candidate) ||
                    hasKeywordMatch(candidate.appName, newCategoryId) ||
                    hasPackageKeywordMatch(candidate.packageName, newCategoryId)
            }
            .sortedWith(compareBy<AppInfo> { it.appName.lowercase(java.util.Locale("tr")) })
            .take(limit)
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
        const val LOW_CONFIDENCE_THRESHOLD = ClassificationConfidence.REVIEW_THRESHOLD
    }
}



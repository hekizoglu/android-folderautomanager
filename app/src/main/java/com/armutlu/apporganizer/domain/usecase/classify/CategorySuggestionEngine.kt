package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import java.util.Locale

/**
 * P0.7 - CAT_OTHER / dusuk guvenli uygulamalar icin kategori onerisi uretir.
 *
 * Sadece sinyal varsa oneri dondurur - sahte/rastgele oneri YOK. `suggestFor` null
 * donduren ekran "Yeterli sinyal yok" gosterir; kullanici manuel secim yapar.
 *
 * Sinyal onceligi (guclu -> zayif):
 *  1. KEYWORD  - uygulama adinda/paket adinda KeywordDatabase eslesmesi (AppClassifier'daki
 *     appNameKeywordDecision/packageKeywordDecision ile ayni mantik, CAT_OTHER haric).
 *  2. VENDOR   - paket adi BILINEN bir uretici onekine sahip (AppClassifier.
 *     MANUFACTURER_PREFIX_MAP ile ayni liste, ornegin com.google, com.samsung) VE ayni
 *     uretici onekli, HALIHAZIRDA gercek bir kategoriye atanmis baska uygulama var.
 *  3. SIMILAR_PACKAGE - bilinen uretici listesinde OLMAYAN, jenerik de olmayan paket
 *     onekleri icin: ayni ilk 2 segment onekini paylasan 2+ farkli uygulamanin en
 *     yaygin kategorisi.
 */
object CategorySuggestionEngine {

    enum class SignalType { VENDOR, KEYWORD, SIMILAR_PACKAGE }

    data class Suggestion(
        val categoryId: String,
        val signal: SignalType,
        val confidence: Float,
    )

    // AppClassifier.MANUFACTURER_PREFIX_MAP ile ayni bilinen uretici onekleri - VENDOR
    // sinyali sadece bu listede olan oneklerde tetiklenir (kategori adi onemsiz, sadece
    // "bilinen bir uretici paketi mi" sorusu).
    private val KNOWN_VENDOR_PREFIXES = setOf(
        "com.google",
        "com.android.google",
        "com.samsung",
        "com.sec.android",
        "com.microsoft",
        "com.xiaomi",
        "com.miui",
        "com.huawei",
        "com.hihonor",
        "com.meta",
        "com.facebook",
        "com.instagram",
        "com.spotify",
        "com.amazon",
        "com.apple",
    )

    // Cok genis/jenerik onekler - SIMILAR_PACKAGE sinyali icin kullanilmaz (ayni onekte
    // onlarca alakasiz uygulama olabilir, yanlis oneri riski yuksek). Bilinen uretici
    // onekleri zaten VENDOR tarafinda ele alindigi icin burada da haric tutulur.
    private val GENERIC_PREFIXES = KNOWN_VENDOR_PREFIXES + setOf(
        "com.android",
    )

    // Oneri hedefi olamayacak kategoriler - "Diger" veya "Kategorisiz" onerisi anlamsizdir.
    private val NON_SUGGESTABLE_CATEGORIES = setOf(
        Category.CAT_OTHER,
        Category.CAT_UNCATEGORIZED,
    )

    /**
     * Verilen uygulama icin kategori onerisi dondurur. Sinyal yoksa null.
     *
     * @param app oneri aranan uygulama (genelde CAT_OTHER / dusuk guvenli)
     * @param allApps cihazdaki tum uygulamalar (vendor/benzer paket sinyali icin)
     */
    fun suggestFor(app: AppInfo, allApps: List<AppInfo>): Suggestion? {
        keywordSuggestion(app)?.let { return it }
        vendorSuggestion(app, allApps)?.let { return it }
        similarPackageSuggestion(app, allApps)?.let { return it }
        return null
    }

    private fun keywordSuggestion(app: AppInfo): Suggestion? {
        val lowerName = app.appName.lowercase(Locale("tr"))
        val lowerPkg = app.packageName.lowercase(Locale.ROOT)

        // Do not return the first map hit: overlapping keywords (for example `fit`
        // and `fitness`) made the result depend on KeywordDatabase insertion order.
        // The most specific/longest matching keyword wins; ties use category id so the
        // result remains deterministic even if the map implementation changes.
        val bestMatch = KeywordDatabase.getKeywordMap()
            .asSequence()
            .filter { (categoryId, keywords) ->
                categoryId !in NON_SUGGESTABLE_CATEGORIES && keywords.isNotEmpty()
            }
            .flatMap { (categoryId, keywords) ->
                keywords.asSequence().mapNotNull { keyword ->
                    val nameHit = lowerName.contains(keyword)
                    val packageHit = lowerPkg.contains(keyword)
                    if (!nameHit && !packageHit) {
                        null
                    } else {
                        KeywordMatch(
                            categoryId = categoryId,
                            keywordLength = keyword.length,
                            bothSources = nameHit && packageHit,
                        )
                    }
                }
            }
            .maxWithOrNull(
                compareBy<KeywordMatch> { it.keywordLength }
                    .thenBy { it.bothSources }
                    .thenBy { it.categoryId },
            ) ?: return null

        return Suggestion(
            categoryId = bestMatch.categoryId,
            signal = SignalType.KEYWORD,
            confidence = if (bestMatch.bothSources) {
                KEYWORD_BOTH_CONFIDENCE
            } else {
                KEYWORD_CONFIDENCE
            },
        )
    }

    private data class KeywordMatch(
        val categoryId: String,
        val keywordLength: Int,
        val bothSources: Boolean,
    )

    private fun vendorSuggestion(app: AppInfo, allApps: List<AppInfo>): Suggestion? {
        val vendorPrefix = knownVendorPrefixOf(app.packageName) ?: return null

        val categoryCounts = allApps
            .asSequence()
            .filter { it.packageName != app.packageName }
            .filter { it.categoryId !in NON_SUGGESTABLE_CATEGORIES }
            .filter { knownVendorPrefixOf(it.packageName) == vendorPrefix }
            .groupingBy { it.categoryId }
            .eachCount()

        val bestEntry = categoryCounts.entries.maxByOrNull { it.value } ?: return null
        return Suggestion(
            categoryId = bestEntry.key,
            signal = SignalType.VENDOR,
            confidence = VENDOR_CONFIDENCE,
        )
    }

    private fun similarPackageSuggestion(app: AppInfo, allApps: List<AppInfo>): Suggestion? {
        val prefix = packagePrefixSegments(app.packageName, segments = 2) ?: return null
        if (prefix in GENERIC_PREFIXES) return null

        val candidates = allApps
            .asSequence()
            .filter { it.packageName != app.packageName }
            .filter { it.categoryId !in NON_SUGGESTABLE_CATEGORIES }
            .filter { packagePrefixSegments(it.packageName, segments = 2) == prefix }
            .toList()

        // Ayni onekli en az 2 farkli uygulama olmali - tek eslesme guvenilir sinyal degil.
        if (candidates.size < 2) return null

        val categoryCounts = candidates.groupingBy { it.categoryId }.eachCount()
        val bestEntry = categoryCounts.entries.maxByOrNull { it.value } ?: return null

        return Suggestion(
            categoryId = bestEntry.key,
            signal = SignalType.SIMILAR_PACKAGE,
            confidence = SIMILAR_PACKAGE_CONFIDENCE,
        )
    }

    /** Paket adinin ilk N segmentini dondurur (nokta ile ayrilmis). Yetersiz segment varsa null. */
    private fun packagePrefixSegments(packageName: String, segments: Int): String? {
        val parts = packageName.lowercase(Locale.ROOT).split(".")
        if (parts.size < segments) return null
        return parts.take(segments).joinToString(".")
    }

    private fun knownVendorPrefixOf(packageName: String): String? {
        val lowerPkg = packageName.lowercase(Locale.ROOT)
        return KNOWN_VENDOR_PREFIXES.firstOrNull { prefix ->
            lowerPkg == prefix || lowerPkg.startsWith("$prefix.")
        }
    }

    private const val KEYWORD_CONFIDENCE = 0.6f
    private const val KEYWORD_BOTH_CONFIDENCE = 0.75f
    private const val VENDOR_CONFIDENCE = 0.55f
    private const val SIMILAR_PACKAGE_CONFIDENCE = 0.45f
}

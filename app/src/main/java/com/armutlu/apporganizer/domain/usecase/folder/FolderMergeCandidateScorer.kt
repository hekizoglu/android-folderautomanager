package com.armutlu.apporganizer.domain.usecase.folder

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import java.util.Locale

/**
 * R3.1 - Klasor birlestirme aday skorlama motoru.
 *
 * Sabit kategori eslestirme tablosunu (eskiden FolderSuggestionEngine icinde tanimliydi)
 * burada tutar; kucuk/az kullanilan klasorler icin FolderMergePlan uretir.
 *
 * Kurallar:
 * - Sadece bilinen (static mapping tablosunda tanimli) hedefler icin plan uretilir; rastgele/tahmini
 *   hedef ONERILMEZ.
 * - Manuel kilitli (isCategoryLocked=true) uygulamalar varsayilan tasima secimine dahil edilmez,
 *   ayri bir listede (lockedPackageNames) raporlanir.
 * - Kaynak klasordeki tasinabilir uygulama sayisi minimum esigin altinda veya sifirsa plan uretilmez.
 * - Sonuc listesi guvene gore azalan, esitlikte kaynak kategori id'sine gore artan - deterministik siralanir.
 */
object FolderMergeCandidateScorer {

    /** Kaynak klasor bu adetten fazla (kilitsiz) uygulama icerirse birlestirme onerilmez. */
    const val DEFAULT_SMALL_FOLDER_THRESHOLD = 2

    /** Hedef klasor birlestirme sonrasi bu adedi asarsa UI'da uyari gosterilmesi beklenir (bkz. R3.3). */
    const val LARGE_TARGET_WARNING_THRESHOLD = 20

    private const val BASE_STATIC_MAPPING_CONFIDENCE = 76
    private const val MIN_CONFIDENCE = 0
    private const val MAX_CONFIDENCE = 100

    /** Sabit kategori -> onerilen hedef kategori eslestirmesi. Bilinmeyen hedef URETILMEZ. */
    val staticMergeTargets: Map<String, String> = mapOf(
        Category.CAT_VIDEO to Category.CAT_ENTERTAINMENT,
        Category.CAT_MUSIC to Category.CAT_ENTERTAINMENT,
        Category.CAT_DATING to Category.CAT_SOCIAL,
        Category.CAT_MAPS to Category.CAT_TRAVEL,
        Category.CAT_HOUSE to Category.CAT_LIFESTYLE,
        Category.CAT_BEAUTY to Category.CAT_LIFESTYLE,
        Category.CAT_EVENTS to Category.CAT_LIFESTYLE,
        Category.CAT_COMICS to Category.CAT_BOOKS,
    )

    /**
     * Verilen uygulama listesinden (kategoriye gore onceden gruplanmis olabilir ya da burada
     * gruplanir) tum uygun FolderMergePlan adaylarini uretir.
     *
     * @param apps Degerlendirilecek tum uygulamalar (gizli/sistem uygulamalari filtrelenir).
     * @param smallFolderThreshold Kaynak klasordeki (kilitsiz) uygulama sayisi bu degerden fazlaysa
     *   aday uretilmez (varsayilan [DEFAULT_SMALL_FOLDER_THRESHOLD]).
     * @param minConfidence Bu degerin altindaki guven skoruna sahip adaylar elenir.
     * @param mergeTargets Kategori -> hedef kategori eslestirme tablosu (test icin override edilebilir).
     */
    fun score(
        apps: List<AppInfo>,
        smallFolderThreshold: Int = DEFAULT_SMALL_FOLDER_THRESHOLD,
        minConfidence: Int = MIN_CONFIDENCE,
        mergeTargets: Map<String, String> = staticMergeTargets,
    ): List<FolderMergePlan> {
        val eligibleApps = apps.filter {
            !it.isHidden && !it.isSystemApp && it.categoryId != Category.CAT_UNCATEGORIZED
        }
        val grouped = eligibleApps.groupBy { it.categoryId }

        val plans = mutableListOf<FolderMergePlan>()
        grouped.forEach { (sourceCategoryId, folderApps) ->
            val targetCategoryId = mergeTargets[sourceCategoryId] ?: return@forEach
            if (!grouped.containsKey(targetCategoryId)) return@forEach

            val movable = folderApps.filterNot { it.isCategoryLocked }
            val locked = folderApps.filter { it.isCategoryLocked }
            if (movable.isEmpty() || movable.size > smallFolderThreshold) return@forEach

            val confidence = computeConfidence(
                baseConfidence = BASE_STATIC_MAPPING_CONFIDENCE,
                movableCount = movable.size,
                smallFolderThreshold = smallFolderThreshold,
                lockedCount = locked.size,
            )
            if (confidence < minConfidence) return@forEach

            plans += FolderMergePlan(
                sourceCategoryId = sourceCategoryId,
                targetCategoryId = targetCategoryId,
                movablePackageNames = movable.map { it.packageName }.sortedWith(packageComparator()),
                lockedPackageNames = locked.map { it.packageName }.sortedWith(packageComparator()),
                reason = FolderSuggestionReason.STATIC_MAPPING,
                confidence = confidence,
                sourceAppCount = folderApps.size,
                targetAppCount = grouped[targetCategoryId]?.size ?: 0,
            )
        }

        return plans.sortedWith(
            compareByDescending<FolderMergePlan> { it.confidence }
                .thenBy { it.sourceCategoryId.lowercase(Locale("tr")) }
        )
    }

    /**
     * Guven skorunu hesaplar: az uygulama sayisi ve kilitli uygulama YOKLUGU guveni artirir,
     * esige yaklasmak veya kilitli uygulama varligi guveni azaltir. Sonuc 0-100 arasina siyirilir.
     */
    private fun computeConfidence(
        baseConfidence: Int,
        movableCount: Int,
        smallFolderThreshold: Int,
        lockedCount: Int,
    ): Int {
        val slack = smallFolderThreshold - movableCount
        val slackBonus = slack * 4
        val lockedPenalty = lockedCount * 6
        return (baseConfidence + slackBonus - lockedPenalty).coerceIn(MIN_CONFIDENCE, MAX_CONFIDENCE)
    }

    private fun packageComparator(): Comparator<String> = compareBy { it.lowercase(Locale("tr")) }
}

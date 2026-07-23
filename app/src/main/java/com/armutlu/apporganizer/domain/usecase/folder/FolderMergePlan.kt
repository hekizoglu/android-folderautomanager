package com.armutlu.apporganizer.domain.usecase.folder

/**
 * Klasor birlestirme icin kullaniciya onerilen tam plan.
 * R3.1 - FolderMergeCandidateScorer tarafindan uretilir, R3.2/R3.3'te
 * FolderMergeUiState ve FolderMergeReviewScreen tarafindan tuketilir.
 *
 * @param sourceCategoryId Birlestirilecek (kucuk/az kullanilan) klasorun kategori id'si.
 * @param targetCategoryId Uygulamalarin tasinacagi hedef klasorun kategori id'si.
 * @param movablePackageNames Kilitli OLMAYAN, varsayilan olarak secili tasinacak paketler (siralanmis).
 * @param lockedPackageNames Manuel kilitli (isCategoryLocked=true) oldugu icin varsayilan secimde
 *   YER ALMAYAN ama kullaniciya acikca gosterilmesi gereken paketler (siralanmis).
 * @param reason Bu onerinin uretilme gerekcesi.
 * @param confidence 0-100 arasi guven skoru; siralama ve esik kontrolu icin kullanilir.
 * @param sourceAppCount Birlestirme oncesi kaynak klasordeki toplam uygulama sayisi (kilitli dahil).
 * @param targetAppCount Birlestirme oncesi hedef klasordeki toplam uygulama sayisi.
 */
data class FolderMergePlan(
    val sourceCategoryId: String,
    val targetCategoryId: String,
    val movablePackageNames: List<String>,
    val lockedPackageNames: List<String> = emptyList(),
    val reason: FolderSuggestionReason,
    val confidence: Int,
    val sourceAppCount: Int,
    val targetAppCount: Int,
) {
    /** Birlestirme sonrasi hedef klasorde olusacak tahmini uygulama sayisi (sadece tasinabilirler dahil). */
    val projectedTargetAppCount: Int
        get() = targetAppCount + movablePackageNames.size

    /** Kaynak klasorde en az bir kilitli uygulama var mi - UI'da uyari gostermek icin. */
    val hasLockedApps: Boolean
        get() = lockedPackageNames.isNotEmpty()
}

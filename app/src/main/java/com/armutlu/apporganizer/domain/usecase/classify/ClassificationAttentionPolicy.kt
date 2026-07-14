package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category

/**
 * Tek dogruluk kaynagi: bir uygulamanin "dikkat gerektirip gerektirmedigini" belirler.
 *
 * P0.2 oncesi durum (tutarsizlik):
 * - AppDao.getPendingClassificationApps(): classificationReviewState == 'PENDING'
 *   AND isSystemApp == 0 AND (snooze suresi gecmis) — ClassificationReviewScreen/
 *   AppListViewModel.pendingClassificationApps bunu kullaniyordu.
 * - AppListViewModel.otherApps: sadece categoryId == CAT_OTHER (review state,
 *   confidence, isSystemApp hic dikkate alinmiyor) — SettingsAppsSection "Diger
 *   Klasoru" sayaci bunu kullaniyordu. Sayac ile liste farkli kumeler donduruyordu.
 * - AppDao.getUncategorizedApps(): sadece categoryId == 'uncategorized' — baska
 *   hicbir ekranda kullanilmiyordu, ayri bir ucuncu tanim olarak duruyordu.
 * - NiagaraComponents.kt: categoryId == "other" || "uncategorized" kontrolu ile
 *   etiket gizleme kararı veriyordu — dorduncu, UI'a gomulu bir tanim.
 *
 * Bu policy butun bu tanimlari tek `evaluate()` fonksiyonunda birlestirir. Mevcut
 * davranislar (isHidden/isSystemApp haric tutma) korunur; sadece kaynak tekillestirilir.
 */
enum class AttentionReason {
    /** categoryId hala "uncategorized" — hic siniflandirilmamis. */
    UNCATEGORIZED,

    /** categoryId "other" ama guven skoru dusuk/gecersiz (kullanici onayi yok). */
    OTHER_WITHOUT_CONFIDENCE,

    /** Kategori atanmis ama guven skoru inceleme esiginin altinda. */
    LOW_CONFIDENCE,

    /** classificationReviewState == PENDING ve snooze suresi gecmis/yok. */
    REVIEW_PENDING,

    /** categoryId bos/blank — beklenmeyen veri durumu. */
    MISSING_CATEGORY,

    /** Ayni uygulamada birden fazla celiskili sinyal var (reviewState PENDING
     *  degil ama kaynak UNKNOWN ya da reason CONFLICTING_SIGNALS). */
    CLASSIFIER_CONFLICT,
}

object ClassificationAttentionPolicy {

    /**
     * Verilen uygulama icin dikkat gerekcesini dondurur. null = dikkat gerekmiyor.
     *
     * Mevcut ekranlarin ortak davranisini korur:
     * - Gizli uygulamalar (isHidden) dikkat listelerine hic girmez (hicbir ekran
     *   gizli uygulamalari gostermiyordu).
     * - Sistem uygulamalari (isSystemApp) ClassificationReviewScreen'de disarida
     *   birakiliyordu (AppDao.getPendingClassificationApps WHERE isSystemApp = 0);
     *   bu davranis korunur — sistem app'leri hicbir AttentionReason almaz.
     * - Kullanici onayli/duzeltilmis kayitlar (isCategoryLocked ya da
     *   classificationReviewState CONFIRMED/CORRECTED/NOT_REQUIRED/SKIPPED-aktif-degil)
     *   dikkat listesine girmez.
     */
    fun evaluate(app: AppInfo, now: Long = System.currentTimeMillis()): AttentionReason? {
        if (app.isHidden || app.isSystemApp) return null

        // Kullanicinin bizzat onayladigi/duzelttigi ve kilitledigi kayitlar guvenilirdir.
        if (app.isCategoryLocked) return null

        if (app.categoryId.isBlank()) return AttentionReason.MISSING_CATEGORY

        if (app.categoryId == Category.CAT_UNCATEGORIZED) return AttentionReason.UNCATEGORIZED

        // Snooze aktifse (kullanici "7 gun ertele" dedi) simdilik dikkat listesine girmez.
        val isSnoozed = app.reviewSnoozedUntil > 0 && app.reviewSnoozedUntil > now

        val reviewState = runCatching {
            ClassificationReviewState.valueOf(app.classificationReviewState)
        }.getOrNull()

        if (reviewState == ClassificationReviewState.PENDING && !isSnoozed) {
            return AttentionReason.REVIEW_PENDING
        }

        if (isSnoozed) return null

        // Kullanicinin onayladigi/duzelttigi ama henuz kilitlenmemis kayitlar
        // (teorik olarak olmamali ama guvenlik icin) dikkat listesine girmez.
        if (reviewState == ClassificationReviewState.CONFIRMED ||
            reviewState == ClassificationReviewState.CORRECTED
        ) {
            return null
        }

        val source = runCatching {
            ClassificationSource.valueOf(app.classificationSource)
        }.getOrNull()

        val reason = runCatching {
            ClassificationReason.valueOf(app.classificationReason)
        }.getOrNull()

        if (source == ClassificationSource.UNKNOWN || reason == ClassificationReason.CONFLICTING_SIGNALS) {
            return AttentionReason.CLASSIFIER_CONFLICT
        }

        if (app.categoryId == Category.CAT_OTHER) {
            // "Diger" nihai bir kategori degildir; kullanici onayi olmadan
            // (isCategoryLocked yukarida zaten elendi) her zaman dikkat gerektirir.
            return AttentionReason.OTHER_WITHOUT_CONFIDENCE
        }

        if (app.classificationConfidence < ClassificationConfidence.REVIEW_THRESHOLD) {
            return AttentionReason.LOW_CONFIDENCE
        }

        return null
    }

    /**
     * Verilen liste icin dikkat gerekcesine gore gruplanmis uygulamalari dondurur.
     * Bos liste donen anahtar hic olusmaz (Map bos kalabilir bos girdi icermez).
     */
    fun attentionApps(apps: List<AppInfo>, now: Long = System.currentTimeMillis()): Map<AttentionReason, List<AppInfo>> {
        val result = linkedMapOf<AttentionReason, MutableList<AppInfo>>()
        for (app in apps) {
            val reason = evaluate(app, now) ?: continue
            result.getOrPut(reason) { mutableListOf() }.add(app)
        }
        return result
    }

    /** Toplam dikkat gerektiren uygulama sayisi — sayac/liste tutarliligi icin tek kaynak. */
    fun attentionCount(apps: List<AppInfo>, now: Long = System.currentTimeMillis()): Int {
        return apps.count { evaluate(it, now) != null }
    }

    /** Dikkat gerektiren tum uygulamalarin duz listesi (reason siralamasindan bagimsiz). */
    fun attentionList(apps: List<AppInfo>, now: Long = System.currentTimeMillis()): List<AppInfo> {
        return apps.filter { evaluate(it, now) != null }
    }

    /**
     * AttentionReason -> strings.xml kaynagi. UI katmani "Neden burada?" metnini
     * bu resId ile stringResource(...) uzerinden cozer (values/values-en, P0.2).
     */
    fun reasonStringRes(reason: AttentionReason): Int {
        return when (reason) {
            AttentionReason.UNCATEGORIZED -> com.armutlu.apporganizer.R.string.attention_reason_uncategorized
            AttentionReason.OTHER_WITHOUT_CONFIDENCE -> com.armutlu.apporganizer.R.string.attention_reason_other_without_confidence
            AttentionReason.LOW_CONFIDENCE -> com.armutlu.apporganizer.R.string.attention_reason_low_confidence
            AttentionReason.REVIEW_PENDING -> com.armutlu.apporganizer.R.string.attention_reason_review_pending
            AttentionReason.MISSING_CATEGORY -> com.armutlu.apporganizer.R.string.attention_reason_missing_category
            AttentionReason.CLASSIFIER_CONFLICT -> com.armutlu.apporganizer.R.string.attention_reason_classifier_conflict
        }
    }
}

package com.armutlu.apporganizer.domain.usecase.folder

/**
 * Bir klasor birlestirme onerisinin neden uretildigini aciklayan sinyal turu.
 * R3.1 - FolderMergeCandidateScorer bu enum'u kullanarak oneri gerekcesini raporlar.
 */
enum class FolderSuggestionReason {
    /** Kaynak ve hedef klasor ayni uretici/marka onekine sahip uygulamalar icerir. */
    VENDOR,

    /** Kaynak klasordeki uygulamalarin adi/paketi hedef klasorun anahtar kelimeleriyle eslesir. */
    KEYWORD,

    /** Kaynak klasor cok az uygulama icerir (minimum esigin altinda) - dagitilmasi/gomulmesi onerilir. */
    CONFIDENCE_LOW,

    /** Kaynak klasordeki uygulamalarin cogu uzun suredir kullanilmiyor. */
    USAGE_STALE,

    /** Sabit (static) kategori eslestirme tablosunda tanimli bir hedef bulundu. */
    STATIC_MAPPING,
}

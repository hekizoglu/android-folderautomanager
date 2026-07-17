package com.armutlu.apporganizer.domain.common

/**
 * Gorev/skor/serit bilesenlerinin ortak tazelik dili (Dongu H03 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satir 557-598).
 *
 * Kullaniciya eski veri yeniymis gibi gosterilmemesi icin her bilesen ayni sinirlarla
 * ([DataFreshnessResolver]) hesaplanan bu enum'u referans alir.
 */
enum class DataFreshness {
    /** Son 5 dakika icinde hesaplanmis. */
    LIVE,

    /** 5-30 dakika arasi. */
    RECENT,

    /** 30 dakikadan eski. */
    STALE,

    /** Izin/veri yok veya hesaplama basarisiz — computedAt bilinmiyor. */
    UNAVAILABLE,
}

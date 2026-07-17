package com.armutlu.apporganizer.domain.usecase.missions

/**
 * Gorev yasam dongusu durumu (Dongu M00 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md).
 *
 * `completed: Boolean` yerine gercek durum makinesi: donemsel (ust sinir / haftalik
 * karsilastirma) gorevler donem bitmeden COMPLETED olamaz, sadece bu enum uzerinden
 * ilerleme/guvenlik/basarisizlik durumu tasinir.
 */
enum class MissionStatus {
    /** Gorevi degerlendirmek icin gereken sinyal (izin, veri) mevcut degil. */
    DATA_UNAVAILABLE,

    /** Donem/gorev henuz baslamadi (orn. gece gorevi 23:00'ten once). */
    NOT_STARTED,

    /** Gorev suruyor, henuz ne basarili ne basarisiz kesinlesmedi. */
    IN_PROGRESS,

    /** Donem bitmeden once, mevcut veriyle "guvenli" bolgede (orn. gece yarisina kadar kullanim yok). */
    SAFE,

    /** Hedefe cok yaklasilmis (esik >= %80 kullanim) — basarisizlik riski yuksek. */
    AT_RISK,

    /** Donem bitti, sonuc (settlement) henuz islenmedi — M04'un isi. */
    AWAITING_SETTLEMENT,

    /** Gorev basariyla tamamlandi (eylem sayisi: aninda; donemsel: settlement sonrasi). */
    COMPLETED,

    /** Gorev basarisiz oldu (ust sinir asildi veya donem sonunda hedef tutmadi). */
    FAILED,
}

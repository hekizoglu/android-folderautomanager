package com.armutlu.apporganizer.domain.usecase.missions

/**
 * Gorev ilerleme goruntuleme sekli (Dongu M03 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satir 852-941).
 *
 * MissionEngine her gorev id'sini bu turlerden birine esler; [MissionProgressCalculator]
 * bu tur + [MissionEvaluation] ciftinden UI-hazir [MissionProgress] uretir.
 */
enum class MissionProgressKind {
    /** Ust sinir gorevleri (ekran suresi, kilit acma) — dolu cubuk basari DEGIL, "limit kullanimi". */
    UPPER_LIMIT,

    /** Eylem sayisi gorevleri (siniflandirma temizligi, haftalik pozitif aksiyonlar). */
    ACTION_COUNT,

    /** Tek seferlik bayrak gorevleri (orn. bildirim raporu goruntulendi mi). */
    BOOLEAN_ACTION,

    /** Belirli saatten sonra kullanimdan kacinma gorevi (gece 23:00 sonrasi). */
    AVOID_AFTER_TIME,

    /**
     * Dongu G3a — belirli bir saate KADAR (gunun basinda) bir kategoriden kacinma gorevi
     * (orn. "ilk 30 dk sosyal medya acma"). AVOID_AFTER_TIME'in sabah simetrigi: gorev
     * penceresi gunun basinda kapanir, o pencere gecince (veya ihlal edilince) kesinlesir.
     */
    AVOID_BEFORE_TIME,

    /** Donemsel karsilastirma gorevleri (bu hafta / gecen hafta). */
    PERIOD_COMPARISON,
}

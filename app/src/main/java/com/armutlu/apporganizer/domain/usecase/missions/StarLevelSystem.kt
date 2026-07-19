package com.armutlu.apporganizer.domain.usecase.missions

/**
 * Dongu G6 — Yildiz Ekonomisi (GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md G6, plan satir 40-44).
 *
 * Toplam ⭐ birikiminden SEVIYE turetir. Para/satin alma YOK — tamamen icsel motivasyon.
 * Saf Kotlin, Android bagimliligi yok, unit test edilebilir.
 *
 * Dil notu: seviye adlari NOTR ve YARGISIZ secildi (plan satir 41) — "Caylak" bile asagilayici
 * degil, sadece baslangic noktasini ifade eder; ilerlemedigi icin CEZA/utandirma metni yok
 * (M08 ceza yok ilkesiyle tutarli).
 */
object StarLevelSystem {

    enum class Level(
        val minStars: Int,
        val labelTr: String,
        val labelEn: String,
    ) {
        BEGINNER(0, "Çaylak", "Beginner"),
        STEADY(10, "Düzenli", "Steady"),
        FOCUSED(25, "Odaklı", "Focused"),
        BALANCE_MASTER(50, "Denge Ustası", "Balance Master"),
        MASTER(100, "Usta", "Master"),
        ;

        companion object {
            /** Eşiğe göre azalan sırada — [levelFor] ilk uyanı (>= minStars) bulur. */
            val DESCENDING = entries.sortedByDescending { it.minStars }
        }
    }

    /** [stars] için geçerli seviye — eşiklerin en yükseğinden başlayıp ilk uyanı seçer. */
    fun levelFor(stars: Int): Level =
        Level.DESCENDING.firstOrNull { stars >= it.minStars } ?: Level.BEGINNER

    /**
     * Bir sonraki seviyenin eşiği (min ⭐). Zaten en üst seviyedeyse (MASTER) null döner —
     * "bir sonraki seviye" kavramı yok, çağıran taraf bu durumda ilerleme satırını gizler.
     */
    fun nextLevelAt(stars: Int): Int? {
        val current = levelFor(stars)
        val next = Level.entries.getOrNull(current.ordinal + 1) ?: return null
        return next.minStars
    }

    /** Bir sonraki seviyeye kalan ⭐ sayısı. En üst seviyedeyse null. */
    fun starsToNextLevel(stars: Int): Int? {
        val nextAt = nextLevelAt(stars) ?: return null
        return (nextAt - stars).coerceAtLeast(0)
    }

    /**
     * Mevcut seviye içindeki ilerleme oranı (0f-1f) — [Level.MASTER] için her zaman 1f
     * (üst sınır yok, "tamamlandı" olarak yorumlanır).
     */
    fun progressToNext(stars: Int): Float {
        val current = levelFor(stars)
        val nextAt = nextLevelAt(stars) ?: return 1f
        val span = (nextAt - current.minStars).toFloat()
        if (span <= 0f) return 1f
        return ((stars - current.minStars).toFloat() / span).coerceIn(0f, 1f)
    }
}

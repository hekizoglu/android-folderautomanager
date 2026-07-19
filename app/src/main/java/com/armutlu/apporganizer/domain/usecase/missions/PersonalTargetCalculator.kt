package com.armutlu.apporganizer.domain.usecase.missions

/**
 * Dongu G1 (GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md) — sabit gorev hedefi yerine kullanicinin
 * kendi son 7 gunluk gecmisinden turetilen kisisel hedef. Saf Kotlin, Android bagimliligi yok.
 *
 * Formul: hedef = medyan(son N gun) x tempo katsayisi (G7 — Rahat 1.0 / Dengeli 0.9 / Iddiali 0.8).
 * Sonuc [minClamp]..[maxClamp] araligina sikistirilir. Veri < [MIN_DAYS_REQUIRED] gun ise null
 * doner ("tanisma modu" — cagiran taraf sabit varsayilana duser).
 *
 * Ust sinir gorevleri (ekran suresi/kilit acma) icin "dunden biraz daha az" hissi verir; tempo
 * katsayisi 1.0'dan kucuk oldukca hedef gecmis medyanin ALTINA çekilir (daha iddiali sinirlama).
 */
object PersonalTargetCalculator {
    const val MIN_DAYS_REQUIRED = 3

    const val SCREEN_MIN_MINUTES = 60L
    const val SCREEN_MAX_MINUTES = 360L
    const val UNLOCK_MIN_COUNT = 15L
    const val UNLOCK_MAX_COUNT = 80L

    /**
     * @param dailyValues son gunlerin gunluk degerleri (ekran suresi dakika veya kilit acma adedi).
     * Sira onemli degil — medyan hesaplanir. Bos/gecersiz (negatif) degerler filtrelenmez, cagiran
     * taraf temiz veri gecirmelidir.
     * @param tempo G7 katsayisi (RAHAT=1.0, DENGELI=0.9, IDDIALI=0.8 — bkz. AppPrefs.MissionTempo).
     * @return kisisel hedef, veri yetersizse null (tanisma modu).
     */
    fun calculateScreenTimeTarget(dailyValues: List<Long>, tempo: Double): Long? =
        calculate(dailyValues, tempo, SCREEN_MIN_MINUTES, SCREEN_MAX_MINUTES)

    fun calculateUnlockTarget(dailyValues: List<Long>, tempo: Double): Long? =
        calculate(dailyValues, tempo, UNLOCK_MIN_COUNT, UNLOCK_MAX_COUNT)

    /** Genel amacli medyan x tempo + clamp — testlerde dogrudan da cagrilabilir. */
    fun calculate(dailyValues: List<Long>, tempo: Double, minClamp: Long, maxClamp: Long): Long? {
        if (dailyValues.size < MIN_DAYS_REQUIRED) return null
        val median = median(dailyValues)
        val target = (median * tempo).toLong()
        return target.coerceIn(minClamp, maxClamp)
    }

    private fun median(values: List<Long>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid].toDouble()
        }
    }
}

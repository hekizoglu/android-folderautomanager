package com.armutlu.apporganizer.domain.usecase.missions

import com.armutlu.apporganizer.domain.models.Category

/**
 * Dongu G3b (GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md G3 - "Uygulama-spesifik" gorev) —
 * saf Kotlin aday secim + hedef hesabi. Android/DB bagimliligi yok, unit test edilebilir.
 *
 * Amac: kullanicinin SOSYAL/OYUN/VIDEO kategorisinde en cok kullandigi (ve gercekten alisilmis
 * yuksek kullanimli) uygulamayi bulup, o uygulamaya ozel gunluk "kullanimi sinirla" gorevi
 * (DAILY_APP_LIMIT) icin aday olarak sunmak. Uygulama adi/paketi SADECE gorev METNINDE gorunur,
 * hicbir zaman telemetriye/diagnostics'e gitmez (U02 kurali - bkz. MissionEngine.WeakAreaCategory
 * gibi kategori-bazli alanlarin aksine, burada paket YALNIZCA UI/baslik uretimi icin tasinir).
 */
object AppLimitCandidateSelector {

    /** Aday olabilmesi icin son 7 gunun GUNLUK ORTALAMASI en az bu kadar dakika olmali. */
    const val MIN_DAILY_AVERAGE_MINUTES = 30L

    /** Hedef, medyanin bu oraninda baslar (tempo katsayisi SONRA ayrica uygulanir). */
    const val MEDIAN_TARGET_RATIO = 0.8

    /** Hedefin asla altina inmeyecegi taban (dakika) - tempo/oran ne kadar sikistirirsa sikistirsin. */
    const val MIN_TARGET_MINUTES = 15L

    /** Hedefin yukari sinirini PersonalTargetCalculator'daki ekran suresi max'i ile hizali tutar. */
    const val MAX_TARGET_MINUTES = PersonalTargetCalculator.SCREEN_MAX_MINUTES

    /** Aday secimine giren kategoriler (sosyal/oyun/video - roadmap G3 tanimi). */
    val ELIGIBLE_CATEGORY_IDS = setOf(
        Category.CAT_SOCIAL,
        Category.CAT_GAMES,
        Category.CAT_VIDEO,
    )

    /**
     * Tek bir paketin son 7 gunluk kullanim gecmisi - caller (MissionMetricSnapshotProvider)
     * per-package gunluk dakika listesini + kategori id'sini hazirlar, bu fonksiyon hicbir
     * Android/DB cagrisi yapmaz.
     */
    data class PackageUsageCandidate(
        val packageName: String,
        val categoryId: String,
        /** Son (en fazla 7) TAMAMLANMIS gunun gunluk dakikalari, eskiden yeniye veya sirasiz olabilir. */
        val dailyMinutesLast7Days: List<Long>,
    )

    data class SelectedAppLimitTarget(
        val packageName: String,
        val targetMinutes: Long,
    )

    /**
     * Aday secimi + hedef hesabi tek adimda. Uygun aday yoksa null doner (gorev havuza girmez -
     * MissionEngine bu null'i gordugunde DAILY_APP_LIMIT'i eligible saymaz).
     *
     * Secim kurali: eligible kategoride, gunluk ortalamasi >= [MIN_DAILY_AVERAGE_MINUTES] olan
     * adaylar arasindan EN YUKSEK gunluk ortalamaya sahip olan secilir (kullanicinin en cok
     * zaman harcadigi tek uygulama - birden fazla dusuk-orta kullanimli uygulama yerine).
     *
     * @param tempo G7 tempo katsayisi (RAHAT=1.0/DENGELI=0.9/IDDIALI=0.8) - PersonalTargetCalculator
     * ile AYNI sozlesme: medyan*oran sonrasi ayrica carpilir.
     */
    fun selectCandidate(
        candidates: List<PackageUsageCandidate>,
        tempo: Double,
    ): SelectedAppLimitTarget? {
        val eligible = candidates
            .filter { it.categoryId in ELIGIBLE_CATEGORY_IDS }
            .filter { it.dailyMinutesLast7Days.isNotEmpty() }
            .filter { average(it.dailyMinutesLast7Days) >= MIN_DAILY_AVERAGE_MINUTES }
        val winner = eligible.maxByOrNull { average(it.dailyMinutesLast7Days) } ?: return null

        val target = calculateTarget(winner.dailyMinutesLast7Days, tempo) ?: return null
        return SelectedAppLimitTarget(packageName = winner.packageName, targetMinutes = target)
    }

    /**
     * Hedef = medyan(son N gun) x [MEDIAN_TARGET_RATIO] x tempo, [MIN_TARGET_MINUTES]..
     * [MAX_TARGET_MINUTES] araligina sikistirilir. Veri bossa null (aday zaten bu durumda
     * elenmis olur, ama fonksiyon dogrudan da test edilebilsin diye ayri tutulur).
     */
    fun calculateTarget(dailyMinutes: List<Long>, tempo: Double): Long? {
        if (dailyMinutes.isEmpty()) return null
        val median = median(dailyMinutes)
        val target = (median * MEDIAN_TARGET_RATIO * tempo).toLong()
        return target.coerceIn(MIN_TARGET_MINUTES, MAX_TARGET_MINUTES)
    }

    private fun average(values: List<Long>): Double =
        if (values.isEmpty()) 0.0 else values.sum().toDouble() / values.size

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

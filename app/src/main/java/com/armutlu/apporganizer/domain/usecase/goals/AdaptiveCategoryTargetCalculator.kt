package com.armutlu.apporganizer.domain.usecase.goals

/**
 * P2 — roadmap §4. Kullanıcıdan dakika tahmini istemeden, geçmiş kategori kullanımından haftalık
 * "en fazla kullanım" hedefi türetir. Saf Kotlin, Android/Room/Context bağımlılığı YOK — unit test
 * edilebilir ([PersonalTargetCalculator] ile aynı stil, farklı formül: blend + %15 klips + outlier).
 *
 * Formül (roadmap §4.4):
 * - İlk hedef = önceki tam hafta kullanımı × tempo katsayısı.
 * - Sonraki haftalarda: harmanlanmışTaban = önceki gerçek kullanım × 0.60 + önceki hedef × 0.40;
 *   hamYeniHedef = harmanlanmışTaban × tempo katsayısı.
 * - Sonuç en yakın 5 dakikaya yuvarlanır, [MIN_TARGET_MINUTES]..[MAX_TARGET_MINUTES] aralığına
 *   sıkıştırılır, önceki hedefe göre ±[MAX_CHANGE_RATIO] ile klipslenir (ilk hedef hariç).
 *
 * Veri yeterliliği (§4.1): en az [MIN_VALID_DATA_DAYS] geçerli gün olmadan, kullanım izni yokken
 * veya son hafta 90 dakikanın altındaysa (roadmap §4.2) hedef ÜRETİLMEZ — null döner, çağıran
 * taraf mevcut hedefi korur / tanıma modunu gösterir. Sıfır kullanım ile veri-yokluğu AYRI ele
 * alınır: bu sınıf yalnız [previousWeekMinutes] null OLMAYAN girdilerle çağrılmalıdır (çağıran
 * taraf, [CategoryUsageSnapshot.previousWeekMinutes] null dönerse bu fonksiyonu hiç çağırmaz).
 */
object AdaptiveCategoryTargetCalculator {

    const val MIN_VALID_DATA_DAYS = 5
    const val MIN_ELIGIBLE_WEEKLY_MINUTES = 90L
    const val MIN_TARGET_MINUTES = 60L
    const val MAX_TARGET_MINUTES = 10_080L
    const val MAX_CHANGE_RATIO = 0.15
    const val ROUNDING_STEP_MINUTES = 5L
    private const val ACTUAL_WEIGHT = 0.60
    private const val PREVIOUS_TARGET_WEIGHT = 0.40

    data class Input(
        /** Önceki tamamlanmış ISO haftasının kategori kullanım dakikası (outlier korumalı). */
        val previousWeekActualMinutes: Long,
        /** Daha önce otomatik üretilmiş bir hedef varsa dakikası; ilk hedefte null. */
        val previousTargetMinutes: Long?,
        /** Önceki ISO haftasında UsageStats'ın kaç günü geçerli veri döndürdüğü. */
        val validDataDayCount: Int,
        val pace: AdaptiveGoalPace,
    )

    sealed interface Result {
        /** Hedef üretildi/güncellendi. */
        data class Target(val targetMinutes: Long) : Result

        /** Veri yetersiz (gün sayısı az) — tanıma modu, mevcut hedefe dokunulmaz. */
        data object InsufficientData : Result

        /** Kullanım eşiğin altında (roadmap §4.2 — 90dk altı kategoriye otomatik hedef açılmaz). */
        data object BelowEligibilityThreshold : Result
    }

    fun calculate(input: Input): Result {
        if (input.validDataDayCount < MIN_VALID_DATA_DAYS) return Result.InsufficientData
        if (input.previousWeekActualMinutes < MIN_ELIGIBLE_WEEKLY_MINUTES && input.previousTargetMinutes == null) {
            return Result.BelowEligibilityThreshold
        }

        val previousTarget = input.previousTargetMinutes
        val rawTarget = if (previousTarget == null) {
            // İlk hedef — blend yok, doğrudan önceki tam hafta × tempo.
            input.previousWeekActualMinutes * input.pace.coefficient
        } else {
            val blendedBaseline =
                input.previousWeekActualMinutes * ACTUAL_WEIGHT + previousTarget * PREVIOUS_TARGET_WEIGHT
            blendedBaseline * input.pace.coefficient
        }

        val changeClamped = if (previousTarget != null) {
            val minAllowed = previousTarget * (1 - MAX_CHANGE_RATIO)
            val maxAllowed = previousTarget * (1 + MAX_CHANGE_RATIO)
            rawTarget.coerceIn(minAllowed, maxAllowed)
        } else {
            rawTarget
        }

        // Yuvarlama EN SON yapılır (roadmap: "en yakın 5 dakikaya"). Nearest-rounding ±2.5dk
        // sapma yaratabilir — bu sapma ±%15 klips sınırını veya min/max aralığını dışarı taşırsa
        // (örn. previousTarget küçükken %15'in kendisi 5dk'dan az olabilir) sınırlar yuvarlama
        // SONRASI bir daha uygulanır, sonucun sözleşmeyi ASLA aşmaması garanti edilir.
        val rounded = roundToNearestStep(changeClamped)
        val rangeClamped = rounded.coerceIn(MIN_TARGET_MINUTES, MAX_TARGET_MINUTES)
        val finalTarget = if (previousTarget != null) {
            val minAllowed = (previousTarget * (1 - MAX_CHANGE_RATIO)).toLong()
            val maxAllowed = (previousTarget * (1 + MAX_CHANGE_RATIO)).toLong()
            rangeClamped.coerceIn(minAllowed, maxAllowed)
        } else {
            rangeClamped
        }

        return Result.Target(finalTarget)
    }

    private fun roundToNearestStep(minutes: Double): Long {
        val steps = Math.round(minutes / ROUNDING_STEP_MINUTES)
        return steps * ROUNDING_STEP_MINUTES
    }
}

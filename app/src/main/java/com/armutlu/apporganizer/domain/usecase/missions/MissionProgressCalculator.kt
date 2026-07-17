package com.armutlu.apporganizer.domain.usecase.missions

/**
 * [MissionEvaluation] + [MissionProgressKind] ciftinden UI-hazir [MissionProgress] uretir
 * (Dongu M03 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satir 852-941).
 *
 * Kurallar:
 * - Negatif kalan deger gosterilmez; hedef asildiysa [MissionProgress.exceededValue]'ya tasinir.
 * - `progressFraction` her zaman `0f..1f` araliginda sinirlandirilir.
 * - Ust sinir gorevinde (UPPER_LIMIT) dolu progress cubugu basari anlamina gelmez —
 *   `progressTextRes` "limit kullanimi" etiketi tasir, asilmissa "asildi" etiketine doner.
 * - `currentValue`/`targetValue` null ise (DATA_UNAVAILABLE) hicbir text spec uretilmez.
 */
object MissionProgressCalculator {

    fun calculate(evaluation: MissionEvaluation, kind: MissionProgressKind): MissionProgress {
        val current = evaluation.currentValue
        val target = evaluation.targetValue

        if (current == null || target == null) {
            return MissionProgress(
                currentValue = current,
                targetValue = target,
                remainingValue = null,
                progressFraction = null,
                exceededValue = null,
                currentTextRes = null,
                remainingTextRes = null,
                progressTextRes = null,
            )
        }

        return when (kind) {
            MissionProgressKind.UPPER_LIMIT -> calculateUpperLimit(current, target)
            MissionProgressKind.ACTION_COUNT -> calculateActionCount(current, target)
            MissionProgressKind.BOOLEAN_ACTION -> calculateActionCount(current, target)
            MissionProgressKind.AVOID_AFTER_TIME -> calculateAvoidAfterTime(current, target)
            MissionProgressKind.PERIOD_COMPARISON -> calculatePeriodComparison(current, target)
        }
    }

    /** Ekran suresi / kilit acma gibi ust sinir gorevleri — dakika/adet ayrimini cagiran (kind uzerinden degil, deger buyuklugune gore) degil, ortak dakika bicimi ile ele alir; adet-bazli (kilit acma) ekranlar remaining/current icin count spec'i tercih eder. */
    private fun calculateUpperLimit(current: Long, target: Long): MissionProgress {
        val rawRemaining = target - current
        val reachedOrExceeded = current >= target
        val remaining = rawRemaining.coerceAtLeast(0L)
        // Hedefe ulasilinca (esitlik dahil) kalan artik gosterilmez; exceededValue devreye girer
        // (esitlikte 0, asimda pozitif) — roadmap M03 test senaryosu: 180/180 -> exceeded 0.
        val exceeded = if (reachedOrExceeded) (current - target).coerceAtLeast(0L) else null
        val fraction = fractionOf(current, target)

        val progressText = if (exceeded != null && exceeded > 0L) {
            MissionValueFormatter.exceededCountSpec(exceeded)
        } else {
            MissionValueFormatter.percentUsedSpec(fraction)
        }

        return MissionProgress(
            currentValue = current,
            targetValue = target,
            remainingValue = if (reachedOrExceeded) null else remaining,
            progressFraction = fraction,
            exceededValue = exceeded,
            currentTextRes = MissionValueFormatter.currentDurationSpec(current),
            remainingTextRes = if (reachedOrExceeded) null else MissionValueFormatter.remainingDurationSpec(remaining),
            progressTextRes = progressText,
        )
    }

    /** Eylem sayisi / bayrak gorevleri — "12 / 30" adet formati, negatif kalan olamaz (hedef sabit ve current onu asamaz varsayimi ile clamp edilir). */
    private fun calculateActionCount(current: Long, target: Long): MissionProgress {
        val remaining = (target - current).coerceAtLeast(0L)
        val fraction = fractionOf(current, target)
        return MissionProgress(
            currentValue = current,
            targetValue = target,
            remainingValue = remaining,
            progressFraction = fraction,
            exceededValue = null,
            currentTextRes = MissionValueFormatter.currentCountSpec(current, target),
            remainingTextRes = MissionValueFormatter.remainingCountSpec(remaining),
            progressTextRes = MissionValueFormatter.percentUsedSpec(fraction),
        )
    }

    /** Gece kullanmama gorevi — current/target 0L/1L bayrak degerleridir (bkz. MissionEngine.evaluateNoLateNight), sayisal ilerleme cubugu anlamli degildir. */
    private fun calculateAvoidAfterTime(current: Long, target: Long): MissionProgress {
        val fraction = if (current > 0L) 1f else 0f
        return MissionProgress(
            currentValue = current,
            targetValue = target,
            remainingValue = if (current > 0L) null else 0L,
            progressFraction = fraction,
            exceededValue = if (current > 0L) current else null,
            currentTextRes = null,
            remainingTextRes = null,
            progressTextRes = null,
        )
    }

    /** Haftalik karsilastirma — current=bu hafta, target=gecen hafta (baseline); "kalan" bu hafta gecen haftaya gore ne kadar dusurulmesi gerektigidir. */
    private fun calculatePeriodComparison(current: Long, target: Long): MissionProgress {
        val rawRemaining = target - current
        val reachedOrExceeded = current >= target
        val remaining = rawRemaining.coerceAtLeast(0L)
        val exceeded = if (reachedOrExceeded) (current - target).coerceAtLeast(0L) else null
        val fraction = fractionOf(current, target)
        return MissionProgress(
            currentValue = current,
            targetValue = target,
            remainingValue = if (reachedOrExceeded) null else remaining,
            progressFraction = fraction,
            exceededValue = exceeded,
            currentTextRes = MissionValueFormatter.currentDurationSpec(current),
            remainingTextRes = if (reachedOrExceeded) null else MissionValueFormatter.remainingDurationSpec(remaining),
            progressTextRes = if (exceeded != null && exceeded > 0L) {
                MissionValueFormatter.exceededDurationSpec(exceeded)
            } else {
                MissionValueFormatter.percentUsedSpec(fraction)
            },
        )
    }

    /** fraction her zaman 0f..1f — target <= 0 ise (bolme hatasi/anlamsiz hedef) 0f doner. */
    private fun fractionOf(current: Long, target: Long): Float {
        if (target <= 0L) return 0f
        return (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    }
}

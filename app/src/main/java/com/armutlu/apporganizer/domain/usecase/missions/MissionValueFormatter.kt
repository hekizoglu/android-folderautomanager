package com.armutlu.apporganizer.domain.usecase.missions

import com.armutlu.apporganizer.R

/**
 * Sure/miktar degerlerini [MissionTextSpec]'e cevirir (Dongu M03 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satir 852-941).
 *
 * Saf Kotlin — yalnizca resource id + argumanlari tasir, string uretmez. UI/ViewModel
 * `context.getString(spec.resId, *spec.args.toTypedArray())` ile cozer.
 */
object MissionValueFormatter {

    /**
     * Dakikayi "1 sa. 30 dk." / "2 sa." / "45 dk." bicimine cevirir.
     * 0 dakika "0 dk." olarak gosterilir (gercek 0 ile veri-yok ayrimi cagiran tarafta yapilir).
     */
    fun durationSpec(totalMinutes: Long): MissionTextSpec {
        val minutes = totalMinutes.coerceAtLeast(0L)
        val hours = minutes / 60
        val remainderMinutes = minutes % 60
        return when {
            hours > 0 && remainderMinutes > 0 ->
                MissionTextSpec(R.string.mission_duration_hours_minutes, listOf(hours, remainderMinutes))
            hours > 0 -> MissionTextSpec(R.string.mission_duration_hours_only, listOf(hours))
            else -> MissionTextSpec(R.string.mission_duration_minutes_only, listOf(remainderMinutes))
        }
    }

    /**
     * "Şu an: <süre>" — dakika bazlı gorevler icin (ekran suresi). Ic ice [MissionTextSpec]
     * doner: dis spec'in tek argumani, ic (sure) spec'inin kendisidir. Cozumleme iki asamalidir
     * — cagiran taraf (MissionsViewModel.resolveTextSpec) once ic spec'i coze, sonucu String
     * olarak dis spec'in argumanina koyar. Bu, "1 sa. 30 dk." gibi bilesik surelerin ayri
     * plural/format mantigi olmadan tek noktadan (durationSpec) uretilmesini saglar.
     */
    fun currentDurationSpec(totalMinutes: Long): MissionTextSpec =
        MissionTextSpec(R.string.mission_progress_current_duration, listOf(durationSpec(totalMinutes)))

    /** "Şu an: 12 / 30" — adet bazli gorevler icin (kilit acma, eylem sayisi). */
    fun currentCountSpec(current: Long, target: Long): MissionTextSpec =
        MissionTextSpec(R.string.mission_progress_current_count, listOf(current, target))

    /** "Kalan: <süre>" — negatif olamaz, cagiran taraf 0'a clamp'lenmis deger gecirmelidir. */
    fun remainingDurationSpec(remainingMinutes: Long): MissionTextSpec =
        MissionTextSpec(R.string.mission_progress_remaining_duration, listOf(durationSpec(remainingMinutes)))

    /** "Kalan: 18" — adet bazli gorevler icin. */
    fun remainingCountSpec(remaining: Long): MissionTextSpec =
        MissionTextSpec(R.string.mission_progress_remaining_count, listOf(remaining))

    /** "Limitin %50'si kullanıldı" — ust sinir gorevlerinde ozet metin. */
    fun percentUsedSpec(progressFraction: Float): MissionTextSpec {
        val percent = (progressFraction.coerceIn(0f, 1f) * 100).toInt()
        return MissionTextSpec(R.string.mission_progress_percent_used, listOf(percent))
    }

    /** "<süre> aşıldı" — ust sinir asildiginda. */
    fun exceededDurationSpec(exceededMinutes: Long): MissionTextSpec =
        MissionTextSpec(R.string.mission_progress_exceeded_duration, listOf(durationSpec(exceededMinutes)))

    /** "<adet> aşıldı" — adet bazli ust sinirlar icin. */
    fun exceededCountSpec(exceeded: Long): MissionTextSpec =
        MissionTextSpec(R.string.mission_progress_exceeded_count, listOf(exceeded))
}

package com.armutlu.apporganizer.domain.usecase.goals

/**
 * P2 — Adaptif kategori hedefi tempo katsayıları (roadmap §4.3 / §9 karar 6). Görev sisteminin
 * kendi [com.armutlu.apporganizer.utils.AppPrefs.MissionTempo] (1.0/0.9/0.8) ile AYNI isim/UI
 * kavramını paylaşır ama kategori hedefleri için daha az sert katsayılar taşır — üst-sınır
 * kategori hedefi, günlük görev hedefinden farklı bir "biraz azalt" hissi hedefler.
 *
 * UI'da kullanıcıya TEK bir "tempo" ayarı olarak sunulur (Rahat/Dengeli/İddialı) — arka planda
 * [com.armutlu.apporganizer.utils.AppPrefs.MissionTempo] ile aynı isimle eşleşir, [fromMissionTempo]
 * bu eşlemeyi tek yerde tutar.
 */
enum class AdaptiveGoalPace(val coefficient: Double) {
    RAHAT(0.95),
    DENGELI(0.90),
    IDDIALI(0.85),
    ;

    companion object {
        val DEFAULT = DENGELI

        fun fromMissionTempo(tempo: com.armutlu.apporganizer.utils.AppPrefs.MissionTempo): AdaptiveGoalPace =
            when (tempo) {
                com.armutlu.apporganizer.utils.AppPrefs.MissionTempo.RAHAT -> RAHAT
                com.armutlu.apporganizer.utils.AppPrefs.MissionTempo.DENGELI -> DENGELI
                com.armutlu.apporganizer.utils.AppPrefs.MissionTempo.IDDIALI -> IDDIALI
            }
    }
}

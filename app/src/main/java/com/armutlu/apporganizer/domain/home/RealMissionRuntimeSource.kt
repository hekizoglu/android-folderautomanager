package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.usecase.missions.MissionSummaryUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dongu M07 — Ana ekran "Görevler" kartinin gercek veri kaynagi. [MissionSummaryUseCase]
 * ile AYNI hesaplama yolunu (Gorevler ekrani ile paylasilan) kullanir, ama
 * `awardStars = false` ile cagirir: bu kaynak arka planda (HOME_RESUME/APP_START gibi
 * kullanicinin fark etmedigi anlarda) sessizce tetiklenebilir, bu yuzden yildiz yazimi/DB
 * yan etkisi YAPMAZ — sadece gorevlerin GUNCEL durumunu okur.
 *
 * HomeIntelligenceCoordinator diger kaynaklar gibi refresh()'i kendi try/catch'i icinde
 * cagirir (bkz. HomeIntelligenceCoordinator.refreshSource) — burada ikinci bir try/catch
 * EKLENMEZ, DigitalPulseRepository/NoOp kaynaklar da ayni deseni izler.
 */
@Singleton
class RealMissionRuntimeSource @Inject constructor(
    private val missionSummaryUseCase: MissionSummaryUseCase,
) : MissionRuntimeRepository {

    private val _state = MutableStateFlow(MissionSourceState())
    override val state: StateFlow<MissionSourceState> = _state.asStateFlow()

    override suspend fun refresh() {
        val result = missionSummaryUseCase.compute(awardStars = false)
        val combined = result.daily + result.weekly
        val summary = HomeMissionSummarySelector.build(combined)
        _state.value = MissionSourceState(summary = summary)
    }
}

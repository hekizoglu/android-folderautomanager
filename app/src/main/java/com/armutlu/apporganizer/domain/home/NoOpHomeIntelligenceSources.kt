package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.MissingReason
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Döngü H02 — geçici no-op implementasyonlar. D00 döngüsünde [DigitalPulseRepository]
 * gerçek kaynağa ([RealDigitalPulseSource]) bağlandı; bu sınıf artık yalnızca testler/DI
 * fallback'i için tutulur — refresh() hiçbir şey yapmaz, state hep Missing döner.
 */
@Singleton
class NoOpDigitalPulseSource @Inject constructor() : DigitalPulseRepository {
    private val _state = MutableStateFlow<HomeDataResult<DigitalPulseSnapshot>>(
        HomeDataResult.Missing(MissingReason.NO_DATA_YET),
    )
    override val state: StateFlow<HomeDataResult<DigitalPulseSnapshot>> = _state.asStateFlow()
    override suspend fun refresh(force: Boolean) = Unit
}

@Singleton
class NoOpMissionRuntimeSource @Inject constructor() : MissionRuntimeRepository {
    private val _state = MutableStateFlow(MissionSourceState())
    override val state: StateFlow<MissionSourceState> = _state.asStateFlow()
    override suspend fun refresh() = Unit
}

@Singleton
class NoOpSmartTickerSource @Inject constructor() : SmartTickerEngine {
    private val _state = MutableStateFlow(TickerSourceState())
    override val state: StateFlow<TickerSourceState> = _state.asStateFlow()
    override suspend fun refresh() = Unit
}

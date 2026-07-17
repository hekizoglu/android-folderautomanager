package com.armutlu.apporganizer.domain.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Döngü H02 — geçici no-op implementasyonlar. D00/M/T döngüleri gerçek repository/engine'leri
 * bağlayana kadar uygulamanın derlenmesini ve çalışmasını sağlar. refresh() hiçbir şey yapmaz,
 * state hep boş placeholder döner.
 */
@Singleton
class NoOpDigitalPulseSource @Inject constructor() : DigitalPulseRepository {
    private val _state = MutableStateFlow(PulseSourceState())
    override val state: StateFlow<PulseSourceState> = _state.asStateFlow()
    override suspend fun refresh() = Unit
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

package com.armutlu.apporganizer.domain.home

import kotlinx.coroutines.flow.StateFlow

/**
 * Döngü H02 — MINIMAL sözleşme. Gerçek implementasyon M döngülerinde bağlanacak
 * (bkz. roadmap §3.2 MissionRuntimeState). [MissionSourceState] şimdilik placeholder'dır;
 * M döngüsü bu tipi gerçek `MissionRuntimeState` listesiyle değiştirebilir.
 */
interface MissionRuntimeRepository {
    val state: StateFlow<MissionSourceState>
    suspend fun refresh()
}

/** Placeholder kaynak durumu — M döngüsünde roadmap §3.2 MissionRuntimeState'e evrilecek. */
data class MissionSourceState(
    val missions: List<Any> = emptyList(),
)

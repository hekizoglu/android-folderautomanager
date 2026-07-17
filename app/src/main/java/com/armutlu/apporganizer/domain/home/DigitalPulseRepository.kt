package com.armutlu.apporganizer.domain.home

import kotlinx.coroutines.flow.StateFlow

/**
 * Döngü H02 — MINIMAL sözleşme. Gerçek implementasyon D00 döngülerinde bağlanacak
 * (bkz. roadmap §3.1 DigitalPulseSnapshot). [PulseSourceState] şimdilik placeholder'dır;
 * D00 döngüsü bu tipi gerçek `DigitalPulseSnapshot` ile değiştirebilir veya sarmalayabilir.
 */
interface DigitalPulseRepository {
    val state: StateFlow<PulseSourceState>
    suspend fun refresh()
}

/** Placeholder kaynak durumu — D00 döngüsünde roadmap §3.1 DigitalPulseSnapshot'a evrilecek. */
data class PulseSourceState(
    val snapshot: Any? = null,
)

package com.armutlu.apporganizer.domain.home

import kotlinx.coroutines.flow.StateFlow

/**
 * Döngü H02 — MINIMAL sözleşme. Gerçek implementasyon T döngülerinde bağlanacak
 * (bkz. roadmap §3.3 SmartTickerItem). [TickerSourceState] şimdilik placeholder'dır;
 * T döngüsü bu tipi gerçek `SmartTickerItem` listesiyle değiştirebilir.
 */
interface SmartTickerEngine {
    val state: StateFlow<TickerSourceState>
    suspend fun refresh()
}

/** Placeholder kaynak durumu — T döngüsünde roadmap §3.3 SmartTickerItem'a evrilecek. */
data class TickerSourceState(
    val items: List<Any> = emptyList(),
)

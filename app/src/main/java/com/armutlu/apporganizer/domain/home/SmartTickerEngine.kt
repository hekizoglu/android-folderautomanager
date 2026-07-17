package com.armutlu.apporganizer.domain.home

import kotlinx.coroutines.flow.StateFlow

/**
 * Döngü H02 — MINIMAL sözleşme. Döngü T01'de gerçek implementasyona bağlandı
 * (bkz. [RealSmartTickerSource], roadmap §3.3 SmartTickerItem).
 */
interface SmartTickerEngine {
    val state: StateFlow<TickerSourceState>
    suspend fun refresh()
}

/**
 * Kaynak durumu. Döngü H02'de `items: List<Any>` placeholder'dı; Döngü T01'de gerçek
 * [SmartTickerItem] tipine bağlandı — [RealSmartTickerSource] bu alanı doldurur,
 * [NoOpSmartTickerSource] boş liste bırakır.
 */
data class TickerSourceState(
    val items: List<SmartTickerItem> = emptyList(),
)

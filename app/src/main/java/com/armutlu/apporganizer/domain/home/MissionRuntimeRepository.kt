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

/**
 * Kaynak durumu. [missions] eski placeholder alan — HomeIntelligenceCoordinatorTest ve diğer
 * mevcut testler bunu opak (`List<Any>`, örn. `listOf("mission-1")`) test verisiyle kullanır,
 * o yüzden davranışını değiştirmeden korunur. Dönü M07: gerçek ana ekran kartı verisi
 * [summary] alanından akar — [RealMissionRuntimeSource] bu alanı doldurur, no-op kaynak
 * her ikisini de varsayılan (boş/null) bırakır.
 */
data class MissionSourceState(
    val missions: List<Any> = emptyList(),
    val summary: HomeMissionSummary? = null,
)

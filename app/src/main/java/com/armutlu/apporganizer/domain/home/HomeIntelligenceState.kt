package com.armutlu.apporganizer.domain.home

/**
 * Ana ekranın birleşik durumu — HomeIntelligenceCoordinator.state bu tipi taşır.
 * Döngü H02: alanlar kaynak interface'lerinin placeholder tipleri; roadmap §3.4
 * HomeIntelligenceUiState'e (missionSummary/pulseSummary/tickerItems) evrilecek —
 * o dönüşüm ilgili D00/M/T döngüleri gerçek modelleri bağladıkça yapılacak.
 */
data class HomeIntelligenceState(
    val pulse: PulseSourceState? = null,
    val mission: MissionSourceState? = null,
    val ticker: TickerSourceState? = null,
    val loading: Boolean = false,
    val lastRefreshAt: Long? = null,
    val lastRefreshReason: RefreshReason? = null,
)

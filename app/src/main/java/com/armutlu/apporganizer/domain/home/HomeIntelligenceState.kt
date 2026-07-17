package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.MissingReason

/**
 * Ana ekranın birleşik durumu — HomeIntelligenceCoordinator.state bu tipi taşır.
 * Döngü H02: alanlar kaynak interface'lerinin placeholder tipleri; roadmap §3.4
 * HomeIntelligenceUiState'e (missionSummary/pulseSummary/tickerItems) evrilecek —
 * o dönüşüm ilgili D00/M/T döngüleri gerçek modelleri bağladıkça yapılacak.
 *
 * Döngü H04: her kaynak alanı artık çıplak nullable değil, [HomeDataResult] ile
 * sarmalı — bir kaynağın başarısız/eksik olması diğer alanları veya ekranı etkilemez,
 * durum (Ready/Stale/Missing/Failed) tip güvenli şekilde taşınır.
 */
data class HomeIntelligenceState(
    val pulse: HomeDataResult<PulseSourceState> = HomeDataResult.Missing(MissingReason.NO_DATA_YET),
    val mission: HomeDataResult<MissionSourceState> = HomeDataResult.Missing(MissingReason.NO_DATA_YET),
    val ticker: HomeDataResult<TickerSourceState> = HomeDataResult.Missing(MissingReason.NO_DATA_YET),
    val loading: Boolean = false,
    val lastRefreshAt: Long? = null,
    val lastRefreshReason: RefreshReason? = null,
)

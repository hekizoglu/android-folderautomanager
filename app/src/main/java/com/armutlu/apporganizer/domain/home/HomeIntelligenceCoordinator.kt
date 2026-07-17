package com.armutlu.apporganizer.domain.home

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Döngü H02 — Ana ekranın görev/skor/şerit verisini üç ayrı dağınık kaynaktan değil
 * tek koordinatörden almasını sağlayan iskelet.
 *
 * SADECE orkestrasyon yapar: hiçbir skor veya görev kuralı burada HESAPLANMAZ —
 * bu sorumluluk [DigitalPulseRepository]/[MissionRuntimeRepository]/[SmartTickerEngine]
 * implementasyonlarındadır (D00/M/T döngüleri).
 *
 * Kurallar:
 * - Aynı anda tek refresh çalışır — eşzamanlı çağrılar aynı çalışan işe katılır
 *   (in-flight [Deferred] paylaşımı), yeni bir refresh başlatmazlar.
 * - Son başarılı snapshot korunur; bir kaynağın geçici hatası UI state'ini boşaltmaz
 *   (kaynak bazlı try/catch — hatalı kaynak eski değerini korur).
 * - Ağ bağımlılığı yoktur.
 * - IO işleri [ioDispatcher] üzerinde ([withContext]) çalışır.
 */
@Singleton
class HomeIntelligenceCoordinator @Inject constructor(
    private val digitalPulseRepository: DigitalPulseRepository,
    private val missionRuntimeRepository: MissionRuntimeRepository,
    private val smartTickerEngine: SmartTickerEngine,
    @HomeIoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    // İç işi başlatmak/paylaşmak için kısa ömürlü coroutine scope — refresh() çağıran taraf
    // suspend olarak bekler, ama işin kendisi burada tek bir Deferred'e toplanır ki eşzamanlı
    // çağrılar aynı işe katılsın (yeni iş başlatmasınlar).
    private val coordinatorJob = SupervisorJob()
    private val coordinatorScope = kotlinx.coroutines.CoroutineScope(coordinatorJob + ioDispatcher)

    private val inFlightMutex = Mutex()
    private var inFlightRefresh: Deferred<Unit>? = null

    private val _state = MutableStateFlow(HomeIntelligenceState())
    val state: StateFlow<HomeIntelligenceState> = _state.asStateFlow()

    /**
     * Üç kaynağı yeniler; her kaynak bağımsız try/catch ile korunur — biri hata atarsa
     * diğer ikisinin yeni değeri state'e girer, hatalı kaynağın son başarılı değeri korunur.
     *
     * Eşzamanlı çağrılar tek işe düşer: çağrı sırasında zaten bir refresh sürüyorsa, bu çağrı
     * yeni bir iş başlatmak yerine sürmekte olan işe katılır ve onun bitişini bekler.
     */
    suspend fun refresh(reason: RefreshReason) {
        Timber.d("HomeIntelligenceCoordinator.refresh: reason=$reason")
        val deferred = inFlightMutex.withLock {
            inFlightRefresh?.takeIf { it.isActive } ?: coordinatorScope.async {
                runRefresh(reason)
            }.also { inFlightRefresh = it }
        }
        deferred.await()
    }

    private suspend fun runRefresh(reason: RefreshReason) {
        _state.update { it.copy(loading = true) }

        val pulseResult = refreshSource(
            sourceName = "DigitalPulseRepository",
            previous = _state.value.pulse,
        ) {
            digitalPulseRepository.refresh()
            digitalPulseRepository.state.value
        }

        val missionResult = refreshSource(
            sourceName = "MissionRuntimeRepository",
            previous = _state.value.mission,
        ) {
            missionRuntimeRepository.refresh()
            missionRuntimeRepository.state.value
        }

        val tickerResult = refreshSource(
            sourceName = "SmartTickerEngine",
            previous = _state.value.ticker,
        ) {
            smartTickerEngine.refresh()
            smartTickerEngine.state.value
        }

        _state.update {
            it.copy(
                pulse = pulseResult,
                mission = missionResult,
                ticker = tickerResult,
                loading = false,
                lastRefreshAt = System.currentTimeMillis(),
                lastRefreshReason = reason,
            )
        }
    }

    /**
     * Tek bir kaynağı IO dispatcher üzerinde yeniler. Hata durumunda [previous] değeri
     * korunarak döndürülür — UI state'i asla boşalmaz.
     */
    private suspend fun <T> refreshSource(
        sourceName: String,
        previous: T?,
        block: suspend () -> T,
    ): T? = withContext(ioDispatcher) {
        runCatching { block() }
            .onFailure { Timber.w(it, "HomeIntelligenceCoordinator: $sourceName refresh hatası, önceki değer korunuyor") }
            .getOrDefault(previous)
    }
}

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
import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.HomeErrorCodes
import com.armutlu.apporganizer.domain.common.MissingReason
import com.armutlu.apporganizer.domain.common.valueOrNull
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
            errorCode = HomeErrorCodes.PULSE_COMPUTE_FAILED,
        ) {
            digitalPulseRepository.refresh()
            // DigitalPulseRepository.state kendi HomeDataResult sarmalamasini D00'dan itibaren
            // yapiyor (15 dk cache + Ready/Stale/Failed) — koordinator burada onu tekrar
            // sarmalamaz, PulseSourceState.snapshot alanina duzlestirir (valueOrNull: Ready/Stale
            // -> snapshot dolu, Missing/Failed -> null). Kaynagin kendi hata durumu boylece
            // korunur ama coordinator'in genel refreshSource sozlesmesiyle uyumlu kalir.
            PulseSourceState(snapshot = digitalPulseRepository.state.value.valueOrNull())
        }

        val missionResult = refreshSource(
            sourceName = "MissionRuntimeRepository",
            previous = _state.value.mission,
            errorCode = HomeErrorCodes.MISSION_SETTLEMENT_FAILED,
        ) {
            missionRuntimeRepository.refresh()
            missionRuntimeRepository.state.value
        }

        val tickerResult = refreshSource(
            sourceName = "SmartTickerEngine",
            previous = _state.value.ticker,
            errorCode = HomeErrorCodes.NOTIFICATION_DATA_UNAVAILABLE,
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
     * Tek bir kaynağı IO dispatcher üzerinde yeniler ve sonucu [HomeDataResult] ile sarar
     * (Döngü H04). Kaynak başarılı olursa [HomeDataResult.Ready]; hata atarsa ve önceki
     * başarılı bir değer varsa [HomeDataResult.Stale] (UI eski veriyi göstermeye devam eder,
     * [errorCode] uyarı olarak taşınır); hata atarsa ve önceki değer de yoksa
     * [HomeDataResult.Failed]. UI state'i hiçbir durumda boşalmaz.
     */
    private suspend fun <T> refreshSource(
        sourceName: String,
        previous: HomeDataResult<T>,
        errorCode: String,
        block: suspend () -> T,
    ): HomeDataResult<T> = withContext(ioDispatcher) {
        runCatching { block() }
            .fold(
                onSuccess = { HomeDataResult.Ready(it) },
                onFailure = { error ->
                    Timber.w(error, "HomeIntelligenceCoordinator: $sourceName refresh hatası")
                    when (previous) {
                        is HomeDataResult.Ready -> HomeDataResult.Stale(previous.value, errorCode)
                        is HomeDataResult.Stale -> HomeDataResult.Stale(previous.value, errorCode)
                        is HomeDataResult.Missing,
                        is HomeDataResult.Failed -> HomeDataResult.Failed(errorCode)
                    }
                },
            )
    }
}

package com.armutlu.apporganizer.domain.home

import android.content.Context
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.HomeErrorCodes
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseEngine
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInputFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Döngü D00 — Dijital Nabız için GERÇEK TEK kaynak. [PulseInputFactory] ile [PulseInput]
 * hazırlar, [DigitalPulseEngine.compute] çağırır (motorun hesap mantığına dokunulmaz) ve
 * sonucu [DigitalPulseSnapshot] olarak 15 dakika cache'ler.
 *
 * Eskiden bu hazırlık `PulseClockViewModel.compute()` içindeydi ve `TickerComposer`
 * bağımsız olarak KENDİ skorunu hesaplıyordu (P0 2.1) — artık PulseClockViewModel,
 * WrappedViewModel ve ana ekran ticker'ı hepsi bu tek [state] StateFlow'unu okur.
 */
@Singleton
class RealDigitalPulseSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val notificationEventDao: NotificationEventDao,
) : DigitalPulseRepository {

    private val _state = MutableStateFlow<HomeDataResult<DigitalPulseSnapshot>>(
        HomeDataResult.Missing(com.armutlu.apporganizer.domain.common.MissingReason.NO_DATA_YET),
    )
    override val state: StateFlow<HomeDataResult<DigitalPulseSnapshot>> = _state.asStateFlow()

    private val refreshMutex = Mutex()

    override suspend fun refresh(force: Boolean) {
        refreshMutex.withLock {
            val now = System.currentTimeMillis()
            val cached = (_state.value as? HomeDataResult.Ready)?.value
                ?: (_state.value as? HomeDataResult.Stale)?.value
            if (!isCacheExpired(cached, now, force)) {
                // Cache taze — yeniden hesaplama yapma.
                return
            }

            runCatching {
                val input = PulseInputFactory.build(
                    context = context,
                    appRepository = appRepository,
                    notificationEventDao = notificationEventDao,
                    nowMillis = now,
                )
                val score = DigitalPulseEngine.compute(input)
                DigitalPulseSnapshot(
                    score = score,
                    computedAt = now,
                    validUntil = now + CACHE_TTL_MS,
                )
            }.fold(
                onSuccess = { snapshot -> _state.value = HomeDataResult.Ready(snapshot) },
                onFailure = { error ->
                    Timber.w(error, "RealDigitalPulseSource: refresh hatasi")
                    val previous = cached
                    _state.value = if (previous != null) {
                        HomeDataResult.Stale(previous, HomeErrorCodes.PULSE_COMPUTE_FAILED)
                    } else {
                        HomeDataResult.Failed(HomeErrorCodes.PULSE_COMPUTE_FAILED)
                    }
                },
            )
        }
    }

    companion object {
        const val CACHE_TTL_MS = 15L * 60 * 1000

        /**
         * Saf karar fonksiyonu (Android bağımlılığı yok, doğrudan unit test edilebilir) —
         * cache'in yeniden hesaplama gerektirip gerektirmediğini belirler. [force]=true her
         * zaman true döner (cache'i aşar); aksi halde [cached] yoksa veya süresi
         * ([DigitalPulseSnapshot.validUntil]) geçtiyse true döner.
         */
        internal fun isCacheExpired(cached: DigitalPulseSnapshot?, nowMillis: Long, force: Boolean): Boolean {
            if (force) return true
            if (cached == null) return true
            return nowMillis >= cached.validUntil
        }
    }
}

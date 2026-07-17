package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import kotlinx.coroutines.flow.StateFlow

/**
 * Döngü D00 — Dijital Nabız için TEK kaynak sözleşmesi (roadmap §3.1 DigitalPulseSnapshot).
 * [DigitalPulseEngine.compute] her zaman burada çağrılır; ana ekran kartı, Pulse Clock ve
 * Haftalık Rapor AYNI [state] StateFlow'unu tüketir — böylece iki farklı skor motorunun
 * aynı cihazda farklı sonuç göstermesi (P0 2.1) mümkün olmaz.
 *
 * [refresh] 15 dakikalık cache uygular; [force]=true cache'i aşıp yeniden hesaplar.
 */
interface DigitalPulseRepository {
    val state: StateFlow<HomeDataResult<DigitalPulseSnapshot>>
    suspend fun refresh(force: Boolean = false)
}

/**
 * Koordinatörün (HomeIntelligenceCoordinator) taşıdığı hafif kaynak durumu — [snapshot] alanı
 * [DigitalPulseSnapshot] taşır. Eskiden `Any?` placeholder'dı (Döngü H02); D00 döngüsünde
 * gerçek tipe bağlandı.
 */
data class PulseSourceState(
    val snapshot: DigitalPulseSnapshot? = null,
)

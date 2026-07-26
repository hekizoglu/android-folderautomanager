package com.armutlu.apporganizer.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LauncherViewModel tamamen [SmartNotificationRepository] enjekte edecek hale gelene kadar
 * mevcut statik servis API'sini güvenli biçimde besleyen geçiş adaptörü.
 *
 * Verinin sahibi repository'dir. Bu nesne sınıflandırma yapmaz, içerik tutmaz ve kalıcı
 * depolamaya yazmaz; yalnız okunmamış/eyleme değer paket sayılarını process içinde yayınlar.
 */
object SmartNotificationLegacyBadgeBridge {
    private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

    internal fun publish(counts: Map<String, Int>) {
        _badgeCounts.value = counts.toMap()
    }

    internal fun clear() {
        _badgeCounts.value = emptyMap()
    }
}

package com.armutlu.apporganizer.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Paket bazlı son okunma zamanını SharedPreferences'ta saklar ve process içinde reaktif yayınlar.
 * Room geçmişiyle bağımsızdır; yalnız launcher rozetinin okunmuş/okunmamış durumunu etkiler.
 */
object NotificationReadPrefs {
    private const val FILE_NAME = "notification_read_prefs"
    private const val KEY_LAST_READ_MAP = "last_read_at_map"
    private val lock = Any()

    private val _lastReadAt = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastReadAt: StateFlow<Map<String, Long>> = _lastReadAt.asStateFlow()

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Persist edilmiş başlangıç değerini yükler ve sonraki markRead değişikliklerini yayınlar. */
    fun observe(context: Context): StateFlow<Map<String, Long>> {
        getAll(context)
        return lastReadAt
    }

    /** Paketin en son okundu (açıldı) zamanını günceller; flow hemen emit eder. */
    fun markRead(context: Context, packageName: String, atMillis: Long = System.currentTimeMillis()) {
        if (packageName.isBlank()) return
        synchronized(lock) {
            val map = readPersisted(context).toMutableMap()
            map[packageName] = atMillis
            prefs(context).edit().putString(KEY_LAST_READ_MAP, map.toJson()).apply()
            _lastReadAt.value = map.toMap()
        }
    }

    fun getLastReadAt(context: Context, packageName: String): Long? = getAll(context)[packageName]

    fun getAll(context: Context): Map<String, Long> = synchronized(lock) {
        val persisted = readPersisted(context)
        if (_lastReadAt.value != persisted) _lastReadAt.value = persisted
        persisted
    }

    fun clearAll(context: Context) {
        synchronized(lock) {
            prefs(context).edit().remove(KEY_LAST_READ_MAP).apply()
            _lastReadAt.value = emptyMap()
        }
    }

    private fun readPersisted(context: Context): Map<String, Long> {
        val raw = prefs(context).getString(KEY_LAST_READ_MAP, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getLong(it) }
        }.getOrDefault(emptyMap())
    }

    private fun Map<String, Long>.toJson(): String {
        val json = JSONObject()
        forEach { (key, value) -> json.put(key, value) }
        return json.toString()
    }
}

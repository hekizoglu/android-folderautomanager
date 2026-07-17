package com.armutlu.apporganizer.utils

import android.content.Context
import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import org.json.JSONObject
import timber.log.Timber

/**
 * Haftalık Rapor ("Wrapped") snapshot kalıcılığı — ayrı SharedPreferences dosyası
 * ("wrapped_prefs"). Yalnızca kategori bazlı agregat sayaçlar saklanır, kişisel veri
 * (paket adı bazlı kullanım, bildirim içeriği vb.) yazılmaz. Room migration gerekmez.
 */
object WrappedSnapshotPrefs {
    private const val PREFS_NAME = "wrapped_prefs"

    private const val KEY_CURRENT_CATEGORY_USAGE = "current_category_usage"
    private const val KEY_CURRENT_TOTAL_APPS = "current_total_apps"
    private const val KEY_CURRENT_SAVED_DAY = "current_saved_epoch_day"
    private const val KEY_CURRENT_UNLOCK_COUNT = "current_unlock_count"

    private const val KEY_PREVIOUS_CATEGORY_USAGE = "previous_category_usage"
    private const val KEY_PREVIOUS_TOTAL_APPS = "previous_total_apps"
    private const val KEY_PREVIOUS_SAVED_DAY = "previous_saved_epoch_day"
    private const val KEY_PREVIOUS_UNLOCK_COUNT = "previous_unlock_count"

    private const val KEY_LAST_SCORE = "last_score"

    // Not: eski haftalık/günlük skor rotasyonu anahtarları (pulse_week_*, ticker_score_*) Döngü
    // D01'de kaldırıldı — trend/baseline artık PulseHistoryPrefs'te ISO takvim haftasına göre
    // saklanıyor (bkz. utils/PulseHistoryPrefs.kt). Eski veri varsa PulseHistoryPrefs tek seferlik
    // migration ile okur (LEGACY_KEY_PULSE_WEEK_SCORE/DAY sabitleri orada tanımlı).

    // Motorun en son hesapladığı toplam skor — ticker gibi hafif tüketiciler için cache
    // (tek skor motoru kuralı: ticker kendi skorunu HESAPLAMAZ, bunu okur).
    private const val KEY_PULSE_LATEST_TOTAL = "pulse_latest_total"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun nowEpochDay(): Long = System.currentTimeMillis() / (24L * 60 * 60 * 1000)

    /**
     * Mevcut haftanın snapshot'ını kaydeder. Önceki "current" varsa "previous" konumuna kaydırılır
     * — böylece bir sonraki çağrıda getPrevious() bu haftanın verisini görebilir.
     */
    fun saveCurrent(context: Context, categoryUsage: Map<String, Long>, totalApps: Int, unlockCount: Int? = null) {
        runCatching {
            val p = prefs(context)
            val editor = p.edit()

            // Mevcut "current" varsa önceki hafta konumuna taşı (rotasyon).
            val existingCurrentJson = p.getString(KEY_CURRENT_CATEGORY_USAGE, null)
            if (existingCurrentJson != null) {
                editor.putString(KEY_PREVIOUS_CATEGORY_USAGE, existingCurrentJson)
                editor.putInt(KEY_PREVIOUS_TOTAL_APPS, p.getInt(KEY_CURRENT_TOTAL_APPS, 0))
                editor.putLong(KEY_PREVIOUS_SAVED_DAY, p.getLong(KEY_CURRENT_SAVED_DAY, 0L))
                if (p.contains(KEY_CURRENT_UNLOCK_COUNT)) {
                    editor.putInt(KEY_PREVIOUS_UNLOCK_COUNT, p.getInt(KEY_CURRENT_UNLOCK_COUNT, 0))
                }
            }

            editor.putString(KEY_CURRENT_CATEGORY_USAGE, mapToJson(categoryUsage))
            editor.putInt(KEY_CURRENT_TOTAL_APPS, totalApps)
            editor.putLong(KEY_CURRENT_SAVED_DAY, nowEpochDay())
            unlockCount?.let { editor.putInt(KEY_CURRENT_UNLOCK_COUNT, it) }
            editor.apply()
        }.onFailure { e -> Timber.e(e, "WrappedSnapshotPrefs.saveCurrent basarisiz") }
    }

    /** Geçen haftanın snapshot'ı — hiç kaydedilmemişse null (UI "veri birikiyor" gösterir). */
    fun getPrevious(context: Context): WrappedEngine.PreviousSnapshot? {
        return runCatching {
            val p = prefs(context)
            val json = p.getString(KEY_PREVIOUS_CATEGORY_USAGE, null) ?: return null
            WrappedEngine.PreviousSnapshot(
                categoryUsage = jsonToMap(json),
                totalApps = p.getInt(KEY_PREVIOUS_TOTAL_APPS, 0),
                savedAtEpochDay = p.getLong(KEY_PREVIOUS_SAVED_DAY, 0L),
            )
        }.onFailure { e -> Timber.e(e, "WrappedSnapshotPrefs.getPrevious basarisiz") }.getOrNull()
    }

    fun getLastScore(context: Context): Int? {
        val v = prefs(context).getInt(KEY_LAST_SCORE, -1)
        return if (v in 0..100) v else null
    }

    fun getPreviousUnlockCount(context: Context): Int? {
        val p = prefs(context)
        if (!p.contains(KEY_PREVIOUS_UNLOCK_COUNT)) return null
        return p.getInt(KEY_PREVIOUS_UNLOCK_COUNT, 0).coerceAtLeast(0)
    }

    fun setLastScore(context: Context, score: Int) {
        runCatching { prefs(context).edit().putInt(KEY_LAST_SCORE, score).apply() }
            .onFailure { e -> Timber.e(e, "WrappedSnapshotPrefs.setLastScore basarisiz") }
    }

    /** Motorun en son hesapladığı toplam skoru cache'ler — ticker bunu okur, kendi hesaplamaz. */
    fun setLatestPulseScore(context: Context, score: Int) {
        runCatching { prefs(context).edit().putInt(KEY_PULSE_LATEST_TOTAL, score.coerceIn(0, 100)).apply() }
            .onFailure { e -> Timber.e(e, "WrappedSnapshotPrefs.setLatestPulseScore basarisiz") }
    }

    fun getLatestPulseScore(context: Context): Int? {
        val v = prefs(context).getInt(KEY_PULSE_LATEST_TOTAL, -1)
        return if (v in 0..100) v else null
    }

    // P0.4: İstatistik sıfırlama sihirbazı — Wrapped/haftalık karşılaştırma kapsamı tek seferde temizlenir.
    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun mapToJson(map: Map<String, Long>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    private fun jsonToMap(jsonString: String): Map<String, Long> {
        if (jsonString.isBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(jsonString)
            json.keys().asSequence().associateWith { json.getLong(it) }
        }.getOrDefault(emptyMap())
    }
}

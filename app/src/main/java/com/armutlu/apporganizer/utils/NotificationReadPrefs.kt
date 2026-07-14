package com.armutlu.apporganizer.utils

import android.content.Context
import org.json.JSONObject

/**
 * P0.5 — Paket bazli "en son okundu (uygulama acildi)" zaman damgasi.
 *
 * Room'a yeni tablo/kolon EKLEMEZ (migration riski sifir) — kendi kucuk SharedPreferences
 * dosyasinda pkg -> epoch ms JSON map olarak saklar. AppPrefs.PREFS_NAME'den ayri dosya
 * kullanilir ki genel ayarlar temizlenirken (orn. reset) bu veri yanlislikla silinmesin/silinsin
 * diye ayri yonetilebilsin.
 *
 * Kullanim: LauncherViewModel.launchApp() paketi baslatinca markRead() cagirir. Badge hesabi
 * UnreadNotificationModel ile bu zaman damgasini son bildirim zamaniyla karsilastirir.
 */
object NotificationReadPrefs {
    private const val FILE_NAME = "notification_read_prefs"
    private const val KEY_LAST_READ_MAP = "last_read_at_map"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Paketin en son okundu (acildi) zamanini simdiki zamana ayarlar. */
    fun markRead(context: Context, packageName: String, atMillis: Long = System.currentTimeMillis()) {
        val map = getAll(context).toMutableMap()
        map[packageName] = atMillis
        prefs(context).edit().putString(KEY_LAST_READ_MAP, map.toJson()).apply()
    }

    /** Tek bir paketin en son okundu zamanini dondurur, hic okunmadiysa null. */
    fun getLastReadAt(context: Context, packageName: String): Long? = getAll(context)[packageName]

    /** Tum paketlerin en son okundu zamanlarini dondurur (pkg -> epoch ms). */
    fun getAll(context: Context): Map<String, Long> {
        val raw = prefs(context).getString(KEY_LAST_READ_MAP, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getLong(it) }
        }.getOrDefault(emptyMap())
    }

    /** Test/temizlik amacli — tum kayitlari siler. */
    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_LAST_READ_MAP).apply()
    }

    private fun Map<String, Long>.toJson(): String {
        val json = JSONObject()
        forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }
}

package com.armutlu.apporganizer.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * P1.3 - Kisi aksiyonu yerel olay logu.
 *
 * Gizlilik karari: READ_CALL_LOG ISTENMEZ. Bu log SADECE launcher icinden baslatilan
 * kisi aksiyonlarini (arama sonucundaki Ara/SMS/WhatsApp butonlari) kaydeder - telefonun
 * genel arama gecmisine erisim YOKTUR. Telefon numarasi DEGIL, ContactsContract kisi id'si
 * (contactId, SearchCache.ContactEntry.id.toString()) saklanir; boylece numara degisirse de
 * ayni kisiye ait skorlar korunur ve hassas veri (numara) diskte tutulmaz.
 *
 * Depolama: AppPrefs.PREFS_NAME'den ayri kucuk SharedPreferences dosyasinda JSON array olarak
 * saklanir. Max 500 kayit - FIFO (en eski kayit yeni kayit eklenince silinir).
 *
 * Kullanim: ContactSuggestionEngine bu event listesini okuyup saat dilimi + gun + recency +
 * siklik skoruyla oneri uretir (domain/usecase/contacts/ContactSuggestionEngine.kt).
 */
object ContactActionPrefs {
    private const val FILE_NAME = "contact_action_prefs"
    private const val KEY_EVENTS = "contact_action_events"
    const val MAX_EVENTS = 500

    enum class ActionType { CALL, SMS, WHATSAPP }

    data class ContactActionEvent(
        val contactId: String,
        val action: ActionType,
        val atMillis: Long,
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Yeni bir kisi aksiyonu kaydeder. 500 kayit sinirini asarsa en eski kayit silinir (FIFO). */
    fun logAction(
        context: Context,
        contactId: String,
        action: ActionType,
        atMillis: Long = System.currentTimeMillis(),
    ) {
        if (contactId.isBlank()) return
        val events = getAll(context).toMutableList()
        events += ContactActionEvent(contactId, action, atMillis)
        val trimmed = trimToMax(events, MAX_EVENTS)
        prefs(context).edit().putString(KEY_EVENTS, trimmed.toJson()).apply()
    }

    /** Tum kayitli olaylari zaman sirasiyla (en eski -> en yeni) dondurur. */
    fun getAll(context: Context): List<ContactActionEvent> {
        val raw = prefs(context).getString(KEY_EVENTS, null) ?: return emptyList()
        return parseJson(raw)
    }

    /** Test/kullanici talebi amacli - tum kayitlari siler. */
    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_EVENTS).apply()
    }

    // ── Saf parse/serialize fonksiyonlari (Context'siz test edilebilir) ────────

    /** FIFO sinirlama - liste MAX_EVENTS'i asarsa bastan (en eski) kayitlari atar. */
    fun trimToMax(events: List<ContactActionEvent>, maxSize: Int): List<ContactActionEvent> =
        if (events.size <= maxSize) events else events.takeLast(maxSize)

    fun List<ContactActionEvent>.toJson(): String {
        val arr = JSONArray()
        for (e in this) {
            val obj = JSONObject()
            obj.put("contactId", e.contactId)
            obj.put("action", e.action.name)
            obj.put("atMillis", e.atMillis)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseJson(raw: String): List<ContactActionEvent> {
        return runCatching {
            val arr = JSONArray(raw)
            val result = ArrayList<ContactActionEvent>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val contactId = obj.optString("contactId", "")
                if (contactId.isBlank()) continue
                val actionRaw = obj.optString("action", "")
                val action = runCatching { ActionType.valueOf(actionRaw) }.getOrNull() ?: continue
                val atMillis = obj.optLong("atMillis", 0L)
                result += ContactActionEvent(contactId, action, atMillis)
            }
            result
        }.getOrDefault(emptyList())
    }
}

package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gorev/yildiz sistemi kaliciligi (D257) — SharedPreferences + org.json, Room migration YOK.
 * Gunluk durum epochDay'e, haftalik durum epochWeek'e baglidir; gun/hafta degisince
 * tamamlanan set otomatik sifirlanir (eski gunun kaydi uzerine yazilir).
 * Toplam yildiz birikimli tutulur, asla sifirlanmaz.
 */
object MissionPrefs {

    private const val PREFS_NAME = "mission_prefs"
    private const val KEY_TOTAL_STARS = "total_stars"
    private const val KEY_DAILY_STATE = "daily_state"    // {"epochDay":N,"completed":["id"]}
    private const val KEY_WEEKLY_STATE = "weekly_state"  // {"epochWeek":N,"completed":["id"]}
    private const val KEY_MANUAL_STATE = "manual_state"  // {"epochDay":N,"ids":["id"]}
    private const val KEY_LAST_REWARD_AT = "last_reward_at"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Yildiz ──────────────────────────────────────────────────────────────

    fun getTotalStars(context: Context): Int = prefs(context).getInt(KEY_TOTAL_STARS, 0)

    fun addStars(context: Context, count: Int) {
        if (count <= 0) return
        prefs(context).edit()
            .putInt(KEY_TOTAL_STARS, getTotalStars(context) + count)
            .putLong(KEY_LAST_REWARD_AT, System.currentTimeMillis())
            .apply()
    }

    fun getLastRewardAt(context: Context): Long = prefs(context).getLong(KEY_LAST_REWARD_AT, 0L)

    // ── Gunluk / haftalik tamamlanan gorevler ───────────────────────────────

    fun getCompletedDailyIds(context: Context, epochDay: Long): Set<String> =
        readPeriodIds(prefs(context).getString(KEY_DAILY_STATE, null), "epochDay", epochDay, "completed")

    fun markDailyCompleted(context: Context, epochDay: Long, missionId: String) {
        val ids = getCompletedDailyIds(context, epochDay) + missionId
        prefs(context).edit()
            .putString(KEY_DAILY_STATE, writePeriodIds("epochDay", epochDay, "completed", ids))
            .apply()
    }

    fun getCompletedWeeklyIds(context: Context, epochWeek: Long): Set<String> =
        readPeriodIds(prefs(context).getString(KEY_WEEKLY_STATE, null), "epochWeek", epochWeek, "completed")

    fun markWeeklyCompleted(context: Context, epochWeek: Long, missionId: String) {
        val ids = getCompletedWeeklyIds(context, epochWeek) + missionId
        prefs(context).edit()
            .putString(KEY_WEEKLY_STATE, writePeriodIds("epochWeek", epochWeek, "completed", ids))
            .apply()
    }

    // ── Manuel isaretlemeler (ekran ziyareti flag'leri) ─────────────────────

    fun getManuallyCompletedIds(context: Context, epochDay: Long): Set<String> =
        readPeriodIds(prefs(context).getString(KEY_MANUAL_STATE, null), "epochDay", epochDay, "ids")

    fun markManuallyCompleted(context: Context, epochDay: Long, missionId: String) {
        val ids = getManuallyCompletedIds(context, epochDay) + missionId
        prefs(context).edit()
            .putString(KEY_MANUAL_STATE, writePeriodIds("epochDay", epochDay, "ids", ids))
            .apply()
    }

    // ── JSON yardimcilari ───────────────────────────────────────────────────

    private fun readPeriodIds(json: String?, periodKey: String, period: Long, arrayKey: String): Set<String> {
        if (json.isNullOrBlank()) return emptySet()
        return runCatching {
            val obj = JSONObject(json)
            if (obj.optLong(periodKey, -1L) != period) return emptySet() // eski gun/hafta — sifirla
            val arr = obj.optJSONArray(arrayKey) ?: return emptySet()
            buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) }
        }.getOrDefault(emptySet())
    }

    private fun writePeriodIds(periodKey: String, period: Long, arrayKey: String, ids: Set<String>): String =
        JSONObject()
            .put(periodKey, period)
            .put(arrayKey, JSONArray(ids.sorted()))
            .toString()
}

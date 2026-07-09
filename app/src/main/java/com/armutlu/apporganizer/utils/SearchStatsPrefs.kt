package com.armutlu.apporganizer.utils

import android.content.Context

/**
 * Lokal arama istatistikleri - SharedPreferences tabanli anonim sayaclar.
 *
 * Gizlilik cizgisi: aranan metin, kisi adi, telefon numarasi ASLA kaydedilmez.
 * Yalnizca sayaclar + sorgu uzunlugu EMA'si (ortalama karakter sayisi) tutulur.
 * Ayri prefs dosyasi kullanilir ("search_stats_prefs"), tum yazimlar apply() ile async.
 */
object SearchStatsPrefs {

    private const val PREFS_NAME = "search_stats_prefs"

    private const val KEY_TOTAL_SEARCHES = "total_searches"
    private const val KEY_ZERO_RESULT_COUNT = "zero_result_count"
    private const val KEY_AVG_LATENCY_MS = "avg_latency_ms"
    private const val KEY_AVG_QUERY_LEN = "avg_query_len"
    private const val KEY_TOTAL_CLICKS = "total_clicks"
    private const val KEY_FIRST_RESULT_CLICKS = "first_result_clicks"
    private const val KEY_CLICK_COUNTS_PREFIX = "click_count_"
    private const val KEY_ACTION_COUNTS_PREFIX = "action_count_"
    private const val KEY_CLICK_TYPES_SET = "click_source_types"
    private const val KEY_ACTION_TYPES_SET = "action_types"

    /** EMA katsayisi - son deger %20 agirlikli, mevcut ortalama %80 */
    private const val EMA_ALPHA = 0.2

    data class Summary(
        val totalSearches: Int,
        val zeroResultCount: Int,
        val avgLatencyMs: Long,
        val clickCountsByType: Map<String, Int>,
        val totalClicks: Int,
        val firstResultClicks: Int,
        val actionCounts: Map<String, Int>
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * EMA hesabini pure function'a cikarilmis hali - test edilebilir.
     * currentAvg == 0.0 ise (ilk olcum) direkt newValue donulur.
     */
    fun computeEma(currentAvg: Double, newValue: Double, alpha: Double = EMA_ALPHA): Double {
        if (currentAvg <= 0.0) return newValue
        return alpha * newValue + (1 - alpha) * currentAvg
    }

    fun getSummary(context: Context): Summary {
        val p = prefs(context)
        val clickTypes = p.getStringSet(KEY_CLICK_TYPES_SET, emptySet()) ?: emptySet()
        val actionTypes = p.getStringSet(KEY_ACTION_TYPES_SET, emptySet()) ?: emptySet()

        val clickCountsByType = clickTypes.associateWith { type ->
            p.getInt(KEY_CLICK_COUNTS_PREFIX + type, 0)
        }
        val actionCounts = actionTypes.associateWith { type ->
            p.getInt(KEY_ACTION_COUNTS_PREFIX + type, 0)
        }

        return Summary(
            totalSearches = p.getInt(KEY_TOTAL_SEARCHES, 0),
            zeroResultCount = p.getInt(KEY_ZERO_RESULT_COUNT, 0),
            avgLatencyMs = p.getFloat(KEY_AVG_LATENCY_MS, 0f).toLong(),
            clickCountsByType = clickCountsByType,
            totalClicks = p.getInt(KEY_TOTAL_CLICKS, 0),
            firstResultClicks = p.getInt(KEY_FIRST_RESULT_CLICKS, 0),
            actionCounts = actionCounts
        )
    }

    /**
     * Bir arama yapildiginda cagrilir. Kaydedilen: sorgu uzunlugu (karakter sayisi,
     * metin degil), sonuc sayisi, gecikme. Sorgu METNI hicbir yerde tutulmaz.
     */
    fun logSearch(context: Context, queryLength: Int, resultCount: Int, latencyMs: Long) {
        val p = prefs(context)
        val total = p.getInt(KEY_TOTAL_SEARCHES, 0)
        val zeroResult = p.getInt(KEY_ZERO_RESULT_COUNT, 0)
        val currentAvgLatency = p.getFloat(KEY_AVG_LATENCY_MS, 0f).toDouble()
        val currentAvgLen = p.getFloat(KEY_AVG_QUERY_LEN, 0f).toDouble()

        val newAvgLatency = computeEma(currentAvgLatency, latencyMs.toDouble())
        val newAvgLen = computeEma(currentAvgLen, queryLength.toDouble())

        p.edit()
            .putInt(KEY_TOTAL_SEARCHES, total + 1)
            .putInt(KEY_ZERO_RESULT_COUNT, if (resultCount == 0) zeroResult + 1 else zeroResult)
            .putFloat(KEY_AVG_LATENCY_MS, newAvgLatency.toFloat())
            .putFloat(KEY_AVG_QUERY_LEN, newAvgLen.toFloat())
            .apply()
    }

    /**
     * Bir arama sonucuna tiklandiginda cagrilir.
     * @param sourceType SourceType.key degeri ("app", "category", "contact", "file")
     * @param position sonuc listesindeki sirasi (0 = ilk sonuc)
     */
    fun logClick(context: Context, sourceType: String, position: Int) {
        val p = prefs(context)
        val clickTypes = (p.getStringSet(KEY_CLICK_TYPES_SET, emptySet()) ?: emptySet()).toMutableSet()
        clickTypes.add(sourceType)

        val currentTypeCount = p.getInt(KEY_CLICK_COUNTS_PREFIX + sourceType, 0)
        val totalClicks = p.getInt(KEY_TOTAL_CLICKS, 0)
        val firstResultClicks = p.getInt(KEY_FIRST_RESULT_CLICKS, 0)

        p.edit()
            .putStringSet(KEY_CLICK_TYPES_SET, clickTypes)
            .putInt(KEY_CLICK_COUNTS_PREFIX + sourceType, currentTypeCount + 1)
            .putInt(KEY_TOTAL_CLICKS, totalClicks + 1)
            .putInt(KEY_FIRST_RESULT_CLICKS, if (position == 0) firstResultClicks + 1 else firstResultClicks)
            .apply()
    }

    /**
     * Bir hizli aksiyon calistirildiginda cagrilir.
     * @param actionType "CALL" | "WHATSAPP" | "SMS" | "EMAIL" | "OPEN_APP" ...
     */
    fun logAction(context: Context, actionType: String) {
        val p = prefs(context)
        val actionTypes = (p.getStringSet(KEY_ACTION_TYPES_SET, emptySet()) ?: emptySet()).toMutableSet()
        actionTypes.add(actionType)

        val currentCount = p.getInt(KEY_ACTION_COUNTS_PREFIX + actionType, 0)

        p.edit()
            .putStringSet(KEY_ACTION_TYPES_SET, actionTypes)
            .putInt(KEY_ACTION_COUNTS_PREFIX + actionType, currentCount + 1)
            .apply()
    }

    /** Tum arama istatistiklerini sifirlar. */
    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

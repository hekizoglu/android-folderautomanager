package com.armutlu.apporganizer.telemetry

import android.content.Context
import java.time.LocalDate

/** Local-only counters. No user content or exact remote counts leave this store. */
object LocalTelemetryStore {
    @Volatile private var context: Context? = null
    private val aggregateNames = mapOf("search_performed" to "search", "folder_opened" to "folder", "mission_completed" to "mission", "report_viewed" to "report")

    fun initialize(context: Context) { this.context = context.applicationContext }

    @Synchronized fun recordIfAggregated(event: TelemetryEvent): Boolean {
        val prefs = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return false
        if (event.eventName == "widget_added") {
            prefs.edit().putBoolean("widget_active", true).apply()
            return false
        }
        if (event.eventName == "health_warning") {
            prefs.edit().putInt("warning_count", prefs.getInt("warning_count", 0) + 1).apply()
            return false
        }
        val key = aggregateNames[event.eventName] ?: return false
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
        return true
    }

    @Synchronized fun takeDailySummaries(context: Context, day: LocalDate = LocalDate.now()): Pair<TelemetryEvent, TelemetryEvent>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString("sent_day", null) == day.toString()) return null
        val counts = aggregateNames.values.associateWith { prefs.getInt(it, 0) }
        val top = counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: "none"
        val usage = TelemetryEvent.DailyUsageSummary(mapOf(
            "search_count_bucket" to CountBucket.from(counts.getValue("search")).wireValue,
            "folder_open_count_bucket" to CountBucket.from(counts.getValue("folder")).wireValue,
            "mission_complete_bucket" to CountBucket.from(counts.getValue("mission")).wireValue,
            "report_view_bucket" to CountBucket.from(counts.getValue("report")).wireValue,
            "widget_active" to prefs.getBoolean("widget_active", false).toString(), "top_feature" to top
        ))
        val health = TelemetryEvent.DailyHealthSummary(mapOf(
            "health_score_bucket" to prefs.getString("health_score_bucket", "unknown")!!,
            "warning_count_bucket" to CountBucket.from(prefs.getInt("warning_count", 0)).wireValue,
            "classification_consistent" to prefs.getString("classification_consistent", "unknown")!!,
            "worker_failure_bucket" to CountBucket.from(prefs.getInt("worker_failure_count", 0)).wireValue,
            "search_latency_bucket" to prefs.getString("search_latency_bucket", "unknown")!!,
            "file_index_age_bucket" to prefs.getString("file_index_age_bucket", "unknown")!!
        ))
        prefs.edit().putString("sent_day", day.toString()).also { edit -> aggregateNames.values.forEach { edit.remove(it) } }.commit()
        return usage to health
    }

    private const val PREFS = "local_telemetry_summary"
}

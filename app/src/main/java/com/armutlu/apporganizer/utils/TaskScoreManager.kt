package com.armutlu.apporganizer.utils

import android.content.Context

/**
 * Durum bazli gorev puani (ROADMAP #15).
 *
 * Ilk faz SharedPreferences kullanir: Room migration acmadan toplam puan, son delta
 * ve olay sayaclari kalici tutulur. Puan 0'in altina dusmez.
 */
object TaskScoreManager {

    enum class EventType(
        val delta: Int,
        val eventKey: String,
        val defaultLabel: String,
    ) {
        ClassificationApproved(
            delta = 3,
            eventKey = "classification_approved",
            defaultLabel = "Siniflandirma onaylandi",
        ),
        ClassificationCorrected(
            delta = 4,
            eventKey = "classification_corrected",
            defaultLabel = "Siniflandirma duzeltildi",
        ),
        ClassificationSnoozed(
            delta = -1,
            eventKey = "classification_snoozed",
            defaultLabel = "Siniflandirma ertelendi",
        ),
        FolderSuggestionAccepted(
            delta = 5,
            eventKey = "folder_suggestion_accepted",
            defaultLabel = "Klasor onerisi kabul edildi",
        ),
        FolderSuggestionSnoozed(
            delta = -1,
            eventKey = "folder_suggestion_snoozed",
            defaultLabel = "Klasor onerisi ertelendi",
        ),
        FolderSuggestionDismissed(
            delta = -2,
            eventKey = "folder_suggestion_dismissed",
            defaultLabel = "Klasor onerisi gizlendi",
        ),
        SimilarAppsAccepted(
            delta = 4,
            eventKey = "similar_apps_accepted",
            defaultLabel = "Benzer uygulama onerisi kabul edildi",
        ),
    }

    data class Snapshot(
        val totalScore: Int,
        val lastDelta: Int,
        val lastEventLabel: String,
        val lastEventAt: Long,
    )

    private const val PREFS_NAME = "task_score_prefs"
    private const val KEY_TOTAL_SCORE = "total_score"
    private const val KEY_LAST_DELTA = "last_delta"
    private const val KEY_LAST_EVENT_LABEL = "last_event_label"
    private const val KEY_LAST_EVENT_AT = "last_event_at"
    private const val KEY_EVENT_PREFIX = "event_count_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSnapshot(context: Context): Snapshot {
        val prefs = prefs(context)
        return Snapshot(
            totalScore = prefs.getInt(KEY_TOTAL_SCORE, 0),
            lastDelta = prefs.getInt(KEY_LAST_DELTA, 0),
            lastEventLabel = prefs.getString(KEY_LAST_EVENT_LABEL, "").orEmpty(),
            lastEventAt = prefs.getLong(KEY_LAST_EVENT_AT, 0L),
        )
    }

    fun record(context: Context, eventType: EventType, weight: Int = 1): Snapshot {
        if (weight <= 0) return getSnapshot(context)
        val prefs = prefs(context)
        val delta = eventType.delta * weight
        val total = (prefs.getInt(KEY_TOTAL_SCORE, 0) + delta).coerceAtLeast(0)
        val eventCountKey = KEY_EVENT_PREFIX + eventType.eventKey
        prefs.edit()
            .putInt(KEY_TOTAL_SCORE, total)
            .putInt(KEY_LAST_DELTA, delta)
            .putString(KEY_LAST_EVENT_LABEL, eventType.defaultLabel)
            .putLong(KEY_LAST_EVENT_AT, System.currentTimeMillis())
            .putInt(eventCountKey, prefs.getInt(eventCountKey, 0) + weight)
            .apply()
        return getSnapshot(context)
    }
}

package com.armutlu.apporganizer.utils

import android.content.Context
import kotlin.math.roundToInt
import java.time.Instant
import java.time.ZoneId

/**
 * Durum bazli gorev puani (ROADMAP #15).
 *
 * Ilk faz SharedPreferences kullanir: Room migration acmadan toplam puan, son delta
 * ve olay sayaclari kalici tutulur. Puan 0'in altina dusmez.
 */
object TaskScoreManager {
    private const val PULSE_WINDOW_DAYS = 14L
    private const val PULSE_MAX_CONTRIBUTION = 10
    private const val PULSE_DIVISOR = 3f

    enum class EventType(
        val delta: Int,
        val eventKey: String,
        val defaultLabel: String,
    ) {
        // ROADMAP M08: adil puan tablosu - reddetme/erteleme cezalandirmaz (delta=0),
        // toplu kabuller dogrusal degil bulkReward() ile hesaplanir (bkz. recordBulk).
        ClassificationApproved(
            delta = 2,
            eventKey = "classification_approved",
            defaultLabel = "Siniflandirma onaylandi",
        ),
        ClassificationCorrected(
            delta = 4,
            eventKey = "classification_corrected",
            defaultLabel = "Siniflandirma duzeltildi",
        ),
        ClassificationSnoozed(
            delta = 0,
            eventKey = "classification_snoozed",
            defaultLabel = "Siniflandirma ertelendi",
        ),
        // Tekil kabul - toplu kabul icin recordBulk() + bulkReward() kullanilir.
        FolderSuggestionAccepted(
            delta = 3,
            eventKey = "folder_suggestion_accepted",
            defaultLabel = "Klasor onerisi kabul edildi",
        ),
        FolderSuggestionSnoozed(
            delta = 0,
            eventKey = "folder_suggestion_snoozed",
            defaultLabel = "Klasor onerisi ertelendi",
        ),
        FolderSuggestionDismissed(
            delta = 0,
            eventKey = "folder_suggestion_dismissed",
            defaultLabel = "Klasor onerisi gizlendi",
        ),
        // Tekil kabul - toplu kabul icin recordBulk() + bulkReward() kullanilir.
        SimilarAppsAccepted(
            delta = 3,
            eventKey = "similar_apps_accepted",
            defaultLabel = "Benzer uygulama onerisi kabul edildi",
        ),
        NotificationReportViewed(
            delta = 1,
            eventKey = "notification_report_viewed",
            defaultLabel = "Bildirim raporu acildi",
        ),
        // Dongu G3a — DAILY_CUSTOMIZE_FOLDER gorevi icin sinyal. Gunde bir kez sayilmasi
        // (insertOnceBetween ile) gorev tarafinda degil, TaskScoreManager.record cagrisinda
        // NotificationReportViewed ile ayni "gunde bir" desenini izler.
        FolderCustomized(
            delta = 2,
            eventKey = "folder_customized",
            defaultLabel = "Klasor ozellestirildi (emoji/renk)",
        ),
        // Dongu G3a — DISCOVER_WEEKLY gorevi icin sinyal (haftalik rapor acildi).
        WrappedReportViewed(
            delta = 1,
            eventKey = "wrapped_report_viewed",
            defaultLabel = "Haftalik rapor acildi",
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
        error("Use suspend getSnapshotV2 for Room-backed task score state")
    }

    fun getLegacySnapshot(context: Context): Snapshot {
        val prefs = prefs(context)
        return Snapshot(
            totalScore = prefs.getInt(KEY_TOTAL_SCORE, 0),
            lastDelta = prefs.getInt(KEY_LAST_DELTA, 0),
            lastEventLabel = prefs.getString(KEY_LAST_EVENT_LABEL, "").orEmpty(),
            lastEventAt = prefs.getLong(KEY_LAST_EVENT_AT, 0L),
        )
    }

    suspend fun getSnapshotV2(context: Context): Snapshot {
        val dao = com.armutlu.apporganizer.data.local.AppDatabase.getInstance(context).taskScoreEventDao()
        val latest = dao.getLatestEvent()
        return Snapshot(
            totalScore = dao.getTotalScore().coerceAtLeast(0),
            lastDelta = latest?.delta ?: 0,
            lastEventLabel = latest?.label.orEmpty(),
            lastEventAt = latest?.createdAt ?: 0L,
        )
    }

    /**
     * Toplu odul agirligi (ROADMAP M08) - dogrusal DEGIL. Tek buyuk toplu islem
     * (orn. 100 uygulama) diger tum davranislari golgelemesin diye ust sinir 10'dur.
     */
    fun bulkReward(itemCount: Int): Int = when (itemCount) {
        0 -> 0
        1 -> 3
        in 2..5 -> 5
        in 6..10 -> 7
        else -> 10
    }

    suspend fun record(context: Context, eventType: EventType, weight: Int = 1): Snapshot {
        if (weight <= 0) return getSnapshotV2(context)
        val delta = eventType.delta * weight
        val dao = com.armutlu.apporganizer.data.local.AppDatabase.getInstance(context).taskScoreEventDao()
        val entry = com.armutlu.apporganizer.domain.models.TaskScoreEventEntry(
                eventKey = eventType.eventKey,
                label = eventType.defaultLabel,
                delta = delta,
            )
        if (eventType == EventType.NotificationReportViewed ||
            eventType == EventType.FolderCustomized ||
            eventType == EventType.WrappedReportViewed
        ) {
            val zone = ZoneId.systemDefault()
            val day = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
            val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
            dao.insertOnceBetween(entry, from, to)
        } else {
            dao.insert(entry)
        }
        return getSnapshotV2(context)
    }

    /**
     * Toplu kabul olaylarini (klasor onerisi / benzer uygulama) kaydeder. Delta,
     * itemCount'a dogrusal olarak degil [bulkReward] ile hesaplanir - boylece tek
     * buyuk toplu islem diger butun gorev puanlarini golgelemez.
     */
    suspend fun recordBulk(context: Context, eventType: EventType, itemCount: Int): Snapshot {
        val delta = bulkReward(itemCount)
        if (delta <= 0) return getSnapshotV2(context)
        val dao = com.armutlu.apporganizer.data.local.AppDatabase.getInstance(context).taskScoreEventDao()
        val entry = com.armutlu.apporganizer.domain.models.TaskScoreEventEntry(
            eventKey = eventType.eventKey,
            label = eventType.defaultLabel,
            delta = delta,
        )
        dao.insert(entry)
        return getSnapshotV2(context)
    }

    suspend fun getPulseContribution(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        val dao = com.armutlu.apporganizer.data.local.AppDatabase.getInstance(context).taskScoreEventDao()
        val fromInclusive = nowMillis - (PULSE_WINDOW_DAYS * 24L * 60L * 60L * 1000L)
        val recentNet = dao.getScoreBetween(fromInclusive, nowMillis)
        return (recentNet / PULSE_DIVISOR)
            .roundToInt()
            .coerceIn(-PULSE_MAX_CONTRIBUTION, PULSE_MAX_CONTRIBUTION)
    }

    fun clearLegacyPrefs(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

package com.armutlu.apporganizer.utils

import android.content.Context

enum class SuggestionChannel(val priority: Int) {
    TASK_CARD(priority = 3),
    TICKER(priority = 2),
    SYSTEM_NOTIFICATION(priority = 1),
}

data class SuggestionCandidate(
    val dedupeKey: String,
    val highValue: Boolean = false,
    val timeSensitive: Boolean = false,
)

interface SuggestionHistoryStore {
    fun getLastShownAt(channel: SuggestionChannel, dedupeKey: String): Long?
    fun getLastRejectedAt(dedupeKey: String): Long?
    fun recordShown(channel: SuggestionChannel, dedupeKey: String, shownAt: Long)
    fun recordRejected(dedupeKey: String, rejectedAt: Long)
}

data class SuggestionCoordinatorPolicy(
    val crossChannelCooldownMs: Long = 6L * 60L * 60L * 1000L,
    val rejectionCooldownMs: Long = 3L * 24L * 60L * 60L * 1000L,
)

object SuggestionCoordinator {

    fun canShow(
        candidate: SuggestionCandidate,
        channel: SuggestionChannel,
        store: SuggestionHistoryStore,
        nowMillis: Long,
        policy: SuggestionCoordinatorPolicy = SuggestionCoordinatorPolicy(),
    ): Boolean {
        if (channel == SuggestionChannel.SYSTEM_NOTIFICATION &&
            (!candidate.highValue || !candidate.timeSensitive)
        ) {
            return false
        }

        val rejectedAt = store.getLastRejectedAt(candidate.dedupeKey)
        if (rejectedAt != null && nowMillis - rejectedAt < policy.rejectionCooldownMs) {
            return false
        }

        return SuggestionChannel.entries.none { other ->
            val lastShownAt = store.getLastShownAt(other, candidate.dedupeKey) ?: return@none false
            nowMillis - lastShownAt < policy.crossChannelCooldownMs &&
                other.priority >= channel.priority
        }
    }

    fun recordShown(
        candidate: SuggestionCandidate,
        channel: SuggestionChannel,
        store: SuggestionHistoryStore,
        nowMillis: Long,
    ) {
        store.recordShown(channel, candidate.dedupeKey, nowMillis)
    }

    fun recordRejected(
        dedupeKey: String,
        store: SuggestionHistoryStore,
        nowMillis: Long,
    ) {
        store.recordRejected(dedupeKey, nowMillis)
    }
}

class SharedPrefsSuggestionHistoryStore(
    context: Context,
) : SuggestionHistoryStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getLastShownAt(channel: SuggestionChannel, dedupeKey: String): Long? =
        parseLongMap(prefs.getString(channelKey(channel), null))[dedupeKey]

    override fun getLastRejectedAt(dedupeKey: String): Long? =
        parseLongMap(prefs.getString(KEY_REJECTED, null))[dedupeKey]

    override fun recordShown(channel: SuggestionChannel, dedupeKey: String, shownAt: Long) {
        val next = parseLongMap(prefs.getString(channelKey(channel), null)).toMutableMap()
        next[dedupeKey] = shownAt
        prefs.edit().putString(channelKey(channel), serializeLongMap(next)).apply()
    }

    override fun recordRejected(dedupeKey: String, rejectedAt: Long) {
        val next = parseLongMap(prefs.getString(KEY_REJECTED, null)).toMutableMap()
        next[dedupeKey] = rejectedAt
        prefs.edit().putString(KEY_REJECTED, serializeLongMap(next)).apply()
    }

    private fun channelKey(channel: SuggestionChannel): String = when (channel) {
        SuggestionChannel.TASK_CARD -> KEY_TASK_CARD_SHOWN
        SuggestionChannel.TICKER -> KEY_TICKER_SHOWN
        SuggestionChannel.SYSTEM_NOTIFICATION -> KEY_NOTIFICATION_SHOWN
    }

    private fun parseLongMap(raw: String?): Map<String, Long> =
        raw.orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val sep = line.indexOf('=')
                if (sep <= 0) return@mapNotNull null
                val key = line.substring(0, sep)
                val value = line.substring(sep + 1).toLongOrNull() ?: return@mapNotNull null
                key to value
            }
            .toMap()

    private fun serializeLongMap(values: Map<String, Long>): String =
        values.entries.joinToString(separator = "\n") { (key, value) -> "$key=$value" }

    private companion object {
        const val PREFS_NAME = "suggestion_coordinator_prefs"
        const val KEY_TASK_CARD_SHOWN = "task_card_shown"
        const val KEY_TICKER_SHOWN = "ticker_shown"
        const val KEY_NOTIFICATION_SHOWN = "notification_shown"
        const val KEY_REJECTED = "rejected"
    }
}

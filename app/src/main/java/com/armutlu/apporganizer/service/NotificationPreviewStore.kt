package com.armutlu.apporganizer.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

data class NotificationPreview(
    val key: String,
    val packageName: String,
    val postedAt: Long,
    /** Klasör altı eski tek satırlık gösterim için birleştirilmiş metin. */
    val text: String,
    /** Smart Notification Hub için ayrı başlık/gönderen alanı. */
    val title: String = "",
    /** Smart Notification Hub için ayrı içerik özeti. */
    val body: String = "",
)

object NotificationPreviewStore {
    private const val MAX_PREVIEWS_PER_PACKAGE = 2
    private const val MAX_PREVIEW_LENGTH = 80

    fun extractPreview(sbn: StatusBarNotification): NotificationPreview? {
        val extras = sbn.notification?.extras ?: return null
        val content = buildPreviewContent(extras) ?: return null
        val postedAt = when {
            sbn.postTime > 0L -> sbn.postTime
            sbn.notification.`when` > 0L -> sbn.notification.`when`
            else -> System.currentTimeMillis()
        }
        return NotificationPreview(
            key = sbn.key,
            packageName = sbn.packageName,
            postedAt = postedAt,
            text = content.combined,
            title = content.title,
            body = content.body,
        )
    }

    fun removePreview(
        current: Map<String, List<NotificationPreview>>,
        packageName: String,
        notificationKey: String?,
    ): Map<String, List<NotificationPreview>> {
        val key = notificationKey ?: return current
        val updated = current[packageName].orEmpty().filterNot { it.key == key }
        return if (updated.isEmpty()) current - packageName else current + (packageName to updated)
    }

    fun summarize(previews: List<NotificationPreview>, count: Int, showContent: Boolean): String {
        if (count <= 0) return ""
        if (!showContent) return countLabel(count)
        val summary = previews
            .sortedByDescending { it.postedAt }
            .map { it.text }
            .filter { it.isNotBlank() }
            .take(MAX_PREVIEWS_PER_PACKAGE)
            .joinToString(separator = "  •  ")
        return if (summary.isNotBlank()) summary else countLabel(count)
    }

    fun countLabel(count: Int): String = if (count == 1) "1 bildirim" else "$count bildirim"

    private fun buildPreviewContent(extras: android.os.Bundle): PreviewContent? {
        val rawTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)
            ?.toString()
            .orEmpty()
            .trim()
        val rawText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)
            ?.toString()
            .orEmpty()
            .trim()
        val rawBigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)
            ?.toString()
            .orEmpty()
            .trim()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.map { it?.toString().orEmpty().trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val rawBody = listOf(rawBigText, rawText).firstOrNull { it.isNotBlank() }
            ?: lines.firstOrNull().orEmpty()
        val rawCombined = when {
            rawTitle.isNotBlank() && rawBody.isNotBlank() -> "$rawTitle: $rawBody"
            rawTitle.isNotBlank() -> rawTitle
            rawBody.isNotBlank() -> rawBody
            else -> ""
        }
        val combined = sanitize(rawCombined)
        if (combined.isBlank()) return null

        return PreviewContent(
            title = sanitize(rawTitle),
            body = sanitize(rawBody),
            combined = combined,
        )
    }

    private fun sanitize(value: String): String {
        return value
            .replace(Regex("\\s+"), " ")
            .trim()
            .let { normalized ->
                if (normalized.length <= MAX_PREVIEW_LENGTH) normalized
                else normalized.take(MAX_PREVIEW_LENGTH - 1).trimEnd() + "…"
            }
    }

    private data class PreviewContent(
        val title: String,
        val body: String,
        val combined: String,
    )
}

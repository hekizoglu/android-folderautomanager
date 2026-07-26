package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.NotificationBadgeMode
import com.armutlu.apporganizer.domain.models.NotificationCategory
import org.json.JSONArray
import org.json.JSONObject

/**
 * Akıllı bildirim ayarlarının içeriksiz JSON yedek şeması.
 *
 * Bildirim başlığı, gövdesi, göndereni, paket adı, OTP veya finansal içerik bu şemaya girmez.
 */
internal object SmartNotificationBackupCodec {
    const val ROOT_KEY = "smartNotificationSettings"

    fun toJson(fields: SmartNotificationPrefs.BackupFields): JSONObject = JSONObject().apply {
        put("engineEnabled", fields.engineEnabled)
        put("filterPromotions", fields.filterPromotions)
        put("hideSensitiveContent", fields.hideSensitiveContent)
        put("visibleCategories", JSONArray(fields.visibleCategoryNames))
        put("badgeMode", fields.badgeMode)
    }

    fun fromJson(
        json: JSONObject,
        fallback: SmartNotificationPrefs.BackupFields,
    ): SmartNotificationPrefs.BackupFields {
        val visibleCategories = json.optJSONArray("visibleCategories")
            ?.let { array ->
                (0 until array.length())
                    .mapNotNull { index -> array.optString(index).takeIf(String::isNotBlank) }
                    .filter { name ->
                        runCatching { NotificationCategory.valueOf(name) }.isSuccess
                    }
                    .distinct()
                    .sorted()
            }
            ?: fallback.visibleCategoryNames

        val badgeMode = json.optString("badgeMode")
            .takeIf { name ->
                name.isNotBlank() && runCatching { NotificationBadgeMode.valueOf(name) }.isSuccess
            }
            ?: fallback.badgeMode

        return SmartNotificationPrefs.BackupFields(
            engineEnabled = if (json.has("engineEnabled")) {
                json.optBoolean("engineEnabled", fallback.engineEnabled)
            } else {
                fallback.engineEnabled
            },
            filterPromotions = if (json.has("filterPromotions")) {
                json.optBoolean("filterPromotions", fallback.filterPromotions)
            } else {
                fallback.filterPromotions
            },
            hideSensitiveContent = if (json.has("hideSensitiveContent")) {
                json.optBoolean("hideSensitiveContent", fallback.hideSensitiveContent)
            } else {
                fallback.hideSensitiveContent
            },
            visibleCategoryNames = visibleCategories,
            badgeMode = badgeMode,
        )
    }
}
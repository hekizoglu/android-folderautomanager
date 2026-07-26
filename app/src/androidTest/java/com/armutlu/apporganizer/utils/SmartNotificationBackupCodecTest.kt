package com.armutlu.apporganizer.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.domain.models.NotificationBadgeMode
import com.armutlu.apporganizer.domain.models.NotificationCategory
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmartNotificationBackupCodecTest {

    private val fallback = SmartNotificationPrefs.BackupFields(
        engineEnabled = false,
        filterPromotions = true,
        hideSensitiveContent = true,
        visibleCategoryNames = NotificationCategory.entries.map { it.name }.sorted(),
        badgeMode = NotificationBadgeMode.CLASSIC_APP.name,
    )

    @Test
    fun completePayloadRoundTripsWithoutNotificationContent() {
        val expected = SmartNotificationPrefs.BackupFields(
            engineEnabled = true,
            filterPromotions = false,
            hideSensitiveContent = false,
            visibleCategoryNames = listOf(
                NotificationCategory.FINANCE.name,
                NotificationCategory.MESSAGING.name,
            ),
            badgeMode = NotificationBadgeMode.CATEGORY.name,
        )

        val json = SmartNotificationBackupCodec.toJson(expected)
        val restored = SmartNotificationBackupCodec.fromJson(json, fallback)

        assertEquals(expected, restored)
        listOf(
            "notificationText",
            "title",
            "body",
            "sender",
            "packageName",
            "otp",
            "amount",
            "iban",
        ).forEach { forbiddenKey ->
            assertFalse(json.has(forbiddenKey))
        }
    }

    @Test
    fun partialLegacyPayloadPreservesMissingCurrentValues() {
        val json = JSONObject().put("engineEnabled", true)

        val restored = SmartNotificationBackupCodec.fromJson(json, fallback)

        assertEquals(true, restored.engineEnabled)
        assertEquals(fallback.filterPromotions, restored.filterPromotions)
        assertEquals(fallback.hideSensitiveContent, restored.hideSensitiveContent)
        assertEquals(fallback.visibleCategoryNames, restored.visibleCategoryNames)
        assertEquals(fallback.badgeMode, restored.badgeMode)
    }

    @Test
    fun invalidEnumsAreDiscardedAndValidCategoriesRemainSorted() {
        val json = JSONObject().apply {
            put(
                "visibleCategories",
                JSONArray(
                    listOf(
                        "UNKNOWN_CATEGORY",
                        NotificationCategory.PROMOTION.name,
                        NotificationCategory.FINANCE.name,
                        NotificationCategory.PROMOTION.name,
                    )
                )
            )
            put("badgeMode", "UNKNOWN_MODE")
        }

        val restored = SmartNotificationBackupCodec.fromJson(json, fallback)

        assertEquals(
            listOf(NotificationCategory.FINANCE.name, NotificationCategory.PROMOTION.name),
            restored.visibleCategoryNames,
        )
        assertEquals(fallback.badgeMode, restored.badgeMode)
    }

    @Test
    fun explicitEmptyCategoryListRemainsEmpty() {
        val json = JSONObject().put("visibleCategories", JSONArray())

        val restored = SmartNotificationBackupCodec.fromJson(json, fallback)

        assertEquals(emptyList<String>(), restored.visibleCategoryNames)
    }
}
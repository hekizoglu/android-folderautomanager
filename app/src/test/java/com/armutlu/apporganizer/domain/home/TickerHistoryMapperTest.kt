package com.armutlu.apporganizer.domain.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [TickerHistoryMapper] — [SmartTickerItem] -> [com.armutlu.apporganizer.data.local.TickerHistoryEntity]
 * dönüşümü + 7 gün saklama süresi hesabı. Room bağımlılığı yok (saf JVM test).
 */
class TickerHistoryMapperTest {

    private fun sampleItem(
        id: String = "item1",
        suggestionKey: String? = null,
        action: TickerAction = TickerAction.OpenDashboard,
        sensitive: Boolean = false,
        createdAt: Long = 1_000L,
    ) = SmartTickerItem(
        id = id,
        type = SmartTickerType.CONTEXTUAL_SUGGESTION,
        title = "Başlık",
        subtitle = "Alt başlık",
        icon = "🔔",
        priority = 10,
        createdAt = createdAt,
        action = action,
        suggestionKey = suggestionKey,
        sensitive = sensitive,
    )

    @Test
    fun `toEntity uses dedupeKey as id`() {
        val item = sampleItem(id = "item1", suggestionKey = "notification_summary")
        val entity = TickerHistoryMapper.toEntity(item)
        assertEquals("notification_summary", entity.id)
        assertEquals(item.dedupeKey, entity.id)
    }

    @Test
    fun `toEntity falls back to id when no suggestionKey`() {
        val item = sampleItem(id = "raw_id", suggestionKey = null)
        val entity = TickerHistoryMapper.toEntity(item)
        assertEquals("raw_id", entity.id)
    }

    @Test
    fun `toEntity copies fields and encodes action`() {
        val item = sampleItem(action = TickerAction.OpenFolder("CAT_SOCIAL"), sensitive = true)
        val entity = TickerHistoryMapper.toEntity(item)
        assertEquals(item.type.name, entity.type)
        assertEquals(item.title, entity.title)
        assertEquals(item.subtitle, entity.subtitle)
        assertEquals(item.icon, entity.icon)
        assertEquals(item.createdAt, entity.createdAt)
        assertFalse(entity.isRead)
        assertTrue(entity.sensitive)
        assertEquals(TickerActionCodec.encode(item.action), entity.actionType)
    }

    @Test
    fun `toEntities maps a list preserving order`() {
        val items = listOf(sampleItem(id = "a"), sampleItem(id = "b", suggestionKey = "b_key"))
        val entities = TickerHistoryMapper.toEntities(items)
        assertEquals(listOf("a", "b_key"), entities.map { it.id })
    }

    @Test
    fun `cutoffMillis is exactly 7 days before now`() {
        val now = 10_000_000_000L
        val cutoff = TickerHistoryMapper.cutoffMillis(now)
        assertEquals(now - 7L * 24 * 60 * 60 * 1000, cutoff)
    }

    @Test
    fun `item older than cutoff is eligible for deletion, newer is not`() {
        val now = 1_000_000_000L
        val cutoff = TickerHistoryMapper.cutoffMillis(now)
        val old = sampleItem(createdAt = cutoff - 1)
        val recent = sampleItem(createdAt = cutoff + 1)
        assertTrue(TickerHistoryMapper.toEntity(old).createdAt < cutoff)
        assertFalse(TickerHistoryMapper.toEntity(recent).createdAt < cutoff)
    }
}

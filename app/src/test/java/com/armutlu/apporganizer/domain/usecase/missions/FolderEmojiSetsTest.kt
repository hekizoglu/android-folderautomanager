package com.armutlu.apporganizer.domain.usecase.missions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dongu G6 — Yildiz Ekonomisi kozmetik acilim: Doga/Uzay emoji setlerinin kilit mantigi.
 * Kirmizi cizgi: mevcut EMOJI_PICKER (FolderRenameDialog.kt) bu testin kapsami DISINDA —
 * hicbir zaman kilitlenmez, burada dogrulanan sadece YENI setler.
 */
class FolderEmojiSetsTest {

    @Test
    fun `nature set requires focused level (25 stars)`() {
        val nature = FolderEmojiSets.SETS.first { it.id == "nature" }
        assertEquals(StarLevelSystem.Level.FOCUSED, nature.requiredLevel)
        assertFalse(FolderEmojiSets.isUnlocked(nature, totalStars = 24))
        assertTrue(FolderEmojiSets.isUnlocked(nature, totalStars = 25))
    }

    @Test
    fun `space set requires balance master level (50 stars)`() {
        val space = FolderEmojiSets.SETS.first { it.id == "space" }
        assertEquals(StarLevelSystem.Level.BALANCE_MASTER, space.requiredLevel)
        assertFalse(FolderEmojiSets.isUnlocked(space, totalStars = 49))
        assertTrue(FolderEmojiSets.isUnlocked(space, totalStars = 50))
    }

    @Test
    fun `zero stars locks all cosmetic sets`() {
        FolderEmojiSets.SETS.forEach { set ->
            assertFalse(FolderEmojiSets.isUnlocked(set, totalStars = 0))
        }
    }

    @Test
    fun `master level unlocks all cosmetic sets`() {
        FolderEmojiSets.SETS.forEach { set ->
            assertTrue(FolderEmojiSets.isUnlocked(set, totalStars = 100))
        }
    }

    @Test
    fun `lockInfo returns required level and star threshold`() {
        val nature = FolderEmojiSets.SETS.first { it.id == "nature" }
        val (level, threshold) = FolderEmojiSets.lockInfo(nature)
        assertEquals(StarLevelSystem.Level.FOCUSED, level)
        assertEquals(25, threshold)
    }

    @Test
    fun `each set has exactly 8 emojis and unique ids`() {
        FolderEmojiSets.SETS.forEach { set ->
            assertEquals(8, set.emojis.size)
        }
        val ids = FolderEmojiSets.SETS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}

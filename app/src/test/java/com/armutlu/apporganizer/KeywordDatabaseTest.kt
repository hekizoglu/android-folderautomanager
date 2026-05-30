package com.armutlu.apporganizer

import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.KeywordDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordDatabaseTest {

    @Test
    fun `getKeywordMap returns non-empty map`() {
        val map = KeywordDatabase.getKeywordMap()
        assertTrue(map.isNotEmpty())
    }

    @Test
    fun `all default categories exist in keyword map`() {
        val map = KeywordDatabase.getKeywordMap()
        val expected = listOf(
            Category.CAT_SOCIAL, Category.CAT_PRODUCTIVITY, Category.CAT_GAMES,
            Category.CAT_SHOPPING, Category.CAT_NEWS, Category.CAT_HEALTH,
            Category.CAT_FINANCE, Category.CAT_EDUCATION, Category.CAT_UTILITIES
        )
        expected.forEach { cat ->
            assertNotNull("$cat keyword listesi eksik", map[cat])
        }
    }

    @Test
    fun `getKeywords returns list for valid category`() {
        val keywords = KeywordDatabase.getKeywords(Category.CAT_SOCIAL)
        assertTrue(keywords.isNotEmpty())
        assertTrue(keywords.contains("social"))
    }

    @Test
    fun `getKeywords returns empty list for unknown category`() {
        val keywords = KeywordDatabase.getKeywords("nonexistent_cat")
        assertTrue(keywords.isEmpty())
    }

    @Test
    fun `getTotalKeywords returns positive number`() {
        assertTrue(KeywordDatabase.getTotalKeywords() > 50)
    }

    @Test
    fun `social keywords contain expected values`() {
        val keywords = KeywordDatabase.getKeywords(Category.CAT_SOCIAL)
        assertTrue(keywords.contains("facebook"))
        assertTrue(keywords.contains("twitter"))
        assertTrue(keywords.contains("whatsapp"))
    }

    @Test
    fun `finance keywords contain expected values`() {
        val keywords = KeywordDatabase.getKeywords(Category.CAT_FINANCE)
        assertTrue(keywords.contains("bank"))
        assertTrue(keywords.contains("finance"))
        assertTrue(keywords.contains("crypto"))
    }

    @Test
    fun `addKeywordToCategory adds new keyword`() {
        val before = KeywordDatabase.getKeywords(Category.CAT_EDUCATION).size
        KeywordDatabase.addKeywordToCategory(Category.CAT_EDUCATION, "testKeyword999")
        val after = KeywordDatabase.getKeywords(Category.CAT_EDUCATION)
        assertEquals(before + 1, after.size)
        assertTrue(after.contains("testKeyword999"))
    }

    @Test
    fun `addKeywordToCategory does not add duplicate`() {
        KeywordDatabase.addKeywordToCategory(Category.CAT_HEALTH, "fitness")
        val count1 = KeywordDatabase.getKeywords(Category.CAT_HEALTH).count { it == "fitness" }
        KeywordDatabase.addKeywordToCategory(Category.CAT_HEALTH, "fitness")
        val count2 = KeywordDatabase.getKeywords(Category.CAT_HEALTH).count { it == "fitness" }
        assertEquals(count1, count2)
    }
}

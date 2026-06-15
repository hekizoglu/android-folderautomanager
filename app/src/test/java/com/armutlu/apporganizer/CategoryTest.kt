package com.armutlu.apporganizer

import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun `getDefaultCategories returns 15 categories`() {
        assertTrue("En az 32 kategori olmali", Category.getDefaultCategories().size >= 32)
    }

    @Test
    fun `getDefaultCategories contains all required IDs`() {
        val ids = Category.getDefaultCategories().map { it.categoryId }
        listOf(
            Category.CAT_SOCIAL, Category.CAT_PRODUCTIVITY, Category.CAT_GAMES,
            Category.CAT_SHOPPING, Category.CAT_NEWS, Category.CAT_HEALTH,
            Category.CAT_FINANCE, Category.CAT_EDUCATION, Category.CAT_UTILITIES,
            Category.CAT_TRAVEL, Category.CAT_ENTERTAINMENT, Category.CAT_FOOD,
            Category.CAT_PHOTOGRAPHY, Category.CAT_OTHER, Category.CAT_UNCATEGORIZED
        ).forEach { id ->
            assertTrue("$id eksik", ids.contains(id))
        }
    }

    @Test
    fun `all default categories have non-blank names`() {
        Category.getDefaultCategories().forEach { cat ->
            assertTrue("${cat.categoryId} iÃ§in categoryName boÅŸ", cat.categoryName.isNotBlank())
        }
    }

    @Test
    fun `all default categories have valid hex color`() {
        val hexPattern = Regex("^#[0-9A-Fa-f]{6,8}$")
        Category.getDefaultCategories().forEach { cat ->
            assertTrue(
                "${cat.categoryId} iÃ§in geÃ§ersiz renk: ${cat.colorHex}",
                hexPattern.matches(cat.colorHex)
            )
        }
    }

    @Test
    fun `displayOrder values are unique`() {
        val orders = Category.getDefaultCategories().map { it.displayOrder }
        assertEquals("displayOrder deÄŸerleri tekrarlanmamalÄ±", orders.size, orders.toSet().size)
    }

    @Test
    fun `social category has correct emoji`() {
        val social = Category.getDefaultCategories().first { it.categoryId == Category.CAT_SOCIAL }
        assertEquals("👥", social.iconEmoji)
    }
}


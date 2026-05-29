package com.armutlu.apporganizer.domain.models

import org.junit.Test
import org.junit.Assert.*

class CategoryTest {
    
    @Test
    fun testCategoryCreation() {
        val category = Category(
            categoryId = "social",
            categoryName = "Sosyal Medya",
            iconEmoji = "👥"
        )
        
        assertEquals("social", category.categoryId)
        assertEquals("Sosyal Medya", category.categoryName)
        assertEquals("👥", category.iconEmoji)
        assertTrue(category.isSystemCategory)
    }
    
    @Test
    fun testDefaultCategories() {
        val defaults = Category.getDefaultCategories()
        
        // Should have 11 default categories
        assertEquals(11, defaults.size)
        
        // Check critical ones exist
        val categoryIds = defaults.map { it.categoryId }
        assertTrue(categoryIds.contains("social"))
        assertTrue(categoryIds.contains("games"))
        assertTrue(categoryIds.contains("productivity"))
        assertTrue(categoryIds.contains("other"))
        assertTrue(categoryIds.contains("uncategorized"))
    }
    
    @Test
    fun testCategoryConstants() {
        assertEquals("social", Category.CAT_SOCIAL)
        assertEquals("games", Category.CAT_GAMES)
        assertEquals("uncategorized", Category.CAT_UNCATEGORIZED)
    }
    
    @Test
    fun testCategoryColorParsing() {
        val category = Category(
            categoryId = "test",
            categoryName = "Test",
            colorHex = "#FF6200EE"
        )
        
        val colorInt = category.getColorInt()
        assertNotEquals(0, colorInt)
        // Color should be parseable
        assertTrue(colorInt > 0)
    }
    
    @Test
    fun testUncategorizedCategory() {
        val defaults = Category.getDefaultCategories()
        val uncategorized = defaults.find { it.categoryId == "uncategorized" }
        
        assertNotNull(uncategorized)
        assertEquals(0, uncategorized!!.displayOrder)
        assertEquals("❓", uncategorized.iconEmoji)
    }
    
    @Test
    fun testCategoryDisplayOrder() {
        val defaults = Category.getDefaultCategories()
        
        // Should be sorted by displayOrder
        for (i in 1 until defaults.size) {
            // Allow 0 for uncategorized
            if (defaults[i-1].displayOrder > 0) {
                assertTrue(defaults[i].displayOrder >= defaults[i-1].displayOrder)
            }
        }
    }
    
    @Test
    fun testSystemCategoryFlag() {
        val defaults = Category.getDefaultCategories()
        
        // All default categories should be marked as system
        defaults.forEach { category ->
            assertTrue("${category.categoryId} should be system category", 
                category.isSystemCategory)
        }
    }
    
    @Test
    fun testCategoryEquality() {
        val cat1 = Category(
            categoryId = "social",
            categoryName = "Sosyal Medya",
            colorHex = "#FF1F77F2"
        )
        
        val cat2 = Category(
            categoryId = "social",
            categoryName = "Sosyal Medya",
            colorHex = "#FF1F77F2"
        )
        
        assertEquals(cat1, cat2)
    }
    
    @Test
    fun testCategoryDefaults() {
        val category = Category(
            categoryId = "custom",
            categoryName = "Custom Category"
        )
        
        assertEquals("#FF6200EE", category.colorHex)
        assertEquals("📁", category.iconEmoji)
        assertFalse(category.isSystemCategory)
    }
}

package com.armutlu.apporganizer.domain.models

import org.junit.Test
import org.junit.Assert.*

class AppInfoTest {
    
    @Test
    fun testAppInfoCreation() {
        val appInfo = AppInfo(
            packageName = "com.example.app",
            appName = "Test App",
            categoryId = "social"
        )
        
        assertEquals("com.example.app", appInfo.packageName)
        assertEquals("Test App", appInfo.appName)
        assertEquals("social", appInfo.categoryId)
        assertFalse(appInfo.isSystemApp)
        assertTrue(appInfo.isInstalled)
    }
    
    @Test
    fun testCreateSampleApp() {
        val sample = AppInfo.createSample()
        
        assertNotNull(sample)
        assertEquals("com.example.app", sample.packageName)
        assertEquals("Example App", sample.appName)
        assertEquals("uncategorized", sample.categoryId)
    }
    
    @Test
    fun testCreateMultipleSampleApps() {
        val samples = AppInfo.createSamples(10)
        
        assertEquals(10, samples.size)
        
        // First app should be in games
        assertEquals("games", samples[0].categoryId)
        
        // Second should be in social
        assertEquals("social", samples[1].categoryId)
        
        // Check that some are marked as system apps
        val hasSystemApp = samples.any { it.isSystemApp }
        assertTrue(hasSystemApp)
    }
    
    @Test
    fun testBelongsToCategory() {
        val appInfo = AppInfo(
            packageName = "com.facebook.katana",
            appName = "Facebook",
            categoryId = "social"
        )
        
        assertTrue(appInfo.belongsToCategory("social"))
        assertFalse(appInfo.belongsToCategory("games"))
    }
    
    @Test
    fun testUpdateCategory() {
        val original = AppInfo(
            packageName = "com.instagram.android",
            appName = "Instagram",
            categoryId = "social"
        )
        
        val updated = original.updateCategory("shopping")
        
        assertEquals("shopping", updated.categoryId)
        assertEquals("social", original.categoryId) // Original unchanged (immutable)
        assertNotEquals(original.lastUpdated, updated.lastUpdated)
    }
    
    @Test
    fun testAppInfoEquality() {
        val app1 = AppInfo(
            packageName = "com.example.app",
            appName = "Test App",
            categoryId = "games"
        )
        
        val app2 = AppInfo(
            packageName = "com.example.app",
            appName = "Test App",
            categoryId = "games"
        )
        
        // Data class equality works even with different timestamps
        assertEquals(app1, app2)
    }
    
    @Test
    fun testAppInfoDefaultValues() {
        val appInfo = AppInfo(
            packageName = "com.test.app",
            appName = "Test"
        )
        
        assertEquals("uncategorized", appInfo.categoryId)
        assertEquals("", appInfo.iconUrl)
        assertFalse(appInfo.isSystemApp)
        assertTrue(appInfo.isInstalled)
        assertEquals("", appInfo.customNotes)
    }
}

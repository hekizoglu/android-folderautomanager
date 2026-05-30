package com.armutlu.apporganizer

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInfoTest {

    private val sampleApp = AppInfo(
        packageName = "com.example.app",
        appName = "Example App",
        categoryId = Category.CAT_SOCIAL
    )

    @Test
    fun `belongsToCategory returns true for correct category`() {
        assertTrue(sampleApp.belongsToCategory(Category.CAT_SOCIAL))
    }

    @Test
    fun `belongsToCategory returns false for wrong category`() {
        assertFalse(sampleApp.belongsToCategory(Category.CAT_GAMES))
    }

    @Test
    fun `updateCategory changes categoryId`() {
        val updated = sampleApp.updateCategory(Category.CAT_GAMES)
        assertEquals(Category.CAT_GAMES, updated.categoryId)
    }

    @Test
    fun `updateCategory does not mutate original`() {
        sampleApp.updateCategory(Category.CAT_GAMES)
        assertEquals(Category.CAT_SOCIAL, sampleApp.categoryId)
    }

    @Test
    fun `updateCategory preserves packageName and appName`() {
        val updated = sampleApp.updateCategory(Category.CAT_EDUCATION)
        assertEquals(sampleApp.packageName, updated.packageName)
        assertEquals(sampleApp.appName, updated.appName)
    }

    @Test
    fun `updateCategory updates lastUpdated timestamp`() {
        val before = System.currentTimeMillis()
        val updated = sampleApp.updateCategory(Category.CAT_FINANCE)
        assertTrue(updated.lastUpdated >= before)
    }

    @Test
    fun `createSample returns valid AppInfo`() {
        val app = AppInfo.createSample()
        assertTrue(app.packageName.isNotBlank())
        assertTrue(app.appName.isNotBlank())
    }

    @Test
    fun `createSamples returns correct count`() {
        val apps = AppInfo.createSamples(10)
        assertEquals(10, apps.size)
    }

    @Test
    fun `createSamples all have unique packageNames`() {
        val apps = AppInfo.createSamples(5)
        val unique = apps.map { it.packageName }.toSet()
        assertEquals(5, unique.size)
    }
}

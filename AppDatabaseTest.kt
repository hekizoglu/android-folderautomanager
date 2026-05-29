package com.armutlu.apporganizer.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

/**
 * Integration tests for Room Database and DAOs
 * These tests verify CRUD operations and data persistence
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    
    private lateinit var database: AppDatabase
    private lateinit var appDao: AppDao
    private lateinit var categoryDao: CategoryDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        appDao = database.appDao()
        categoryDao = database.categoryDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    // ============= APP DAO TESTS =============
    
    @Test
    fun testInsertAndRetrieveApp() = runBlocking {
        val app = AppInfo(
            packageName = "com.test.app",
            appName = "Test App",
            categoryId = "games"
        )
        
        appDao.insertApp(app)
        val retrieved = appDao.getAppByPackageName("com.test.app")
        
        Assert.assertNotNull(retrieved)
        Assert.assertEquals("Test App", retrieved?.appName)
    }
    
    @Test
    fun testInsertMultipleApps() = runBlocking {
        val apps = AppInfo.createSamples(5)
        
        appDao.insertApps(apps)
        val count = appDao.countApps()
        
        Assert.assertEquals(5, count)
    }
    
    @Test
    fun testUpdateAppCategory() = runBlocking {
        val app = AppInfo("com.facebook.katana", "Facebook")
        appDao.insertApp(app)
        
        appDao.updateAppCategory("com.facebook.katana", "social")
        val updated = appDao.getAppByPackageName("com.facebook.katana")
        
        Assert.assertEquals("social", updated?.categoryId)
    }
    
    @Test
    fun testDeleteApp() = runBlocking {
        val app = AppInfo("com.test.app", "Test")
        appDao.insertApp(app)
        
        Assert.assertTrue(appDao.appExists("com.test.app"))
        
        appDao.deleteAppByPackageName("com.test.app")
        
        Assert.assertFalse(appDao.appExists("com.test.app"))
    }
    
    @Test
    fun testGetAppsByCategory() = runBlocking {
        val apps = listOf(
            AppInfo("com.app1", "App 1", "social"),
            AppInfo("com.app2", "App 2", "social"),
            AppInfo("com.app3", "App 3", "games")
        )
        
        appDao.insertApps(apps)
        val socialApps = appDao.getAppsByCategory("social").first()
        
        Assert.assertEquals(2, socialApps.size)
    }
    
    @Test
    fun testSearchApps() = runBlocking {
        val apps = listOf(
            AppInfo("com.facebook.katana", "Facebook"),
            AppInfo("com.instagram.android", "Instagram"),
            AppInfo("com.google.android.apps.docs", "Google Docs")
        )
        
        appDao.insertApps(apps)
        val results = appDao.searchAppsByName("Face").first()
        
        Assert.assertEquals(1, results.size)
        Assert.assertEquals("Facebook", results[0].appName)
    }
    
    @Test
    fun testCountApps() = runBlocking {
        val apps = AppInfo.createSamples(10)
        appDao.insertApps(apps)
        
        val count = appDao.countApps()
        Assert.assertEquals(10, count)
    }
    
    @Test
    fun testGetAllAppsFlow() = runBlocking {
        val apps = AppInfo.createSamples(3)
        appDao.insertApps(apps)
        
        val flowApps = appDao.getAllAppsFlow().first()
        Assert.assertEquals(3, flowApps.size)
    }
    
    // ============= CATEGORY DAO TESTS =============
    
    @Test
    fun testInsertAndRetrieveCategory() = runBlocking {
        val category = Category(
            categoryId = "test",
            categoryName = "Test Category"
        )
        
        categoryDao.insertCategory(category)
        val retrieved = categoryDao.getCategoryById("test")
        
        Assert.assertNotNull(retrieved)
        Assert.assertEquals("Test Category", retrieved?.categoryName)
    }
    
    @Test
    fun testDefaultCategoriesInserted() = runBlocking {
        // Database callback inserts default categories on creation
        val categories = categoryDao.getAllCategories()
        
        // Should have 11 default categories (including uncategorized)
        Assert.assertTrue(categories.size >= 10)
        
        // Check critical ones exist
        val ids = categories.map { it.categoryId }
        Assert.assertTrue(ids.contains("social"))
        Assert.assertTrue(ids.contains("games"))
        Assert.assertTrue(ids.contains("other"))
    }
    
    @Test
    fun testUpdateCategory() = runBlocking {
        val category = Category("test", "Test")
        categoryDao.insertCategory(category)
        
        categoryDao.updateCategoryColor("test", "#FFFF0000")
        val updated = categoryDao.getCategoryById("test")
        
        Assert.assertEquals("#FFFF0000", updated?.colorHex)
    }
    
    @Test
    fun testDeleteCategory() = runBlocking {
        val category = Category("test", "Test")
        categoryDao.insertCategory(category)
        
        Assert.assertTrue(categoryDao.categoryExists("test"))
        
        categoryDao.deleteCategoryById("test")
        
        Assert.assertFalse(categoryDao.categoryExists("test"))
    }
    
    @Test
    fun testGetSystemCategories() = runBlocking {
        val categories = categoryDao.getSystemCategories().first()
        
        // All should be system categories
        categories.forEach { category ->
            Assert.assertTrue("${category.categoryId} should be system", category.isSystemCategory)
        }
    }
    
    @Test
    fun testCountCategories() = runBlocking {
        val count = categoryDao.countCategories()
        Assert.assertTrue(count >= 10)
    }
    
    @Test
    fun testUpdateCategoryOrder() = runBlocking {
        categoryDao.updateCategoryOrder("social", 5)
        val category = categoryDao.getCategoryById("social")
        
        Assert.assertEquals(5, category?.displayOrder)
    }
    
    // ============= INTEGRATION TESTS =============
    
    @Test
    fun testAppAndCategoryIntegration() = runBlocking {
        // Insert apps with categories
        val apps = listOf(
            AppInfo("com.facebook.katana", "Facebook", "social"),
            AppInfo("com.minecraft", "Minecraft", "games")
        )
        
        appDao.insertApps(apps)
        
        // Verify categories exist
        val socialCategory = categoryDao.getCategoryById("social")
        val gamesCategory = categoryDao.getCategoryById("games")
        
        Assert.assertNotNull(socialCategory)
        Assert.assertNotNull(gamesCategory)
        
        // Count apps by category
        val socialAppCount = appDao.countAppsByCategory("social")
        val gameAppCount = appDao.countAppsByCategory("games")
        
        Assert.assertEquals(1, socialAppCount)
        Assert.assertEquals(1, gameAppCount)
    }
    
    @Test
    fun testLargeDatasetInsert() = runBlocking {
        // Test with 100+ apps
        val apps = (1..150).map { i ->
            AppInfo(
                packageName = "com.app$i",
                appName = "App $i",
                categoryId = when {
                    i % 3 == 0 -> "social"
                    i % 3 == 1 -> "games"
                    else -> "productivity"
                }
            )
        }
        
        appDao.insertApps(apps)
        val count = appDao.countApps()
        
        Assert.assertEquals(150, count)
    }
    
    @Test
    fun testDataPersistenceAfterUpdate() = runBlocking {
        val originalApp = AppInfo("com.test", "Test App", "games")
        appDao.insertApp(originalApp)
        
        // Update multiple times
        appDao.updateAppCategory("com.test", "social")
        appDao.updateAppCategory("com.test", "productivity")
        appDao.updateAppCategory("com.test", "other")
        
        val final = appDao.getAppByPackageName("com.test")
        Assert.assertEquals("other", final?.categoryId)
    }
}

package com.armutlu.apporganizer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room veritabanı instrumented testleri.
 * Gerçek bir Android cihaz veya emülatör üzerinde çalışır.
 * In-memory DB kullanılır — test sonrası otomatik temizlenir.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var appDao: AppDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = db.appDao()
        categoryDao = db.categoryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ─── AppDao ────────────────────────────────────────────────────────────────

    @Test
    fun insertAndGetApp() = runTest {
        val app = AppInfo(packageName = "com.test.app", appName = "Test App")
        appDao.insertApp(app)
        val result = appDao.getAppByPackage("com.test.app")
        assertNotNull(result)
        assertEquals("Test App", result?.appName)
    }

    @Test
    fun getAllApps_returnsAllInserted() = runTest {
        repeat(3) { i ->
            appDao.insertApp(AppInfo(packageName = "com.test.app$i", appName = "App $i"))
        }
        val all = appDao.getAllApps().first()
        assertEquals(3, all.size)
    }

    @Test
    fun updateAppCategory_persists() = runTest {
        val app = AppInfo(packageName = "com.test.app", appName = "Test", categoryId = "other")
        appDao.insertApp(app)
        appDao.updateAppCategory("com.test.app", "social")
        val updated = appDao.getAppByPackage("com.test.app")
        assertEquals("social", updated?.categoryId)
    }

    @Test
    fun deleteApp_removesFromDb() = runTest {
        val app = AppInfo(packageName = "com.test.delete", appName = "Delete Me")
        appDao.insertApp(app)
        appDao.deleteApp("com.test.delete")
        assertNull(appDao.getAppByPackage("com.test.delete"))
    }

    @Test
    fun insertOrReplace_updatesExisting() = runTest {
        val app = AppInfo(packageName = "com.test.app", appName = "Old Name")
        appDao.insertApp(app)
        val updated = app.copy(appName = "New Name")
        appDao.insertApp(updated)
        val result = appDao.getAppByPackage("com.test.app")
        assertEquals("New Name", result?.appName)
    }

    @Test
    fun getAppsByCategory_returnsCorrectApps() = runTest {
        appDao.insertApp(AppInfo(packageName = "com.social.1", appName = "Social 1", categoryId = "social"))
        appDao.insertApp(AppInfo(packageName = "com.social.2", appName = "Social 2", categoryId = "social"))
        appDao.insertApp(AppInfo(packageName = "com.other.1", appName = "Other 1", categoryId = "other"))
        val social = appDao.getAppsByCategory("social").first()
        assertEquals(2, social.size)
        assertTrue(social.all { it.categoryId == "social" })
    }

    // ─── CategoryDao ──────────────────────────────────────────────────────────

    @Test
    fun insertAndGetCategory() = runTest {
        val cat = Category(categoryId = "test_cat", categoryName = "Test Category")
        categoryDao.insertCategory(cat)
        val result = categoryDao.getCategoryById("test_cat")
        assertNotNull(result)
        assertEquals("Test Category", result?.categoryName)
    }

    @Test
    fun getAllCategories_returnsAllInserted() = runTest {
        Category.getDefaultCategories().forEach { categoryDao.insertCategory(it) }
        val all = categoryDao.getAllCategories().first()
        assertEquals(11, all.size)
    }

    @Test
    fun deleteCategory_removesFromDb() = runTest {
        val cat = Category(categoryId = "to_delete", categoryName = "Delete Me")
        categoryDao.insertCategory(cat)
        categoryDao.deleteCategory("to_delete")
        assertNull(categoryDao.getCategoryById("to_delete"))
    }
}

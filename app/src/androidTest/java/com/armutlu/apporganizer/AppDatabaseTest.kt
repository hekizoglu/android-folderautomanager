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
import com.armutlu.apporganizer.domain.models.NotificationEvent
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
        val result = appDao.getAppByPackageName("com.test.app")
        assertNotNull(result)
        assertEquals("Test App", result?.appName)
    }

    @Test
    fun getAllApps_returnsAllInserted() = runTest {
        repeat(3) { i ->
            appDao.insertApp(AppInfo(packageName = "com.test.app$i", appName = "App $i"))
        }
        val all = appDao.getAllApps()
        assertEquals(3, all.size)
    }

    @Test
    fun updateAppCategory_persists() = runTest {
        val app = AppInfo(packageName = "com.test.app", appName = "Test", categoryId = "other")
        appDao.insertApp(app)
        appDao.updateAppCategory("com.test.app", "social")
        val updated = appDao.getAppByPackageName("com.test.app")
        assertEquals("social", updated?.categoryId)
    }

    @Test
    fun deleteApp_removesFromDb() = runTest {
        val app = AppInfo(packageName = "com.test.delete", appName = "Delete Me")
        appDao.insertApp(app)
        appDao.deleteAppByPackageName("com.test.delete")
        assertNull(appDao.getAppByPackageName("com.test.delete"))
    }

    @Test
    fun updateApp_updatesExisting() = runTest {
        val app = AppInfo(packageName = "com.test.app", appName = "Old Name")
        appDao.insertApp(app)
        val updated = app.copy(appName = "New Name")
        appDao.updateApp(updated)
        val result = appDao.getAppByPackageName("com.test.app")
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

    @Test
    fun appExists_returnsTrueAfterInsert() = runTest {
        appDao.insertApp(AppInfo(packageName = "com.exists.app", appName = "Exists"))
        assertTrue(appDao.appExists("com.exists.app"))
    }

    @Test
    fun countApps_returnsCorrectCount() = runTest {
        repeat(5) { i ->
            appDao.insertApp(AppInfo(packageName = "com.count.app$i", appName = "App $i"))
        }
        assertEquals(5, appDao.countApps())
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
        val all = categoryDao.getAllCategories()
        assertEquals(Category.getDefaultCategories().size, all.size)
    }

    @Test
    fun deleteCategory_removesFromDb() = runTest {
        val cat = Category(categoryId = "to_delete", categoryName = "Delete Me", isSystemCategory = false)
        categoryDao.insertCategory(cat)
        categoryDao.deleteCategoryById("to_delete")
        assertNull(categoryDao.getCategoryById("to_delete"))
    }

    @Test
    fun categoryExists_returnsTrueAfterInsert() = runTest {
        categoryDao.insertCategory(Category(categoryId = "exists_cat", categoryName = "Exists"))
        assertTrue(categoryDao.categoryExists("exists_cat"))
    }

    @Test
    fun notificationEvent_deleteOlderThan_removesOnlyExpiredRows() = runTest {
        val notificationDao = db.notificationEventDao()
        val dayMs = 24L * 60L * 60L * 1000L
        val now = 1_800_000_000_000L
        val cutoff = now - (30L * dayMs)

        notificationDao.insert(NotificationEvent(packageName = "com.old.app", postedAt = cutoff - 1L))
        notificationDao.insert(NotificationEvent(packageName = "com.boundary.app", postedAt = cutoff))
        notificationDao.insert(NotificationEvent(packageName = "com.new.app", postedAt = now - dayMs))

        notificationDao.deleteOlderThan(cutoff)

        val remaining = notificationDao.eventsSince(0L).sortedBy { it.postedAt }
        assertEquals(listOf("com.boundary.app", "com.new.app"), remaining.map { it.packageName })
    }
}

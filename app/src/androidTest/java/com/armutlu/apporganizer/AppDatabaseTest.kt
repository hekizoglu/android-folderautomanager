package com.armutlu.apporganizer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.local.MissionInstanceDao
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.MissionInstanceEntity
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
    private lateinit var missionInstanceDao: MissionInstanceDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = db.appDao()
        categoryDao = db.categoryDao()
        missionInstanceDao = db.missionInstanceDao()
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

    // ─── MissionInstanceDao (Dongu M01) ──────────────────────────────────────────

    private fun sampleInstance(
        missionId: String = "daily_screen_under_3h",
        periodType: String = MissionInstanceEntity.PERIOD_DAILY,
        periodStartEpoch: Long = 20_650L,
        status: String = MissionInstanceEntity.STATUS_ASSIGNED,
        assignedAt: Long = 1_800_000_000_000L,
    ) = MissionInstanceEntity(
        instanceId = MissionInstanceEntity.buildInstanceId(missionId, periodType, periodStartEpoch),
        missionId = missionId,
        periodType = periodType,
        periodStartEpoch = periodStartEpoch,
        periodStartAt = 1_799_900_000_000L,
        periodEndAt = 1_800_100_000_000L,
        targetValue = 180L,
        baselineValue = null,
        starReward = 1,
        status = status,
        assignedAt = assignedAt,
        settledAt = null,
        definitionVersion = MissionInstanceEntity.CURRENT_DEFINITION_VERSION,
    )

    @Test
    fun missionInstance_insertAllIgnore_skipsDuplicatePeriodAndMission() = runTest {
        val first = sampleInstance()
        missionInstanceDao.insertAllIgnore(listOf(first))

        // Ayni missionId+periodType+periodStartEpoch icin ikinci deneme — hedef degistirilmis
        // olsa bile (targetValue=999) unique index nedeniyle IGNORE edilir, ilk kayit korunur.
        val duplicateAttempt = first.copy(targetValue = 999L)
        missionInstanceDao.insertAllIgnore(listOf(duplicateAttempt))

        val stored = missionInstanceDao.getInstancesForPeriod(
            MissionInstanceEntity.PERIOD_DAILY,
            20_650L,
        )
        assertEquals(1, stored.size)
        assertEquals(180L, stored.first().targetValue)
    }

    @Test
    fun missionInstance_getInstancesForPeriod_filtersByPeriodTypeAndEpoch() = runTest {
        missionInstanceDao.insertAllIgnore(
            listOf(
                sampleInstance(missionId = "daily_screen_under_3h", periodStartEpoch = 20_650L),
                sampleInstance(missionId = "daily_no_late_night", periodStartEpoch = 20_650L),
                sampleInstance(missionId = "daily_unlock_under_30", periodStartEpoch = 20_651L),
                sampleInstance(missionId = "weekly_positive_actions", periodType = MissionInstanceEntity.PERIOD_WEEKLY, periodStartEpoch = 20_650L),
            )
        )

        val dayResult = missionInstanceDao.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, 20_650L)
        assertEquals(2, dayResult.size)
        assertTrue(dayResult.all { it.periodType == MissionInstanceEntity.PERIOD_DAILY && it.periodStartEpoch == 20_650L })

        val weekResult = missionInstanceDao.getInstancesForPeriod(MissionInstanceEntity.PERIOD_WEEKLY, 20_650L)
        assertEquals(1, weekResult.size)
        assertEquals("weekly_positive_actions", weekResult.first().missionId)
    }

    @Test
    fun missionInstance_settleInstance_writesStatusAndSettledAt() = runTest {
        val instance = sampleInstance()
        missionInstanceDao.insertAllIgnore(listOf(instance))

        missionInstanceDao.settleInstance(instance.instanceId, MissionInstanceEntity.STATUS_COMPLETED, 1_800_050_000_000L)

        val stored = missionInstanceDao.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, 20_650L).first()
        assertEquals(MissionInstanceEntity.STATUS_COMPLETED, stored.status)
        assertEquals(1_800_050_000_000L, stored.settledAt)
    }

    @Test
    fun missionInstance_updateStatus_updatesOnlyStatus() = runTest {
        val instance = sampleInstance()
        missionInstanceDao.insertAllIgnore(listOf(instance))

        missionInstanceDao.updateStatus(instance.instanceId, MissionInstanceEntity.STATUS_FAILED)

        val stored = missionInstanceDao.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, 20_650L).first()
        assertEquals(MissionInstanceEntity.STATUS_FAILED, stored.status)
        assertNull(stored.settledAt)
    }

    @Test
    fun missionInstance_getUnsettledBefore_returnsOnlyAssignedPastPeriodEnd() = runTest {
        val past = sampleInstance(missionId = "daily_screen_under_3h", periodStartEpoch = 20_649L)
            .copy(periodEndAt = 1_000L)
        val future = sampleInstance(missionId = "daily_no_late_night", periodStartEpoch = 20_650L)
            .copy(periodEndAt = 5_000L)
        val alreadySettled = sampleInstance(missionId = "daily_unlock_under_30", periodStartEpoch = 20_649L)
            .copy(periodEndAt = 1_000L, status = MissionInstanceEntity.STATUS_COMPLETED, settledAt = 900L)
        missionInstanceDao.insertAllIgnore(listOf(past, future, alreadySettled))

        val unsettled = missionInstanceDao.getUnsettledBefore(2_000L)
        assertEquals(listOf("daily_screen_under_3h"), unsettled.map { it.missionId })
    }

    @Test
    fun missionInstance_clearAll_removesEverything() = runTest {
        missionInstanceDao.insertAllIgnore(listOf(sampleInstance()))
        missionInstanceDao.clearAll()
        assertTrue(missionInstanceDao.getInstancesForPeriod(MissionInstanceEntity.PERIOD_DAILY, 20_650L).isEmpty())
    }
}

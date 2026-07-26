package com.armutlu.apporganizer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.NotificationEvent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationMetadataDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: NotificationEventDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.notificationEventDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun metadataQueries_countCategoriesSuppressionAndImportanceWithoutContent() = runTest {
        val since = 1_000L
        dao.insert(
            event(
                packageName = "com.whatsapp",
                postedAt = 2_000L,
                category = NotificationCategory.MESSAGING,
                score = 65,
            )
        )
        dao.insert(
            event(
                packageName = "com.bank",
                postedAt = 3_000L,
                category = NotificationCategory.FINANCE,
                score = 90,
            )
        )
        dao.insert(
            event(
                packageName = "com.shop",
                postedAt = 4_000L,
                category = NotificationCategory.PROMOTION,
                score = 15,
                suppressed = true,
            )
        )
        dao.insert(
            event(
                packageName = "com.old",
                postedAt = 500L,
                category = NotificationCategory.OTHER,
                score = 35,
            )
        )

        val categories = dao.categoryCountsSince(since)
            .associate { it.category to it.count }

        assertEquals(1, categories[NotificationCategory.MESSAGING.name])
        assertEquals(1, categories[NotificationCategory.FINANCE.name])
        assertEquals(1, categories[NotificationCategory.PROMOTION.name])
        assertEquals(null, categories[NotificationCategory.OTHER.name])
        assertEquals(1, dao.suppressedCountSince(since))
        assertEquals(1, dao.importanceCountSince(since, minScore = 80, maxScore = 100))
        assertEquals(1, dao.importanceCountSince(since, minScore = 0, maxScore = 39))
    }

    private fun event(
        packageName: String,
        postedAt: Long,
        category: NotificationCategory,
        score: Int,
        suppressed: Boolean = false,
    ) = NotificationEvent(
        packageName = packageName,
        postedAt = postedAt,
        category = category.name,
        importanceScore = score,
        wasSuppressed = suppressed,
        systemPriority = 0,
    )
}

package com.armutlu.apporganizer.utils

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.data.local.TaskScoreEventDao
import com.armutlu.apporganizer.domain.models.TaskScoreEventEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TaskScoreManager — Dongu M08 (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satir 1200-1249). Adil puan tablosu: toplu odul dogrusal degil, reddetme/erteleme
 * cezalandirmaz, bildirim raporu gunde bir kez sayilir, pulse katkisi +-10 ile sinirli.
 */
class TaskScoreManagerTest {

    /** In-memory sahte DAO — MissionMetricSnapshotProviderTest.kt ile ayni pattern. */
    private class FakeTaskScoreEventDao(
        val events: MutableList<TaskScoreEventEntry> = mutableListOf(),
    ) : TaskScoreEventDao {

        override suspend fun insert(entry: TaskScoreEventEntry): Long {
            events += entry
            return events.size.toLong()
        }

        override suspend fun getTotalScore(): Int = events.sumOf { it.delta }

        override suspend fun getScoreBetween(fromInclusive: Long, toInclusive: Long): Int =
            events.filter { it.createdAt in fromInclusive..toInclusive }.sumOf { it.delta }

        override suspend fun getLatestEvent(): TaskScoreEventEntry? =
            events.maxByOrNull { it.createdAt }

        override suspend fun countEventsBetween(
            fromInclusive: Long,
            toInclusive: Long,
            positiveOnly: Boolean,
        ): Int = events.count {
            it.createdAt in fromInclusive..toInclusive && (!positiveOnly || it.delta > 0)
        }

        override suspend fun countEventsBetweenByKeys(
            fromInclusive: Long,
            toInclusive: Long,
            eventKeys: List<String>,
        ): Int = events.count {
            it.createdAt in fromInclusive..toInclusive && it.eventKey in eventKeys
        }

        override suspend fun insertOnceBetween(
            entry: TaskScoreEventEntry,
            fromInclusive: Long,
            toInclusive: Long,
        ): Boolean {
            if (countEventsBetweenByKeys(fromInclusive, toInclusive, listOf(entry.eventKey)) > 0) return false
            insert(entry)
            return true
        }

        override suspend fun getPositiveScore(): Int = events.filter { it.delta > 0 }.sumOf { it.delta }

        override suspend fun getNegativeScore(): Int = events.filter { it.delta < 0 }.sumOf { it.delta }

        override suspend fun clearAll() {
            events.clear()
        }
    }

    private lateinit var fakeDao: FakeTaskScoreEventDao
    private lateinit var mockContext: Context
    private lateinit var mockDatabase: AppDatabase

    @Before
    fun setup() {
        fakeDao = FakeTaskScoreEventDao()
        mockContext = mockk(relaxed = true)
        mockDatabase = mockk()
        every { mockDatabase.taskScoreEventDao() } returns fakeDao

        mockkObject(AppDatabase.Companion)
        every { AppDatabase.getInstance(mockContext) } returns mockDatabase
    }

    @After
    fun tearDown() {
        unmockkObject(AppDatabase.Companion)
    }

    // ---- bulkReward sinir degerleri ----

    @Test
    fun `bulkReward sinir degerleri roadmap tablosuyla eslesir`() {
        assertEquals(0, TaskScoreManager.bulkReward(0))
        assertEquals(3, TaskScoreManager.bulkReward(1))
        assertEquals(5, TaskScoreManager.bulkReward(2))
        assertEquals(5, TaskScoreManager.bulkReward(5))
        assertEquals(7, TaskScoreManager.bulkReward(6))
        assertEquals(7, TaskScoreManager.bulkReward(10))
        assertEquals(10, TaskScoreManager.bulkReward(11))
        assertEquals(10, TaskScoreManager.bulkReward(100))
    }

    // ---- 100 uygulamalik toplu islem +10'u asmaz ----

    @Test
    fun `100 uygulamalik toplu klasor kabulu 10 puani asmaz`() = runTest {
        val snapshot = TaskScoreManager.recordBulk(
            mockContext,
            TaskScoreManager.EventType.FolderSuggestionAccepted,
            itemCount = 100,
        )
        assertEquals(10, snapshot.totalScore)
        assertEquals(1, fakeDao.events.size)
        assertEquals(10, fakeDao.events.single().delta)
    }

    @Test
    fun `100 uygulamalik toplu benzer uygulama kabulu 10 puani asmaz`() = runTest {
        val snapshot = TaskScoreManager.recordBulk(
            mockContext,
            TaskScoreManager.EventType.SimilarAppsAccepted,
            itemCount = 100,
        )
        assertEquals(10, snapshot.totalScore)
    }

    // ---- Reddetme / erteleme toplam skoru dusurmez ----

    @Test
    fun `siniflandirma ertelemesi delta sifirdir ve skoru dusurmez`() = runTest {
        TaskScoreManager.record(mockContext, TaskScoreManager.EventType.ClassificationApproved)
        val before = TaskScoreManager.getSnapshotV2(mockContext).totalScore

        val after = TaskScoreManager.record(mockContext, TaskScoreManager.EventType.ClassificationSnoozed)

        assertEquals(before, after.totalScore)
        assertTrue(fakeDao.events.none { it.delta < 0 })
    }

    @Test
    fun `klasor onerisi reddetme ve erteleme skoru dusurmez`() = runTest {
        TaskScoreManager.record(mockContext, TaskScoreManager.EventType.ClassificationApproved)
        val before = TaskScoreManager.getSnapshotV2(mockContext).totalScore

        TaskScoreManager.record(mockContext, TaskScoreManager.EventType.FolderSuggestionDismissed)
        TaskScoreManager.record(mockContext, TaskScoreManager.EventType.FolderSuggestionSnoozed)
        val after = TaskScoreManager.getSnapshotV2(mockContext).totalScore

        assertEquals(before, after)
        assertTrue(fakeDao.events.all { it.delta >= 0 })
    }

    // ---- Bildirim raporu ayni gun bir kez sayilir ----

    @Test
    fun `bildirim raporu ayni gun ikinci goruntuleme sayilmaz`() = runTest {
        val first = TaskScoreManager.record(mockContext, TaskScoreManager.EventType.NotificationReportViewed)
        val second = TaskScoreManager.record(mockContext, TaskScoreManager.EventType.NotificationReportViewed)

        assertEquals(1, first.totalScore)
        assertEquals(1, second.totalScore)
        assertEquals(1, fakeDao.events.count { it.eventKey == "notification_report_viewed" })
    }

    // ---- Yeni puan tablosu dogrulamasi ----

    @Test
    fun `yeni puan tablosu roadmap ile eslesir`() {
        assertEquals(2, TaskScoreManager.EventType.ClassificationApproved.delta)
        assertEquals(4, TaskScoreManager.EventType.ClassificationCorrected.delta)
        assertEquals(0, TaskScoreManager.EventType.ClassificationSnoozed.delta)
        assertEquals(0, TaskScoreManager.EventType.FolderSuggestionSnoozed.delta)
        assertEquals(0, TaskScoreManager.EventType.FolderSuggestionDismissed.delta)
        assertEquals(1, TaskScoreManager.EventType.NotificationReportViewed.delta)
    }

    // ---- Pulse katkisi +-10 sinirinda ----

    @Test
    fun `pulse katkisi ust sinirda 10 ile sinirlanir`() = runTest {
        // 14 gunluk pencerede cok yuksek pozitif net skor birikir.
        repeat(50) {
            fakeDao.events += TaskScoreEventEntry(
                eventKey = "folder_suggestion_accepted",
                label = "x",
                delta = 10,
                createdAt = System.currentTimeMillis(),
            )
        }
        val contribution = TaskScoreManager.getPulseContribution(mockContext)
        assertEquals(10, contribution)
    }

    @Test
    fun `pulse katkisi alt sinirda eksi 10 ile sinirlanir`() = runTest {
        repeat(50) {
            fakeDao.events += TaskScoreEventEntry(
                eventKey = "legacy_negative",
                label = "x",
                delta = -10,
                createdAt = System.currentTimeMillis(),
            )
        }
        val contribution = TaskScoreManager.getPulseContribution(mockContext)
        assertEquals(-10, contribution)
    }

    @Test
    fun `pulse katkisi pencere disindaki olaylari saymaz`() = runTest {
        val fifteenDaysAgo = System.currentTimeMillis() - 15L * 24L * 60L * 60L * 1000L
        fakeDao.events += TaskScoreEventEntry(
            eventKey = "folder_suggestion_accepted",
            label = "eski",
            delta = 10,
            createdAt = fifteenDaysAgo,
        )
        val contribution = TaskScoreManager.getPulseContribution(mockContext)
        assertEquals(0, contribution)
    }
}

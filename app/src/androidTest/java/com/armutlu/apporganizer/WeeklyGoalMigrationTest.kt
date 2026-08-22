package com.armutlu.apporganizer

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.armutlu.apporganizer.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P3 — [AppDatabase.MIGRATION_24_25]. Mevcut manuel `weekly_goals` satırlarının veri kaybı
 * olmadan `mode='MANUAL'`e migrate edildiğini, `achievedAt>0` olan satırların `status='COMPLETED'`
 * türetildiğini doğrular (roadmap §6).
 */
@RunWith(AndroidJUnit4::class)
class WeeklyGoalMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migration24To25_preservesManualGoalsAndDerivesCompletedStatus() {
        helper.createDatabase(TEST_DB, 24).apply {
            execSQL(
                """
                INSERT INTO weekly_goals(categoryId, targetMinutes, weekStartEpochDay, createdAt, achievedAt)
                VALUES ('social', 300, 19000, 1000, 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO weekly_goals(categoryId, targetMinutes, weekStartEpochDay, createdAt, achievedAt)
                VALUES ('games', 200, 19000, 1000, 5000)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            25,
            true,
            AppDatabase.MIGRATION_24_25,
        )

        migrated.query(
            """
            SELECT categoryId, targetMinutes, mode, status, achievedAt
            FROM weekly_goals
            WHERE categoryId = 'social'
            """.trimIndent(),
        ).use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            assertEquals(300, cursor.getInt(cursor.getColumnIndexOrThrow("targetMinutes")))
            assertEquals("MANUAL", cursor.getString(cursor.getColumnIndexOrThrow("mode")))
            assertEquals("ACTIVE", cursor.getString(cursor.getColumnIndexOrThrow("status")))
        }

        migrated.query(
            """
            SELECT categoryId, mode, status
            FROM weekly_goals
            WHERE categoryId = 'games'
            """.trimIndent(),
        ).use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            assertEquals("MANUAL", cursor.getString(cursor.getColumnIndexOrThrow("mode")))
            assertEquals("COMPLETED", cursor.getString(cursor.getColumnIndexOrThrow("status")))
        }

        migrated.close()
    }

    private companion object {
        const val TEST_DB = "weekly-goal-migration-test"
    }
}

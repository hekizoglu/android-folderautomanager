package com.armutlu.apporganizer

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.armutlu.apporganizer.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationMetadataMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migration23To24_preservesRowsAndAddsContentFreeDefaults() {
        helper.createDatabase(TEST_DB, 23).apply {
            execSQL(
                """
                INSERT INTO notification_events(packageName, postedAt)
                VALUES ('com.legacy.app', 1234)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            24,
            true,
            AppDatabase.MIGRATION_23_24,
        )

        migrated.query(
            """
            SELECT packageName, postedAt, category, importanceScore, wasSuppressed, systemPriority
            FROM notification_events
            WHERE packageName = 'com.legacy.app'
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.legacy.app", cursor.getString(cursor.getColumnIndexOrThrow("packageName")))
            assertEquals(1234L, cursor.getLong(cursor.getColumnIndexOrThrow("postedAt")))
            assertEquals("OTHER", cursor.getString(cursor.getColumnIndexOrThrow("category")))
            assertEquals(35, cursor.getInt(cursor.getColumnIndexOrThrow("importanceScore")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("wasSuppressed")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("systemPriority")))
        }

        assertNoNotificationContentColumns(migrated)
        migrated.close()
    }

    private fun assertNoNotificationContentColumns(db: SupportSQLiteDatabase) {
        val forbidden = setOf(
            "title",
            "text",
            "body",
            "sender",
            "otp",
            "amount",
            "account",
            "iban",
        )
        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info(notification_events)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                columns += cursor.getString(nameIndex).lowercase()
            }
        }
        forbidden.forEach { forbiddenColumn ->
            assertFalse(
                "notification_events must not persist content column: $forbiddenColumn",
                forbiddenColumn in columns,
            )
        }
    }

    private companion object {
        const val TEST_DB = "notification-metadata-migration-test"
    }
}

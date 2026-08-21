package com.armutlu.apporganizer

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration12To13_preservesLegacySearchHistory() {
        val dbName = "migration-12-13-preserves-search-history"
        helper.createDatabase(dbName, 12).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS search_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    query TEXT NOT NULL,
                    timestamp INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            execSQL("INSERT INTO search_history (query, timestamp) VALUES ('weather', 1234)")
            close()
        }

        helper.runMigrationsAndValidate(
            dbName,
            13,
            true,
            AppDatabase.MIGRATION_12_13,
        ).use { db ->
            db.query("SELECT query, timestamp FROM search_history").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("weather", cursor.getString(cursor.getColumnIndexOrThrow("query")))
                assertEquals(1234L, cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
            }
        }

        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(dbName)
    }

    @Test
    fun migration21To22_preservesExistingOperationRows() {
        val dbName = "migration-21-22-preserves-operations"
        helper.createDatabase(dbName, 21).apply {
            execSQL("DROP TABLE IF EXISTS operations")
            execSQL(
                """
                CREATE TABLE operations (
                    id TEXT NOT NULL PRIMARY KEY,
                    type TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    sourceCategoryId TEXT NOT NULL,
                    targetCategoryId TEXT,
                    movedPackageNames TEXT NOT NULL,
                    oldCategoryMapping TEXT NOT NULL,
                    rolledBack INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO operations
                (id, type, timestamp, sourceCategoryId, targetCategoryId,
                 movedPackageNames, oldCategoryMapping, rolledBack)
                VALUES ('op-1', 'FOLDER_MERGE', 1234, 'social', 'communication',
                        '[\"com.example.app\"]', '{\"com.example.app\":\"social\"}', 0)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            dbName,
            22,
            true,
            AppDatabase.MIGRATION_21_22,
        ).use { db ->
            db.query("SELECT * FROM operations WHERE id = 'op-1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("FOLDER_MERGE", cursor.getString(cursor.getColumnIndexOrThrow("type")))
                assertEquals("social", cursor.getString(cursor.getColumnIndexOrThrow("sourceCategoryId")))
                assertEquals(
                    "[\"com.example.app\"]",
                    cursor.getString(cursor.getColumnIndexOrThrow("movedPackageNames")),
                )
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("rolledBack")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("rolledBackAt")))
            }
        }

        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(dbName)
    }

    @Test
    fun migration18To19_addsAppFileNameColumnAndIndex() {
        val dbName = "migration-18-19"
        helper.createDatabase(dbName, 18).apply {
            close()
        }

        helper.runMigrationsAndValidate(
            dbName,
            19,
            true,
            AppDatabase.MIGRATION_18_19,
        ).use { db ->
            val appColumns = mutableMapOf<String, String>()
            db.query("PRAGMA table_info(apps)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")
                while (cursor.moveToNext()) {
                    appColumns[cursor.getString(nameIndex)] = cursor.getString(defaultIndex) ?: ""
                }
            }

            val indexes = mutableSetOf<String>()
            db.query("PRAGMA index_list(apps)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    indexes += cursor.getString(nameIndex)
                }
            }

            assertEquals("''", appColumns.getValue("appFileName"))
            assertTrue(indexes.contains("index_apps_appFileName"))
        }

        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(dbName)
    }
}

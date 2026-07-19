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

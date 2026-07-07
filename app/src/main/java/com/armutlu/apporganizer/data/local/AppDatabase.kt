package com.armutlu.apporganizer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SearchHistory
import timber.log.Timber

/**
 * Room Database for AppOrganizer
 * Handles persistence of apps and categories
 *
 * Not: search_fts FTS5 sanal tablosu Migration 8→9'daki raw SQL ile yönetilir.
 * Room @Fts5 entity yerine raw SQL tercih edildi — kapt stub uyumsuzluğunu önler.
 */
@Database(
    entities = [AppInfo::class, Category::class, SearchDocument::class, SearchHistory::class, com.armutlu.apporganizer.domain.models.NotificationEvent::class],
    version = 12,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
    abstract fun categoryDao(): CategoryDao
    abstract fun searchDao(): SearchDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun notificationEventDao(): NotificationEventDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // v1→v2: notificationCount sütunu eklendi
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "notificationCount", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v2→v3: isHidden sütunu eklendi
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "isHidden", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v3→v4: lastUsedTimestamp sütunu eklendi
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "lastUsedTimestamp", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v4→v5: notificationText sütunu eklendi
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "notificationText", "TEXT NOT NULL DEFAULT ''")
            }
        }

        // v5→v6: customNotes sütunu eklendi
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "customNotes", "TEXT NOT NULL DEFAULT ''")
            }
        }

        // v6→v7: şema değişimi yok, categories tablosuna yeni kategoriler eklendi (DatabaseCallback ile)
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Şema değişikliği yok — yeni kategoriler DatabaseCallback.onOpen içinde eklenir
            }
        }

        // v7→v8: AppInfo'ya firstInstalledTime, lastUpdatedTime, targetSdkVersion, versionName eklendi
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "firstInstalledTime", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "lastUpdatedTime", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "targetSdkVersion", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "versionName", "TEXT NOT NULL DEFAULT ''")
            }
        }

        // v9→v10: SearchHistory tablosu eklendi (arama geçmişi B1)
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS search_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        query TEXT NOT NULL,
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        // v10→v11: apps tablosuna query optimize için index'ler eklendi (CS13: SELECT * ORDER BY appName sınırsız)
        // NOT: Index adları Room'un entity'den ürettiği adlarla (index_apps_*) birebir aynı olmalı —
        // aksi halde bir sonraki migration'da şema doğrulaması "Migration didn't properly handle" ile çöker.
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_appName ON apps(appName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_categoryId ON apps(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_appName_categoryId ON apps(appName, categoryId)")
            }
        }

        // v11→v12: notification_events tablosu — Bildirim Analiz Raporu veri kaynağı.
        // Ayrıca eski MIGRATION_10_11'in yanlış adla (idx_apps_*) oluşturduğu index'ler
        // Room'un beklediği adlarla (index_apps_*) onarılır — v11 cihazlarda upgrade çökmesini önler.
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS idx_apps_appName")
                db.execSQL("DROP INDEX IF EXISTS idx_apps_categoryId")
                db.execSQL("DROP INDEX IF EXISTS idx_apps_appName_categoryId")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_appName ON apps(appName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_categoryId ON apps(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_appName_categoryId ON apps(appName, categoryId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS notification_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        postedAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_events_packageName ON notification_events(packageName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_events_postedAt ON notification_events(postedAt)")
            }
        }

        // v8→v9: SearchDocument tablosu + FTS5 sanal tablo (birleşik arama Sprint 1)
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS search_documents (
                        docId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        source_type TEXT NOT NULL,
                        source_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        subtitle TEXT NOT NULL DEFAULT '',
                        icon_key TEXT NOT NULL DEFAULT '',
                        source_group TEXT NOT NULL DEFAULT 'app',
                        last_modified INTEGER NOT NULL DEFAULT 0
                    )
                """)
                ensureSearchTables(db)
            }
        }

        /**
         * SQLite'ta "ALTER TABLE ADD COLUMN IF NOT EXISTS" yok — sütun zaten varsa
         * "duplicate column name" ile çöker (backup/restore veya versiyon karışıklığında
         * user_version ile gerçek şema uyuşmayabilir). PRAGMA table_info ile önce kontrol et.
         */
        internal fun SupportSQLiteDatabase.addColumnIfNotExists(table: String, column: String, definition: String) {
            val exists = query("PRAGMA table_info($table)").use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIdx) == column) { found = true; break }
                }
                found
            }
            if (!exists) {
                execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            } else {
                Timber.w("Migration atlandı — $table.$column zaten mevcut (şema/versiyon uyuşmazlığı)")
            }
        }

        // FTS5 bazı AOSP build'lerinde yoktur; try/catch ile graceful degrade
        internal fun ensureSearchTables(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS search_fts USING fts5(
                        search_text, keywords,
                        content='search_documents',
                        content_rowid='docId',
                        tokenize='unicode61'
                    )
                """)
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS search_fts_ai AFTER INSERT ON search_documents BEGIN
                        INSERT INTO search_fts(rowid, search_text, keywords)
                        VALUES (new.docId, new.title || ' ' || new.subtitle, '');
                    END
                """)
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS search_fts_ad AFTER DELETE ON search_documents BEGIN
                        INSERT INTO search_fts(search_fts, rowid, search_text, keywords)
                        VALUES ('delete', old.docId, old.title || ' ' || old.subtitle, '');
                    END
                """)
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS search_fts_au AFTER UPDATE ON search_documents BEGIN
                        INSERT INTO search_fts(search_fts, rowid, search_text, keywords)
                        VALUES ('delete', old.docId, old.title || ' ' || old.subtitle, '');
                        INSERT INTO search_fts(rowid, search_text, keywords)
                        VALUES (new.docId, new.title || ' ' || new.subtitle, '');
                    END
                """)
                Timber.d("FTS5 sanal tablosu ve trigger'lar oluşturuldu")
            } catch (e: Exception) {
                Timber.w("FTS5 desteklenmiyor, LIKE araması kullanılacak: ${e.message}")
            }
        }

        fun isFts5Available(db: SupportSQLiteDatabase): Boolean {
            return try {
                db.query("SELECT * FROM search_fts LIMIT 0", emptyArray<Any?>()).close()
                true
            } catch (_: Exception) {
                false
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_organizer_db"
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Callback to initialize database with default categories
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Timber.d("Database created")
                
                // Insert default categories
                val defaultCategories = Category.getDefaultCategories()
                defaultCategories.forEach { category ->
                    val query = """
                        INSERT INTO categories 
                        (categoryId, categoryName, description, colorHex, iconEmoji, isSystemCategory, displayOrder, createdAt)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    
                    db.execSQL(
                        query,
                        arrayOf(
                            category.categoryId,
                            category.categoryName,
                            category.description,
                            category.colorHex,
                            category.iconEmoji,
                            if (category.isSystemCategory) 1 else 0,
                            category.displayOrder,
                            category.createdAt
                        )
                    )
                }

                ensureSearchTables(db)
                
                Timber.d("Default categories inserted: ${defaultCategories.size}")
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                ensureSearchTables(db)
                Timber.d("Database opened")
            }
        }
    }
}

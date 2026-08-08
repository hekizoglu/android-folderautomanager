package com.armutlu.apporganizer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.MissionHistoryEntry
import com.armutlu.apporganizer.domain.models.MissionInstanceEntity
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.TaskScoreEventEntry
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import timber.log.Timber

/**
 * Room Database for AppOrganizer
 * Handles persistence of apps and categories
 *
 * Not: search_fts FTS5 sanal tablosu Migration 8→9'daki raw SQL ile yönetilir.
 * Room @Fts5 entity yerine raw SQL tercih edildi — kapt stub uyumsuzluğunu önler.
 */
@Database(
    entities = [AppInfo::class, Category::class, SearchDocument::class, com.armutlu.apporganizer.domain.models.NotificationEvent::class, WeeklyGoal::class, MissionHistoryEntry::class, TaskScoreEventEntry::class, MissionInstanceEntity::class, TickerHistoryEntity::class, HomeGridItemEntity::class, com.armutlu.apporganizer.domain.models.Operation::class, UndoMergeEntity::class, com.armutlu.apporganizer.domain.models.NotificationHistoryEntity::class],
    version = 28,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
    abstract fun categoryDao(): CategoryDao
    abstract fun searchDao(): SearchDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun weeklyGoalDao(): WeeklyGoalDao
    abstract fun missionHistoryDao(): MissionHistoryDao
    abstract fun taskScoreEventDao(): TaskScoreEventDao
    abstract fun missionInstanceDao(): MissionInstanceDao
    abstract fun tickerHistoryDao(): TickerHistoryDao
    abstract fun homeGridItemDao(): HomeGridItemDao
    abstract fun operationDao(): OperationDao
    abstract fun undoMergeDao(): UndoMergeDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "notificationCount", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "isHidden", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "lastUsedTimestamp", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "notificationText", "TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "customNotes", "TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "firstInstalledTime", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "lastUpdatedTime", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "targetSdkVersion", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "versionName", "TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
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
                    """
                )
                ensureSearchTables(db)
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS search_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        query TEXT NOT NULL,
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_appName ON apps(appName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_categoryId ON apps(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_appName_categoryId ON apps(appName, categoryId)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS idx_apps_appName")
                db.execSQL("DROP INDEX IF EXISTS idx_apps_categoryId")
                db.execSQL("DROP INDEX IF EXISTS idx_apps_appName_categoryId")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_appName ON apps(appName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_categoryId ON apps(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_appName_categoryId ON apps(appName, categoryId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        postedAt INTEGER NOT NULL
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_events_packageName ON notification_events(packageName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_events_postedAt ON notification_events(postedAt)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS search_history")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "launchCount", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weekly_goals (
                        categoryId TEXT NOT NULL,
                        targetMinutes INTEGER NOT NULL,
                        weekStartEpochDay INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        achievedAt INTEGER NOT NULL,
                        PRIMARY KEY(categoryId, weekStartEpochDay)
                    )
                    """
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "classificationSource", "TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.addColumnIfNotExists("apps", "classificationConfidence", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "classificationReason", "TEXT NOT NULL DEFAULT 'NO_RELIABLE_MATCH'")
                db.addColumnIfNotExists("apps", "classificationReviewState", "TEXT NOT NULL DEFAULT 'PENDING'")
                db.addColumnIfNotExists("apps", "isCategoryLocked", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "classificationVersion", "INTEGER NOT NULL DEFAULT 1")
                db.addColumnIfNotExists("apps", "lastClassifiedAt", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "lastReviewedAt", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "reviewSnoozedUntil", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mission_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        missionId TEXT NOT NULL,
                        periodType TEXT NOT NULL,
                        periodStartEpoch INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL,
                        starReward INTEGER NOT NULL,
                        source TEXT NOT NULL DEFAULT 'auto'
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mission_history_periodType_periodStartEpoch ON mission_history(periodType, periodStartEpoch)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_mission_history_missionId_periodType_periodStartEpoch ON mission_history(missionId, periodType, periodStartEpoch)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_score_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventKey TEXT NOT NULL,
                        label TEXT NOT NULL,
                        delta INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_score_events_eventKey_createdAt ON task_score_events(eventKey, createdAt)")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mission_instances (
                        instanceId TEXT NOT NULL,
                        missionId TEXT NOT NULL,
                        periodType TEXT NOT NULL,
                        periodStartEpoch INTEGER NOT NULL,
                        periodStartAt INTEGER NOT NULL,
                        periodEndAt INTEGER NOT NULL,
                        targetValue INTEGER,
                        baselineValue INTEGER,
                        starReward INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        assignedAt INTEGER NOT NULL,
                        settledAt INTEGER,
                        definitionVersion INTEGER NOT NULL,
                        PRIMARY KEY(instanceId)
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mission_instances_periodType_periodStartEpoch ON mission_instances(periodType, periodStartEpoch)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_mission_instances_missionId_periodType_periodStartEpoch ON mission_instances(missionId, periodType, periodStartEpoch)")
            }
        }

        internal val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "appFileName", "TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apps_appFileName ON apps(appFileName)")
            }
        }

        internal val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ticker_history (
                        id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        subtitle TEXT,
                        icon TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        actionType TEXT NOT NULL,
                        sensitive INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """
                )
            }
        }

        internal val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS home_grid_items (
                        itemId TEXT NOT NULL,
                        itemType TEXT NOT NULL,
                        screenIndex INTEGER NOT NULL,
                        cellX INTEGER NOT NULL,
                        cellY INTEGER NOT NULL,
                        spanX INTEGER NOT NULL,
                        spanY INTEGER NOT NULL,
                        PRIMARY KEY(itemId)
                    )
                    """
                )
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `operations`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `operations` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `type` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0,
                        `sourceCategoryId` TEXT NOT NULL DEFAULT '',
                        `targetCategoryId` TEXT,
                        `movedPackageNames` TEXT NOT NULL DEFAULT '',
                        `oldCategoryMapping` TEXT NOT NULL DEFAULT '',
                        `rolledBack` INTEGER NOT NULL DEFAULT 0,
                        `rolledBackAt` INTEGER
                    )
                    """
                )
                db.addColumnIfNotExists("operations", "type", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("operations", "timestamp", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("operations", "sourceCategoryId", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("operations", "targetCategoryId", "TEXT")
                db.addColumnIfNotExists("operations", "movedPackageNames", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("operations", "oldCategoryMapping", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("operations", "rolledBack", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("operations", "rolledBackAt", "INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_operations_timestamp` ON `operations`(`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_operations_type` ON `operations`(`type`)")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `undo_merges` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceCategoryId` TEXT NOT NULL DEFAULT '',
                        `targetCategoryId` TEXT NOT NULL DEFAULT '',
                        `affectedPackages` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0,
                        `mergedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )
                db.addColumnIfNotExists("undo_merges", "sourceCategoryId", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("undo_merges", "targetCategoryId", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("undo_merges", "affectedPackages", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("undo_merges", "timestamp", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("undo_merges", "mergedAt", "INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_undo_merges_timestamp` ON `undo_merges`(`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_undo_merges_source` ON `undo_merges`(`sourceCategoryId`)")
            }
        }

        internal val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists(
                    "notification_events",
                    "category",
                    "TEXT NOT NULL DEFAULT 'OTHER'",
                )
                db.addColumnIfNotExists(
                    "notification_events",
                    "importanceScore",
                    "INTEGER NOT NULL DEFAULT 35",
                )
                db.addColumnIfNotExists(
                    "notification_events",
                    "wasSuppressed",
                    "INTEGER NOT NULL DEFAULT 0",
                )
                db.addColumnIfNotExists(
                    "notification_events",
                    "systemPriority",
                    "INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        internal val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // P3 — Adaptif kategori hedefleri (roadmap §6). Additive migration, veri kaybı
                // yok. Mevcut satırlar mode='MANUAL' alır — açık rıza olmadan otomatik moda
                // GEÇİRİLMEZ (roadmap §3 ürün kararı).
                db.addColumnIfNotExists("weekly_goals", "mode", "TEXT NOT NULL DEFAULT 'MANUAL'")
                db.addColumnIfNotExists("weekly_goals", "baselineMinutes", "INTEGER")
                db.addColumnIfNotExists("weekly_goals", "previousWeekActualMinutes", "INTEGER")
                db.addColumnIfNotExists("weekly_goals", "pace", "TEXT NOT NULL DEFAULT 'DENGELI'")
                db.addColumnIfNotExists("weekly_goals", "status", "TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.addColumnIfNotExists("weekly_goals", "generatedAt", "INTEGER")
                db.addColumnIfNotExists("weekly_goals", "settledAt", "INTEGER")
                db.addColumnIfNotExists("weekly_goals", "algorithmVersion", "INTEGER NOT NULL DEFAULT 1")
                // Mevcut satırlarda achievedAt>0 ise status'u COMPLETED'e türet — eski
                // "başarıldı" bilgisini yeni status alanına kaybetmeden taşır.
                db.execSQL("UPDATE weekly_goals SET status = 'COMPLETED' WHERE achievedAt > 0")
            }
        }

        internal val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bildirim Geçmişi (D242c) — yalnızca kullanıcı "Bildirim metnini göster" ayarını
                // AÇTIYSA doldurulur (AppNotificationListenerService). NotificationEvent (paket+zaman,
                // içeriksiz) ile karıştırılmamalı — bu tablo gerçek başlık/metin taşır.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        title TEXT NOT NULL,
                        text TEXT NOT NULL DEFAULT '',
                        postedAt INTEGER NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_history_packageName ON notification_history(packageName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_history_postedAt ON notification_history(postedAt)")
            }
        }

        internal val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists("apps", "lastNotificationPostedAt", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "appSizeBytes", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "notificationImportance", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "isInstalled", "INTEGER NOT NULL DEFAULT 1")
                db.addColumnIfNotExists("apps", "installTime", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "lastUpdated", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "iconUrl", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("apps", "isSystemApp", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        internal val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration 26->27 icerigini 27->28'e de kopyala (D239); eger kullanıcı
                // v27'ye destructive ile gecmisse 26->27 calısmaz ama eksik kolonlar
                // hala lazım olabilir. addColumnIfNotExists zaten varlık kontrolü yapar.
                db.addColumnIfNotExists("apps", "lastNotificationPostedAt", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "appSizeBytes", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "notificationImportance", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "isInstalled", "INTEGER NOT NULL DEFAULT 1")
                db.addColumnIfNotExists("apps", "installTime", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "lastUpdated", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfNotExists("apps", "iconUrl", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfNotExists("apps", "isSystemApp", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        internal fun SupportSQLiteDatabase.addColumnIfNotExists(
            table: String,
            column: String,
            definition: String,
        ) {
            val exists = query("PRAGMA table_info($table)").use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIdx) == column) {
                        found = true
                        break
                    }
                }
                found
            }
            if (!exists) {
                execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            } else {
                Timber.w("Migration atlandı — $table.$column zaten mevcut (şema/versiyon uyuşmazlığı)")
            }
        }

        internal fun ensureSearchTables(db: SupportSQLiteDatabase) {
            try {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS search_fts USING fts5(
                        search_text, keywords,
                        content='search_documents',
                        content_rowid='docId',
                        tokenize='unicode61'
                    )
                    """
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS search_fts_ai AFTER INSERT ON search_documents BEGIN
                        INSERT INTO search_fts(rowid, search_text, keywords)
                        VALUES (new.docId, new.title || ' ' || new.subtitle, '');
                    END
                    """
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS search_fts_ad AFTER DELETE ON search_documents BEGIN
                        INSERT INTO search_fts(search_fts, rowid, search_text, keywords)
                        VALUES ('delete', old.docId, old.title || ' ' || old.subtitle, '');
                    END
                    """
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS search_fts_au AFTER UPDATE ON search_documents BEGIN
                        INSERT INTO search_fts(search_fts, rowid, search_text, keywords)
                        VALUES ('delete', old.docId, old.title || ' ' || old.subtitle, '');
                        INSERT INTO search_fts(rowid, search_text, keywords)
                        VALUES (new.docId, new.title || ' ' || new.subtitle, '');
                    END
                    """
                )
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
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23,
                        MIGRATION_23_24,
                        MIGRATION_24_25,
                        MIGRATION_25_26,
                        MIGRATION_26_27,
                        MIGRATION_27_28,
                    )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Timber.d("Database created")

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
                            category.createdAt,
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

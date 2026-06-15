package com.armutlu.apporganizer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import timber.log.Timber

/**
 * Room Database for AppOrganizer
 * Handles persistence of apps and categories
 */
@Database(
    entities = [AppInfo::class, Category::class],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun appDao(): AppDao
    abstract fun categoryDao(): CategoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // v1→v2: notificationCount sütunu eklendi
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apps ADD COLUMN notificationCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v2→v3: isHidden sütunu eklendi
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apps ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v3→v4: lastUsedTimestamp sütunu eklendi
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apps ADD COLUMN lastUsedTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v4→v5: notificationText sütunu eklendi
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apps ADD COLUMN notificationText TEXT NOT NULL DEFAULT ''")
            }
        }

        // v5→v6: customNotes sütunu eklendi
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apps ADD COLUMN customNotes TEXT NOT NULL DEFAULT ''")
            }
        }

        // v6→v7: şema değişimi yok, categories tablosuna yeni kategoriler eklendi (DatabaseCallback ile)
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Şema değişikliği yok — yeni kategoriler DatabaseCallback.onOpen içinde eklenir
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
                        MIGRATION_6_7
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
                
                Timber.d("Default categories inserted: ${defaultCategories.size}")
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Timber.d("Database opened")
            }
        }
    }
}

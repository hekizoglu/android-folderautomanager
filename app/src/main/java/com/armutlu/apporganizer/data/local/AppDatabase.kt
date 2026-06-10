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
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun appDao(): AppDao
    abstract fun categoryDao(): CategoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_organizer_db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
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

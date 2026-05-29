package com.armutlu.apporganizer.data.local

import androidx.room.*
import com.armutlu.apporganizer.domain.models.Category
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Category entity
 * Handles all database operations related to categories
 */
@Dao
interface CategoryDao {
    
    /**
     * Insert a single category
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)
    
    /**
     * Insert multiple categories
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)
    
    /**
     * Update a category
     */
    @Update
    suspend fun updateCategory(category: Category)
    
    /**
     * Delete a category
     */
    @Delete
    suspend fun deleteCategory(category: Category)
    
    /**
     * Delete category by ID
     */
    @Query("DELETE FROM categories WHERE categoryId = :categoryId")
    suspend fun deleteCategoryById(categoryId: String)
    
    /**
     * Get category by ID
     */
    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    suspend fun getCategoryById(categoryId: String): Category?
    
    /**
     * Get all categories (one-time)
     */
    @Query("SELECT * FROM categories ORDER BY displayOrder ASC")
    suspend fun getAllCategories(): List<Category>
    
    /**
     * Get all categories as Flow (real-time updates)
     */
    @Query("SELECT * FROM categories ORDER BY displayOrder ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>
    
    /**
     * Get system categories only
     */
    @Query("SELECT * FROM categories WHERE isSystemCategory = 1 ORDER BY displayOrder ASC")
    fun getSystemCategories(): Flow<List<Category>>
    
    /**
     * Get custom user categories
     */
    @Query("SELECT * FROM categories WHERE isSystemCategory = 0 ORDER BY displayOrder ASC")
    fun getCustomCategories(): Flow<List<Category>>
    
    /**
     * Search categories by name
     */
    @Query("SELECT * FROM categories WHERE categoryName LIKE '%' || :query || '%' ORDER BY displayOrder ASC")
    fun searchCategories(query: String): Flow<List<Category>>
    
    /**
     * Count total categories
     */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int
    
    /**
     * Count custom categories
     */
    @Query("SELECT COUNT(*) FROM categories WHERE isSystemCategory = 0")
    suspend fun countCustomCategories(): Int
    
    /**
     * Get category by emoji
     */
    @Query("SELECT * FROM categories WHERE iconEmoji = :emoji")
    suspend fun getCategoryByEmoji(emoji: String): Category?
    
    /**
     * Check if category exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE categoryId = :categoryId)")
    suspend fun categoryExists(categoryId: String): Boolean
    
    /**
     * Update category display order
     */
    @Query("UPDATE categories SET displayOrder = :order WHERE categoryId = :categoryId")
    suspend fun updateCategoryOrder(categoryId: String, order: Int)
    
    /**
     * Update category color
     */
    @Query("UPDATE categories SET colorHex = :colorHex WHERE categoryId = :categoryId")
    suspend fun updateCategoryColor(categoryId: String, colorHex: String)
    
    /**
     * Update category icon
     */
    @Query("UPDATE categories SET iconEmoji = :emoji WHERE categoryId = :categoryId")
    suspend fun updateCategoryIcon(categoryId: String, emoji: String)
    
    /**
     * Update category name and description
     */
    @Query("UPDATE categories SET categoryName = :name, description = :description WHERE categoryId = :categoryId")
    suspend fun updateCategoryInfo(
        categoryId: String,
        name: String,
        description: String
    )
    
    /**
     * Get maximum display order
     */
    @Query("SELECT MAX(displayOrder) FROM categories")
    suspend fun getMaxDisplayOrder(): Int?
    
    /**
     * Delete all custom categories
     */
    @Query("DELETE FROM categories WHERE isSystemCategory = 0")
    suspend fun deleteAllCustomCategories()
    
    /**
     * Reset all categories to defaults
     */
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
    
    /**
     * Get all category IDs
     */
    @Query("SELECT categoryId FROM categories")
    suspend fun getAllCategoryIds(): List<String>
}

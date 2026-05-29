package com.armutlu.apporganizer.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Data model representing a category for organizing apps.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val categoryId: String,
    
    val categoryName: String,
    
    val description: String = "",
    
    val colorHex: String = "#FF6200EE", // Material Purple
    
    val iconEmoji: String = "📁",
    
    val isSystemCategory: Boolean = true,
    
    val displayOrder: Int = 0,
    
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {
    
    companion object {
        // Default system categories
        const val CAT_SOCIAL = "social"
        const val CAT_PRODUCTIVITY = "productivity"
        const val CAT_GAMES = "games"
        const val CAT_SHOPPING = "shopping"
        const val CAT_NEWS = "news"
        const val CAT_HEALTH = "health"
        const val CAT_FINANCE = "finance"
        const val CAT_EDUCATION = "education"
        const val CAT_UTILITIES = "utilities"
        const val CAT_OTHER = "other"
        const val CAT_UNCATEGORIZED = "uncategorized"
        
        /**
         * Get default system categories
         */
        fun getDefaultCategories(): List<Category> {
            return listOf(
                Category(
                    categoryId = CAT_SOCIAL,
                    categoryName = "Sosyal Medya",
                    iconEmoji = "👥",
                    colorHex = "#FF1F77F2",
                    displayOrder = 1
                ),
                Category(
                    categoryId = CAT_PRODUCTIVITY,
                    categoryName = "Üretkenlik",
                    iconEmoji = "📝",
                    colorHex = "#FF4CAF50",
                    displayOrder = 2
                ),
                Category(
                    categoryId = CAT_GAMES,
                    categoryName = "Oyunlar",
                    iconEmoji = "🎮",
                    colorHex = "#FFFF6F00",
                    displayOrder = 3
                ),
                Category(
                    categoryId = CAT_SHOPPING,
                    categoryName = "Alışveriş",
                    iconEmoji = "🛍️",
                    colorHex = "#FFE91E63",
                    displayOrder = 4
                ),
                Category(
                    categoryId = CAT_NEWS,
                    categoryName = "Haber",
                    iconEmoji = "📰",
                    colorHex = "#FF2196F3",
                    displayOrder = 5
                ),
                Category(
                    categoryId = CAT_HEALTH,
                    categoryName = "Sağlık",
                    iconEmoji = "❤️",
                    colorHex = "#FFF44336",
                    displayOrder = 6
                ),
                Category(
                    categoryId = CAT_FINANCE,
                    categoryName = "Finans",
                    iconEmoji = "💰",
                    colorHex = "#FF009688",
                    displayOrder = 7
                ),
                Category(
                    categoryId = CAT_EDUCATION,
                    categoryName = "Eğitim",
                    iconEmoji = "🎓",
                    colorHex = "#FF673AB7",
                    displayOrder = 8
                ),
                Category(
                    categoryId = CAT_UTILITIES,
                    categoryName = "Araçlar",
                    iconEmoji = "🔧",
                    colorHex = "#FF00BCD4",
                    displayOrder = 9
                ),
                Category(
                    categoryId = CAT_OTHER,
                    categoryName = "Diğer",
                    iconEmoji = "📦",
                    colorHex = "#FF9C27B0",
                    displayOrder = 10
                ),
                Category(
                    categoryId = CAT_UNCATEGORIZED,
                    categoryName = "Kategorisiz",
                    iconEmoji = "❓",
                    colorHex = "#FF9E9E9E",
                    displayOrder = 0,
                    isSystemCategory = true
                )
            )
        }
        
        /**
         * Create sample categories for testing
         */
        fun createSamples(): List<Category> {
            return getDefaultCategories()
        }
    }
    
    /**
     * Get color as Android Color Int
     */
    fun getColorInt(): Int {
        return android.graphics.Color.parseColor(colorHex)
    }
}

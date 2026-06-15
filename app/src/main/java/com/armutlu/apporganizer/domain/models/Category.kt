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
        // Mevcut kategoriler (Google Play karsiliklari)
        const val CAT_SOCIAL = "social"               // Social
        const val CAT_PRODUCTIVITY = "productivity"   // Productivity
        const val CAT_GAMES = "games"                 // Games (18 alt kategori tek klasorde)
        const val CAT_SHOPPING = "shopping"           // Shopping
        const val CAT_NEWS = "news"                   // News & Magazines
        const val CAT_HEALTH = "health"               // Health & Fitness + Medical
        const val CAT_FINANCE = "finance"             // Finance
        const val CAT_EDUCATION = "education"         // Education
        const val CAT_UTILITIES = "utilities"         // Tools
        const val CAT_TRAVEL = "travel"               // Travel & Local
        const val CAT_ENTERTAINMENT = "entertainment" // Entertainment
        const val CAT_FOOD = "food"                   // Food & Drink
        const val CAT_PHOTOGRAPHY = "photography"     // Photography
        const val CAT_OTHER = "other"
        const val CAT_UNCATEGORIZED = "uncategorized"

        // Yeni kategoriler — Google Play eslesmeleri
        const val CAT_MUSIC = "music"                 // Music & Audio
        const val CAT_VIDEO = "video"                 // Video Players & Editors
        const val CAT_COMMUNICATION = "communication" // Communication
        const val CAT_MAPS = "maps"                   // Maps & Navigation
        const val CAT_SPORTS = "sports"               // Sports
        const val CAT_BOOKS = "books"                 // Books & Reference
        const val CAT_LIFESTYLE = "lifestyle"         // Lifestyle
        const val CAT_BUSINESS = "business"           // Business
        const val CAT_DATING = "dating"               // Dating
        const val CAT_ART = "art"                     // Art & Design
        const val CAT_BEAUTY = "beauty"               // Beauty
        const val CAT_AUTO = "auto"                   // Auto & Vehicles
        const val CAT_HOUSE = "house"                 // House & Home
        const val CAT_WEATHER = "weather"             // Weather
        const val CAT_PARENTING = "parenting"         // Parenting
        const val CAT_EVENTS = "events"               // Events
        const val CAT_COMICS = "comics"               // Comics
        const val CAT_PERSONALIZATION = "personalization" // Personalization

        // Üretici/marka kategorileri — manufacturerClassifyEnabled=true iken kullanılır
        const val CAT_GOOGLE = "google_apps"
        const val CAT_SAMSUNG = "samsung_apps"
        const val CAT_MICROSOFT = "microsoft_apps"
        const val CAT_XIAOMI = "xiaomi_apps"
        const val CAT_HUAWEI = "huawei_apps"
        const val CAT_META = "meta_apps"
        const val CAT_APPLE = "apple_apps"
        const val CAT_SPOTIFY = "spotify_apps"
        const val CAT_AMAZON = "amazon_apps"

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
                    categoryId = CAT_TRAVEL,
                    categoryName = "Seyahat",
                    iconEmoji = "✈️",
                    colorHex = "#FF03A9F4",
                    displayOrder = 10
                ),
                Category(
                    categoryId = CAT_ENTERTAINMENT,
                    categoryName = "Eğlence",
                    iconEmoji = "🎬",
                    colorHex = "#FFFF5722",
                    displayOrder = 11
                ),
                Category(
                    categoryId = CAT_FOOD,
                    categoryName = "Yemek",
                    iconEmoji = "🍔",
                    colorHex = "#FFFF9800",
                    displayOrder = 12
                ),
                Category(
                    categoryId = CAT_PHOTOGRAPHY,
                    categoryName = "Fotoğraf",
                    iconEmoji = "📸",
                    colorHex = "#FF9C27B0",
                    displayOrder = 13
                ),
                // Yeni Google Play kategorileri
                Category(
                    categoryId = CAT_MUSIC,
                    categoryName = "Müzik",
                    iconEmoji = "🎵",
                    colorHex = "#FF3F51B5",
                    displayOrder = 14
                ),
                Category(
                    categoryId = CAT_VIDEO,
                    categoryName = "Video",
                    iconEmoji = "🎥",
                    colorHex = "#FFE53935",
                    displayOrder = 15
                ),
                Category(
                    categoryId = CAT_COMMUNICATION,
                    categoryName = "İletişim",
                    iconEmoji = "💬",
                    colorHex = "#FF00ACC1",
                    displayOrder = 16
                ),
                Category(
                    categoryId = CAT_MAPS,
                    categoryName = "Harita & Navigasyon",
                    iconEmoji = "🗺️",
                    colorHex = "#FF43A047",
                    displayOrder = 17
                ),
                Category(
                    categoryId = CAT_SPORTS,
                    categoryName = "Spor",
                    iconEmoji = "⚽",
                    colorHex = "#FF1E88E5",
                    displayOrder = 18
                ),
                Category(
                    categoryId = CAT_BOOKS,
                    categoryName = "Kitap & Referans",
                    iconEmoji = "📚",
                    colorHex = "#FF8D6E63",
                    displayOrder = 19
                ),
                Category(
                    categoryId = CAT_LIFESTYLE,
                    categoryName = "Yaşam Tarzı",
                    iconEmoji = "🌿",
                    colorHex = "#FF66BB6A",
                    displayOrder = 20
                ),
                Category(
                    categoryId = CAT_BUSINESS,
                    categoryName = "İş",
                    iconEmoji = "💼",
                    colorHex = "#FF455A64",
                    displayOrder = 21
                ),
                Category(
                    categoryId = CAT_DATING,
                    categoryName = "Arkadaşlık",
                    iconEmoji = "❤️‍🔥",
                    colorHex = "#FFEC407A",
                    displayOrder = 22
                ),
                Category(
                    categoryId = CAT_ART,
                    categoryName = "Sanat & Tasarım",
                    iconEmoji = "🎨",
                    colorHex = "#FFAB47BC",
                    displayOrder = 23
                ),
                Category(
                    categoryId = CAT_BEAUTY,
                    categoryName = "Güzellik",
                    iconEmoji = "💄",
                    colorHex = "#FFF06292",
                    displayOrder = 24
                ),
                Category(
                    categoryId = CAT_AUTO,
                    categoryName = "Otomotiv",
                    iconEmoji = "🚗",
                    colorHex = "#FF78909C",
                    displayOrder = 25
                ),
                Category(
                    categoryId = CAT_HOUSE,
                    categoryName = "Ev & Yaşam",
                    iconEmoji = "🏠",
                    colorHex = "#FF8BC34A",
                    displayOrder = 26
                ),
                Category(
                    categoryId = CAT_WEATHER,
                    categoryName = "Hava Durumu",
                    iconEmoji = "⛅",
                    colorHex = "#FF29B6F6",
                    displayOrder = 27
                ),
                Category(
                    categoryId = CAT_PARENTING,
                    categoryName = "Ebeveynlik",
                    iconEmoji = "👶",
                    colorHex = "#FFFFB74D",
                    displayOrder = 28
                ),
                Category(
                    categoryId = CAT_EVENTS,
                    categoryName = "Etkinlikler",
                    iconEmoji = "🎟️",
                    colorHex = "#FFFF7043",
                    displayOrder = 29
                ),
                Category(
                    categoryId = CAT_COMICS,
                    categoryName = "Çizgi Roman",
                    iconEmoji = "💥",
                    colorHex = "#FFFFCA28",
                    displayOrder = 30
                ),
                Category(
                    categoryId = CAT_PERSONALIZATION,
                    categoryName = "Kişiselleştirme",
                    iconEmoji = "🎨",
                    colorHex = "#FF7E57C2",
                    displayOrder = 31
                ),
                Category(
                    categoryId = CAT_OTHER,
                    categoryName = "Diğer",
                    iconEmoji = "📦",
                    colorHex = "#FF607D8B",
                    displayOrder = 32
                ),
                // Üretici/marka kategorileri
                Category(
                    categoryId = CAT_GOOGLE,
                    categoryName = "Google",
                    iconEmoji = "🔵",
                    colorHex = "#FF4285F4",
                    displayOrder = 33
                ),
                Category(
                    categoryId = CAT_SAMSUNG,
                    categoryName = "Samsung",
                    iconEmoji = "📱",
                    colorHex = "#FF1428A0",
                    displayOrder = 34
                ),
                Category(
                    categoryId = CAT_MICROSOFT,
                    categoryName = "Microsoft",
                    iconEmoji = "🪟",
                    colorHex = "#FF00A4EF",
                    displayOrder = 35
                ),
                Category(
                    categoryId = CAT_XIAOMI,
                    categoryName = "Xiaomi",
                    iconEmoji = "📲",
                    colorHex = "#FFFF6900",
                    displayOrder = 36
                ),
                Category(
                    categoryId = CAT_HUAWEI,
                    categoryName = "Huawei",
                    iconEmoji = "📡",
                    colorHex = "#FFCF0A2C",
                    displayOrder = 37
                ),
                Category(
                    categoryId = CAT_META,
                    categoryName = "Meta",
                    iconEmoji = "🌐",
                    colorHex = "#FF0082FB",
                    displayOrder = 38
                ),
                Category(
                    categoryId = CAT_APPLE,
                    categoryName = "Apple",
                    iconEmoji = "🍎",
                    colorHex = "#FF555555",
                    displayOrder = 39
                ),
                Category(
                    categoryId = CAT_SPOTIFY,
                    categoryName = "Spotify",
                    iconEmoji = "🎧",
                    colorHex = "#FF1DB954",
                    displayOrder = 40
                ),
                Category(
                    categoryId = CAT_AMAZON,
                    categoryName = "Amazon",
                    iconEmoji = "📦",
                    colorHex = "#FFFF9900",
                    displayOrder = 41
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

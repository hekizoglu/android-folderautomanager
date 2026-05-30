package com.armutlu.apporganizer.domain.usecase

import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppClassifier @Inject constructor(
    private val appDatabaseService: AppDatabaseService
) {

    // Paket adına göre kesin kategori eşlemesi — keyword'den önce kontrol edilir
    private val exactMatchMap = mapOf(
        "com.facebook.katana"                       to Category.CAT_SOCIAL,
        "com.instagram.android"                     to Category.CAT_SOCIAL,
        "com.twitter.android"                       to Category.CAT_SOCIAL,
        "com.whatsapp"                              to Category.CAT_SOCIAL,
        "org.telegram.messenger"                    to Category.CAT_SOCIAL,
        "com.discord"                               to Category.CAT_SOCIAL,
        "com.snapchat.android"                      to Category.CAT_SOCIAL,
        "com.zhiliaoapp.musically"                  to Category.CAT_SOCIAL,
        "com.ss.android.ugc.trill"                  to Category.CAT_SOCIAL,
        // AI asistanlar → Üretkenlik
        "com.openai.chatgpt"                        to Category.CAT_PRODUCTIVITY,
        "com.openai.android"                        to Category.CAT_PRODUCTIVITY,
        "com.deepseek.app"                          to Category.CAT_PRODUCTIVITY,
        "ai.deepseek.app"                           to Category.CAT_PRODUCTIVITY,
        "com.anthropic.claude"                      to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.bard"              to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.gemini"            to Category.CAT_PRODUCTIVITY,
        "com.microsoft.copilot"                     to Category.CAT_PRODUCTIVITY,
        "com.microsoft.bing"                        to Category.CAT_PRODUCTIVITY,
        "com.perplexity.app"                        to Category.CAT_PRODUCTIVITY,
        "io.character.ai"                           to Category.CAT_PRODUCTIVITY,
        "com.inflection.pi"                         to Category.CAT_PRODUCTIVITY,
        // Oyunlar
        "com.valvesoftware.android.steam.steamlink" to Category.CAT_GAMES,
        "com.playstation.mobilegames"               to Category.CAT_GAMES,
    )

    /**
     * Classify a single app into a category
     */
    fun classifyApp(appInfo: AppInfo): String {
        // 1. Online veritabanı (Play Store kategorileri) — en yüksek öncelik
        appDatabaseService.getCategoryForPackage(appInfo.packageName)?.let { return it }
        // 2. Yerel exact match tablosu
        exactMatchMap[appInfo.packageName]?.let { return it }
        // 3. Keyword eşleşmesi
        return classifyByKeywords(appInfo.appName, appInfo.packageName) ?: Category.CAT_OTHER
    }
    
    /**
     * Classify multiple apps at once
     */
    fun classifyApps(apps: List<AppInfo>): Map<String, String> {
        return apps.associateBy(
            { it.packageName },
            { classifyApp(it) }
        )
    }
    
    /**
     * Get classification confidence (0-100)
     * 100 = high confidence, 0 = low confidence
     */
    fun getConfidence(appInfo: AppInfo, categoryId: String): Int {
        return when {
            categoryId == Category.CAT_OTHER -> 30
            hasExactMatch(appInfo.packageName, categoryId) -> 95
            hasKeywordMatch(appInfo.appName, categoryId) -> 80
            hasPackageKeywordMatch(appInfo.packageName, categoryId) -> 70
            else -> 50
        }
    }
    
    /**
     * Main classification method using keywords
     */
    private fun classifyByKeywords(appName: String, packageName: String): String? {
        val lowerAppName = appName.lowercase()
        val lowerPackage = packageName.lowercase()
        
        // Check against keyword database
        val database = KeywordDatabase.getKeywordMap()
        
        database.forEach { (category, keywords) ->
            keywords.forEach { keyword ->
                if (lowerAppName.contains(keyword) || lowerPackage.contains(keyword)) {
                    return category
                }
            }
        }
        
        return null // Will default to CAT_OTHER
    }
    
    /**
     * Check if there's an exact match in package name
     */
    private fun hasExactMatch(packageName: String, categoryId: String): Boolean {
        return exactMatchMap[packageName] == categoryId
    }
    
    /**
     * Check if app name contains category keywords
     */
    private fun hasKeywordMatch(appName: String, categoryId: String): Boolean {
        return KeywordDatabase.getKeywordMap()[categoryId]?.any { keyword ->
            appName.lowercase().contains(keyword)
        } ?: false
    }
    
    /**
     * Check if package name contains category keywords
     */
    private fun hasPackageKeywordMatch(packageName: String, categoryId: String): Boolean {
        return KeywordDatabase.getKeywordMap()[categoryId]?.any { keyword ->
            packageName.lowercase().contains(keyword)
        } ?: false
    }
}

/**
 * Keyword database for app classification.
 * Maps category IDs to lists of keywords that identify that category.
 */
object KeywordDatabase {
    
    private val keywordMap = mapOf(
        Category.CAT_SOCIAL to listOf(
            "social", "facebook", "twitter", "instagram", "whatsapp", "telegram",
            "tiktok", "snapchat", "discord", "messenger", "viber", "linkedin",
            "reddit", "quora", "mastodon", "bluesky", "threads", "tumblr",
            "wechat", "line", "kakaotalk", "signal"
        ),

        Category.CAT_PRODUCTIVITY to listOf(
            "productivity", "office", "calendar", "notes", "todo", "task",
            "mail", "email", "drive", "cloud", "storage", "document",
            "sheet", "excel", "word", "presentation", "notion", "obsidian",
            "evernote", "onenote", "todoist", "asana", "trello", "slack",
            "teams", "zoom", "meet", "google", "microsoft", "amazon",
            // AI asistanlar
            "chatgpt", "openai", "deepseek", "claude", "gemini", "copilot",
            "perplexity", "bard", "gpt", "llm", "assistant", "ai",
            "character.ai", "inflection", "mistral", "groq"
        ),
        
        Category.CAT_GAMES to listOf(
            "game", "games", "gaming", "play", "battle", "royal", "chess",
            "candy", "clash", "strike", "legends", "mobile", "puzzle",
            "racing", "shooting", "action", "adventure", "rpg", "mmo",
            "fortnite", "minecraft", "roblox", "steam", "epic"
        ),
        
        Category.CAT_SHOPPING to listOf(
            "shop", "shopping", "store", "market", "buy", "sell", "cart",
            "payment", "checkout", "price", "discount", "amazon", "ebay",
            "aliexpress", "trendyol", "hepsiburada", "n11", "sahibinden",
            "letgo", "mercari", "walmart", "target", "costco", "alibaba"
        ),
        
        Category.CAT_NEWS to listOf(
            "news", "newspaper", "article", "press", "tribune", "gazette",
            "daily", "breaking", "headline", "media", "journalist", "reader",
            "rss", "feed", "bbc", "cnn", "reuters", "bloomberg", "anadolu",
            "dha", "ntvmsnbc", "habertürk", "milliyet", "hürriyet"
        ),
        
        Category.CAT_HEALTH to listOf(
            "health", "fitness", "workout", "gym", "exercise", "sport",
            "medical", "doctor", "hospital", "clinic", "medicine", "pharma",
            "wellness", "yoga", "diet", "nutrition", "calorie", "step",
            "heart", "run", "walk", "cycle", "bike", "swim"
        ),
        
        Category.CAT_FINANCE to listOf(
            "finance", "bank", "payment", "money", "invest", "stock",
            "crypto", "bitcoin", "wallet", "card", "credit", "loan",
            "tax", "accounting", "trading", "forex", "commodity", "bitcoin",
            "ethereum", "ripple", "trading", "thinkorswim"
        ),
        
        Category.CAT_EDUCATION to listOf(
            "education", "learn", "course", "class", "school", "university",
            "exam", "test", "quiz", "study", "lesson", "tutorial",
            "udemy", "coursera", "skillshare", "duolingo", "babbel",
            "memrise", "brilliant", "codecademy", "edx", "khan"
        ),
        
        Category.CAT_UTILITIES to listOf(
            "utility", "tools", "tool", "manager", "cleaner", "antivirus",
            "security", "lock", "safe", "backup", "restore", "file",
            "explorer", "download", "torrent", "vpn", "proxy", "browser",
            "keyboard", "launcher", "theme", "widget", "widget"
        ),
        
        Category.CAT_OTHER to listOf()
    )
    
    fun getKeywordMap(): Map<String, List<String>> = keywordMap
    
    /**
     * Get keywords for a specific category
     */
    fun getKeywords(categoryId: String): List<String> {
        return keywordMap[categoryId] ?: emptyList()
    }
    
    /**
     * Get total keyword count
     */
    fun getTotalKeywords(): Int {
        return keywordMap.values.sumOf { it.size }
    }
    
    /**
     * Add custom keyword to a category
     */
    fun addKeywordToCategory(categoryId: String, keyword: String) {
        val currentKeywords = keywordMap[categoryId] ?: emptyList()
        if (!currentKeywords.contains(keyword)) {
            (keywordMap as MutableMap)[categoryId] = currentKeywords + keyword
        }
    }
}

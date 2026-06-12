package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.Category

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
            "letgo", "mercari", "walmart", "target", "costco", "alibaba",
            "ciceksepeti", "cicek", "dolap", "gittigidiyor", "pttavm",
            "teknosa", "mediamarkt", "vatanbilgisayar", "koton", "defacto",
            "lcwaikiki", "boyner", "modanisa", "mavi", "alışveriş"
        ),
        Category.CAT_NEWS to listOf(
            "news", "newspaper", "article", "press", "tribune", "gazette",
            "daily", "breaking", "headline", "media", "journalist", "reader",
            "rss", "feed", "bbc", "cnn", "reuters", "bloomberg", "anadolu",
            "dha", "ntvmsnbc", "haberturk", "habertürk", "milliyet", "hurriyet"
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
            "tax", "accounting", "trading", "forex", "commodity",
            "ethereum", "ripple", "thinkorswim"
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
            "keyboard", "launcher", "theme", "widget"
        ),
        Category.CAT_TRAVEL to listOf(
            "travel", "trip", "flight", "hotel", "booking", "airbnb", "hostel",
            "vacation", "holiday", "tour", "map", "navigation", "gps", "route",
            "taxi", "uber", "lyft", "bus", "train", "subway", "airline",
            "airport", "passport", "visa", "seyahat", "ucus", "otel",
            "flightradar", "tripadvisor", "expedia", "skyscanner", "kayak",
            "bitaksi", "obilet", "enuygun", "pegasus", "thy", "tcdd",
            "istanbulkart", "kentkart", "marti", "scooter", "bisiklet"
        ),
        Category.CAT_ENTERTAINMENT to listOf(
            "entertainment", "movie", "film", "video", "stream", "watch",
            "netflix", "youtube", "twitch", "disney", "hulu", "prime",
            "music", "spotify", "podcast", "radio", "audio", "sound",
            "tv", "series", "show", "cinema", "theater", "concert",
            "tiktok", "reels", "shorts", "anime", "manga", "webtoon",
            "eglence", "dizi", "muzik",
            "blutv", "exxen", "puhutv", "tabii", "gain", "trt",
            "dsmart", "digiturk", "fizy", "muud"
        ),
        Category.CAT_FOOD to listOf(
            "food", "restaurant", "delivery", "eat", "meal", "recipe",
            "cook", "kitchen", "yemek", "siparis", "pizza", "burger",
            "cafe", "coffee", "grocery", "market",
            "getir", "yemeksepeti", "migros", "a101", "bim", "carrefour",
            "trendyolexpress", "express", "mutfak", "tarif"
        ),
        Category.CAT_PHOTOGRAPHY to listOf(
            "photo", "camera", "picture", "image", "gallery", "edit",
            "filter", "selfie", "video", "record", "capture", "lens",
            "lightroom", "photoshop", "canva", "vsco", "snapseed"
        ),
        Category.CAT_OTHER to listOf()
    )

    fun getKeywordMap(): Map<String, List<String>> = keywordMap

    fun getKeywords(categoryId: String): List<String> = keywordMap[categoryId] ?: emptyList()

    fun getTotalKeywords(): Int = keywordMap.values.sumOf { it.size }

    fun addKeywordToCategory(categoryId: String, keyword: String) {
        val current = keywordMap[categoryId] ?: emptyList()
        if (!current.contains(keyword)) {
            (keywordMap as MutableMap)[categoryId] = current + keyword
        }
    }
}

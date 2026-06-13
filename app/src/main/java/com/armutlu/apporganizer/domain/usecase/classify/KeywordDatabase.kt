package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.Category

object KeywordDatabase {

    private val keywordMap = mapOf(
        Category.CAT_SOCIAL to listOf(
            "social", "facebook", "twitter", "instagram", "whatsapp", "telegram",
            "tiktok", "snapchat", "discord", "messenger", "viber", "linkedin",
            "reddit", "quora", "mastodon", "bluesky", "threads", "tumblr",
            "wechat", "line", "kakaotalk", "signal", "vk", "odnoklassniki",
            "badoo", "tinder", "bumble", "hinge", "grindr", "skout", "meetup",
            "pinterest", "flickr", "behance", "deviantart", "weibo", "naver",
            "zalo", "imo", "textme", "tantan", "tagged", "hi5", "myspace",
            "chat", "sohbet", "arkadas", "sosyal", "topluluk", "bip"
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
            "fortnite", "minecraft", "roblox", "steam", "epic",
            "supercell", "gameloft", "rockstar", "habby", "voodoo",
            "madfingergames", "pixonic", "noodlecake", "fingersoft",
            "township", "archero", "hillclimb", "subway", "helix",
            "blizzard", "diablo", "warcraft", "overwatch",
            "brawl", "squad", "arena", "dungeon", "quest", "hero", "war",
            "zombie", "sniper", "fighter", "empire", "kingdom", "dragon",
            "oyun", "oyunu"
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
            "dha", "ntvmsnbc", "haberturk", "habertürk", "milliyet", "hurriyet",
            "foxnews", "nbcnews", "abcnews", "cbsnews", "npr", "huffpost",
            "politico", "axios", "vox", "guardian", "independent", "times",
            "washingtonpost", "wsj", "financialtimes", "telegraph", "economist",
            "techcrunch", "verge", "wired", "engadget", "cnet", "arstechnica",
            "haber", "manset", "son dakika", "gazetesi", "gazete", "dergi",
            "bianet", "diken", "t24", "cumhuriyet", "sozcu", "posta", "sabah",
            "haberler", "basın", "medya", "ajans"
        ),
        Category.CAT_HEALTH to listOf(
            "health", "fitness", "workout", "gym", "exercise", "sport",
            "medical", "doctor", "hospital", "clinic", "medicine", "pharma",
            "wellness", "yoga", "diet", "nutrition", "calorie", "step",
            "heart", "run", "walk", "cycle", "bike", "swim",
            "strava", "garmin", "fitbit", "nike", "adidas", "peloton",
            "calm", "headspace", "meditation", "sleep", "mindfulness",
            "myfitnesspal", "loseit", "noom", "weight", "bmi",
            "mental", "therapy", "dentist", "period", "pregnancy", "fertility",
            "blood", "glucose", "pressure", "oximeter",
            "saglik", "spor", "antrenman", "kosu", "doktor", "hastane",
            "eczane", "ilac", "randevu", "sigorta", "sağlık", "egzersiz"
        ),
        Category.CAT_FINANCE to listOf(
            "finance", "bank", "payment", "money", "invest", "stock",
            "crypto", "bitcoin", "wallet", "card", "credit", "loan",
            "tax", "accounting", "trading", "forex", "commodity",
            "ethereum", "ripple", "thinkorswim", "paypal", "venmo",
            "cashapp", "zelle", "revolut", "n26", "monzo", "wise",
            "transferwise", "coinbase", "binance", "kraken", "robinhood",
            "bybit", "okex", "kucoin", "etoro", "webull",
            "insurance", "sigorta", "pension", "emeklilik",
            "debit", "transfer", "remittance", "exchange",
            "banka", "finans", "odeme", "yatirim", "borsa", "kripto", "mobil",
            "cebi", "cüzdan", "fatura", "kart", "kredi", "borç", "faiz",
            "dolar", "euro", "altin", "doviz", "param", "papara", "enpara",
            "garanti", "isbank", "akbank", "vakifbank", "ziraat",
            "halkbank", "denizbank", "fibabanka", "ykb", "finansbank"
        ),
        Category.CAT_EDUCATION to listOf(
            "education", "learn", "course", "class", "school", "university",
            "exam", "test", "quiz", "study", "lesson", "tutorial",
            "udemy", "coursera", "skillshare", "duolingo", "babbel",
            "memrise", "brilliant", "codecademy", "edx", "khan",
            "rosetta", "busuu", "quizlet", "ted", "encyclopedia",
            "dictionary", "thesaurus", "language", "grammar", "math",
            "egitim", "okul", "universite", "sinav", "odev", "ders",
            "ogren", "kurs", "sertifika", "yds", "tyt", "ayt", "yks",
            "meb", "dershane", "hazirlik", "lgs", "kpss", "ales"
        ),
        Category.CAT_UTILITIES to listOf(
            "utility", "tools", "tool", "manager", "cleaner", "antivirus",
            "security", "lock", "safe", "backup", "restore", "file",
            "explorer", "download", "torrent", "vpn", "proxy", "browser",
            "keyboard", "launcher", "theme", "widget",
            "scanner", "barcode", "qr", "translate", "clock", "alarm",
            "calculator", "flashlight", "compass", "weather", "battery",
            "booster", "optimizer", "wifi", "bluetooth", "nfc", "airdrop",
            "password", "authenticator", "2fa", "remote", "cast", "screen",
            "araç", "hesap", "çeviri", "tarayıcı", "dosya", "yönetici",
            "temizle", "hız", "pil", "fener", "pusula", "hava"
        ),
        Category.CAT_TRAVEL to listOf(
            "travel", "trip", "flight", "hotel", "booking", "airbnb", "hostel",
            "vacation", "holiday", "tour", "map", "navigation", "gps", "route",
            "taxi", "uber", "lyft", "bus", "train", "subway", "airline",
            "airport", "passport", "visa", "seyahat", "ucus", "otel",
            "flightradar", "tripadvisor", "expedia", "skyscanner", "kayak",
            "bitaksi", "obilet", "enuygun", "pegasus", "thy", "tcdd",
            "istanbulkart", "kentkart", "marti", "scooter", "bisiklet",
            "kart", "ulasim", "metro", "tramvay", "vapur", "feribot",
            "rent", "kiralama", "araba", "araç", "transfer", "shuttle",
            "iett", "eshot", "ego", "izulas", "buski", "belbim"
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
            "trendyolexpress", "express", "mutfak", "tarif",
            "paket", "manav", "kasap", "pastane",
            "kafe", "restoran", "lokanta", "fast", "döner", "kebap",
            "sok", "kiler", "macro", "tekel", "icecek",
            "smoothie", "vegan", "organik", "gluten", "diyet",
            "doordash", "ubereats", "grubhub", "postmates", "instacart",
            "hellofresh", "gopuff", "opentable", "resy", "seamless",
            "zomato", "swiggy", "blinkit", "zepto", "bigbasket",
            "talabat", "deliveroo", "rappi", "pedidosya",
            "supermarket", "supermarkt", "hypermarket", "hypermarkt",
            "mealplan", "nutrition", "calorie", "macro", "diet",
            "catering", "takeaway", "takeout", "order", "sipariş"
        ),
        Category.CAT_PHOTOGRAPHY to listOf(
            "photo", "camera", "picture", "image", "gallery", "edit",
            "filter", "selfie", "video", "record", "capture", "lens",
            "lightroom", "photoshop", "canva", "vsco", "snapseed",
            "facetune", "retouch", "beautify", "portrait", "collage",
            "sticker", "gif", "reel", "clip", "trim", "crop",
            "foto", "kamera", "resim", "fotograf",
            "beauty", "b612", "snow", "meitu", "cymera", "retrica",
            "fotor", "pixlr", "picsart", "afterlight", "prequel",
            "efectum", "capcut", "filmora", "kinemaster", "inshot",
            "remove.bg", "background", "eraser", "cutout", "ai photo",
            "photoroom", "canva", "designer", "adobe"
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

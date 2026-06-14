package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.Category

object KeywordDatabase {

    // Loop 77 fix: mapOf() duplicate key bug — her kategori tek listede birlestirildi.
    // Onceki yapisinda CAT_TRAVEL, CAT_SHOPPING, CAT_FINANCE, CAT_HEALTH, CAT_UTILITIES
    // iki kez tanimliydi; Kotlin'de son tanim kazanir, ilk (daha kapsamli) liste kayboluyordu.
    private val keywordMap = mapOf(
        Category.CAT_SOCIAL to listOf(
            "social", "facebook", "twitter", "instagram", "whatsapp", "telegram",
            "tiktok", "snapchat", "discord", "messenger", "viber", "linkedin",
            "reddit", "quora", "mastodon", "bluesky", "threads", "tumblr",
            "wechat", "line", "kakaotalk", "signal", "vk", "odnoklassniki",
            "badoo", "tinder", "bumble", "hinge", "grindr", "skout", "meetup",
            "pinterest", "flickr", "behance", "deviantart", "weibo", "naver",
            "zalo", "imo", "textme", "tantan", "tagged", "hi5", "myspace",
            "chat", "sohbet", "arkadas", "sosyal", "topluluk", "bip",
            // Loop 77 — India social
            "sharechat", "roposo", "mojapp", "mxtakatak", "koo", "josh"
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
            // Genel
            "shop", "shopping", "store", "market", "buy", "sell", "cart",
            "payment", "checkout", "price", "discount", "ebay",
            "aliexpress", "etsy", "wish", "shein", "shopee", "lazada",
            "wildberries", "ozon", "jd", "pinduoduo", "walmart", "target",
            "bestbuy", "ikea", "zara", "hm", "gap", "nike", "adidas", "puma",
            "farfetch", "zalando", "vinted", "depop", "poshmark", "thredup",
            "amazon", "ebay",
            // TR
            "trendyol", "hepsiburada", "n11", "sahibinden", "letgo", "mercari",
            "ciceksepeti", "cicek", "dolap", "gittigidiyor", "pttavm",
            "teknosa", "mediamarkt", "vatanbilgisayar", "koton", "defacto",
            "lcwaikiki", "boyner", "modanisa", "mavi", "alisveris",
            "migros", "bim", "a101", "sok", "carrefour", "metro", "koctas",
            "vatan", "ikinciyeni", "arabam", "emlakjet", "zingat",
            "hepsiemlak", "ipekyol",
            // Loop 77 — India e-commerce
            "flipkart", "meesho", "myntra", "nykaa", "snapdeal", "jiomart",
            "tatacliq", "udaan",
            // Loop 77 — Middle East
            "noon", "namshi", "opensooq", "haraj", "dubizzle", "souq",
            // Loop 77 — SEA
            "shopback", "blibli", "kilimall", "jumia"
        ),
        Category.CAT_NEWS to listOf(
            "news", "newspaper", "article", "press", "tribune", "gazette",
            "daily", "breaking", "headline", "media", "journalist", "reader",
            "rss", "feed", "bbc", "cnn", "reuters", "bloomberg", "anadolu",
            "dha", "ntvmsnbc", "haberturk", "milliyet", "hurriyet",
            "foxnews", "nbcnews", "abcnews", "cbsnews", "npr", "huffpost",
            "politico", "axios", "vox", "guardian", "independent", "times",
            "washingtonpost", "wsj", "financialtimes", "telegraph", "economist",
            "techcrunch", "verge", "wired", "engadget", "cnet", "arstechnica",
            "haber", "manset", "son dakika", "gazetesi", "gazete", "dergi",
            "bianet", "diken", "t24", "cumhuriyet", "sozcu", "posta", "sabah",
            "haberler", "basin", "medya", "ajans", "fanatik", "ntvspor",
            "spiegel", "buzzfeed", "vice", "trt", "ahaber", "ensonhaber",
            "lemonde", "lefigaro", "elpais", "corriere", "repubblica"
        ),
        Category.CAT_HEALTH to listOf(
            // Genel
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
            "eczane", "ilac", "randevu", "sigorta", "saglik", "egzersiz",
            // Loop 40 additions (birlestirildi)
            "whoop", "oura", "polar", "suunto", "coros", "wahoo", "zwift",
            "trainingpeaks", "freeletics", "sworkit", "loseit",
            "cronometer", "lifesum", "yazio", "weightwatchers",
            "mysugr", "dexcom", "abbott", "omada", "livongo",
            "clue", "flo", "glow", "cycle",
            "acibademsaglik", "medikal"
        ),
        Category.CAT_FINANCE to listOf(
            // Genel
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
            "cebi", "cuzdan", "fatura", "kart", "kredi", "faiz",
            "dolar", "euro", "altin", "doviz", "param", "papara", "enpara",
            "garanti", "isbank", "akbank", "vakifbank", "ziraat",
            "halkbank", "denizbank", "fibabanka", "ykb", "finansbank",
            // Loop 40 additions (birlestirildi)
            "sofi", "ally", "marcus", "stripe", "tdameritrade", "schwab", "fidelity",
            "wealthfront", "acorns", "stash", "metamask", "trustwallet", "moonpay", "exodus",
            "mint", "ynab", "personalcapital", "quicken", "nubank", "mercadopago",
            "aktifbank", "ingbank", "hsbc", "qnbfinansbank",
            "sekerbank", "albaraka", "kuveytturk", "vakifkatilim", "ziraatkatilim",
            "bkmexpress", "fastpay", "ininal", "paycell", "masterpass",
            "midas", "gedik",
            // Loop 77 — India fintech
            "zerodha", "groww", "cred", "hdfc", "icici", "kotak", "yono",
            "upstox", "angelone", "angelbroking", "smallcase", "iifl",
            "jazzcash", "bkash", "easypaisa",
            // Loop 77 — Africa fintech
            "mpesa", "safaricom", "opay", "palmpay", "kuda", "flutterwave",
            "paystack", "tala", "sendwave", "chipper", "gtbank",
            // Loop 77 — SEA fintech
            "gopay", "linkaja", "truemoney", "shopeepay", "dana", "ovo",
            // Loop 77 — MENA fintech
            "tamara", "tabby",
            // Loop 77 — LATAM
            "picpay", "banco inter", "clip"
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
            // Genel
            "utility", "tools", "tool", "manager", "cleaner", "antivirus",
            "security", "lock", "safe", "backup", "restore", "file",
            "explorer", "download", "torrent", "vpn", "proxy", "browser",
            "keyboard", "launcher", "theme", "widget",
            "scanner", "barcode", "qr", "translate", "clock", "alarm",
            "calculator", "flashlight", "compass", "weather", "battery",
            "booster", "optimizer", "wifi", "bluetooth", "nfc", "airdrop",
            "password", "authenticator", "2fa", "remote", "cast", "screen",
            "arac", "hesap", "ceviri", "tarayici", "dosya", "yonetici",
            "temizle", "hiz", "pil", "fener", "pusula", "hava",
            // Loop 40 additions (birlestirildi)
            "authenticator", "authy", "otp", "lastpass", "dashlane",
            "bitwarden", "1password", "nordvpn", "expressvpn", "purevpn",
            "surfshark", "privateinternetaccess", "protonvpn", "mullvad",
            "translate", "deepl", "translator", "alexa", "siri", "assistant",
            "chatgpt", "claude", "bard", "copilot", "perplexity", "mistral",
            "cleanmaster", "avast", "bitdefender", "kaspersky", "norton",
            "malwarebytes", "filemanager", "esfile", "totalcommander",
            // Loop 77 — India/Africa gov utilities
            "maadhaar", "digilocker", "mygov", "umang",
            "safaricom", "mtn", "airtel", "stc", "zain"
        ),
        Category.CAT_TRAVEL to listOf(
            // Genel
            "travel", "trip", "flight", "hotel", "booking", "airbnb", "hostel",
            "vacation", "holiday", "tour", "map", "navigation", "gps", "route",
            "taxi", "uber", "lyft", "bus", "train", "subway", "airline",
            "airport", "passport", "visa", "seyahat", "ucus", "otel",
            "flightradar", "tripadvisor", "expedia", "skyscanner", "kayak",
            "bitaksi", "obilet", "enuygun", "pegasus", "thy", "tcdd",
            "istanbulkart", "kentkart", "marti", "scooter", "bisiklet",
            "ulasim", "metro", "tramvay", "vapur", "feribot",
            "rent", "kiralama", "araba", "arac", "transfer", "shuttle",
            "iett", "eshot", "ego", "izulas", "buski", "belbim",
            // Loop 40 additions (birlestirildi)
            "bolt", "cabify", "gett", "curb", "ride",
            "cheapflights", "rome2rio", "citymapper", "moovit", "transit",
            "waze", "maps", "tren", "otobus", "bilet", "ucak",
            "thyajans", "sunexpress", "anadolujet", "ets", "tatil",
            "flightaware", "rental", "hertz", "enterprise", "biletix",
            // Loop 77 — India travel
            "irctc", "redbus", "goibibo", "cleartrip", "ixigo", "rapido",
            "easemytrip", "yatra", "makemytrip",
            // Loop 77 — SEA travel
            "traveloka", "tiket",
            // Loop 77 — MENA travel
            "wego", "careem",
            // Loop 77 — Africa/Global
            "bykea", "indriver", "yandex taxi"
        ),
        Category.CAT_ENTERTAINMENT to listOf(
            "entertainment", "movie", "film", "video", "stream", "watch",
            "netflix", "youtube", "twitch", "disney", "hulu", "prime",
            "music", "spotify", "podcast", "radio", "audio", "sound",
            "tv", "series", "show", "cinema", "theater", "concert",
            "tiktok", "reels", "shorts", "anime", "manga", "webtoon",
            "eglence", "dizi", "muzik",
            "blutv", "exxen", "puhutv", "tabii", "gain", "trt",
            "dsmart", "digiturk", "fizy", "muud",
            // Loop 77 — India entertainment
            "kuku", "pratilipi", "moj", "takatak", "roposo", "mx player"
        ),
        Category.CAT_FOOD to listOf(
            "food", "restaurant", "delivery", "eat", "meal", "recipe",
            "cook", "kitchen", "yemek", "siparis", "pizza", "burger",
            "cafe", "coffee", "grocery", "market",
            "getir", "yemeksepeti", "migros", "a101", "bim", "carrefour",
            "trendyolexpress", "express", "mutfak", "tarif",
            "paket", "manav", "kasap", "pastane",
            "kafe", "restoran", "lokanta", "fast", "doner", "kebap",
            "wolt", "grubhub", "doordash", "ubereats", "justeat", "ifood",
            "tacobell", "chipotle", "wendys", "dunkin", "chick", "dominos",
            "hellofresh", "blueapron", "yummly", "allrecipes", "cookpad",
            "sok", "kiler", "macro", "tekel", "icecek",
            "smoothie", "vegan", "organik", "gluten", "diyet",
            "gopuff", "opentable", "resy", "seamless",
            "zomato", "swiggy", "blinkit", "zepto", "bigbasket",
            "talabat", "deliveroo", "rappi", "pedidosya",
            "supermarket", "takeaway", "takeout", "order", "siparis",
            // Loop 77 — India food
            "dunzo", "fassos", "rebel foods"
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
            "photoroom", "designer", "adobe"
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

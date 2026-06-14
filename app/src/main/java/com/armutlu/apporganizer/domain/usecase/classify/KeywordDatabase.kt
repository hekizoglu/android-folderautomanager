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
            "sharechat", "roposo", "mojapp", "mxtakatak", "koo", "josh",
            // Loop 84 — yeni nesil platformlar
            "lemon8", "bluesky", "yubo", "gas", "poparazzi", "spill", "post"
        ),
        Category.CAT_PRODUCTIVITY to listOf(
            "productivity", "office", "calendar", "notes", "todo", "task",
            "mail", "email", "drive", "cloud", "storage", "document",
            "sheet", "excel", "word", "presentation", "notion", "obsidian",
            "evernote", "onenote", "todoist", "asana", "trello", "slack",
            "teams", "zoom", "meet", "google", "microsoft", "amazon",
            "chatgpt", "openai", "deepseek", "claude", "gemini", "copilot",
            "perplexity", "bard", "gpt", "llm", "assistant", "ai",
            "character.ai", "inflection", "mistral", "groq",
            // Loop 84 — PKM / bilgi yonetimi
            "anytype", "appflowy", "workflowy", "dynalist", "milanote", "mindmeister",
            "roam", "logseq", "obsidian", "foam", "pkm", "zettelkasten", "taskade"
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
            "oyun", "oyunu",
            // Loop 84 — gacha / anime RPG
            "wuthering", "nikke", "punishing", "reverse1999", "aether", "gazer",
            "maplestory", "dragonraja", "summoners", "genshin", "honkai", "zenless"
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
            "lemonde", "lefigaro", "elpais", "corriere", "repubblica",
            // Loop 84 — TR dijital medya
            "onedio", "webtekno", "donanimhaber", "haberler", "lidya", "teknoblog",
            "webrazzi", "chip", "pcworld", "shiftdelete", "log", "bilisim"
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
            "acibademsaglik", "medikal",
            // Loop 84 — mental wellness
            "youper", "brightside", "sevencups", "7cups", "daylio", "moodfit",
            "moodistory", "happify", "sanvello", "reflectly", "finch", "woebot"
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
            "picpay", "banco inter", "clip",
            // Loop 84 — Web3 / kripto cuzdanlar
            "rainbow", "phantom", "solflare", "solana", "magic eden", "nft", "defi",
            "metamask", "trustwallet", "ledger", "trezor", "uniswap", "aave",
            "compound", "opensea", "blur", "gem wallet", "argent", "zerion"
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
            "safaricom", "mtn", "airtel", "stc", "zain",
            // Loop 84 — guc kullanici araclari
            "aurora", "shizuku", "netguard", "blokada", "tasker", "macrodroid",
            "kdeconnect", "syncthing", "pushbullet", "flud", "libretorrent",
            "automate", "foldersync", "ladb", "termux", "supersu", "kingoroot",
            "root", "adb", "wireless", "automation", "macro", "trigger"
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
        // ─── YENİ KATEGORİLER (Loop 90 — Google Play eslesmesi) ─────────────────
        Category.CAT_COMMUNICATION to listOf(
            "whatsapp", "telegram", "signal", "viber", "skype", "messenger",
            "call", "voice", "sms", "text", "chat", "mail", "email", "inbox",
            "zoom", "meet", "webex", "jitsi", "gotomeeting", "teams", "slack",
            "discord", "hangouts", "imo", "textme", "textplus", "textnow",
            "vonage", "avaya", "ringcentral", "twilio", "8x8", "whereby",
            "hushed", "openphone", "grasshopper", "talkatone", "nextcloud.talk",
            "zoho.cliq", "flock", "revolt", "wire", "olvid",
            "tutanota", "fastmail", "protonmail", "yahoo.mail", "outlook",
            "spark.mail", "airmail", "edison.mail", "spike.email", "superhuman",
            "bip", "turkcell", "iletisim", "mesaj", "arama", "gorusme"
        ),
        Category.CAT_MUSIC to listOf(
            "spotify", "deezer", "tidal", "apple.music", "youtube.music",
            "soundcloud", "bandcamp", "napster", "qobuz", "amazon.music",
            "pandora", "iheart", "tunein", "podcast", "music", "audio",
            "kuku.fm", "gaana", "jiosaavn", "wynk", "resso", "anghami",
            "boomplay", "yandex.music", "shazam", "musixmatch", "genius",
            "soundhound", "beats", "audiomack", "livexlive", "radyo", "fm",
            "fl.studio", "bandlab", "voloco", "groovepad", "cross.dj",
            "djay", "edjing", "virtual.dj", "simply.piano", "flowkey",
            "yousician", "smule", "fender", "guitar", "piano", "muzik",
            "radyo7", "kral.fm", "powerapp.fm", "enerji.fm", "muud", "fizy"
        ),
        Category.CAT_VIDEO to listOf(
            "vlc", "mx.player", "kodi", "plex", "bsplayer", "nplayer",
            "video", "player", "stream", "watch", "netflix", "youtube",
            "capcut", "kinemaster", "inshot", "filmorago", "vn.video",
            "premiere.rush", "lomotif", "vivavideo", "splice", "magisto",
            "unfold", "prequel", "videoleap", "actiondirector", "powerdirector",
            "filmmaker", "recut", "vizmato", "quik", "gopro", "edit"
        ),
        Category.CAT_MAPS to listOf(
            "maps", "navigation", "navigate", "gps", "route", "direction",
            "waze", "here.maps", "osmand", "komoot", "alltrails", "citymapper",
            "transit.app", "moovit", "rome2rio", "omio", "trainline", "wanderu",
            "sygic", "tomtom", "copilot.gps", "navmii", "maps.me",
            "openstreetmap", "organic.maps", "radar", "compass",
            "iett", "ankarakart", "izulas", "bursaulasimlari",
            "mapy.cz", "geocaching", "harita", "yol", "rota", "navigasyon"
        ),
        Category.CAT_SPORTS to listOf(
            "sport", "football", "soccer", "basketball", "baseball", "tennis",
            "golf", "cricket", "rugby", "nfl", "nba", "mlb", "nhl", "espn",
            "score", "athlete", "league", "team", "match", "fixture",
            "onesoccer", "laliga", "bundesliga", "ligue1", "premierleague",
            "formula1", "f1", "ufc", "mma", "fighting", "wrestling",
            "cycling", "triathlon", "skiing", "snowboard", "surfing",
            "dazn", "bein", "sky.sports", "nbc.sports", "fox.sports",
            "fanatik", "ntvspor", "trtspor", "futbol", "basketbol",
            "spor", "lig", "mac", "gol", "takim", "nesine", "bilyoner"
        ),
        Category.CAT_BOOKS to listOf(
            "book", "ebook", "read", "kindle", "epub", "library", "literature",
            "novel", "story", "audiobook", "reader", "tome", "page",
            "goodreads", "kobo", "libby", "bookmate", "pocket", "readwise",
            "scribd", "overdrive", "moonreader", "cool.reader", "aldiko",
            "fbreader", "wattpad", "radish", "tapas", "webtoon",
            "comixology", "marvel.unlimited", "dc.universe",
            "audible", "blinkist", "storytel", "nextory", "bookbub",
            "turkcell.yayin", "dergi", "okuma", "roman", "hikaye", "gazete"
        ),
        Category.CAT_LIFESTYLE to listOf(
            "lifestyle", "horoscope", "astrology", "zodiac", "tarot",
            "meditation", "mindfulness", "relax", "breath", "calm",
            "yoga", "zen", "spiritual", "wellness", "gratitude", "journal",
            "insight.timer", "ten.percent", "breethe", "buddhify",
            "daily.calm", "headspace", "waking.up", "aware", "meditopia",
            "habit", "tracker", "morning", "routine", "ritual", "affirmation",
            "vision.board", "motivation", "self.care", "self.improvement",
            "life.coach", "therapist", "counselor", "yasamtarzi"
        ),
        Category.CAT_BUSINESS to listOf(
            "business", "crm", "erp", "invoice", "sales", "hr", "hiring",
            "payroll", "expense", "receipt", "timesheet", "attendance",
            "salesforce", "hubspot", "zendesk", "freshdesk", "intercom",
            "pipedrive", "zoho.crm", "monday.com", "basecamp", "wrike",
            "teamwork", "smartsheet", "podio", "servicenow", "workday",
            "concur", "expensify", "bamboohr", "adp", "gusto",
            "docusign", "hellosign", "pandadoc", "adobe.sign", "dropbox.sign",
            "bionluk", "youthall", "kariyer.net", "linkedin.jobs",
            "xing", "glassdoor", "indeed", "monster", "ziprecruiter",
            "asana", "jira", "clickup", "linear", "notion", "confluence",
            "kanban", "agile", "scrum", "sprint", "iş", "sirket", "proje"
        ),
        Category.CAT_DATING to listOf(
            "dating", "match", "tinder", "bumble", "hinge", "grindr",
            "okcupid", "love", "couple", "romance", "swipe", "profile",
            "blind.date", "zoosk", "eharmony", "coffee.meets.bagel",
            "meetyou", "meetme", "ngl", "her", "lovoo", "badoo",
            "mamba", "once", "happn", "pure", "feeld", "pair",
            "askeri", "flirt", "cift", "partner", "eslesme", "randevu"
        ),
        Category.CAT_ART to listOf(
            "art", "design", "draw", "paint", "sketch", "creative",
            "illustration", "graphic", "digital.art", "procreate", "figma",
            "canva", "adobe", "affinity", "photoshop", "illustrator",
            "lightroom", "premiere", "after.effects", "animate", "xd",
            "sketchbook", "autodesk", "medibangpaint", "ibis.paint",
            "pixel.art", "tayasui", "concepts", "mischief", "pixelmator",
            "gimp", "krita", "inkscape", "vector", "artboard", "artflow",
            "sanat", "cizim", "tasarim", "grafik", "renk", "boya"
        ),
        Category.CAT_BEAUTY to listOf(
            "beauty", "makeup", "cosmetic", "skincare", "nail", "hair",
            "salon", "lipstick", "foundation", "blush", "mascara",
            "brow", "glam", "sephora", "ulta", "youcam", "perfect365",
            "avon", "loreal", "maybelline", "mac.cosmetics", "nyx",
            "fenty", "kylie", "morphe", "too.faced", "tarte",
            "flormar", "farmasi", "elidor", "watsons",
            "guzellik", "makyaj", "cilt", "sac", "tirnak", "krem"
        ),
        Category.CAT_AUTO to listOf(
            "car", "auto", "vehicle", "drive", "parking", "garage",
            "fuel", "gas", "petrol", "oil", "tire", "engine", "mechanic",
            "tesla", "bmw.connected", "mercedes", "audi.connect", "volkswagen",
            "ford.pass", "toyota", "honda", "hyundai", "kia.connect",
            "renault", "peugeot", "togg", "gasbuddy", "opet", "shell",
            "bp.app", "sixt", "enterprise.rent", "hertz", "avis", "budget",
            "turo", "getaround", "zipcar", "otopark", "epark", "parkopedia",
            "araba", "otomobil", "lastik", "akaryakit", "servis", "sigorta"
        ),
        Category.CAT_HOUSE to listOf(
            "home", "house", "smart.home", "interior", "furniture", "decor",
            "remodel", "garden", "plant", "nest", "thermostat", "alarm",
            "security", "camera", "doorbell", "lock", "smart",
            "ikea", "houzz", "wayfair", "overstock", "homeaway",
            "philips.hue", "lifx", "nanoleaf", "govee", "wemo", "kasa",
            "ring", "arlo", "nest.cam", "wyze", "eufy", "blink",
            "roomba", "roborock", "ecovacs", "dyson",
            "ev", "bahce", "guvenlik", "kamera", "kapi", "akilli",
            "elektrik", "su", "dogalgaz", "fatura", "sayac"
        ),
        Category.CAT_WEATHER to listOf(
            "weather", "forecast", "temperature", "rain", "snow", "wind",
            "storm", "humidity", "pressure", "uv", "sun", "cloud",
            "lightning", "thunder", "accuweather", "meteo", "clima",
            "weather.channel", "weather.underground", "dark.sky", "carrot",
            "weatherpro", "windy", "ventusky", "meteoblue", "hava",
            "yagmur", "gunes", "bulut", "firtina", "hava.durumu",
            "mgm", "meteoroloji", "sicaklik", "nem", "ruzgar"
        ),
        Category.CAT_PARENTING to listOf(
            "parent", "baby", "child", "kid", "toddler", "pregnancy",
            "birth", "newborn", "school", "educational.game", "cartoon",
            "story.book", "bedtime", "babysit", "nanny", "daycare",
            "toca.boca", "youtube.kids", "pbs.kids", "nickelodeon",
            "cartoon.network", "disney.junior", "daniel.tiger",
            "cocomelon", "peppa.pig", "bluey", "paw.patrol",
            "khan.kids", "abc.mouse", "starfall", "monkey.junior",
            "cocuk", "bebek", "anne", "baba", "aile", "okul.oncesi",
            "hikaye", "masal", "oyun.cocuk", "egitici"
        ),
        Category.CAT_EVENTS to listOf(
            "event", "ticket", "concert", "festival", "show", "theater",
            "cinema", "movie", "music.event", "venue", "booking", "reservation",
            "ticketmaster", "stubhub", "eventbrite", "bandsintown", "songkick",
            "seatgeek", "viagogo", "axs", "dice.fm", "resident.advisor",
            "biletix", "biletinial", "iksv", "bilet", "konser", "tiyatro",
            "sinema", "festival", "etkinlik", "gece", "sahne", "kulup"
        ),
        Category.CAT_COMICS to listOf(
            "comic", "manga", "webtoon", "manhwa", "anime", "cartoon",
            "graphic.novel", "strip", "comixology", "marvel", "dc.comics",
            "image.comics", "dark.horse", "idw", "heavy.metal",
            "crunchyroll", "funimation", "vrv", "hidive", "animelab",
            "bilibili", "tapas", "webtoon", "lezhin", "pocket.comics",
            "izneo", "readera", "mangaplus", "shonen.jump",
            "anime", "manga", "cizgiroman", "animasyon", "kahraman"
        ),
        Category.CAT_PERSONALIZATION to listOf(
            "launcher", "wallpaper", "theme", "icon.pack", "widget",
            "ringtone", "lock.screen", "keyboard", "font", "customize",
            "tweak", "style", "nova", "action.launcher", "niagara",
            "lawnchair", "rootless", "poco.launcher", "oneui", "miui",
            "kustom", "kwgt", "klwp", "klch", "zooper", "conky",
            "zedge", "backgrounds", "amoled", "dark.wallpaper",
            "live.wallpaper", "video.wallpaper", "parallax",
            "good.lock", "one.ui", "samsung.theme", "miui.theme",
            "substratum", "pixelify", "magisk.module", "xposed",
            "tasker", "shortcut.maker", "back.button",
            "tema", "duvar.kagidi", "ikon", "kilit.ekrani", "zil.sesi"
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

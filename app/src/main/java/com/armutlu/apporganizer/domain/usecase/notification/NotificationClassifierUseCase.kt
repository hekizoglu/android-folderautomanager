package com.armutlu.apporganizer.domain.usecase.notification

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/**
 * Device-local notification classifier. No network or heavy ML is required.
 * Classification is intentionally ordered: security/direct communication first,
 * then transactional/contextual categories, and low-value content last.
 */
class NotificationClassifierUseCase @Inject constructor() {

    fun classify(
        key: String,
        packageName: String,
        title: String,
        text: String,
        timestamp: Long,
        systemPriority: Int = 0,
    ): SmartNotification {
        val normalizedPackage = normalizePackage(packageName)
        val normalizedContent = normalizeText("$title $text")
        val category = detectCategory(normalizedPackage, normalizedContent)
        val sensitive = category == NotificationCategory.FINANCE ||
            normalizedContent.containsAny(SENSITIVE_MATCHERS)
        val score = score(category, normalizedPackage, normalizedContent, systemPriority)

        return SmartNotification(
            key = key,
            packageName = packageName,
            title = title.trim(),
            text = text.trim(),
            category = category,
            importanceScore = score,
            timestamp = timestamp,
            isSensitive = sensitive,
            shouldSuppress = category.suppressible && score < SUPPRESSION_SCORE_LIMIT,
        )
    }

    private fun detectCategory(packageName: String, content: String): NotificationCategory = when {
        // Security/OTP always wins over generic package classification.
        content.containsAny(AUTH_CODE_MATCHERS) -> NotificationCategory.FINANCE
        content.containsAny(MISSED_CALL_MATCHERS) || matchesPackage(packageName, PHONE_PACKAGES) ->
            NotificationCategory.MISSED_CALL
        content.containsAny(FAMILY_MATCHERS) || matchesPackage(packageName, FAMILY_PACKAGES) ->
            NotificationCategory.FAMILY
        content.containsAny(MARKET_MATCHERS) || matchesPackage(packageName, MARKET_PACKAGES) ->
            NotificationCategory.MARKET
        content.containsAny(PROMOTION_MATCHERS) -> NotificationCategory.PROMOTION
        content.containsAny(DELIVERY_MATCHERS) -> NotificationCategory.DELIVERY
        content.containsAny(FINANCE_MATCHERS) -> NotificationCategory.FINANCE
        // Messaging package wins over generic words such as "meeting" in a message.
        matchesPackage(packageName, MESSAGING_PACKAGES) -> NotificationCategory.MESSAGING
        content.containsAny(CALENDAR_MATCHERS) || matchesPackage(packageName, CALENDAR_PACKAGES) ->
            NotificationCategory.CALENDAR
        content.containsAny(REMINDER_MATCHERS) -> NotificationCategory.REMINDER
        content.containsAny(MESSAGING_MATCHERS) -> NotificationCategory.MESSAGING
        content.containsAny(UPDATE_MATCHERS) || matchesPackage(packageName, UPDATE_PACKAGES) ->
            NotificationCategory.UPDATE
        content.containsAny(NEWS_MATCHERS) || matchesPackage(packageName, NEWS_PACKAGES) ->
            NotificationCategory.NEWS
        content.containsAny(MEDIA_MATCHERS) || matchesPackage(packageName, MEDIA_PACKAGES) ->
            NotificationCategory.MEDIA
        matchesPackage(packageName, DELIVERY_PACKAGES) -> NotificationCategory.DELIVERY
        matchesPackage(packageName, FINANCE_PACKAGES) -> NotificationCategory.FINANCE
        matchesPackage(packageName, SOCIAL_PACKAGES) || content.containsAny(SOCIAL_MATCHERS) ->
            NotificationCategory.SOCIAL
        matchesPackage(packageName, SYSTEM_PACKAGES) || content.containsAny(SYSTEM_MATCHERS) ->
            NotificationCategory.SYSTEM
        else -> NotificationCategory.OTHER
    }

    private fun score(
        category: NotificationCategory,
        packageName: String,
        content: String,
        systemPriority: Int,
    ): Int {
        val hasAuthentication = content.containsAny(AUTH_CODE_MATCHERS)
        val hasSecurity = content.containsAny(SECURITY_MATCHERS)
        val hasUrgency = content.containsAny(URGENT_MATCHERS)
        val hasLowValue = content.containsAny(LOW_VALUE_MATCHERS)
        val hasTimeSensitivity = content.containsAny(TIME_SENSITIVE_MATCHERS)

        // Düşük değerli kategoriler pazarlama diliyle ("Acil! Kritik! Güvenlik fırsatı!")
        // şişirilebilir; urgency/security bonusları bu kategorilerde uygulanmaz ki
        // promotion/NEWS içerikleri suppression eşiğinden kaçamasın
        // (NotificationPriorityPolicyTest ile doğrulanır).
        val lowValueCategory = category == NotificationCategory.PROMOTION ||
            category == NotificationCategory.NEWS ||
            category == NotificationCategory.MEDIA ||
            category == NotificationCategory.UPDATE

        var result = category.defaultImportance
        result += systemPriority.coerceIn(MIN_SYSTEM_PRIORITY, MAX_SYSTEM_PRIORITY) * PRIORITY_STEP
        if (hasUrgency && !lowValueCategory) result += URGENCY_BONUS
        if (hasSecurity && !lowValueCategory) result += SECURITY_BONUS
        if (hasTimeSensitivity && !lowValueCategory) result += TIME_SENSITIVITY_BONUS
        if (matchesPackage(packageName, MESSAGING_PACKAGES) && category == NotificationCategory.MESSAGING) {
            result += MESSAGING_PACKAGE_BONUS
        }
        if (hasLowValue) result -= LOW_VALUE_PENALTY

        if (lowValueCategory) {
            result -= LOW_VALUE_CATEGORY_PENALTY
        }
        if (category == NotificationCategory.PROMOTION) {
            result = result.coerceAtMost(SUPPRESSION_SCORE_LIMIT - 1)
        }
        if (hasAuthentication || (category == NotificationCategory.FINANCE && hasSecurity)) {
            result = result.coerceAtLeast(CRITICAL_SECURITY_MIN_SCORE)
        }
        return result.coerceIn(MIN_SCORE, MAX_SCORE)
    }

    private fun normalizePackage(value: String): String = value.lowercase(Locale.ROOT).trim()
    private fun normalizeText(value: String): String = normalizeLiteral(value)
    private fun String.containsAny(matchers: List<Regex>): Boolean =
        matchers.any { matcher -> matcher.containsMatchIn(this) }

    private fun matchesPackage(packageName: String, rules: PackageRules): Boolean {
        if (rules.prefixes.any { packageName == it || packageName.startsWith("$it.") }) return true
        val segments = packageName.split(PACKAGE_SEPARATOR).filter { it.isNotBlank() }.toSet()
        return rules.segments.any(segments::contains)
    }

    private data class PackageRules(
        val prefixes: Set<String> = emptySet(),
        val segments: Set<String> = emptySet(),
    )

    private companion object {
        const val MIN_SCORE = 0
        const val MAX_SCORE = 100
        const val MIN_SYSTEM_PRIORITY = -2
        const val MAX_SYSTEM_PRIORITY = 2
        const val PRIORITY_STEP = 6
        const val URGENCY_BONUS = 18
        const val SECURITY_BONUS = 12
        const val TIME_SENSITIVITY_BONUS = 12
        const val MESSAGING_PACKAGE_BONUS = 5
        const val LOW_VALUE_PENALTY = 8
        const val LOW_VALUE_CATEGORY_PENALTY = 8
        const val SUPPRESSION_SCORE_LIMIT = 40
        const val CRITICAL_SECURITY_MIN_SCORE = 80

        val COMBINING_MARKS = Regex("\\p{Mn}+")
        val WHITESPACE = Regex("\\s+")
        val PACKAGE_SEPARATOR = Regex("[._-]+")

        fun normalizeLiteral(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
            .replace('ç', 'c').replace('ö', 'o').replace('ü', 'u')
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE, " ")
            .trim()

        fun matchers(vararg values: String): List<Regex> = values
            .map(::normalizeLiteral)
            .distinct()
            .map { term -> Regex("(?<![\\p{L}\\p{N}])${Regex.escape(term)}(?![\\p{L}\\p{N}])") }

        val FINANCE_PACKAGES = PackageRules(
            prefixes = setOf(
                "com.akbank.android.apps.akbank_direkt", "com.garanti.cepsubesi", "com.ykb.android",
                "com.pozitron.iscep", "com.ziraat.ziraatmobil", "com.vakifbank.mobile",
                "com.halkbank.mobile", "com.denizbank.mobildeniz", "com.qnbfinansbank.mobile",
                "com.enpara", "com.papara",
            ),
            segments = setOf("akbank", "garanti", "yapikredi", "ykb", "isbank", "iscep", "ziraat", "vakifbank", "halkbank", "denizbank", "qnb", "enpara", "papara", "paycell"),
        )
        val MARKET_PACKAGES = PackageRules(
            segments = setOf("tradingview", "investing", "midas", "foreks", "bloomberg", "yahoo", "stocks"),
        )
        val DELIVERY_PACKAGES = PackageRules(
            prefixes = setOf("com.trendyol", "com.pozitron.hepsiburada", "com.amazon.mshop", "com.getir", "com.yemeksepeti", "com.migros"),
            segments = setOf("trendyol", "hepsiburada", "amazon", "getir", "yemeksepeti", "migros", "ptt", "aras", "yurtici", "mng", "surat"),
        )
        val MESSAGING_PACKAGES = PackageRules(
            prefixes = setOf("com.whatsapp", "org.telegram", "org.thoughtcrime.securesms", "com.facebook.orca", "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms", "com.discord", "com.slack", "com.microsoft.teams"),
        )
        val SOCIAL_PACKAGES = PackageRules(
            prefixes = setOf(
                "com.instagram.android",
                "com.facebook.katana",
                "com.twitter.android",
                "com.zhiliaoapp.musically",
                "com.snapchat.android",
                "com.linkedin.android",
            ),
        )
        val PHONE_PACKAGES = PackageRules(
            prefixes = setOf("com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer", "com.android.phone"),
            segments = setOf("dialer", "phone", "telephony"),
        )
        val CALENDAR_PACKAGES = PackageRules(
            prefixes = setOf("com.google.android.calendar", "com.android.calendar", "com.samsung.android.calendar"),
            segments = setOf("calendar", "calendarprovider"),
        )
        val FAMILY_PACKAGES = PackageRules(
            segments = setOf("familylink", "family", "parental", "kids", "kidspace"),
        )
        val UPDATE_PACKAGES = PackageRules(
            segments = setOf("updater", "softwareupdate", "systemupdate"),
        )

        // NOT: jenerik "news" segment'i kaldırıldı — "com.example.news" gibi paketlerde
        // içerik kanıtı olmadan NEWS üretiyordu (NotificationPriorityPolicyTest OTHER bekler).
        // Bilinen haber uygulamaları segment/prefix olarak korunur; içerik matcher'ları
        // ("haber", "son dakika", "breaking news"...) zaten NEWS'i yakalar.
        val NEWS_PACKAGES = PackageRules(
            segments = setOf("flipboard", "feedly", "googlequicksearchbox"),
        )
        val MEDIA_PACKAGES = PackageRules(
            segments = setOf("youtube", "music", "spotify", "netflix", "primevideo", "podcast"),
        )
        val SYSTEM_PACKAGES = PackageRules(
            prefixes = setOf(
                "android",
                "com.android.systemui",
                "com.android.settings",
                "com.google.android.permissioncontroller",
                "com.miui.securitycenter",
                "com.samsung.android.securitylogagent",
            ),
        )

        val AUTH_CODE_MATCHERS =
            matchers("dogrulama kodu", "giris kodu", "guvenlik kodu", "tek kullanimlik kod", "verification code", "login code", "security code", "one time code", "one-time code", "one time password", "one-time password", "otp")
        val MISSED_CALL_MATCHERS = matchers("cevapsiz arama", "cevapsiz cagri", "cevapsiz", "missed call", "missed calls", "arama yapti")
        val FAMILY_MATCHERS =
            matchers("yeni uygulama yuklendi", "uygulama yukledi", "ekran suresi", "screen time", "child installed", "parental control", "family link", "cocuk", "ebeveyn")

        // NOT: "yuzde" tek başına MARKET sayılmaz — "yüzde 50 indirim" gibi promosyon
        // içeriklerini yanlışlıkla MARKET'e çekiyordu (NotificationClassifierUseCaseTest).
        // Gerçek piyasa bildirimleri "artti/yukseldi/%," gibi diğer matcher'larla yakalanır.
        val MARKET_MATCHERS =
            matchers("hisse", "hissesi", "hisse senedi", "borsa", "bist", "bist 30", "bist 100", "nasdaq", "s&p 500", "dow jones", "sp 500", "artti", "yukseldi", "dustu", "geriledi", "%,", "stock", "stocks", "shares", "market alert", "price alert", "altin", "dolar", "euro", "exchange rate", "index")
        val FINANCE_MATCHERS =
            matchers("bakiye", "hesap hareketi", "kartiniz", "harcama", "odeme", "transfer", "havale", "eft", "yatirim", "para cekme", "fatura", "balance", "account activity", "card transaction", "transaction", "payment", "bank transfer", "wire transfer", "withdrawal", "invoice")
        val DELIVERY_MATCHERS =
            matchers("kargo", "teslimat", "siparisiniz", "siparis", "kurye", "yola cikti", "dagitima cikti", "teslim edildi", "paketiniz", "gonderiniz", "shipped", "shipment", "out for delivery", "delivered", "your order", "order confirmed", "courier", "your package", "tracking number")
        val CALENDAR_MATCHERS =
            matchers("takvim", "calendar event", "etkinlik", "toplanti", "meeting", "randevu", "appointment", "mesai", "vardiya", "etkinlik basliyor", "starts in", "due today")
        val REMINDER_MATCHERS = matchers("hatirlatici", "alarm", "son tarih", "reminder", "overdue")
        val PROMOTION_MATCHERS =
            matchers("indirim", "kampanya", "firsat", "kupon", "sepette", "reklam", "sana ozel", "hemen al", "stoklarla sinirli", "discount", "sale", "coupon", "offer", "deal", "special price", "limited stock", "buy now", "save now")
        val MESSAGING_MATCHERS =
            matchers("yeni mesaj", "mesaj gonderdi", "cevapladi", "sana yazdi", "goruntulu arama", "new message", "sent you a message", "replied to you", "voice call", "video call")
        val SOCIAL_MATCHERS =
            matchers("begendi", "takip etmeye basladi", "yorum yapti", "hikaye", "reels", "gonderini", "liked your", "started following", "commented on", "new follower", "mentioned you", "new story")
        val NEWS_MATCHERS = matchers("haber", "son dakika", "breaking news", "news alert", "breaking", "gundem", "news")
        val MEDIA_MATCHERS =
            matchers("yeni video", "video onerisi", "seni bekliyor", "yeni sarki", "new video", "recommended video", "now playing", "new song", "playlist")
        val UPDATE_MATCHERS =
            matchers(
                "uygulama guncellemesi",
                "uygulama guncellendi",
                "guncelleme mevcut",
                "app update",
                "update available",
                "yazilim guncellemesi",
                "software update",
                "system update",
            )
        val SYSTEM_MATCHERS =
            matchers(
                "sistem guncellemesi",
                "pil az",
                "depolama alani",
                "izin gerekli",
                "battery low",
                "storage space",
                "permission required",
            )
        val URGENT_MATCHERS =
            matchers("acil", "hemen", "simdi", "kritik", "gecikmis", "urgent", "immediately", "now", "critical", "overdue", "action required")
        val TIME_SENSITIVE_MATCHERS =
            matchers("10 dakika", "15 dakika", "30 dakika", "5 dakika", "in 5 minutes", "in 10 minutes", "in 15 minutes", "in 30 minutes", "starts now", "simdi basliyor", "az sonra")
        val SECURITY_MATCHERS =
            matchers("guvenlik", "supheli", "giris", "sifre", "onay", "dogrulama", "security", "suspicious", "login", "password", "approve", "verification", "authentication", "otp")
        val SENSITIVE_MATCHERS =
            matchers("bakiye", "hesap", "kart", "sifre", "otp", "tutar", "iban", "tl", "balance", "account", "card", "password", "amount", "verification code", "login code", "security code")
        val LOW_VALUE_MATCHERS =
            matchers("bulten", "onerilen", "sizin icin", "trend", "kesfet", "newsletter", "recommended", "for you", "trending", "discover")
    }
}

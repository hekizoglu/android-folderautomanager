package com.armutlu.apporganizer.domain.usecase.notification

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/**
 * Ağ veya ağır ML modeli kullanmadan bildirimi cihaz üzerinde sınıflandırır ve puanlar.
 * İçerik kelime/ifade sınırıyla, paket adı ise tam prefix veya paket segmentiyle eşleşir.
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
        val score = score(
            category = category,
            packageName = normalizedPackage,
            content = normalizedContent,
            systemPriority = systemPriority,
        )

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
        // OTP/giriş kodu her uygulamada güvenlik-finans olayıdır.
        content.containsAny(AUTH_CODE_MATCHERS) -> NotificationCategory.FINANCE
        // Açık kampanya dili paket kategorisinden üstündür.
        content.containsAny(PROMOTION_MATCHERS) -> NotificationCategory.PROMOTION
        content.containsAny(FINANCE_MATCHERS) -> NotificationCategory.FINANCE
        content.containsAny(DELIVERY_MATCHERS) -> NotificationCategory.DELIVERY
        // Mesaj içindeki “toplantı tamamlandı” reminder sayılmaz.
        matchesPackage(packageName, MESSAGING_PACKAGES) -> NotificationCategory.MESSAGING
        content.containsAny(REMINDER_MATCHERS) -> NotificationCategory.REMINDER
        content.containsAny(MESSAGING_MATCHERS) -> NotificationCategory.MESSAGING
        matchesPackage(packageName, FINANCE_PACKAGES) -> NotificationCategory.FINANCE
        matchesPackage(packageName, DELIVERY_PACKAGES) -> NotificationCategory.DELIVERY
        matchesPackage(packageName, SOCIAL_PACKAGES) || content.containsAny(SOCIAL_MATCHERS) ->
            NotificationCategory.SOCIAL
        matchesPackage(packageName, SYSTEM_PACKAGES) || content.containsAny(SYSTEM_MATCHERS) ->
            NotificationCategory.SYSTEM
        else -> NotificationCategory.OTHER
    }

    /**
     * Skor politikası:
     * kategori tabanı + Android priority + aciliyet + güvenlik + mesajlaşma güveni - düşük değer.
     * Saf promosyonlar, priority/“acil” kelimeleriyle görünür hâle gelemesin diye 39'da tavanlanır.
     * OTP ve finansal güvenlik olayları düşük Android priority yüzünden 80'in altına düşmez.
     */
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

        var result = category.defaultImportance
        result += systemPriority.coerceIn(MIN_SYSTEM_PRIORITY, MAX_SYSTEM_PRIORITY) * PRIORITY_STEP
        if (hasUrgency) result += URGENCY_BONUS
        if (hasSecurity) result += SECURITY_BONUS
        if (matchesPackage(packageName, MESSAGING_PACKAGES) && category == NotificationCategory.MESSAGING) {
            result += MESSAGING_PACKAGE_BONUS
        }
        if (hasLowValue) result -= LOW_VALUE_PENALTY

        if (category == NotificationCategory.PROMOTION) {
            result -= PROMOTION_PENALTY
            return result.coerceIn(MIN_SCORE, PROMOTION_MAX_SCORE)
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
        const val MESSAGING_PACKAGE_BONUS = 5
        const val LOW_VALUE_PENALTY = 8
        const val PROMOTION_PENALTY = 18
        const val SUPPRESSION_SCORE_LIMIT = 40
        const val PROMOTION_MAX_SCORE = SUPPRESSION_SCORE_LIMIT - 1
        const val CRITICAL_SECURITY_MIN_SCORE = 80

        val COMBINING_MARKS = Regex("\\p{Mn}+")
        val WHITESPACE = Regex("\\s+")
        val PACKAGE_SEPARATOR = Regex("[._-]+")

        fun normalizeLiteral(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .replace('ı', 'i')
            .replace('ş', 's')
            .replace('ğ', 'g')
            .replace('ç', 'c')
            .replace('ö', 'o')
            .replace('ü', 'u')
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE, " ")
            .trim()

        fun matchers(vararg values: String): List<Regex> = values
            .map(::normalizeLiteral)
            .distinct()
            .map { term -> Regex("(?<![\\p{L}\\p{N}])${Regex.escape(term)}(?![\\p{L}\\p{N}])") }

        val FINANCE_PACKAGES = PackageRules(
            prefixes = setOf(
                "com.akbank.android.apps.akbank_direkt", "com.garanti.cepsubesi",
                "com.ykb.android", "com.pozitron.iscep", "com.ziraat.ziraatmobil",
                "com.vakifbank.mobile", "com.halkbank.mobile", "com.denizbank.mobildeniz",
                "com.qnbfinansbank.mobile", "com.enpara", "com.papara",
            ),
            segments = setOf(
                "akbank", "garanti", "yapikredi", "ykb", "isbank", "iscep", "ziraat",
                "vakifbank", "halkbank", "denizbank", "qnb", "enpara", "papara", "paycell",
            ),
        )
        val DELIVERY_PACKAGES = PackageRules(
            prefixes = setOf(
                "com.trendyol", "com.pozitron.hepsiburada", "com.amazon.mshop", "com.getir",
                "com.yemeksepeti", "com.migros",
            ),
            segments = setOf(
                "trendyol", "hepsiburada", "amazon", "getir", "yemeksepeti", "migros",
                "ptt", "aras", "yurtici", "mng", "surat",
            ),
        )
        val MESSAGING_PACKAGES = PackageRules(
            prefixes = setOf(
                "com.whatsapp", "org.telegram", "org.thoughtcrime.securesms", "com.facebook.orca",
                "com.google.android.apps.messaging", "com.samsung.android.messaging",
                "com.android.mms", "com.discord", "com.slack", "com.microsoft.teams",
            ),
        )
        val SOCIAL_PACKAGES = PackageRules(
            prefixes = setOf(
                "com.instagram.android", "com.facebook.katana", "com.twitter.android",
                "com.zhiliaoapp.musically", "com.snapchat.android", "com.linkedin.android",
            ),
        )
        val SYSTEM_PACKAGES = PackageRules(
            prefixes = setOf(
                "android", "com.android.systemui", "com.android.settings",
                "com.google.android.permissioncontroller", "com.miui.securitycenter",
                "com.samsung.android.securitylogagent",
            ),
        )

        val AUTH_CODE_MATCHERS = matchers(
            "dogrulama kodu", "giris kodu", "guvenlik kodu", "tek kullanimlik kod",
            "verification code", "login code", "security code", "one time code",
            "one-time code", "one time password", "one-time password", "otp",
        )
        val FINANCE_MATCHERS = matchers(
            "bakiye", "hesap hareketi", "kartiniz", "harcama", "odeme", "transfer",
            "havale", "eft", "yatirim", "islem", "para cekme", "fatura", "balance",
            "account activity", "card transaction", "transaction", "payment", "bank transfer",
            "wire transfer", "withdrawal", "invoice",
        )
        val DELIVERY_MATCHERS = matchers(
            "kargo", "teslimat", "siparisiniz", "siparis", "kurye", "yola cikti",
            "dagitima cikti", "teslim edildi", "paketiniz", "gonderiniz", "shipped",
            "shipment", "out for delivery", "delivered", "your order", "order confirmed",
            "courier", "your package", "tracking number",
        )
        val REMINDER_MATCHERS = matchers(
            "hatirlatici", "alarm", "takvim", "toplanti", "randevu", "etkinlik basliyor",
            "son tarih", "reminder", "meeting", "appointment", "starts in", "due today",
            "calendar event",
        )
        val PROMOTION_MATCHERS = matchers(
            "indirim", "kampanya", "firsat", "kupon", "sepette", "reklam", "sana ozel",
            "hemen al", "stoklarla sinirli", "discount", "sale", "coupon", "offer", "deal",
            "special price", "limited stock", "buy now", "save now",
        )
        val MESSAGING_MATCHERS = matchers(
            "yeni mesaj", "mesaj gonderdi", "cevapladi", "sana yazdi", "goruntulu arama",
            "new message", "sent you a message", "replied to you", "missed call", "voice call",
            "video call",
        )
        val SOCIAL_MATCHERS = matchers(
            "begendi", "takip etmeye basladi", "yorum yapti", "hikaye", "reels", "gonderini",
            "liked your", "started following", "commented on", "new follower", "mentioned you",
            "new story",
        )
        val SYSTEM_MATCHERS = matchers(
            "sistem guncellemesi", "yazilim guncellemesi", "pil az", "depolama alani",
            "izin gerekli", "software update", "system update", "battery low", "storage space",
            "permission required", "app update available",
        )
        val URGENT_MATCHERS = matchers(
            "acil", "hemen", "simdi", "kritik", "son dakika", "gecikmis", "urgent",
            "immediately", "now", "critical", "overdue", "action required",
        )
        val SECURITY_MATCHERS = matchers(
            "guvenlik", "supheli", "giris", "sifre", "onay", "dogrulama", "security",
            "suspicious", "login", "password", "approve", "verification", "authentication", "otp",
        )
        val SENSITIVE_MATCHERS = matchers(
            "bakiye", "hesap", "kart", "sifre", "otp", "tutar", "iban", "tl", "balance",
            "account", "card", "password", "amount", "verification code", "login code",
            "security code",
        )
        val LOW_VALUE_MATCHERS = matchers(
            "bulten", "onerilen", "sizin icin", "trend", "kesfet", "newsletter",
            "recommended", "for you", "trending", "discover",
        )
    }
}

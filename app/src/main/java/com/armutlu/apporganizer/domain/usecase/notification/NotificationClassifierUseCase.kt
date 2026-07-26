package com.armutlu.apporganizer.domain.usecase.notification

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/**
 * Ağ veya ağır ML modeli kullanmadan bildirimi cihaz üzerinde sınıflandırır.
 *
 * İçerik kuralları kelime/ifade sınırıyla eşleşir; paket kuralları yalnız tam prefix veya
 * paket segmenti eşleşmesini kabul eder. Böylece `sale`/`wholesale` ya da
 * `com.example.whatsappclone`/`com.whatsapp` gibi false-positive'ler engellenir.
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
        val suppress = category.suppressible && score < SUPPRESSION_SCORE_LIMIT

        return SmartNotification(
            key = key,
            packageName = packageName,
            title = title.trim(),
            text = text.trim(),
            category = category,
            importanceScore = score,
            timestamp = timestamp,
            isSensitive = sensitive,
            shouldSuppress = suppress,
        )
    }

    private fun detectCategory(packageName: String, content: String): NotificationCategory {
        return when {
            // Güvenlik/OTP sinyali uygulama türünden üstündür. Örn. Instagram giriş kodu veya
            // alışveriş uygulamasındaki ödeme doğrulama kodu SOCIAL/DELIVERY sayılmaz.
            content.containsAny(AUTH_CODE_MATCHERS) -> NotificationCategory.FINANCE
            content.containsAny(FINANCE_MATCHERS) -> NotificationCategory.FINANCE
            content.containsAny(DELIVERY_MATCHERS) -> NotificationCategory.DELIVERY
            content.containsAny(PROMOTION_MATCHERS) -> NotificationCategory.PROMOTION

            // Mesaj içindeki “toplantı tamamlandı” gibi sıradan konuşmalar reminder olmamalı.
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
    }

    private fun score(
        category: NotificationCategory,
        packageName: String,
        content: String,
        systemPriority: Int,
    ): Int {
        var result = category.defaultImportance
        result += systemPriority.coerceIn(-2, 2) * 6

        if (content.containsAny(URGENT_MATCHERS)) result += 18
        if (content.containsAny(SECURITY_MATCHERS)) result += 12
        if (matchesPackage(packageName, MESSAGING_PACKAGES) && category == NotificationCategory.MESSAGING) {
            result += 5
        }
        if (content.containsAny(PROMOTION_MATCHERS)) result -= 18
        if (content.containsAny(LOW_VALUE_MATCHERS)) result -= 8

        return result.coerceIn(0, 100)
    }

    private fun normalizePackage(value: String): String =
        value.lowercase(Locale.ROOT).trim()

    private fun normalizeText(value: String): String = normalizeLiteral(value)

    private fun String.containsAny(matchers: List<Regex>): Boolean =
        matchers.any { matcher -> matcher.containsMatchIn(this) }

    private fun matchesPackage(packageName: String, rules: PackageRules): Boolean {
        if (rules.prefixes.any { prefix ->
                packageName == prefix || packageName.startsWith("$prefix.")
            }
        ) {
            return true
        }
        val segments = packageName.split(PACKAGE_SEPARATOR).filter { it.isNotBlank() }.toSet()
        return rules.segments.any(segments::contains)
    }

    private data class PackageRules(
        val prefixes: Set<String> = emptySet(),
        val segments: Set<String> = emptySet(),
    )

    private companion object {
        const val SUPPRESSION_SCORE_LIMIT = 40
        val COMBINING_MARKS = Regex("\\p{Mn}+")
        val WHITESPACE = Regex("\\s+")
        val PACKAGE_SEPARATOR = Regex("[._-]+")

        fun normalizeLiteral(value: String): String {
            val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
            return decomposed
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
        }

        fun matchers(vararg values: String): List<Regex> = values
            .map(::normalizeLiteral)
            .distinct()
            .map { term ->
                Regex("(?<![\\p{L}\\p{N}])${Regex.escape(term)}(?![\\p{L}\\p{N}])")
            }

        val FINANCE_PACKAGES = PackageRules(
            prefixes = setOf(
                "com.akbank.android.apps.akbank_direkt",
                "com.garanti.cepsubesi",
                "com.ykb.android",
                "com.pozitron.iscep",
                "com.ziraat.ziraatmobil",
                "com.vakifbank.mobile",
                "com.halkbank.mobile",
                "com.denizbank.mobildeniz",
                "com.qnbfinansbank.mobile",
                "com.enpara",
                "com.papara",
            ),
            segments = setOf(
                "akbank", "garanti", "yapikredi", "ykb", "isbank", "iscep", "ziraat",
                "vakifbank", "halkbank", "denizbank", "qnb", "enpara", "papara", "paycell",
            ),
        )
        val DELIVERY_PACKAGES = PackageRules(
            prefixes = setOf(
                "com.trendyol",
                "com.pozitron.hepsiburada",
                "com.amazon.mshop",
                "com.getir",
                "com.yemeksepeti",
                "com.migros",
            ),
            segments = setOf(
                "trendyol", "hepsiburada", "amazon", "getir", "yemeksepeti", "migros",
                "ptt", "aras", "yurtici", "mng", "surat",
            ),
        )
        val MESSAGING_PACKAGES = PackageRules(
            prefixes = setOf(
                "com.whatsapp",
                "org.telegram",
                "org.thoughtcrime.securesms",
                "com.facebook.orca",
                "com.google.android.apps.messaging",
                "com.samsung.android.messaging",
                "com.android.mms",
                "com.discord",
                "com.slack",
                "com.microsoft.teams",
            ),
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

        val AUTH_CODE_MATCHERS = matchers(
            "dogrulama kodu", "giris kodu", "guvenlik kodu", "tek kullanimlik kod",
            "verification code", "login code", "security code", "one time code",
            "one-time code", "one time password", "one-time password", "otp",
        )
        val FINANCE_MATCHERS = matchers(
            "bakiye", "hesap hareketi", "kartiniz", "harcama", "odeme", "transfer",
            "havale", "eft", "yatirim", "islem", "para cekme", "fatura",
            "balance", "account activity", "card transaction", "transaction", "payment",
            "bank transfer", "wire transfer", "withdrawal", "invoice",
        )
        val DELIVERY_MATCHERS = matchers(
            "kargo", "teslimat", "siparisiniz", "siparis", "kurye", "yola cikti",
            "dagitima cikti", "teslim edildi", "paketiniz", "gonderiniz",
            "shipped", "shipment", "out for delivery", "delivered", "your order",
            "order confirmed", "courier", "your package", "tracking number",
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
            "acil", "hemen", "simdi", "kritik", "son dakika", "gecikmis",
            "urgent", "immediately", "now", "critical", "overdue", "action required",
        )
        val SECURITY_MATCHERS = matchers(
            "guvenlik", "supheli", "giris", "sifre", "onay", "dogrulama",
            "security", "suspicious", "login", "password", "approve", "verification",
            "authentication", "otp",
        )
        val SENSITIVE_MATCHERS = matchers(
            "bakiye", "hesap", "kart", "sifre", "otp", "tutar", "iban", "tl",
            "balance", "account", "card", "password", "amount", "verification code",
            "login code", "security code",
        )
        val LOW_VALUE_MATCHERS = matchers(
            "bulten", "onerilen", "sizin icin", "trend", "kesfet",
            "newsletter", "recommended", "for you", "trending", "discover",
        )
    }
}
